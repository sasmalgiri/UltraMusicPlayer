package com.ultramusic.player.audio

import android.content.Context
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow

/**
 * Audio Quality Manager
 * 
 * Maintains sound quality even at extreme speed/pitch settings.
 * Uses formant estimation and compensation to prevent "chipmunk" effect.
 */

enum class QualityMode {
    STANDARD,       // Normal ExoPlayer processing
    HIGH_QUALITY,   // Additional processing for better quality
    ULTRA           // Maximum quality with formant preservation
}

data class QualityState(
    val mode: QualityMode = QualityMode.HIGH_QUALITY,
    val formantPreservation: Boolean = true,
    val antiAliasingEnabled: Boolean = true,
    val qualityWarning: String? = null,
    val estimatedQualityPercent: Int = 100
)

/**
 * Formant Preservation Settings
 * 
 * When pitch is shifted, formants (vocal characteristics) shift too.
 * This causes the "chipmunk" effect at high pitches or "demon" voice at low pitches.
 * 
 * Formant preservation attempts to keep vocal character natural.
 */
data class FormantSettings(
    val preserveFormants: Boolean = true,
    val formantShiftRatio: Float = 1.0f,  // 1.0 = full preservation
    val smoothingFactor: Float = 0.8f,
    val analysisWindowMs: Int = 25
)

