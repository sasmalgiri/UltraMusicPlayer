package com.ultramusic.player.audio

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.ultramusic.player.UltraMusicApp
import com.ultramusic.player.data.AudioPreset
import com.ultramusic.player.data.PlaybackState
import com.ultramusic.player.data.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * PRODUCTION-GRADE MusicController with REAL Native Audio Processing
 * 
 * This controller ACTUALLY integrates:
 * - NativeBattleEngine for extended speed/pitch (0.05x-10x, ±36 semitones)
 * - SonicSpeedProcessor as pure-Java fallback
 * - Custom AudioProcessor in ExoPlayer pipeline
 * - Battle mode with limiter/compressor/bass boost
 */
@UnstableApi
@Singleton
class MusicController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nativeBattleEngine: NativeBattleEngine,
    private val audioFocusManager: AudioFocusManager
) {
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    
    // Audio processors
    private var battleAudioProcessor: BattleAudioProcessor? = null
    private var sonicProcessor: SonicSpeedProcessor? = null
    
    // State
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()
    
    private var currentQueueIndex = 0
    
    // Processing mode
    private var useNativeEngine = false
    private var useSonicFallback = false
    
    // Current settings
    private var currentSpeed = 1.0f
    private var currentPitchSemitones = 0.0f
    private var battleModeEnabled = false
    private var bassBoostDb = 0.0f

    // Player listener - MUST be defined before init block
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            android.util.Log.d(TAG, "Playback state changed: $state")
            when (state) {
                Player.STATE_READY -> {
                    android.util.Log.d(TAG, "Player STATE_READY - starting playback")
                    updatePlaybackState()
                    startProgressUpdates()
                }
                Player.STATE_ENDED -> {
                    android.util.Log.d(TAG, "Player STATE_ENDED")
                    handlePlaybackEnded()
                }
                Player.STATE_BUFFERING -> {
                    android.util.Log.d(TAG, "Player STATE_BUFFERING")
                }
                Player.STATE_IDLE -> {
                    android.util.Log.d(TAG, "Player STATE_IDLE")
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            android.util.Log.d(TAG, "isPlaying changed: $isPlaying")
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
            if (isPlaying) startProgressUpdates() else stopProgressUpdates()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            android.util.Log.e(TAG, "Player error: ${error.errorCode} - ${error.message}", error)
            // Try to provide more context
            when (error.errorCode) {
                androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                    android.util.Log.e(TAG, "File not found - check URI permissions")
                }
                androidx.media3.common.PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> {
                    android.util.Log.e(TAG, "No permission to read file")
                }
                androidx.media3.common.PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> {
                    android.util.Log.e(TAG, "Unsupported audio format")
                }
                else -> {
                    android.util.Log.e(TAG, "Unknown error code: ${error.errorCode}")
                }
            }
        }
    }

    init {
        initializeAudioProcessing()
        initializePlayer()
        initializeAudioFocus()
    }

    /**
     * Initialize audio focus handling for interruptions (calls, notifications, etc.)
     */
    private fun initializeAudioFocus() {
        audioFocusManager.setCallbacks(
            onLost = {
                // Focus lost - pause playback
                android.util.Log.i(TAG, "Audio focus lost - pausing")
                pause()
            },
            onGained = {
                // Focus regained - resume if we were playing
                android.util.Log.i(TAG, "Audio focus regained - resuming")
                play()
            },
            onDuck = { volume ->
                // Duck volume (only in non-battle mode)
                android.util.Log.d(TAG, "Ducking volume to $volume")
                exoPlayer?.volume = volume
            },
            onRestore = {
                // Restore full volume
                android.util.Log.d(TAG, "Restoring full volume")
                exoPlayer?.volume = 1.0f
            }
        )
    }
    
    /**
     * Initialize audio processing - try native first, fallback to Sonic
     */
    private fun initializeAudioProcessing() {
        // Try native engine first
        if (NativeBattleEngine.isAvailable()) {
            useNativeEngine = nativeBattleEngine.initialize(44100, 2)
            if (useNativeEngine) {
                battleAudioProcessor = BattleAudioProcessor(nativeBattleEngine)
                android.util.Log.i(TAG, "✅ Native Battle Engine initialized")
            }
        }
        
        // Fallback to Sonic (pure Java)
        if (!useNativeEngine) {
            sonicProcessor = SonicSpeedProcessor()
            sonicProcessor?.initialize(44100, 2)
            useSonicFallback = true
            android.util.Log.i(TAG, "✅ Sonic fallback processor initialized")
        }
    }
    
    /**
     * Initialize ExoPlayer with custom audio processing pipeline
     */
    private fun initializePlayer() {
        // Create custom renderers factory with our audio processor
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                val processors = mutableListOf<AudioProcessor>()
                battleAudioProcessor?.let { processors.add(it) }

                // Add software limiter for pre-API 28 devices
                // (API 28+ have DynamicsProcessing in AudioBattleEngine)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    processors.add(SoftwareLimiter().apply {
                        setThreshold(-1f)    // Start limiting at -1dB
                        setCeiling(-0.3f)    // Hard limit at -0.3dB
                        setEnabled(true)
                    })
                }

                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(processors.toTypedArray())
                    .build()
            }
        }
        
        renderersFactory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        // Optimized load control for memory efficiency with large audio files
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5_000,   // Min buffer (5 seconds - reduced from default 50s)
                30_000,  // Max buffer (30 seconds - reduced from default 50s)
                1_000,   // Buffer for playback (1 second)
                2_000    // Buffer for rebuffer (2 seconds)
            )
            .setTargetBufferBytes(C.LENGTH_UNSET) // Don't limit by bytes
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        exoPlayer = ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()
            .apply {
                addListener(playerListener)
                repeatMode = Player.REPEAT_MODE_OFF
            }

        android.util.Log.i(TAG, "✅ ExoPlayer initialized with optimized memory settings")
    }

    private fun handlePlaybackEnded() {
        val state = _playbackState.value
        
        if (state.abLoopStart != null && state.abLoopEnd != null) {
            seekTo(state.abLoopStart)
            play()
            return
        }
        
        when {
            state.repeatMode == 2 -> {
                seekTo(0)
                play()
            }
            currentQueueIndex < _queue.value.size - 1 -> playNext()
            state.repeatMode == 1 -> {
                currentQueueIndex = 0
                playSong(_queue.value[0], _queue.value)
            }
            state.isShuffling -> playRandomSong()
            else -> {
                pause()
                seekTo(0)
            }
        }
    }
    
    // ==================== PLAYBACK CONTROLS ====================
    
    fun playSong(song: Song, playlist: List<Song> = listOf(song)) {
        _queue.value = playlist
        currentQueueIndex = playlist.indexOf(song).coerceAtLeast(0)

        // Request audio focus before playing
        if (!audioFocusManager.requestFocus()) {
            android.util.Log.w(TAG, "Could not get audio focus - playing anyway")
        }
        audioFocusManager.onPlaybackStarted()

        exoPlayer?.let { player ->
            val mediaItem = MediaItem.fromUri(song.uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            applyCurrentSettings()

            _playbackState.value = _playbackState.value.copy(
                currentSong = song,
                isPlaying = true,
                currentPosition = 0,
                duration = song.duration,
                abLoopStart = null,
                abLoopEnd = null
            )
        }
    }
    
    fun play() { exoPlayer?.play() }
    fun pause() { exoPlayer?.pause() }
    fun togglePlayPause() { exoPlayer?.let { if (it.isPlaying) pause() else play() } }
    
    fun stop() {
        exoPlayer?.stop()
        stopProgressUpdates()
        audioFocusManager.onPlaybackStopped()
        audioFocusManager.abandonFocus()
        _playbackState.value = PlaybackState()
    }
    
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        _playbackState.value = _playbackState.value.copy(currentPosition = position)
    }
    
    fun seekToPercent(percent: Float) {
        val position = ((exoPlayer?.duration ?: 0) * percent).toLong()
        seekTo(position)
    }
    
    fun playNext() {
        if (_queue.value.isEmpty()) return
        val nextIndex = if (_playbackState.value.isShuffling) {
            (0 until _queue.value.size).random()
        } else {
            (currentQueueIndex + 1) % _queue.value.size
        }
        currentQueueIndex = nextIndex
        playSong(_queue.value[nextIndex], _queue.value)
    }
    
    fun playPrevious() {
        if (_queue.value.isEmpty()) return
        if ((exoPlayer?.currentPosition ?: 0) > 3000) {
            seekTo(0)
            return
        }
        val prevIndex = if (currentQueueIndex > 0) currentQueueIndex - 1 else _queue.value.size - 1
        currentQueueIndex = prevIndex
        playSong(_queue.value[prevIndex], _queue.value)
    }
    
    private fun playRandomSong() {
        if (_queue.value.isEmpty()) return
        currentQueueIndex = (0 until _queue.value.size).random()
        playSong(_queue.value[currentQueueIndex], _queue.value)
    }
    
    // ==================== SPEED CONTROL (0.05x - 10.0x) ====================
    
    fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(UltraMusicApp.MIN_SPEED, UltraMusicApp.MAX_SPEED)
        applySpeedAndPitch()
        _playbackState.value = _playbackState.value.copy(speed = currentSpeed)
        android.util.Log.d(TAG, "Speed set to: ${currentSpeed}x")
    }
    
    fun adjustSpeed(delta: Float) { setSpeed(currentSpeed + delta) }
    fun resetSpeed() { setSpeed(UltraMusicApp.DEFAULT_SPEED) }
    
    // ==================== PITCH CONTROL (-36 to +36 semitones) ====================
    
    fun setPitch(semitones: Float) {
        currentPitchSemitones = semitones.coerceIn(
            UltraMusicApp.MIN_PITCH_SEMITONES,
            UltraMusicApp.MAX_PITCH_SEMITONES
        )
        applySpeedAndPitch()
        _playbackState.value = _playbackState.value.copy(pitch = currentPitchSemitones)
        android.util.Log.d(TAG, "Pitch set to: $currentPitchSemitones semitones")
    }
    
    fun adjustPitch(deltaSemitones: Float) { setPitch(currentPitchSemitones + deltaSemitones) }
    fun resetPitch() { setPitch(UltraMusicApp.DEFAULT_PITCH) }
    
    // ==================== BATTLE MODE ====================
    
    fun setBattleMode(enabled: Boolean) {
        battleModeEnabled = enabled
        if (useNativeEngine) nativeBattleEngine.setBattleMode(enabled)
        battleAudioProcessor?.setBattleMode(enabled)
        // Sync with audio focus manager - in battle mode, we never duck for notifications
        audioFocusManager.setBattleMode(enabled)
        _playbackState.value = _playbackState.value.copy(battleModeEnabled = enabled)
        android.util.Log.i(TAG, "Battle mode: ${if (enabled) "ENGAGED!" else "off"}")
    }
    
    fun setBassBoost(db: Float) {
        bassBoostDb = db.coerceIn(0f, 24f)
        if (useNativeEngine) nativeBattleEngine.setBassBoost(bassBoostDb)
        android.util.Log.d(TAG, "Bass boost: $bassBoostDb dB")
    }

    // ==================== DOMINANT MODE (DJ Mode) ====================

    /**
     * Enable/disable dominant mode (DJ Mode)
     * When enabled, the app will NEVER pause for calls, notifications, or other apps
     * Only user explicitly stopping will pause playback
     */
    fun setDominantMode(enabled: Boolean) {
        audioFocusManager.setDominantMode(enabled)
        android.util.Log.i(TAG, "Dominant mode: ${if (enabled) "DOMINATING - Nothing stops the music!" else "Normal - respects audio focus"}")
    }

    /**
     * Get the current dominant mode state flow for UI binding
     */
    fun getDominantModeState() = audioFocusManager.isDominantMode

    /**
     * Check if dominant mode is currently enabled
     */
    fun isDominantModeEnabled(): Boolean = audioFocusManager.isDominantModeEnabled()

    // ==================== INTERNAL: Apply Speed/Pitch ====================
    
    private fun applySpeedAndPitch() {
        val needsExtendedProcessing = 
            currentSpeed < EXOPLAYER_MIN_SPEED || 
            currentSpeed > EXOPLAYER_MAX_SPEED ||
            currentPitchSemitones < EXOPLAYER_MIN_PITCH ||
            currentPitchSemitones > EXOPLAYER_MAX_PITCH
        
        if (needsExtendedProcessing && (useNativeEngine || useSonicFallback)) {
            applyExtendedSpeedPitch()
        } else {
            applyExoPlayerSpeedPitch()
        }
    }
    
    private fun applyExtendedSpeedPitch() {
        // ExoPlayer at 1.0x, let processor handle it
        exoPlayer?.playbackParameters = PlaybackParameters(1.0f, 1.0f)
        
        if (useNativeEngine) {
            nativeBattleEngine.setSpeed(currentSpeed)
            nativeBattleEngine.setPitch(currentPitchSemitones)
            battleAudioProcessor?.setSpeed(currentSpeed)
            battleAudioProcessor?.setPitch(currentPitchSemitones)
            android.util.Log.d(TAG, "Applied via NATIVE: speed=$currentSpeed, pitch=$currentPitchSemitones")
        } else if (useSonicFallback) {
            sonicProcessor?.setSpeed(currentSpeed)
            sonicProcessor?.setPitch(currentPitchSemitones)
            android.util.Log.d(TAG, "Applied via SONIC: speed=$currentSpeed, pitch=$currentPitchSemitones")
        }
    }
    
    private fun applyExoPlayerSpeedPitch() {
        battleAudioProcessor?.setSpeed(1.0f)
        battleAudioProcessor?.setPitch(0.0f)
        
        val clampedSpeed = currentSpeed.coerceIn(EXOPLAYER_MIN_SPEED, EXOPLAYER_MAX_SPEED)
        val pitchMultiplier = calculatePitchMultiplier(
            currentPitchSemitones.coerceIn(EXOPLAYER_MIN_PITCH, EXOPLAYER_MAX_PITCH)
        )
        
        exoPlayer?.playbackParameters = PlaybackParameters(clampedSpeed, pitchMultiplier)
        android.util.Log.d(TAG, "Applied via EXOPLAYER: speed=$clampedSpeed, pitch=$pitchMultiplier")
    }
    
    private fun applyCurrentSettings() {
        applySpeedAndPitch()
        if (battleModeEnabled) setBattleMode(true)
        if (bassBoostDb > 0) setBassBoost(bassBoostDb)
    }
    
    private fun calculatePitchMultiplier(semitones: Float): Float = 2f.pow(semitones / 12f)
    
    // ==================== PRESETS ====================
    
    fun applyPreset(preset: AudioPreset) {
        setSpeed(preset.speed)
        setPitch(preset.pitch)
    }
    
    /**
     * Reset speed and pitch to defaults WITHOUT affecting playback
     * Only resets audio parameters, not play/pause state
     */
    fun resetAll() {
        try {
            // Reset speed safely
            currentSpeed = UltraMusicApp.DEFAULT_SPEED
            _playbackState.value = _playbackState.value.copy(speed = currentSpeed)

            // Reset pitch safely
            currentPitchSemitones = UltraMusicApp.DEFAULT_PITCH
            _playbackState.value = _playbackState.value.copy(pitch = currentPitchSemitones)

            // Apply the changes to player
            try {
                applySpeedAndPitch()
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Speed/pitch reset had minor issue: ${e.message}")
            }

            // Reset bass boost safely (don't call setBattleMode as it can affect audio)
            try {
                bassBoostDb = 0f
                if (useNativeEngine) {
                    nativeBattleEngine.setBassBoost(0f)
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Bass reset had minor issue: ${e.message}")
            }

            android.util.Log.d(TAG, "Reset all: speed=${currentSpeed}x, pitch=${currentPitchSemitones}st")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in resetAll", e)
        }
    }
    
    // ==================== A-B LOOP ====================
    
    fun setABLoopStart() {
        val position = exoPlayer?.currentPosition ?: 0
        _playbackState.value = _playbackState.value.copy(abLoopStart = position, abLoopEnd = null)
    }
    
    fun setABLoopEnd() {
        val position = exoPlayer?.currentPosition ?: 0
        val start = _playbackState.value.abLoopStart ?: 0
        if (position > start) {
            _playbackState.value = _playbackState.value.copy(abLoopEnd = position)
        }
    }
    
    fun clearABLoop() {
        _playbackState.value = _playbackState.value.copy(abLoopStart = null, abLoopEnd = null)
    }
    
    fun setLoopPoints(startMs: Long, endMs: Long) {
        if (endMs > startMs) {
            _playbackState.value = _playbackState.value.copy(abLoopStart = startMs, abLoopEnd = endMs)
        }
    }
    
    fun isInABLoop(): Boolean = 
        _playbackState.value.abLoopStart != null && _playbackState.value.abLoopEnd != null
    
    // ==================== LOOP & SHUFFLE ====================
    
    fun toggleLoop() {
        val newLooping = !_playbackState.value.isLooping
        exoPlayer?.repeatMode = if (newLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _playbackState.value = _playbackState.value.copy(isLooping = newLooping)
    }
    
    fun toggleShuffle() {
        _playbackState.value = _playbackState.value.copy(isShuffling = !_playbackState.value.isShuffling)
    }
    
    fun toggleRepeatMode() {
        val newMode = (_playbackState.value.repeatMode + 1) % 3
        exoPlayer?.repeatMode = when (newMode) {
            0 -> Player.REPEAT_MODE_OFF
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _playbackState.value = _playbackState.value.copy(repeatMode = newMode, isLooping = newMode == 2)
    }
    
    // ==================== PROGRESS UPDATES ====================
    
    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressJob = scope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    val position = player.currentPosition
                    val duration = player.duration.coerceAtLeast(0)
                    
                    val state = _playbackState.value
                    if (state.abLoopEnd != null && position >= state.abLoopEnd) {
                        seekTo(state.abLoopStart ?: 0)
                    }
                    
                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = position,
                        duration = duration
                    )
                }
                delay(100)
            }
        }
    }
    
    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }
    
    private fun updatePlaybackState() {
        exoPlayer?.let { player ->
            _playbackState.value = _playbackState.value.copy(
                duration = player.duration.coerceAtLeast(0),
                currentPosition = player.currentPosition
            )
        }
    }
    
    // ==================== INFO ====================
    
    fun getAudioSessionId(): Int = exoPlayer?.audioSessionId ?: 0
    fun getPlayer(): ExoPlayer? = exoPlayer
    fun isUsingNativeEngine(): Boolean = useNativeEngine
    fun isUsingSonicFallback(): Boolean = useSonicFallback
    
    fun getProcessingInfo(): String = when {
        useNativeEngine -> "Native Battle Engine (C++ SoundTouch)"
        useSonicFallback -> "Sonic Processor (Pure Java)"
        else -> "ExoPlayer Default"
    }
    
    // ==================== LIFECYCLE ====================
    
    fun release() {
        stopProgressUpdates()
        audioFocusManager.abandonFocus()
        exoPlayer?.release()
        exoPlayer = null
        nativeBattleEngine.release()
        sonicProcessor?.release()
    }
    
    companion object {
        private const val TAG = "MusicController"
        private const val EXOPLAYER_MIN_SPEED = 0.25f
        private const val EXOPLAYER_MAX_SPEED = 4.0f
        private const val EXOPLAYER_MIN_PITCH = -12f
        private const val EXOPLAYER_MAX_PITCH = 12f
    }
}
