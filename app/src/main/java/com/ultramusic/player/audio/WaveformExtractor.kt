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

/**
 * Waveform Extractor
 *
 * Extracts audio waveform data from audio files for visualization.
 * Supports MP3, AAC, FLAC, WAV, and other common formats.
 */
@Singleton
class WaveformExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "WaveformExtractor"
        private const val DEFAULT_SAMPLES = 200  // Number of bars in waveform
        private const val BUFFER_SIZE = 16384
    }

    // Cache for extracted waveforms
    private val waveformCache = mutableMapOf<Long, List<Float>>()

    private val _isExtracting = MutableStateFlow(false)
    val isExtracting: StateFlow<Boolean> = _isExtracting.asStateFlow()

    private val _extractionProgress = MutableStateFlow(0f)
    val extractionProgress: StateFlow<Float> = _extractionProgress.asStateFlow()

    /**
     * Extract waveform data from an audio file
     *
     * @param songId Unique ID for caching
     * @param uri Audio file URI
     * @param numSamples Number of amplitude samples to return (default 200)
     * @return List of normalized amplitudes (0.0 to 1.0)
     */
    suspend fun extractWaveform(
        songId: Long,
        uri: Uri,
        numSamples: Int = DEFAULT_SAMPLES
    ): List<Float> = withContext(Dispatchers.IO) {
        // Check cache first
        waveformCache[songId]?.let { cached ->
            if (cached.size == numSamples) {
                return@withContext cached
            }
        }

        _isExtracting.value = true
        _extractionProgress.value = 0f

        try {
            val waveform = extractFromFile(uri, numSamples)
            waveformCache[songId] = waveform
            waveform
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting waveform", e)
            // Return a dummy waveform on error
            generateDummyWaveform(numSamples)
        } finally {
            _isExtracting.value = false
            _extractionProgress.value = 1f
        }
    }

    /**
     * Get cached waveform if available
     */
    fun getCachedWaveform(songId: Long): List<Float>? {
        return waveformCache[songId]
    }

    /**
     * Clear waveform cache
     */
    fun clearCache() {
        waveformCache.clear()
    }

    /**
     * Clear specific song from cache
     */
    fun clearFromCache(songId: Long) {
        waveformCache.remove(songId)
    }

    private suspend fun extractFromFile(uri: Uri, numSamples: Int): List<Float> = withContext(Dispatchers.IO) {
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
                return@withContext generateDummyWaveform(numSamples)
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)!!
            val duration = format.getLong(MediaFormat.KEY_DURATION) // microseconds
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // Create decoder
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()

            val amplitudes = mutableListOf<Float>()
            val inputBufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false
            var totalSamplesDecoded = 0L
            val totalSamples = duration * sampleRate / 1_000_000

            // Calculate samples per bucket
            val samplesPerBucket = max(1L, totalSamples / numSamples)
            var currentBucketMax = 0f
            var currentBucketSamples = 0L

            while (!sawOutputEOS) {
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

                    // Process PCM data
                    val samples = processPcmBuffer(outputBuffer, inputBufferInfo.size, channels)

                    for (sample in samples) {
                        currentBucketMax = max(currentBucketMax, sample)
                        currentBucketSamples++

                        if (currentBucketSamples >= samplesPerBucket) {
                            amplitudes.add(currentBucketMax)
                            currentBucketMax = 0f
                            currentBucketSamples = 0

                            // Update progress
                            _extractionProgress.value = amplitudes.size.toFloat() / numSamples
                        }
                    }

                    totalSamplesDecoded += samples.size

                    decoder.releaseOutputBuffer(outputBufferIndex, false)

                    if (inputBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEOS = true
                    }
                }
            }

            // Add remaining bucket
            if (currentBucketSamples > 0) {
                amplitudes.add(currentBucketMax)
            }

            // Pad or trim to exact size
            val result = when {
                amplitudes.size < numSamples -> {
                    amplitudes + List(numSamples - amplitudes.size) { 0f }
                }
                amplitudes.size > numSamples -> {
                    amplitudes.take(numSamples)
                }
                else -> amplitudes
            }

            result

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

    private fun generateDummyWaveform(numSamples: Int): List<Float> {
        // Generate a realistic-looking waveform pattern
        return List(numSamples) { i ->
            val base = 0.3f + (kotlin.math.sin(i * 0.1f) * 0.2f)
            val variation = (kotlin.random.Random.nextFloat() * 0.3f)
            (base + variation).coerceIn(0.1f, 1f)
        }
    }

    /**
     * Generate a quick preview waveform without full decode
     * Uses file size and sampling for faster generation
     */
    suspend fun generateQuickPreview(uri: Uri, numSamples: Int = DEFAULT_SAMPLES): List<Float> =
        withContext(Dispatchers.IO) {
            try {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, uri, null)

                // Find audio track
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("audio/")) {
                        extractor.selectTrack(i)
                        break
                    }
                }

                val amplitudes = mutableListOf<Float>()
                val buffer = ByteBuffer.allocate(BUFFER_SIZE)

                // Sample at regular intervals
                val duration = extractor.getTrackFormat(0).getLong(MediaFormat.KEY_DURATION)
                val interval = duration / numSamples

                for (i in 0 until numSamples) {
                    extractor.seekTo(i * interval, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)

                    if (size > 0) {
                        // Calculate RMS of this chunk
                        buffer.rewind()
                        var sum = 0f
                        val shortBuffer = buffer.asShortBuffer()
                        val shortCount = size / 2
                        for (j in 0 until shortCount) {
                            val sample = abs(shortBuffer.get().toFloat()) / Short.MAX_VALUE
                            sum += sample * sample
                        }
                        val rms = kotlin.math.sqrt(sum / shortCount)
                        amplitudes.add(rms.coerceIn(0f, 1f))
                    } else {
                        amplitudes.add(0f)
                    }
                }

                extractor.release()
                amplitudes

            } catch (e: Exception) {
                Log.e(TAG, "Error generating quick preview", e)
                generateDummyWaveform(numSamples)
            }
        }
}

/**
 * Data class for waveform with metadata
 */
data class WaveformData(
    val songId: Long,
    val amplitudes: List<Float>,
    val duration: Long,
    val sampleRate: Int = 44100
)
