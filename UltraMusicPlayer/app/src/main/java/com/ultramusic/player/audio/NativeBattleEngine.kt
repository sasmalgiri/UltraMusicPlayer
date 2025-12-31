package com.ultramusic.player.audio

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio Engine Type - User can select at runtime
 *
 * - SOUNDTOUCH:   8.5/10 quality, balanced CPU, FREE (default)
 * - SUPERPOWERED: 9.5/10 quality, DJ-grade, ultra-low latency
 * - RUBBERBAND:   10/10 quality, studio-grade, used by DAWs (coming soon)
 */
enum class AudioEngineType(val value: Int, val displayName: String, val quality: String) {
    SOUNDTOUCH(0, "SoundTouch", "8.5/10 - Balanced"),
    SUPERPOWERED(1, "Superpowered", "9.5/10 - DJ Grade"),
    RUBBERBAND(2, "Rubberband", "10/10 - Studio Grade");

    companion object {
        fun fromValue(value: Int): AudioEngineType = entries.find { it.value == value } ?: SOUNDTOUCH
    }
}

/**
 * NATIVE BATTLE ENGINE v2.0
 *
 * Kotlin wrapper for the C++ Battle Audio Engine.
 * Provides professional-grade audio processing:
 *
 * AUDIO QUALITY: 9/10 (Frequency-domain phase vocoder)
 *
 * Features:
 * - Speed: 0.05x to 10.0x (tempo without pitch change)
 * - Pitch: -36 to +36 semitones (pitch without tempo change)
 * - Phase coherence preservation (no phasiness artifacts)
 * - Transient detection and preservation (drums stay punchy)
 * - Formant correction (voice sounds natural at extreme pitches)
 * - Battle limiter (no clipping at extreme volumes)
 * - Punch compressor (cuts through the mix)
 * - Mega bass boost (shake the ground)
 *
 * Uses Superpowered-compatible Phase Vocoder for time-stretching/pitch-shifting.
 * Superior to time-domain algorithms (like WSOLA) for most material.
 * Optimized for ARM NEON SIMD on Android devices.
 */
@Singleton
class NativeBattleEngine @Inject constructor() {
    
    companion object {
        private const val TAG = "NativeBattleEngine"
        
        // Speed limits (WAY beyond ExoPlayer's 0.25-4.0)
        const val MIN_SPEED = 0.05f
        const val MAX_SPEED = 10.0f
        
        // Pitch limits (WAY beyond standard -12 to +12)
        const val MIN_PITCH_SEMITONES = -36f
        const val MAX_PITCH_SEMITONES = 36f
        
        // Try to load native library
        // DISABLED: Superpowered SDK crashes on license validation in emulator
        // TODO: Re-enable when proper Superpowered license is configured
        private var isNativeLoaded = false

        init {
            // Native engine now uses SoundTouch (FREE, no license needed)
            try {
                System.loadLibrary("ultramusic_audio")
                isNativeLoaded = true
                Log.i(TAG, "Native Battle Engine loaded! (SoundTouch mode)")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
                isNativeLoaded = false
            }
        }

        fun isAvailable(): Boolean = isNativeLoaded
    }
    
    // Native handle
    private var nativeHandle: Long = 0
    private var soundTouchHandle: Long = 0
    
    // Current settings
    private var sampleRate: Int = 44100
    private var channels: Int = 2
    
    private var speed: Float = 1.0f
    private var pitchSemitones: Float = 0.0f
    private var rate: Float = 1.0f
    private var bassBoostDb: Float = 0.0f
    
    private var battleModeEnabled: Boolean = false
    private var isInitialized: Boolean = false
    
    // Buffers
    private var inputBuffer = ShortArray(0)
    private var outputBuffer = ShortArray(0)
    
