package com.ultramusic.player.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Quality modes for audio processing
 */
enum class QualityMode(val value: Int) {
    ULTRA_HIGH(0),   // Best quality, highest CPU
    HIGH(1),         // Great quality, moderate CPU (default)
    BALANCED(2),     // Good quality, balanced CPU
    PERFORMANCE(3),  // Acceptable quality, low CPU
    VOICE(4),        // Optimized for speech/vocals
    INSTRUMENT(5),   // Optimized for instruments
    PERCUSSION(6)    // Optimized for drums
}

/**
 * Processing algorithm selection
 */
enum class Algorithm(val value: Int) {
    PHASE_VOCODER(0),     // Frequency domain - highest quality
    WSOLA(1),             // Time domain - lower latency
    HYBRID(2),            // Adaptive switching
    ELASTIQUE_STYLE(3)    // Premium algorithm
}

/**
 * Playback state
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val speed: Float = 1.0f,
    val pitchSemitones: Float = 0.0f,
    val pitchCents: Float = 0.0f,
    val isLooping: Boolean = false,
    val loopStartMs: Long = 0,
    val loopEndMs: Long = 0
)

/**
 * Native Audio Engine
 * 
 * Kotlin interface to the native C++ audio engine.
 * Provides professional-grade audio processing with:
 * - Extended speed range: 0.05x to 10.0x
 * - Extended pitch range: -36 to +36 semitones
 * - Formant preservation
 * - Multiple quality modes
 */
@Singleton
class NativeAudioEngine @Inject constructor() {
    
    companion object {
        // Load native library
        init {
            System.loadLibrary("ultramusic_audio")
        }
        
        // Speed limits
        const val MIN_SPEED = 0.05f
        const val MAX_SPEED = 10.0f
        
        // Pitch limits (semitones)
        const val MIN_PITCH = -36f
        const val MAX_PITCH = 36f
        
        // Formant limits
        const val MIN_FORMANT = -24f
        const val MAX_FORMANT = 24f
    }
    
    // State flow for UI observation
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private var isInitialized = false
    private var sampleRate = 44100
    private var channelCount = 2
    
    // ========================================================================
    // Lifecycle
    // ========================================================================
    
    /**
     * Initialize the audio engine
     */
    fun initialize(sampleRate: Int = 44100, channelCount: Int = 2): Boolean {
        this.sampleRate = sampleRate
        this.channelCount = channelCount
        isInitialized = nativeInit(sampleRate, channelCount)
        return isInitialized
    }
    
    /**
     * Shutdown the audio engine
     */
    fun shutdown() {
        if (isInitialized) {
            nativeShutdown()
            isInitialized = false
        }
    }
    
    // ========================================================================
    // Audio Loading
    // ========================================================================
    
    /**
     * Load an audio file from path
     */
    fun loadFile(filePath: String): Boolean {
        if (!isInitialized) return false
        return nativeLoadFile(filePath)
    }
    
    /**
     * Unload current audio
     */
    fun unload() {
        if (isInitialized) {
            nativeUnload()
        }
    }
    
    // ========================================================================
    // Playback Control
    // ========================================================================
    
    fun play() {
        if (isInitialized) {
            nativePlay()
            updateState { it.copy(isPlaying = true) }
        }
    }
    
    fun pause() {
        if (isInitialized) {
            nativePause()
            updateState { it.copy(isPlaying = false) }
        }
    }
    
    fun stop() {
        if (isInitialized) {
            nativeStop()
            updateState { it.copy(isPlaying = false, currentPositionMs = 0) }
        }
    }
    
    fun seekTo(positionMs: Long) {
        if (isInitialized) {
            val seconds = positionMs / 1000.0
            nativeSeekToTime(seconds)
            updateState { it.copy(currentPositionMs = positionMs) }
        }
    }
    
    // ========================================================================
    // Speed Control - EXTENDED RANGE: 0.05x to 10.0x
    // ========================================================================
    
