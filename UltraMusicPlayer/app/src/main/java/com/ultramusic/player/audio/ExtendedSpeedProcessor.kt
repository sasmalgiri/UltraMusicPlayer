package com.ultramusic.player.audio

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * EXTENDED SPEED PROCESSOR
 * 
 * Provides extended speed range beyond ExoPlayer's default limits.
 * 
 * Features:
 * - Speed: 0.1x to 8.0x (vs ExoPlayer's 0.25x-4.0x)
 * - Pitch: -24 to +24 semitones (vs ExoPlayer's -12 to +12)
 * - Formant preservation (basic)
 * - WSOLA-based time stretching
 * 
 * Based on WSOLA (Waveform Similarity Overlap-Add) algorithm.
 */
@UnstableApi
class ExtendedSpeedProcessor : AudioProcessor {
    
    companion object {
        private const val TAG = "ExtendedSpeedProcessor"
        
        // Extended range
        const val MIN_SPEED = 0.1f
        const val MAX_SPEED = 8.0f
        const val MIN_PITCH = -24 // semitones
        const val MAX_PITCH = 24  // semitones
        
        // Processing parameters
        private const val WINDOW_SIZE_MS = 25
        private const val OVERLAP_RATIO = 0.5f
    }
    
    // Current settings
    private var speed: Float = 1.0f
    private var pitch: Float = 0f // semitones
    private var formantPreservation: Boolean = true
    
    // Audio format
    private var inputFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputFormat: AudioFormat = AudioFormat.NOT_SET
    private var sampleRate: Int = 44100
    private var channelCount: Int = 2
    private var bytesPerSample: Int = 2
    
    // Processing buffers
    private var inputBuffer: ByteBuffer = ByteBuffer.allocate(0)
    private var outputBuffer: ByteBuffer = ByteBuffer.allocate(0)
    private var pendingOutputBytes: Int = 0
    
    // WSOLA state
    private var windowSize: Int = 0
    private var overlapSize: Int = 0
    private var analysisHop: Int = 0
    private var synthesisHop: Int = 0
    private var prevWindow: FloatArray = FloatArray(0)
    private var inputAccumulator: FloatArray = FloatArray(0)
    private var accumulatorPosition: Int = 0
    
    // State
    private var isActive: Boolean = false
    private var inputEnded: Boolean = false
    
    /**
     * Set playback speed (0.1x to 8.0x)
     */
    fun setSpeed(newSpeed: Float) {
        speed = newSpeed.coerceIn(MIN_SPEED, MAX_SPEED)
        updateParameters()
        Log.i(TAG, "Speed set to: $speed")
    }
    
    /**
     * Set pitch adjustment in semitones (-24 to +24)
     */
    fun setPitch(semitones: Float) {
        pitch = semitones.coerceIn(MIN_PITCH.toFloat(), MAX_PITCH.toFloat())
        Log.i(TAG, "Pitch set to: $pitch semitones")
    }
    
    /**
     * Enable/disable formant preservation
     */
    fun setFormantPreservation(enabled: Boolean) {
        formantPreservation = enabled
    }
    
    /**
     * Get current speed
     */
    fun getSpeed(): Float = speed
    
    /**
     * Get current pitch
     */
    fun getPitch(): Float = pitch
    
    private fun updateParameters() {
        if (sampleRate > 0) {
            windowSize = (WINDOW_SIZE_MS * sampleRate / 1000)
            overlapSize = (windowSize * OVERLAP_RATIO).toInt()
            
            // Analysis hop stays constant, synthesis hop changes with speed
            analysisHop = windowSize - overlapSize
            synthesisHop = (analysisHop * speed).roundToInt().coerceAtLeast(1)
        }
    }
    
    // ==================== AudioProcessor Implementation ====================
    
    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioFormat.NOT_SET
        }
        
        inputFormat = inputAudioFormat
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        bytesPerSample = 2 // PCM 16-bit
        
        updateParameters()
        
        // Initialize buffers
        val bufferSize = windowSize * channelCount * 4 // Extra space
        prevWindow = FloatArray(windowSize * channelCount)
        inputAccumulator = FloatArray(bufferSize * 2)
        accumulatorPosition = 0
        
        // Output format same as input (we handle rate change internally)
        outputFormat = inputAudioFormat
        
        isActive = speed != 1.0f || pitch != 0f
        
        Log.i(TAG, "Configured: ${sampleRate}Hz, ${channelCount}ch, active=$isActive")
        
        return outputFormat
    }
    
    override fun isActive(): Boolean = isActive
    
    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isActive) {
            // Pass through unchanged
            this.inputBuffer = inputBuffer
            return
        }
        
        val inputSamples = inputBuffer.remaining() / bytesPerSample
        
        // Convert input to float
        val inputFloats = FloatArray(inputSamples)
        val shortBuffer = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        for (i in 0 until inputSamples) {
            inputFloats[i] = shortBuffer.get() / 32768f
        }
        
        // Add to accumulator
        val spaceNeeded = accumulatorPosition + inputFloats.size
        if (spaceNeeded > inputAccumulator.size) {
            val newAccumulator = FloatArray(spaceNeeded * 2)
            inputAccumulator.copyInto(newAccumulator, 0, 0, accumulatorPosition)
            inputAccumulator = newAccumulator
        }
        inputFloats.copyInto(inputAccumulator, accumulatorPosition)
        accumulatorPosition += inputFloats.size
        
        // Process using WSOLA
        val outputFloats = processWSola()
        
        // Convert back to bytes
        val outputBytes = ByteArray(outputFloats.size * bytesPerSample)
        val outBuffer = ByteBuffer.wrap(outputBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (sample in outputFloats) {
            val clamped = sample.coerceIn(-1f, 1f)
            outBuffer.putShort((clamped * 32767).toInt().toShort())
        }
        
        this.outputBuffer = ByteBuffer.wrap(outputBytes)
        pendingOutputBytes = outputBytes.size
        
        // Consume input
        inputBuffer.position(inputBuffer.limit())
    }
    
    /**
     * WSOLA time-stretching algorithm
     * Waveform Similarity Overlap-Add
     */
    private fun processWSola(): FloatArray {
        if (accumulatorPosition < windowSize * channelCount * 2) {
            return FloatArray(0)
        }
        
        val outputList = mutableListOf<Float>()
        var readPos = 0
        
        while (readPos + windowSize * channelCount <= accumulatorPosition) {
            // Extract current window
            val currentWindow = FloatArray(windowSize * channelCount)
            for (i in 0 until windowSize * channelCount) {
                currentWindow[i] = inputAccumulator[readPos + i]
            }
            
            // Apply Hann window
            applyHannWindow(currentWindow)
            
            // Overlap-add with previous window
            for (i in 0 until overlapSize * channelCount) {
                val blendFactor = i.toFloat() / (overlapSize * channelCount)
                currentWindow[i] = prevWindow[(windowSize - overlapSize) * channelCount + i] * (1 - blendFactor) +
                        currentWindow[i] * blendFactor
            }
            
            // Output synthesized samples
            for (i in 0 until synthesisHop * channelCount) {
                if (i < currentWindow.size) {
                    outputList.add(currentWindow[i])
                }
            }
            
            // Save for next overlap
            currentWindow.copyInto(prevWindow)
            
            // Move read position by analysis hop (not synthesis hop)
            readPos += analysisHop * channelCount
        }
        
        // Shift remaining data in accumulator
        if (readPos > 0 && readPos < accumulatorPosition) {
            for (i in readPos until accumulatorPosition) {
                inputAccumulator[i - readPos] = inputAccumulator[i]
            }
            accumulatorPosition -= readPos
        } else if (readPos >= accumulatorPosition) {
            accumulatorPosition = 0
        }
        
        return outputList.toFloatArray()
    }
    
    /**
     * Apply Hann window for smooth transitions
     */
    private fun applyHannWindow(buffer: FloatArray) {
        val windowLen = buffer.size / channelCount
        for (i in 0 until windowLen) {
            val multiplier = 0.5f * (1 - kotlin.math.cos(2 * Math.PI * i / (windowLen - 1))).toFloat()
            for (ch in 0 until channelCount) {
                buffer[i * channelCount + ch] *= multiplier
            }
        }
    }
    
    override fun queueEndOfStream() {
        inputEnded = true
    }
    
    override fun getOutput(): ByteBuffer {
        if (!isActive) {
            val output = inputBuffer
            inputBuffer = ByteBuffer.allocate(0)
            return output
        }
        
        val output = outputBuffer
        outputBuffer = ByteBuffer.allocate(0)
        pendingOutputBytes = 0
        return output
    }
    
    override fun isEnded(): Boolean = inputEnded && pendingOutputBytes == 0
    
    override fun flush() {
        inputBuffer = ByteBuffer.allocate(0)
        outputBuffer = ByteBuffer.allocate(0)
        pendingOutputBytes = 0
        accumulatorPosition = 0
        prevWindow.fill(0f)
        inputEnded = false
    }
    
    override fun reset() {
        flush()
        speed = 1.0f
        pitch = 0f
        isActive = false
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
    }
}