    /**
     * Initialize the engine
     */
    fun initialize(sampleRate: Int = 44100, channels: Int = 2): Boolean {
        if (!isNativeLoaded) {
            Log.w(TAG, "Native library not loaded, cannot initialize")
            return false
        }
        
        this.sampleRate = sampleRate
        this.channels = channels
        
        try {
            // Create native engine
            nativeHandle = nativeCreate()
            if (nativeHandle == 0L) {
                Log.e(TAG, "Failed to create native engine")
                return false
            }
            
            // Configure it
            nativeConfigure(nativeHandle, sampleRate, channels)
            
            // Also create direct SoundTouch for simple operations
            soundTouchHandle = soundTouchCreate()
            if (soundTouchHandle != 0L) {
                soundTouchSetSampleRate(soundTouchHandle, sampleRate)
                soundTouchSetChannels(soundTouchHandle, channels)
            }
            
            // Allocate buffers (1 second at sample rate)
            val bufferSize = sampleRate * channels * 2  // Extra space for slow speeds
            inputBuffer = ShortArray(bufferSize)
            outputBuffer = ShortArray(bufferSize * 4)  // 4x for extreme slow speeds
            
            isInitialized = true
            Log.i(TAG, "Initialized: ${sampleRate}Hz, ${channels}ch")
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            return false
        }
    }
    
    /**
     * Set playback speed (0.05x to 10.0x)
     * Changes tempo WITHOUT changing pitch!
     */
    fun setSpeed(newSpeed: Float) {
        speed = newSpeed.coerceIn(MIN_SPEED, MAX_SPEED)
        
        if (nativeHandle != 0L) {
            nativeSetSpeed(nativeHandle, speed)
        }
        if (soundTouchHandle != 0L) {
            soundTouchSetTempo(soundTouchHandle, speed)
        }
        
        Log.d(TAG, "Speed: ${speed}x")
    }
    
    /**
     * Set pitch in semitones (-36 to +36)
     * Changes pitch WITHOUT changing tempo!
     */
    fun setPitch(semitones: Float) {
        pitchSemitones = semitones.coerceIn(MIN_PITCH_SEMITONES, MAX_PITCH_SEMITONES)
        
        if (nativeHandle != 0L) {
            nativeSetPitch(nativeHandle, pitchSemitones)
        }
        if (soundTouchHandle != 0L) {
            soundTouchSetPitchSemitones(soundTouchHandle, pitchSemitones)
        }
        
        Log.d(TAG, "Pitch: ${pitchSemitones} semitones")
    }
    
    /**
     * Set rate (changes both speed AND pitch together, like vinyl)
     */
    fun setRate(newRate: Float) {
        rate = newRate.coerceIn(MIN_SPEED, MAX_SPEED)
        
        if (nativeHandle != 0L) {
            nativeSetRate(nativeHandle, rate)
        }
        if (soundTouchHandle != 0L) {
            soundTouchSetRate(soundTouchHandle, rate)
        }
        
        Log.d(TAG, "Rate: ${rate}x")
    }
    
    /**
     * Enable battle mode
     * Activates limiter, compressor, and other battle optimizations
     */
    fun setBattleMode(enabled: Boolean) {
        battleModeEnabled = enabled
        
        if (nativeHandle != 0L) {
            nativeSetBattleMode(nativeHandle, enabled)
        }
        
        Log.i(TAG, "Battle Mode: ${if (enabled) "ENGAGED!" else "off"}")
    }
    
    /**
     * Set bass boost amount (0 to 24 dB)
     */
    fun setBassBoost(db: Float) {
        bassBoostDb = db.coerceIn(0f, 24f)

        if (nativeHandle != 0L) {
            nativeSetBassBoost(nativeHandle, bassBoostDb)
        }

        Log.d(TAG, "Bass Boost: ${bassBoostDb} dB")
    }

    /**
     * Set sub-harmonic synthesis amount (0.0-1.0)
     * Generates octave-below frequencies for perceived bass without gain
     * This makes bass SOUND louder without adding actual gain that causes clipping
     */
    fun setSubHarmonicAmount(amount: Float) {
        if (nativeHandle != 0L) {
            nativeSetSubHarmonic(nativeHandle, amount.coerceIn(0f, 1f))
        }
        Log.d(TAG, "Sub-harmonic: $amount")
    }