    /**
     * Set playback speed (0.05x to 10.0x)
     * This exceeds all competition (typically 0.25x - 4x)
     */
    fun setSpeed(speed: Float) {
        if (!isInitialized) return
        
        val clampedSpeed = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        nativeSetSpeed(clampedSpeed)
        updateState { it.copy(speed = clampedSpeed) }
    }
    
    /**
     * Increment speed by given amount
     */
    fun incrementSpeed(delta: Float) {
        setSpeed(_playbackState.value.speed + delta)
    }
    
    /**
     * Get current speed
     */
    fun getSpeed(): Float {
        return if (isInitialized) nativeGetSpeed() else 1.0f
    }
    
    // ========================================================================
    // Pitch Control - EXTENDED RANGE: -36 to +36 semitones
    // ========================================================================
    
    /**
     * Set pitch shift in semitones (-36 to +36)
     * This is 3 octaves - far exceeding typical ±12
     */
    fun setPitchSemitones(semitones: Float) {
        if (!isInitialized) return
        
        val clamped = semitones.coerceIn(MIN_PITCH, MAX_PITCH)
        nativeSetPitchSemitones(clamped)
        updateState { it.copy(pitchSemitones = clamped) }
    }
    
    /**
     * Set fine pitch adjustment in cents (-100 to +100)
     * This provides 0.01 semitone precision
     */
    fun setPitchCents(cents: Float) {
        if (!isInitialized) return
        
        val clamped = cents.coerceIn(-100f, 100f)
        nativeSetPitchCents(clamped)
        updateState { it.copy(pitchCents = clamped) }
    }
    
    /**
     * Set total pitch (semitones + cents combined)
     */
    fun setTotalPitch(semitones: Float, cents: Float) {
        setPitchSemitones(semitones)
        setPitchCents(cents)
    }
    
    /**
     * Increment pitch by semitones
     */
    fun incrementPitch(deltaSemitones: Float) {
        setPitchSemitones(_playbackState.value.pitchSemitones + deltaSemitones)
    }
    
    /**
     * Get current pitch in semitones
     */
    fun getPitchSemitones(): Float {
        return if (isInitialized) nativeGetPitchSemitones() else 0f
    }
    
    // ========================================================================
    // Formant Control
    // ========================================================================
    
    /**
     * Set formant shift (independent of pitch)
     */
    fun setFormantShift(shift: Float) {
        if (!isInitialized) return
        val clamped = shift.coerceIn(MIN_FORMANT, MAX_FORMANT)
        nativeSetFormantShift(clamped)
    }
    
    /**
     * Enable/disable formant preservation
     * When enabled, vocals sound natural even with large pitch shifts
     */
    fun setPreserveFormants(preserve: Boolean) {
        if (isInitialized) {
            nativeSetPreserveFormants(preserve)
        }
    }
    
    // ========================================================================
    // Quality Settings
    // ========================================================================
    
    /**
     * Set quality mode
     */
    fun setQualityMode(mode: QualityMode) {
        if (isInitialized) {
            nativeSetQualityMode(mode.value)
        }
    }
    
    /**
     * Set processing algorithm
     */
    fun setAlgorithm(algorithm: Algorithm) {
        if (isInitialized) {
            nativeSetAlgorithm(algorithm.value)
        }
    }
    
    // ========================================================================
    // Loop Control
    // ========================================================================
    
    /**
     * Set A-B loop region
     */
    fun setLoopRegion(startMs: Long, endMs: Long) {
        if (!isInitialized) return
        
        val startFrame = (startMs * sampleRate / 1000).toLong()
        val endFrame = (endMs * sampleRate / 1000).toLong()
        nativeSetLoopRegion(startFrame, endFrame)
        updateState { it.copy(loopStartMs = startMs, loopEndMs = endMs) }
    }
    
    /**
     * Enable/disable loop
     */
    fun enableLoop(enable: Boolean) {
        if (isInitialized) {
            nativeEnableLoop(enable)
            updateState { it.copy(isLooping = enable) }
        }
    }
    
    // ========================================================================
    // State Queries
    // ========================================================================
    
    fun isPlaying(): Boolean = if (isInitialized) nativeIsPlaying() else false
    