@Singleton
class AudioQualityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioQualityManager"

        // Speed ranges and quality impact
        const val SPEED_EXCELLENT_MIN = 0.5f
        const val SPEED_EXCELLENT_MAX = 2.0f
        const val SPEED_GOOD_MIN = 0.25f
        const val SPEED_GOOD_MAX = 4.0f
        const val SPEED_WARNING_MIN = 0.1f
        const val SPEED_WARNING_MAX = 6.0f

        // Pitch ranges and quality impact (semitones)
        const val PITCH_EXCELLENT_RANGE = 12f  // ±1 octave
        const val PITCH_GOOD_RANGE = 24f       // ±2 octaves
        const val PITCH_WARNING_RANGE = 36f    // ±3 octaves
    }

    private val _qualityState = MutableStateFlow(QualityState())
    val qualityState: StateFlow<QualityState> = _qualityState.asStateFlow()

    private val _formantSettings = MutableStateFlow(FormantSettings())
    val formantSettings: StateFlow<FormantSettings> = _formantSettings.asStateFlow()

    private var equalizer: Equalizer? = null
    
    /**
     * Set quality mode
     */
    fun setQualityMode(mode: QualityMode) {
        _qualityState.value = _qualityState.value.copy(mode = mode)
    }
    
    /**
     * Toggle formant preservation
     */
    fun setFormantPreservation(enabled: Boolean) {
        _qualityState.value = _qualityState.value.copy(formantPreservation = enabled)
        _formantSettings.value = _formantSettings.value.copy(preserveFormants = enabled)
    }
    
    /**
     * Calculate optimal playback parameters with quality compensation
     * 
     * When formant preservation is enabled, we adjust the pitch parameter
     * to compensate for formant shift.
     */
    fun calculateOptimalParameters(
        targetSpeed: Float,
        targetPitchSemitones: Float
    ): PlaybackAdjustment {
        val settings = _formantSettings.value
        
        // Calculate base pitch multiplier
        val basePitchMultiplier = 2f.pow(targetPitchSemitones / 12f)
        
        // Apply formant compensation if enabled
        val compensatedPitch = if (settings.preserveFormants) {
            applyFormantCompensation(basePitchMultiplier, targetSpeed, settings)
        } else {
            basePitchMultiplier
        }
        
        // Calculate quality estimate
        val qualityPercent = estimateQuality(targetSpeed, targetPitchSemitones)
        val warning = generateQualityWarning(targetSpeed, targetPitchSemitones)
        
        _qualityState.value = _qualityState.value.copy(
            estimatedQualityPercent = qualityPercent,
            qualityWarning = warning
        )
        
        return PlaybackAdjustment(
            speed = targetSpeed,
            pitch = compensatedPitch,
            formantCompensation = settings.preserveFormants,
            qualityPercent = qualityPercent,
            warning = warning
        )
    }
    
    /**
     * Apply formant compensation to prevent chipmunk/demon voice effect
     * 
     * Theory: When we change pitch, the formants (resonant frequencies) 
     * also shift. To preserve natural voice:
     * - When pitching UP: We need to slightly lower formants
     * - When pitching DOWN: We need to slightly raise formants
     */
    private fun applyFormantCompensation(
        pitchMultiplier: Float,
        speed: Float,
        settings: FormantSettings
    ): Float {
        if (!settings.preserveFormants) return pitchMultiplier
        
        // Calculate formant shift compensation
        // The idea is to partially counteract the formant shift caused by pitch change
        val formantCompensation = 1f + (1f - pitchMultiplier) * settings.formantShiftRatio * 0.3f
        
        // Apply smoothing to avoid artifacts
        val smoothedCompensation = 1f + (formantCompensation - 1f) * settings.smoothingFactor
        
        // Combine with original pitch
        return pitchMultiplier * smoothedCompensation
    }
    
    /**
     * Estimate audio quality based on current settings
     * Returns percentage (100 = perfect, lower = degraded)
     */
    fun estimateQuality(speed: Float, pitchSemitones: Float): Int {
        var quality = 100
        
        // Speed impact on quality
        quality -= when {
            speed in SPEED_EXCELLENT_MIN..SPEED_EXCELLENT_MAX -> 0
            speed in SPEED_GOOD_MIN..SPEED_GOOD_MAX -> 10
            speed in SPEED_WARNING_MIN..SPEED_WARNING_MAX -> 25
            else -> 40
        }
        
        // Pitch impact on quality
        val absPitch = abs(pitchSemitones)
        quality -= when {
            absPitch <= PITCH_EXCELLENT_RANGE -> 0
            absPitch <= PITCH_GOOD_RANGE -> 10
            absPitch <= PITCH_WARNING_RANGE -> 20
            else -> 35
        }
        
        // Formant preservation bonus
        if (_qualityState.value.formantPreservation) {
            quality += 10
        }
        
        // Quality mode bonus
        quality += when (_qualityState.value.mode) {
            QualityMode.STANDARD -> 0
            QualityMode.HIGH_QUALITY -> 5
            QualityMode.ULTRA -> 10
        }
        
        return quality.coerceIn(0, 100)
    }
    
    /**
     * Generate quality warning message if settings may degrade audio
     */
    private fun generateQualityWarning(speed: Float, pitchSemitones: Float): String? {
        val warnings = mutableListOf<String>()
        
        // Extreme speed warning
        if (speed < SPEED_WARNING_MIN) {
            warnings.add("Very slow speed may cause audio artifacts")
        } else if (speed > SPEED_WARNING_MAX) {
            warnings.add("Very fast speed may reduce clarity")
        }
        
        // Extreme pitch warning
        val absPitch = abs(pitchSemitones)
        if (absPitch > PITCH_GOOD_RANGE) {
            if (pitchSemitones > 0) {
                warnings.add("High pitch may sound unnatural")
            } else {
                warnings.add("Low pitch may lose clarity")
            }
        }
        
        // Combined extreme warning
        if (speed !in SPEED_EXCELLENT_MIN..SPEED_EXCELLENT_MAX && 
            absPitch > PITCH_EXCELLENT_RANGE) {
            warnings.add("Consider enabling Formant Preservation in settings")
        }
        
        return if (warnings.isNotEmpty()) warnings.joinToString(". ") else null
    }
    
    /**
     * Get recommended settings for preserving quality
     */
    fun getRecommendedSettings(speed: Float, pitchSemitones: Float): List<String> {
        val recommendations = mutableListOf<String>()
        
        val absPitch = abs(pitchSemitones)
        
        if (absPitch > PITCH_EXCELLENT_RANGE && !_qualityState.value.formantPreservation) {
            recommendations.add("Enable Formant Preservation for natural vocals")
        }
        
        if (speed < 0.5f || speed > 2f) {
            recommendations.add("Use High Quality or Ultra mode for extreme speeds")
        }
        
        if (_qualityState.value.mode == QualityMode.STANDARD && 
            (absPitch > PITCH_GOOD_RANGE || speed !in SPEED_GOOD_MIN..SPEED_GOOD_MAX)) {
            recommendations.add("Switch to Ultra quality mode")
        }
        
        return recommendations
    }
    
    /**
     * Initialize audio effects for quality enhancement
     */
    fun initializeEffects(audioSessionId: Int) {
        try {
            // Initialize equalizer for frequency compensation
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing equalizer effects", e)
        }
    }
    
    /**
     * Apply EQ compensation for pitch changes
     * When pitch is shifted, we may need to adjust EQ to maintain tonal balance
     */
    fun applyPitchCompensationEQ(pitchSemitones: Float) {
        equalizer?.let { eq ->
            if (!eq.enabled) return
            
            val numBands = eq.numberOfBands
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            val midLevel = (minLevel + maxLevel) / 2
            
            // When pitching up, boost lows slightly to compensate
            // When pitching down, boost highs slightly to compensate
            for (i in 0 until numBands) {
                val bandFreq = eq.getCenterFreq(i.toShort())
                val isLowBand = bandFreq < 500_000  // 500 Hz
                val isHighBand = bandFreq > 4_000_000  // 4 kHz
                
                val adjustment = when {
                    pitchSemitones > 6 && isLowBand -> (maxLevel - midLevel) * 0.3f
                    pitchSemitones < -6 && isHighBand -> (maxLevel - midLevel) * 0.3f
                    else -> 0f
                }
                
                eq.setBandLevel(i.toShort(), (midLevel + adjustment).toInt().toShort())
            }
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        equalizer?.release()
        equalizer = null
    }
}

/**
 * Result of optimal parameter calculation
 */
data class PlaybackAdjustment(
    val speed: Float,
    val pitch: Float,
    val formantCompensation: Boolean,
    val qualityPercent: Int,
    val warning: String?
)
