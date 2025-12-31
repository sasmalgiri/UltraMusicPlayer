package com.ultramusic.player.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio Focus Manager for Battle Mode
 *
 * Handles interruptions from:
 * - Phone calls (AUDIOFOCUS_LOSS)
 * - Notifications (AUDIOFOCUS_LOSS_TRANSIENT)
 * - Other apps taking focus (AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)
 * - Voice assistants
 *
 * Battle Mode Strategy:
 * - LOSS: Pause immediately, save state for resume
 * - LOSS_TRANSIENT: Pause, auto-resume when focus returns
 * - LOSS_TRANSIENT_CAN_DUCK: In battle mode, DON'T duck (we need full power!)
 *                            In normal mode, duck volume to 30%
 *
 * DOMINANT MODE (DJ Mode):
 * - IGNORES ALL focus loss events - app keeps playing through EVERYTHING
 * - Phone calls, notifications, other apps - nothing stops the music
 * - Only user explicitly stopping the app will pause playback
 * - Uses AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE for aggressive focus acquisition
 */
@Singleton
class AudioFocusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioFocusManager"
        private const val DUCK_VOLUME = 0.3f  // 30% volume when ducking
        private const val PREFS_NAME = "audio_focus_settings"
        private const val KEY_DOMINANT_MODE = "dominant_mode_enabled"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // State
    private val _hasFocus = MutableStateFlow(false)
    val hasFocus: StateFlow<Boolean> = _hasFocus.asStateFlow()

    private val _focusState = MutableStateFlow(FocusState.NONE)
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    // Callbacks
    private var onFocusLost: (() -> Unit)? = null
    private var onFocusGained: (() -> Unit)? = null
    private var onDuckVolume: ((Float) -> Unit)? = null
    private var onRestoreVolume: (() -> Unit)? = null

    // Battle mode - when enabled, we never duck
    private var battleModeEnabled = false

    // Dominant mode (DJ Mode) - when enabled, we IGNORE ALL focus loss events
    // The app keeps playing through calls, notifications, everything!
    // Load from prefs on startup - DEFAULT TO TRUE for better UX (music shouldn't stop randomly)
    private var dominantModeEnabled = prefs.getBoolean(KEY_DOMINANT_MODE, true)

    // Expose dominant mode state for UI
    private val _isDominantMode = MutableStateFlow(prefs.getBoolean(KEY_DOMINANT_MODE, true))
    val isDominantMode: StateFlow<Boolean> = _isDominantMode.asStateFlow()

    // Remember if we were playing before losing focus
    private var wasPlayingBeforeLoss = false

    // Audio focus request (for Android O+)
    private var focusRequest: AudioFocusRequest? = null

    // Audio focus listener
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.i(TAG, "AUDIOFOCUS_GAIN - Resuming playback")
                _hasFocus.value = true
                _focusState.value = FocusState.FOCUSED

                // Restore volume first
                onRestoreVolume?.invoke()

                // Auto-resume if we were playing before
                if (wasPlayingBeforeLoss) {
                    onFocusGained?.invoke()
                    wasPlayingBeforeLoss = false
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss (e.g., phone call started, another music app)
                if (dominantModeEnabled) {
                    // DOMINANT MODE: IGNORE - keep playing through everything!
                    Log.w(TAG, "AUDIOFOCUS_LOSS ignored - DOMINANT MODE ACTIVE! Music continues!")
                    // DO NOT pause, DO NOT update state, DO NOT call callback
                } else {
                    Log.w(TAG, "AUDIOFOCUS_LOSS - Pausing playback (permanent)")
                    _hasFocus.value = false
                    _focusState.value = FocusState.LOST
                    wasPlayingBeforeLoss = true
                    onFocusLost?.invoke()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss (e.g., notification, voice message)
                if (dominantModeEnabled) {
                    // DOMINANT MODE: IGNORE - keep playing!
                    Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT ignored - DOMINANT MODE ACTIVE!")
                    // DO NOT pause
                } else {
                    Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT - Pausing temporarily")
                    _hasFocus.value = false
                    _focusState.value = FocusState.LOST_TRANSIENT
                    wasPlayingBeforeLoss = true
                    onFocusLost?.invoke()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Can duck (lower volume) - e.g., navigation instruction
                if (dominantModeEnabled || battleModeEnabled) {
                    // DOMINANT/BATTLE MODE: NEVER duck - full power always!
                    Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ignored - ${if (dominantModeEnabled) "DOMINANT" else "BATTLE"} MODE! FULL POWER!")
                    // Keep playing at full volume
                } else {
                    Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK - Ducking volume")
                    _focusState.value = FocusState.DUCKED
                    onDuckVolume?.invoke(DUCK_VOLUME)
                }
            }
        }
    }

    /**
     * Set callbacks for focus events
     */
    fun setCallbacks(
        onLost: () -> Unit,
        onGained: () -> Unit,
        onDuck: (Float) -> Unit,
        onRestore: () -> Unit
    ) {
        onFocusLost = onLost
        onFocusGained = onGained
        onDuckVolume = onDuck
        onRestoreVolume = onRestore
    }

    /**
     * Request audio focus for playback
     * In dominant mode, uses AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE for aggressive focus
     */
    fun requestFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ uses AudioFocusRequest
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            // Use exclusive focus in dominant mode for aggressive audio control
            val focusGainType = if (dominantModeEnabled) {
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            } else {
                AudioManager.AUDIOFOCUS_GAIN
            }

            focusRequest = AudioFocusRequest.Builder(focusGainType)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(!battleModeEnabled && !dominantModeEnabled)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            // Legacy API - use GAIN_TRANSIENT_EXCLUSIVE in dominant mode
            @Suppress("DEPRECATION")
            val focusGainType = if (dominantModeEnabled) {
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            } else {
                AudioManager.AUDIOFOCUS_GAIN
            }
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                focusGainType
            )
        }

        val success = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        _hasFocus.value = success
        _focusState.value = if (success) FocusState.FOCUSED else FocusState.NONE

        Log.i(TAG, "Focus request ${if (success) "GRANTED" else "DENIED"} (${if (dominantModeEnabled) "DOMINANT" else "normal"} mode)")
        return success
    }

    /**
     * Abandon audio focus
     */
    fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }

        _hasFocus.value = false
        _focusState.value = FocusState.NONE
        wasPlayingBeforeLoss = false

        Log.i(TAG, "Audio focus abandoned")
    }

    /**
     * Enable/disable battle mode
     * In battle mode:
     * - We NEVER duck (notifications won't reduce our volume)
     * - We pause for calls but auto-resume immediately when call ends
     */
    fun setBattleMode(enabled: Boolean) {
        battleModeEnabled = enabled
        Log.i(TAG, "Battle mode ${if (enabled) "ENABLED - Full power, no interruptions!" else "DISABLED"}")

        // Re-request focus with updated settings if we had focus
        if (_hasFocus.value) {
            abandonFocus()
            requestFocus()
        }
    }

    /**
     * Enable/disable dominant mode (DJ Mode)
     * In dominant mode:
     * - App IGNORES ALL focus loss events
     * - Keeps playing through phone calls, notifications, other apps
     * - Only user explicitly stopping will pause playback
     * - Uses exclusive audio focus for aggressive control
     *
     * Setting is persisted to SharedPreferences
     */
    fun setDominantMode(enabled: Boolean) {
        dominantModeEnabled = enabled
        _isDominantMode.value = enabled

        // Persist to SharedPreferences
        prefs.edit().putBoolean(KEY_DOMINANT_MODE, enabled).apply()

        Log.i(TAG, "Dominant mode ${if (enabled) "ENABLED - NOTHING STOPS THE MUSIC!" else "DISABLED - Normal audio focus"}")

        // Re-request focus with updated settings if we had focus
        if (_hasFocus.value) {
            abandonFocus()
            requestFocus()
        }
    }

    /**
     * Check if dominant mode is currently enabled
     */
    fun isDominantModeEnabled(): Boolean = dominantModeEnabled

    /**
     * Check if we should auto-resume after interruption
     */
    fun shouldAutoResume(): Boolean {
        return wasPlayingBeforeLoss && _focusState.value == FocusState.FOCUSED
    }

    /**
     * Call this when playback starts
     */
    fun onPlaybackStarted() {
        wasPlayingBeforeLoss = false
    }

    /**
     * Call this when playback is manually stopped
     */
    fun onPlaybackStopped() {
        wasPlayingBeforeLoss = false
    }
}

/**
 * Audio focus states
 */
enum class FocusState {
    NONE,           // No focus requested
    FOCUSED,        // We have full focus
    LOST,           // Lost focus permanently (pause, don't auto-resume)
    LOST_TRANSIENT, // Lost focus temporarily (pause, auto-resume)
    DUCKED          // Still playing but at reduced volume
}