    /**
     * Set harmonic exciter amount (0.0-1.0)
     * Adds harmonics for bass presence via soft saturation
     * Makes bass cut through the mix without adding raw gain
     */
    fun setExciterAmount(amount: Float) {
        if (nativeHandle != 0L) {
            nativeSetExciter(nativeHandle, amount.coerceIn(0f, 1f))
        }
        Log.d(TAG, "Exciter: $amount")
    }

    /**
     * Process audio samples
     * Input: PCM 16-bit samples
     * Output: Processed PCM 16-bit samples
     * 
     * Returns: Number of output samples
     */
    fun process(input: ShortArray, numSamples: Int): Pair<ShortArray, Int> {
        if (!isInitialized || nativeHandle == 0L) {
            return Pair(input, numSamples)
        }
        
        // Use full engine with battle processing
        val outputSamples = nativeProcess(nativeHandle, input, numSamples, outputBuffer)
        
        return Pair(outputBuffer.copyOf(outputSamples), outputSamples)
    }
    
    /**
     * Process using SoundTouch only (simpler, for just speed/pitch)
     */
    fun processSoundTouchOnly(input: ShortArray, numFrames: Int): Pair<ShortArray, Int> {
        if (soundTouchHandle == 0L) {
            return Pair(input, numFrames * channels)
        }
        
        // Put samples
        soundTouchPutSamples(soundTouchHandle, input, numFrames)
        
        // Receive processed samples
        val maxOutput = (numFrames * 4 / speed).toInt()  // Account for tempo change
        if (outputBuffer.size < maxOutput * channels) {
            outputBuffer = ShortArray(maxOutput * channels * 2)
        }
        
        val received = soundTouchReceiveSamples(soundTouchHandle, outputBuffer, maxOutput)
        
        return Pair(outputBuffer.copyOf(received * channels), received * channels)
    }
    
    /**
     * Flush remaining samples
     */
    fun flush() {
        if (nativeHandle != 0L) {
            nativeFlush(nativeHandle)
        }
        if (soundTouchHandle != 0L) {
            soundTouchFlush(soundTouchHandle)
        }
    }
    
    /**
     * Clear all buffers
     */
    fun clear() {
        if (nativeHandle != 0L) {
            nativeClear(nativeHandle)
        }
        if (soundTouchHandle != 0L) {
            soundTouchClear(soundTouchHandle)
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0
        }
        if (soundTouchHandle != 0L) {
            soundTouchDestroy(soundTouchHandle)
            soundTouchHandle = 0
        }
        
        isInitialized = false
        Log.i(TAG, "Released")
    }
    
    /**
     * Get engine version
     */
    fun getEngineVersion(): String {
        return if (isNativeLoaded) {
            nativeGetVersion()
        } else {
            "Native library not loaded"
        }
    }
    
    // ==================== SAFE MODE (LIMITER BYPASS) ====================

    private var limiterEnabled: Boolean = true

    /**
     * Enable/disable the native limiter
     *
     * When enabled: Limiter prevents clipping at extreme volumes (safe)
     * When disabled: No limiting, FULL SEND mode (maximum power but can clip)
     */
    fun setLimiterEnabled(enabled: Boolean) {
        limiterEnabled = enabled

        if (nativeHandle != 0L) {
            nativeSetLimiterEnabled(nativeHandle, enabled)
        }

        Log.i(TAG, if (enabled)
            "Native Limiter: ON (clipping protection)"
        else
            "Native Limiter: OFF (FULL SEND - no limits!)")
    }

    fun isLimiterEnabled(): Boolean = limiterEnabled

    // ==================== HARDWARE PROTECTION (STRONG SAFETY) ====================

    private var hardwareProtectionEnabled: Boolean = true

