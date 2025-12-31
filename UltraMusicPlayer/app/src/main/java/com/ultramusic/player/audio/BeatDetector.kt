package com.ultramusic.player.audio

import android.content.Context
import android.media.MediaCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Beat Detector
 *
 * Detects beats and rhythm in audio files using energy-based detection.
 * Suitable for sound battle scenarios where beat-matching is important.
 */
@Singleton
class BeatDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BeatDetector"
        private const val DEFAULT_SENSITIVITY = 1.5f  // Beat detection threshold multiplier
        private const val MIN_BEAT_INTERVAL_MS = 200  // Minimum 300 BPM
        private const val ENERGY_WINDOW_MS = 50       // Energy calculation window
    }

    // Cache for detected beats
    private val beatCache = mutableMapOf<Long, List<BeatMarker>>()

    private val _isDetecting = MutableStateFlow(false)
    val isDetecting: StateFlow<Boolean> = _isDetecting.asStateFlow()

    private val _detectionProgress = MutableStateFlow(0f)
    val detectionProgress: StateFlow<Float> = _detectionProgress.asStateFlow()

    private val _estimatedBpm = MutableStateFlow(0f)
    val estimatedBpm: StateFlow<Float> = _estimatedBpm.asStateFlow()

    /**
     * Detect beats in an audio file
     *
     * @param songId Unique ID for caching
     * @param uri Audio file URI
     * @param sensitivity Detection sensitivity (1.0 = normal, higher = fewer beats)
     * @return List of beat markers with timestamps
     */
    suspend fun detectBeats(
        songId: Long,
        uri: Uri,
        sensitivity: Float = DEFAULT_SENSITIVITY
    ): List<BeatMarker> = withContext(Dispatchers.IO) {
        // Check cache first
        beatCache[songId]?.let { cached ->
            return@withContext cached
        }

        _isDetecting.value = true
        _detectionProgress.value = 0f

        try {
            val beats = analyzeAudio(uri, sensitivity)
            beatCache[songId] = beats

            // Estimate BPM from beat intervals
            if (beats.size >= 2) {
                val intervals = beats.zipWithNext { a, b -> b.timestampMs - a.timestampMs }
                val avgInterval = intervals.filter { it in 200L..2000L }.average()
                if (avgInterval > 0) {
                    _estimatedBpm.value = (60000.0 / avgInterval).toFloat()
                }
            }

            beats
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting beats", e)
            emptyList()
        } finally {
            _isDetecting.value = false
            _detectionProgress.value = 1f
        }
    }

    /**
     * Get cached beats if available
     */
    fun getCachedBeats(songId: Long): List<BeatMarker>? {
        return beatCache[songId]
    }

    /**
     * Clear beat cache
     */
    fun clearCache() {
        beatCache.clear()
        _estimatedBpm.value = 0f
    }

    /**
     * Clear specific song from cache
     */
    fun clearFromCache(songId: Long) {
        beatCache.remove(songId)
    }

    /**
     * Quick BPM estimation without full beat detection
     */
    suspend fun estimateBpm(uri: Uri): Float = withContext(Dispatchers.IO) {
        try {
            val beats = analyzeAudio(uri, 1.3f, quickMode = true)
            if (beats.size >= 4) {
                val intervals = beats.zipWithNext { a, b -> b.timestampMs - a.timestampMs }
                val avgInterval = intervals.filter { it in 200L..2000L }.average()
                if (avgInterval > 0) {
                    return@withContext (60000.0 / avgInterval).toFloat()
                }
            }
            0f
        } catch (e: Exception) {
            Log.e(TAG, "Error estimating BPM", e)
            0f
        }
    }

    private suspend fun analyzeAudio(
        uri: Uri,
        sensitivity: Float,
        quickMode: Boolean = false
    ): List<BeatMarker> = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)

            // Find audio track
            var audioTrackIndex = -1
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    break
                }
            }

            if (audioTrackIndex == -1) {
                Log.e(TAG, "No audio track found")
                return@withContext emptyList()
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val duration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // ms
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // Create decoder
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val beats = mutableListOf<BeatMarker>()
            val energyHistory = mutableListOf<Float>()
            val energyWindowSize = (sampleRate * ENERGY_WINDOW_MS / 1000) / channels

            val inputBufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false
            var currentTimeMs = 0L
            var lastBeatTime = -MIN_BEAT_INTERVAL_MS.toLong()

            // For quick mode, analyze only first 30 seconds
            val maxAnalyzeTimeMs = if (quickMode) 30000L else Long.MAX_VALUE

            var sampleBuffer = mutableListOf<Float>()

            while (!sawOutputEOS && currentTimeMs < maxAnalyzeTimeMs) {
                // Feed input
                if (!sawInputEOS) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)

                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(
                                inputBufferIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            sawInputEOS = true
                        } else {
                            decoder.queueInputBuffer(
                                inputBufferIndex, 0, sampleSize,
                                extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }
                }

                // Get output
                val outputBufferIndex = decoder.dequeueOutputBuffer(inputBufferInfo, 10000)
                if (outputBufferIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputBufferIndex)!!
                    currentTimeMs = inputBufferInfo.presentationTimeUs / 1000

                    // Process PCM data
                    val samples = processPcmBuffer(outputBuffer, inputBufferInfo.size, channels)
                    sampleBuffer.addAll(samples)

                    // Calculate energy in windows
                    while (sampleBuffer.size >= energyWindowSize) {
                        val windowSamples = sampleBuffer.take(energyWindowSize)
                        sampleBuffer = sampleBuffer.drop(energyWindowSize).toMutableList()

                        // Calculate RMS energy
                        val energy = sqrt(windowSamples.map { it * it }.average().toFloat())
                        energyHistory.add(energy)

                        // Keep history limited
                        if (energyHistory.size > 100) {
                            energyHistory.removeAt(0)
                        }

                        // Beat detection using local energy average
                        if (energyHistory.size >= 10) {
                            val localAvg = energyHistory.takeLast(20).average().toFloat()
                            val threshold = localAvg * sensitivity

                            // Check if current energy is a peak and above threshold
                            if (energy > threshold &&
                                energy > (energyHistory.getOrNull(energyHistory.size - 2) ?: 0f) &&
                                currentTimeMs - lastBeatTime >= MIN_BEAT_INTERVAL_MS
                            ) {
                                // Determine beat strength
                                val strength = when {
                                    energy > localAvg * 2.0f -> BeatStrength.STRONG
                                    energy > localAvg * 1.5f -> BeatStrength.MEDIUM
                                    else -> BeatStrength.WEAK
                                }

                                beats.add(
                                    BeatMarker(
                                        timestampMs = currentTimeMs,
                                        strength = strength,
                                        energy = energy
                                    )
                                )
                                lastBeatTime = currentTimeMs
                            }
                        }
                    }

                    // Update progress
                    _detectionProgress.value = (currentTimeMs.toFloat() / duration).coerceIn(0f, 1f)

                    decoder.releaseOutputBuffer(outputBufferIndex, false)

                    if (inputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
            }

            beats

        } finally {
            decoder?.stop()
            decoder?.release()
            extractor.release()
        }
    }

    private fun processPcmBuffer(buffer: ByteBuffer, size: Int, channels: Int): List<Float> {
        val samples = mutableListOf<Float>()
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.rewind()

        val numShorts = size / 2
        for (i in 0 until numShorts step channels) {
            if (buffer.remaining() >= 2) {
                val sample = buffer.short
                // Normalize to 0-1 range
                samples.add(abs(sample.toFloat()) / Short.MAX_VALUE)
            }
        }

        return samples
    }
}

/**
 * Represents a detected beat in the audio
 */
data class BeatMarker(
    val timestampMs: Long,
    val strength: BeatStrength,
    val energy: Float
) {
    /**
     * Get position as fraction (0-1) of total duration
     */
    fun getPosition(durationMs: Long): Float {
        return if (durationMs > 0) timestampMs.toFloat() / durationMs else 0f
    }
}

/**
 * Beat strength levels
 */
enum class BeatStrength {
    WEAK,
    MEDIUM,
    STRONG
}

/**
 * Data class for complete beat analysis result
 */
data class BeatAnalysis(
    val songId: Long,
    val beats: List<BeatMarker>,
    val estimatedBpm: Float,
    val averageEnergy: Float,
    val peakEnergy: Float
)
