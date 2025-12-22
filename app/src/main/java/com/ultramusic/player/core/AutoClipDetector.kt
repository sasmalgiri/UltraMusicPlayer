package com.ultramusic.player.core

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import com.ultramusic.player.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * AUTO CLIP DETECTOR
 * 
 * Automatically analyzes songs and detects the best A-B sections for battle!
 * 
 * Detects:
 * - 🔊 BASS DROPS - Sudden increase in low frequency energy
 * - 💣 ENERGY PEAKS - Loudest/most energetic sections
 * - 🎯 DROP MOMENTS - Silence/quiet → loud transitions
 * - 🔪 MID HOOKS - Strong vocal/mid-range sections
 * - 🌊 FULL SPECTRUM - Sections with all frequencies present
 * 
 * How it works:
 * 1. Decode audio to PCM samples
 * 2. Analyze in 1-second windows
 * 3. Calculate energy, bass, mids, highs for each window
 * 4. Detect significant changes (drops, peaks, transitions)
 * 5. Return suggested A-B clip points with purposes
 */
@Singleton
class AutoClipDetector @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AutoClipDetector"
        
        // Analysis parameters
        private const val WINDOW_SIZE_MS = 1000L      // 1 second windows
        private const val MIN_CLIP_DURATION_MS = 5000L  // Minimum 5 second clips
        private const val MAX_CLIP_DURATION_MS = 30000L // Maximum 30 second clips
        private const val IDEAL_CLIP_DURATION_MS = 15000L // Ideal 15 second clips
        
        // Detection thresholds
        private const val DROP_THRESHOLD = 2.5f      // Energy must increase 2.5x for drop
        private const val BASS_THRESHOLD = 0.6f      // 60% bass dominance for bass section
        private const val MID_THRESHOLD = 0.5f       // 50% mid dominance for vocal section
        private const val SILENCE_THRESHOLD = 0.1f   // Below 10% energy = silence
        private const val PEAK_PERCENTILE = 0.9f     // Top 10% energy = peak
    }
    
    /**
     * Analyze a song and return suggested A-B clips
     */
    suspend fun detectClips(song: Song): List<DetectedClip> = withContext(Dispatchers.Default) {
        try {
            Log.i(TAG, "Analyzing: ${song.title}")
            
            // Step 1: Decode audio and get samples
            val audioData = decodeAudio(song.path) ?: return@withContext emptyList()
            
            // Step 2: Analyze in windows
            val windows = analyzeWindows(audioData)
            if (windows.isEmpty()) return@withContext emptyList()
            
            // Step 3: Detect interesting sections
            val clips = mutableListOf<DetectedClip>()
            
            // Detect bass drops
            clips.addAll(detectBassDrops(windows, song))
            
            // Detect energy peaks
            clips.addAll(detectEnergyPeaks(windows, song))
            
            // Detect silence-to-loud transitions
            clips.addAll(detectDropMoments(windows, song))
            
            // Detect mid/vocal hooks
            clips.addAll(detectMidHooks(windows, song))
            
            // Detect full spectrum sections
            clips.addAll(detectFullSpectrum(windows, song))
            
            // Sort by quality score and remove overlaps
            val filtered = filterOverlappingClips(clips.sortedByDescending { it.qualityScore })
            
            Log.i(TAG, "Found ${filtered.size} clips in ${song.title}")
            filtered
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to analyze ${song.title}", e)
            emptyList()
        }
    }
    
    /**
     * Quick analysis - just find the best single clip
     */
    suspend fun detectBestClip(song: Song): DetectedClip? = withContext(Dispatchers.Default) {
        val clips = detectClips(song)
        clips.maxByOrNull { it.qualityScore }
    }
    
    /**
     * Decode audio file to PCM samples
     */
    private fun decodeAudio(path: String): AudioData? {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        
        try {
            extractor.setDataSource(path)
            
            // Find audio track
            var audioTrackIndex = -1
            var format: MediaFormat? = null
            
            for (i in 0 until extractor.trackCount) {
                val trackFormat = extractor.getTrackFormat(i)
                val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = trackFormat
                    break
                }
            }
            
            if (audioTrackIndex < 0 || format == null) {
                Log.w(TAG, "No audio track found")
                return null
            }
            
            extractor.selectTrack(audioTrackIndex)
            
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val duration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // Convert to ms
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            
            // Create decoder
            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(format, null, null, 0)
            decoder.start()
            
            val samples = mutableListOf<Float>()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            
            // Limit analysis to first 3 minutes for performance
            val maxSamples = sampleRate * channels * 180 // 3 minutes
            
            while (!outputDone && samples.size < maxSamples) {
                // Feed input
                if (!inputDone) {
                    val inputIndex = decoder.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                
                // Get output
                val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer = decoder.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        // Convert to float samples
                        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                        val shortBuffer = outputBuffer.asShortBuffer()
                        while (shortBuffer.hasRemaining() && samples.size < maxSamples) {
                            samples.add(shortBuffer.get() / 32768f)
                        }
                    }
                    decoder.releaseOutputBuffer(outputIndex, false)
                    
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }
            
            return AudioData(
                samples = samples.toFloatArray(),
                sampleRate = sampleRate,
                channels = channels,
                durationMs = duration
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Decode failed", e)
            return null
        } finally {
            try {
                decoder?.stop()
                decoder?.release()
            } catch (e: Exception) { }
            extractor.release()
        }
    }
    
    /**
     * Analyze audio in 1-second windows
     */
    private fun analyzeWindows(audio: AudioData): List<AnalysisWindow> {
        val windows = mutableListOf<AnalysisWindow>()
        val samplesPerWindow = (audio.sampleRate * WINDOW_SIZE_MS / 1000).toInt() * audio.channels
        
        var windowStart = 0
        var timeMs = 0L
        
        while (windowStart + samplesPerWindow <= audio.samples.size) {
            val windowSamples = audio.samples.copyOfRange(windowStart, windowStart + samplesPerWindow)
            
            // Calculate energy (RMS)
            var sumSquares = 0f
            for (sample in windowSamples) {
                sumSquares += sample * sample
            }
            val energy = sqrt(sumSquares / windowSamples.size)
            
            // Simple frequency band estimation using zero-crossing rate and energy distribution
            // This is a simplified approach without full FFT for performance
            val (bass, mid, high) = estimateFrequencyBands(windowSamples, audio.sampleRate)
            
            windows.add(AnalysisWindow(
                startMs = timeMs,
                endMs = timeMs + WINDOW_SIZE_MS,
                energy = energy,
                bassStrength = bass,
                midStrength = mid,
                highStrength = high
            ))
            
            windowStart += samplesPerWindow
            timeMs += WINDOW_SIZE_MS
        }
        
        // Normalize energy values
        val maxEnergy = windows.maxOfOrNull { it.energy } ?: 1f
        return windows.map { it.copy(energy = it.energy / maxEnergy) }
    }
    
    /**
     * Estimate frequency bands using simple filtering
     */
    private fun estimateFrequencyBands(samples: FloatArray, sampleRate: Int): Triple<Float, Float, Float> {
        // Simple low-pass for bass (< 250 Hz)
        var bassEnergy = 0f
        var prevSample = 0f
        val bassAlpha = 0.1f // Low-pass coefficient
        
        for (sample in samples) {
            val filtered = bassAlpha * sample + (1 - bassAlpha) * prevSample
            bassEnergy += filtered * filtered
            prevSample = filtered
        }
        bassEnergy = sqrt(bassEnergy / samples.size)
        
        // High-pass for highs (> 4000 Hz)
        var highEnergy = 0f
        prevSample = 0f
        val highAlpha = 0.9f
        
        for (sample in samples) {
            val filtered = highAlpha * (prevSample + sample - prevSample)
            highEnergy += filtered * filtered
            prevSample = sample
        }
        highEnergy = sqrt(highEnergy / samples.size)
        
        // Mid is the remainder
        val totalEnergy = sqrt(samples.map { it * it }.average().toFloat())
        val midEnergy = max(0f, totalEnergy - bassEnergy - highEnergy)
        
        // Normalize
        val total = bassEnergy + midEnergy + highEnergy
        return if (total > 0) {
            Triple(bassEnergy / total, midEnergy / total, highEnergy / total)
        } else {
            Triple(0.33f, 0.34f, 0.33f)
        }
    }
    
    /**
     * Detect bass drops - sudden increase in bass energy
     */
    private fun detectBassDrops(windows: List<AnalysisWindow>, song: Song): List<DetectedClip> {
        val clips = mutableListOf<DetectedClip>()
        
        for (i in 2 until windows.size) {
            val prev = windows[i - 1]
            val curr = windows[i]
            
            // Check for bass drop: low energy → high bass energy
            if (prev.energy < 0.3f && curr.energy > 0.5f && curr.bassStrength > BASS_THRESHOLD) {
                // Found a bass drop! Find the end of the section
                var endIndex = i
                while (endIndex < windows.size - 1 && 
                       windows[endIndex].bassStrength > BASS_THRESHOLD * 0.7f &&
                       endIndex - i < (MAX_CLIP_DURATION_MS / WINDOW_SIZE_MS)) {
                    endIndex++
                }
                
                // Ensure minimum duration
                val duration = windows[endIndex].endMs - windows[i].startMs
                if (duration >= MIN_CLIP_DURATION_MS) {
                    clips.add(DetectedClip(
                        songId = song.id,
                        songTitle = song.title,
                        songArtist = song.artist,
                        songPath = song.path,
                        startMs = max(0, windows[i].startMs - 2000), // Include 2s buildup
                        endMs = min(song.duration, windows[endIndex].endMs),
                        suggestedName = "Bass Drop @ ${formatTime(windows[i].startMs)}",
                        purpose = ClipPurpose.BASS_DESTROYER,
                        qualityScore = curr.energy * curr.bassStrength * 100,
                        reason = "Strong bass drop detected (${(curr.bassStrength * 100).toInt()}% bass)"
                    ))
                }
            }
        }
        
        return clips
    }
    
    /**
     * Detect energy peaks - loudest sections
     */
    private fun detectEnergyPeaks(windows: List<AnalysisWindow>, song: Song): List<DetectedClip> {
        val clips = mutableListOf<DetectedClip>()
        
        // Find peak threshold (top 10%)
        val sortedEnergies = windows.map { it.energy }.sorted()
        val peakThreshold = sortedEnergies[(sortedEnergies.size * PEAK_PERCENTILE).toInt()]
        
        var inPeak = false
        var peakStart = 0
        
        for (i in windows.indices) {
            if (!inPeak && windows[i].energy >= peakThreshold) {
                inPeak = true
                peakStart = i
            } else if (inPeak && (windows[i].energy < peakThreshold * 0.7f || i == windows.lastIndex)) {
                // End of peak
                val duration = windows[i].endMs - windows[peakStart].startMs
                if (duration >= MIN_CLIP_DURATION_MS) {
                    val avgEnergy = windows.subList(peakStart, i).map { it.energy }.average().toFloat()
                    clips.add(DetectedClip(
                        songId = song.id,
                        songTitle = song.title,
                        songArtist = song.artist,
                        songPath = song.path,
                        startMs = windows[peakStart].startMs,
                        endMs = min(song.duration, windows[i].endMs),
                        suggestedName = "Energy Peak @ ${formatTime(windows[peakStart].startMs)}",
                        purpose = ClipPurpose.ENERGY_BOMB,
                        qualityScore = avgEnergy * 90,
                        reason = "High energy section (${(avgEnergy * 100).toInt()}% intensity)"
                    ))
                }
                inPeak = false
            }
        }
        
        return clips
    }
    
    /**
     * Detect silence-to-loud transitions (drop moments)
     */
    private fun detectDropMoments(windows: List<AnalysisWindow>, song: Song): List<DetectedClip> {
        val clips = mutableListOf<DetectedClip>()
        
        for (i in 1 until windows.size) {
            val prev = windows[i - 1]
            val curr = windows[i]
            
            // Check for dramatic energy increase
            if (prev.energy < SILENCE_THRESHOLD && curr.energy > prev.energy * DROP_THRESHOLD) {
                // Found a drop moment!
                var endIndex = i
                while (endIndex < windows.size - 1 && 
                       windows[endIndex].energy > 0.4f &&
                       endIndex - i < (IDEAL_CLIP_DURATION_MS / WINDOW_SIZE_MS)) {
                    endIndex++
                }
                
                val duration = windows[endIndex].endMs - prev.startMs
                if (duration >= MIN_CLIP_DURATION_MS) {
                    clips.add(DetectedClip(
                        songId = song.id,
                        songTitle = song.title,
                        songArtist = song.artist,
                        songPath = song.path,
                        startMs = prev.startMs, // Include the silence
                        endMs = min(song.duration, windows[endIndex].endMs),
                        suggestedName = "Drop @ ${formatTime(curr.startMs)}",
                        purpose = ClipPurpose.DROP_KILLER,
                        qualityScore = (curr.energy / max(prev.energy, 0.01f)) * 20,
                        reason = "Silence-to-impact transition (${((curr.energy / max(prev.energy, 0.01f)) * 100).toInt()}% increase)"
                    ))
                }
            }
        }
        
        return clips
    }
    
    /**
     * Detect mid-range/vocal hooks
     */
    private fun detectMidHooks(windows: List<AnalysisWindow>, song: Song): List<DetectedClip> {
        val clips = mutableListOf<DetectedClip>()
        
        var inHook = false
        var hookStart = 0
        
        for (i in windows.indices) {
            val w = windows[i]
            if (!inHook && w.midStrength > MID_THRESHOLD && w.energy > 0.4f) {
                inHook = true
                hookStart = i
            } else if (inHook && (w.midStrength < MID_THRESHOLD * 0.7f || i == windows.lastIndex)) {
                val duration = windows[i].endMs - windows[hookStart].startMs
                if (duration >= MIN_CLIP_DURATION_MS && duration <= MAX_CLIP_DURATION_MS) {
                    val avgMid = windows.subList(hookStart, i).map { it.midStrength }.average().toFloat()
                    clips.add(DetectedClip(
                        songId = song.id,
                        songTitle = song.title,
                        songArtist = song.artist,
                        songPath = song.path,
                        startMs = windows[hookStart].startMs,
                        endMs = min(song.duration, windows[i].endMs),
                        suggestedName = "Hook @ ${formatTime(windows[hookStart].startMs)}",
                        purpose = ClipPurpose.MID_CUTTER,
                        qualityScore = avgMid * 80,
                        reason = "Strong mid-range/vocal section (${(avgMid * 100).toInt()}% mids)"
                    ))
                }
                inHook = false
            }
        }
        
        return clips
    }
    
    /**
     * Detect full spectrum sections (balanced frequencies)
     */
    private fun detectFullSpectrum(windows: List<AnalysisWindow>, song: Song): List<DetectedClip> {
        val clips = mutableListOf<DetectedClip>()
        
        var inFullSpectrum = false
        var start = 0
        
        for (i in windows.indices) {
            val w = windows[i]
            // Check if all frequencies are well-represented
            val isBalanced = w.bassStrength > 0.2f && w.midStrength > 0.2f && w.highStrength > 0.15f && w.energy > 0.5f
            
            if (!inFullSpectrum && isBalanced) {
                inFullSpectrum = true
                start = i
            } else if (inFullSpectrum && (!isBalanced || i == windows.lastIndex)) {
                val duration = windows[i].endMs - windows[start].startMs
                if (duration >= MIN_CLIP_DURATION_MS) {
                    clips.add(DetectedClip(
                        songId = song.id,
                        songTitle = song.title,
                        songArtist = song.artist,
                        songPath = song.path,
                        startMs = windows[start].startMs,
                        endMs = min(song.duration, windows[i].endMs),
                        suggestedName = "Full Mix @ ${formatTime(windows[start].startMs)}",
                        purpose = ClipPurpose.FREQUENCY_FILLER,
                        qualityScore = windows.subList(start, i).map { it.energy }.average().toFloat() * 70,
                        reason = "Balanced full-spectrum section"
                    ))
                }
                inFullSpectrum = false
            }
        }
        
        return clips
    }
    
    /**
     * Remove overlapping clips, keeping highest quality
     */
    private fun filterOverlappingClips(clips: List<DetectedClip>): List<DetectedClip> {
        if (clips.isEmpty()) return emptyList()
        
        val result = mutableListOf<DetectedClip>()
        val sorted = clips.sortedByDescending { it.qualityScore }
        
        for (clip in sorted) {
            val overlaps = result.any { existing ->
                // Check if clips overlap
                clip.startMs < existing.endMs && clip.endMs > existing.startMs
            }
            
            if (!overlaps) {
                result.add(clip)
            }
            
            // Limit to top 10 clips per song
            if (result.size >= 10) break
        }
        
        return result.sortedBy { it.startMs }
    }
    
    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        val mins = seconds / 60
        val secs = seconds % 60
        return "$mins:${secs.toString().padStart(2, '0')}"
    }
}

// ==================== DATA CLASSES ====================

private data class AudioData(
    val samples: FloatArray,
    val sampleRate: Int,
    val channels: Int,
    val durationMs: Long
)

private data class AnalysisWindow(
    val startMs: Long,
    val endMs: Long,
    val energy: Float,
    val bassStrength: Float,
    val midStrength: Float,
    val highStrength: Float
)

/**
 * A detected clip suggestion
 */
data class DetectedClip(
    val songId: Long,
    val songTitle: String,
    val songArtist: String,
    val songPath: String,
    val startMs: Long,
    val endMs: Long,
    val suggestedName: String,
    val purpose: ClipPurpose,
    val qualityScore: Float,
    val reason: String
) {
    val durationMs: Long get() = endMs - startMs
}