    /**
     * Enable/disable hardware protection
     *
     * This is the STRONGEST safety feature - protects speakers from damage.
     * Even with Safe Mode OFF, this provides a hard ceiling.
     *
     * When enabled:
     * - Hard ceiling at -0.5dB
     * - Sub-bass filter (<20Hz removal)
     * - DC offset removal
     * - Sustained output limiting
     *
     * When disabled:
     * - Full raw output (WARNING: can damage speakers!)
     */
    fun setHardwareProtection(enabled: Boolean) {
        hardwareProtectionEnabled = enabled

        if (nativeHandle != 0L) {
            nativeSetHardwareProtection(nativeHandle, enabled)
        }

        Log.i(TAG, if (enabled)
            "Hardware Protection: ON (speaker safe)"
        else
            "Hardware Protection: OFF (WARNING: damage possible!)")
    }

    fun isHardwareProtectionEnabled(): Boolean = hardwareProtectionEnabled

    // ==================== AUDIOPHILE MODE (PURE QUALITY) ====================

    private var audiophileModeEnabled: Boolean = false

    /**
     * Enable/disable audiophile mode
     *
     * Optimized for the cleanest, most pleasant audio quality.
     * Disables battle-oriented processing for transparent sound.
     *
     * When enabled:
     * - Disables compressor (preserves dynamics)
     * - Disables exciter (no artificial harmonics)
     * - Disables sub-harmonic synthesizer
     * - Enables high-quality processing
     * - Subtle clarity enhancement
     */
    fun setAudiophileMode(enabled: Boolean) {
        audiophileModeEnabled = enabled

        if (nativeHandle != 0L) {
            nativeSetAudiophileMode(nativeHandle, enabled)
        }

        Log.i(TAG, if (enabled)
            "Audiophile Mode: ON (pure quality)"
        else
            "Audiophile Mode: OFF (battle ready)")
    }

    fun isAudiophileModeEnabled(): Boolean = audiophileModeEnabled

    // ==================== AUDIO ENGINE SELECTION ====================

    private var currentEngine: AudioEngineType = AudioEngineType.SOUNDTOUCH

    /**
     * Set the audio engine to use
     *
     * - SOUNDTOUCH:   Balanced quality, lower CPU (default)
     * - SUPERPOWERED: DJ-grade quality, ultra-low latency
     * - RUBBERBAND:   Studio-grade quality, best for critical listening
     */
    fun setAudioEngine(engine: AudioEngineType) {
        currentEngine = engine

        if (nativeHandle != 0L) {
            nativeSetAudioEngine(nativeHandle, engine.value)
        }

        Log.i(TAG, "Audio Engine set to: ${engine.displayName} (${engine.quality})")
    }

    /**
     * Get current audio engine
     */
    fun getAudioEngine(): AudioEngineType {
        if (nativeHandle != 0L) {
            val engineValue = nativeGetAudioEngine(nativeHandle)
            currentEngine = AudioEngineType.fromValue(engineValue)
        }
        return currentEngine
    }

    // Getters
    fun getSpeed(): Float = speed
    fun getPitch(): Float = pitchSemitones
    fun getRate(): Float = rate
    fun isBattleModeEnabled(): Boolean = battleModeEnabled
    fun isReady(): Boolean = isInitialized && nativeHandle != 0L
    
    // ==================== Native Methods ====================
    
    // Full Battle Engine
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeConfigure(handle: Long, sampleRate: Int, channels: Int)
    private external fun nativeSetSpeed(handle: Long, speed: Float)
    private external fun nativeSetPitch(handle: Long, semitones: Float)
    private external fun nativeSetRate(handle: Long, rate: Float)
    private external fun nativeSetBattleMode(handle: Long, enabled: Boolean)
    private external fun nativeSetBassBoost(handle: Long, amount: Float)
    private external fun nativeSetSubHarmonic(handle: Long, amount: Float)
    private external fun nativeSetExciter(handle: Long, amount: Float)
    private external fun nativeSetLimiterEnabled(handle: Long, enabled: Boolean)
    private external fun nativeSetHardwareProtection(handle: Long, enabled: Boolean)
    private external fun nativeSetAudiophileMode(handle: Long, enabled: Boolean)
    private external fun nativeSetAudioEngine(handle: Long, engineType: Int)
    private external fun nativeGetAudioEngine(handle: Long): Int
    private external fun nativeProcess(handle: Long, input: ShortArray, numSamples: Int, output: ShortArray): Int
    private external fun nativeFlush(handle: Long)
    private external fun nativeClear(handle: Long)
    