    fun getCurrentPositionMs(): Long {
        if (!isInitialized) return 0
        return (nativeGetCurrentTimeSeconds() * 1000).toLong()
    }
    
    fun getDurationMs(): Long {
        if (!isInitialized) return 0
        return (nativeGetTotalTimeSeconds() * 1000).toLong()
    }
    
    // ========================================================================
    // BPM
    // ========================================================================
    
    /**
     * Detect BPM of current audio
     */
    fun detectBPM(): Float {
        return if (isInitialized) nativeDetectBPM() else 0f
    }
    
    /**
     * Set target BPM (auto-adjusts speed)
     */
    fun setTargetBPM(bpm: Float) {
        if (isInitialized) {
            nativeSetTargetBPM(bpm)
        }
    }
    
    // ========================================================================
    // Export
    // ========================================================================
    
    /**
     * Export processed audio to file
     */
    fun exportToFile(outputPath: String, format: String = "wav"): Boolean {
        return if (isInitialized) nativeExportToFile(outputPath, format) else false
    }
    
    // ========================================================================
    // Presets
    // ========================================================================
    
    /**
     * Apply Nightcore preset (faster + higher pitch)
     */
    fun applyNightcorePreset() {
        setSpeed(1.25f)
        setPitchSemitones(4f)
    }
    
    /**
     * Apply Slowed/Daycore preset (slower + lower pitch)
     */
    fun applySlowedPreset() {
        setSpeed(0.8f)
        setPitchSemitones(-3f)
    }
    
    /**
     * Apply Vaporwave preset
     */
    fun applyVaporwavePreset() {
        setSpeed(0.7f)
        setPitchSemitones(-5f)
    }
    
    /**
     * Reset to default (normal playback)
     */
    fun resetToDefault() {
        setSpeed(1.0f)
        setPitchSemitones(0f)
        setPitchCents(0f)
        setFormantShift(0f)
    }
    
    // ========================================================================
    // Internal
    // ========================================================================
    
    private fun updateState(update: (PlaybackState) -> PlaybackState) {
        _playbackState.value = update(_playbackState.value)
    }
    
    // ========================================================================
    // Native Methods
    // ========================================================================
    
    // Lifecycle
    private external fun nativeInit(sampleRate: Int, channelCount: Int): Boolean
    private external fun nativeShutdown()
    
    // Loading
    private external fun nativeLoadFile(filePath: String): Boolean
    private external fun nativeUnload()
    
    // Playback
    private external fun nativePlay()
    private external fun nativePause()
    private external fun nativeStop()
    private external fun nativeSeekTo(framePosition: Long)
    private external fun nativeSeekToTime(seconds: Double)
    
    // Speed (0.05 - 10.0)
    private external fun nativeSetSpeed(speed: Float)
    private external fun nativeGetSpeed(): Float
    
    // Pitch (-36 to +36 semitones)
    private external fun nativeSetPitchSemitones(semitones: Float)
    private external fun nativeSetPitchCents(cents: Float)
    private external fun nativeGetPitchSemitones(): Float
    
    // Formant
    private external fun nativeSetFormantShift(shift: Float)
    private external fun nativeSetPreserveFormants(preserve: Boolean)
    
    // Quality
    private external fun nativeSetQualityMode(mode: Int)
    private external fun nativeSetAlgorithm(algorithm: Int)
    
    // Loop
    private external fun nativeSetLoopRegion(startFrame: Long, endFrame: Long)
    private external fun nativeEnableLoop(enable: Boolean)
    
    // State
    private external fun nativeIsPlaying(): Boolean
    private external fun nativeGetCurrentPosition(): Long
    private external fun nativeGetTotalFrames(): Long
    private external fun nativeGetCurrentTimeSeconds(): Double
    private external fun nativeGetTotalTimeSeconds(): Double
    
    // BPM
    private external fun nativeDetectBPM(): Float
    private external fun nativeSetTargetBPM(bpm: Float)
    
    // Export
    private external fun nativeExportToFile(outputPath: String, format: String): Boolean
}