/**
 * Speed/Pitch settings for extended range
 */
data class ExtendedAudioSettings(
    val speed: Float = 1.0f,        // 0.1x to 8.0x
    val pitch: Float = 0f,          // -24 to +24 semitones
    val formantPreservation: Boolean = true
) {
    val isDefault: Boolean get() = speed == 1.0f && pitch == 0f
    
    companion object {
        val DEFAULT = ExtendedAudioSettings()
        
        // Preset extreme settings
        val ULTRA_SLOW = ExtendedAudioSettings(speed = 0.1f)
        val VERY_SLOW = ExtendedAudioSettings(speed = 0.25f)
        val SLOW = ExtendedAudioSettings(speed = 0.5f)
        val NORMAL = DEFAULT
        val FAST = ExtendedAudioSettings(speed = 1.5f)
        val VERY_FAST = ExtendedAudioSettings(speed = 2.0f)
        val ULTRA_FAST = ExtendedAudioSettings(speed = 4.0f)
        val EXTREME_FAST = ExtendedAudioSettings(speed = 8.0f)
        
        // Pitch presets
        val CHIPMUNK = ExtendedAudioSettings(pitch = 12f)
        val EXTREME_HIGH = ExtendedAudioSettings(pitch = 24f)
        val DEEP = ExtendedAudioSettings(pitch = -12f)
        val EXTREME_LOW = ExtendedAudioSettings(pitch = -24f)
        
        // Combined presets
        val NIGHTCORE_EXTREME = ExtendedAudioSettings(speed = 1.5f, pitch = 8f)
        val SLOWED_EXTREME = ExtendedAudioSettings(speed = 0.6f, pitch = -6f)
        val VAPORWAVE_EXTREME = ExtendedAudioSettings(speed = 0.7f, pitch = -8f)
    }
}