    // Direct SoundTouch access
    private external fun soundTouchCreate(): Long
    private external fun soundTouchDestroy(handle: Long)
    private external fun soundTouchSetSampleRate(handle: Long, sampleRate: Int)
    private external fun soundTouchSetChannels(handle: Long, channels: Int)
    private external fun soundTouchSetTempo(handle: Long, tempo: Float)
    private external fun soundTouchSetPitch(handle: Long, pitch: Float)
    private external fun soundTouchSetPitchSemitones(handle: Long, semitones: Float)
    private external fun soundTouchSetRate(handle: Long, rate: Float)
    private external fun soundTouchPutSamples(handle: Long, samples: ShortArray, numSamples: Int)
    private external fun soundTouchReceiveSamples(handle: Long, output: ShortArray, maxSamples: Int): Int
    private external fun soundTouchFlush(handle: Long)
    private external fun soundTouchClear(handle: Long)
    
    private external fun nativeGetVersion(): String
}

/**
 * Media3 AudioProcessor wrapper for NativeBattleEngine
 */
@UnstableApi
class BattleAudioProcessor(
    private val engine: NativeBattleEngine
) : AudioProcessor {
    
    private var inputFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputFormat: AudioFormat = AudioFormat.NOT_SET
    
    private var inputBuffer: ByteBuffer = EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false
    
    private var isActive = false
    private var tempInputShorts = ShortArray(0)
    private var tempOutputShorts = ShortArray(0)
    
    fun setSpeed(speed: Float) {
        engine.setSpeed(speed)
        isActive = engine.getSpeed() != 1.0f || engine.getPitch() != 0.0f
    }
    
    fun setPitch(semitones: Float) {
        engine.setPitch(semitones)
        isActive = engine.getSpeed() != 1.0f || engine.getPitch() != 0.0f
    }
    
    fun setBattleMode(enabled: Boolean) {
        engine.setBattleMode(enabled)
    }
    
    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioFormat.NOT_SET
        }
        
        inputFormat = inputAudioFormat
        
        // Initialize engine with format
        engine.initialize(inputAudioFormat.sampleRate, inputAudioFormat.channelCount)
        
        outputFormat = inputAudioFormat
        return outputFormat
    }
    
    override fun isActive(): Boolean = isActive && NativeBattleEngine.isAvailable()
    
    override fun queueInput(buffer: ByteBuffer) {
        if (!isActive) {
            inputBuffer = buffer
            return
        }
        
        // Convert bytes to shorts
        val shortBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val numShorts = shortBuffer.remaining()
        
        if (tempInputShorts.size < numShorts) {
            tempInputShorts = ShortArray(numShorts)
        }
        shortBuffer.get(tempInputShorts, 0, numShorts)
        
        // Process through engine
        val (processed, outputCount) = engine.process(tempInputShorts, numShorts)
        
        if (outputCount > 0) {
            // Convert back to bytes
            val outputBytes = ByteArray(outputCount * 2)
            val outBuf = ByteBuffer.wrap(outputBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until outputCount) {
                outBuf.putShort(processed[i])
            }
            outputBuffer = ByteBuffer.wrap(outputBytes)
        } else {
            outputBuffer = EMPTY_BUFFER
        }
        
        buffer.position(buffer.limit())
    }
    
    override fun queueEndOfStream() {
        inputEnded = true
        engine.flush()
    }
    
    override fun getOutput(): ByteBuffer {
        if (!isActive) {
            val out = inputBuffer
            inputBuffer = EMPTY_BUFFER
            return out
        }
        
        val out = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return out
    }
    
    override fun isEnded(): Boolean = inputEnded && outputBuffer === EMPTY_BUFFER
    
    override fun flush() {
        inputBuffer = EMPTY_BUFFER
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        engine.clear()
    }
    
    override fun reset() {
        flush()
        isActive = false
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
        engine.release()
    }
}

private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocate(0)

