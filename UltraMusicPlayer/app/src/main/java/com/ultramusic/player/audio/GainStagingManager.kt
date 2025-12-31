package com.ultramusic.player.audio

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Gain Staging Manager
 *
 * Coordinates all gain sources to prevent cumulative clipping.
 * Implements automatic gain reduction (AGR) when total gain exceeds safe limits.
 *
 * This solves the audio quality degradation issue when:
 * - Bass boost is maxed (adds ~12dB)
 * - Loudness enhancer is maxed (adds ~10dB)
 * - EQ bands are boosted (adds variable dB)
 * - Compressor makeup gain is high (adds up to 24dB)
 *
 * Without coordination, these can stack to 40+ dB which causes severe clipping.
 */
@Singleton
class GainStagingManager @Inject constructor() {

    companion object {
        private const val TAG = "GainStagingManager"

        // Maximum total gain before AGR kicks in (dB)
        const val MAX_TOTAL_GAIN_DB = 12f

        // Safety margin added to gain reduction (dB)
        const val SAFETY_MARGIN_DB = 3f

        // Maximum gain reduction to apply (dB)
        const val MAX_REDUCTION_DB = 24f

        // Gain contribution estimates per effect at max setting
        const val BASS_BOOST_MAX_GAIN_DB = 12f  // At strength 1000
        const val LOUDNESS_MAX_GAIN_DB = 10f     // At 1000 mB
        const val EQ_MAX_GAIN_DB = 15f           // Per band at max
        const val COMPRESSOR_MAKEUP_MAX_DB = 24f // Direct dB value
        const val EXCITER_MAX_GAIN_DB = 6f       // Perceived loudness increase
    }

    // ==================== SAFE MODE (AGR BYPASS) ====================

    private val _safeMode = MutableStateFlow(true) // Default: Safe Mode ON
    val safeMode: StateFlow<Boolean> = _safeMode.asStateFlow()

    /**
     * Toggle Safe Mode (AGR)
     *
     * Safe Mode ON (default): AGR reduces gain to prevent clipping
     * Safe Mode OFF (FULL SEND): 100% raw gain, no reduction - YOU CONTROL EVERYTHING
     *
     * WARNING: Safe Mode OFF can cause:
     * - Audio clipping/distortion
     * - Speaker damage at high volumes
     * - But also MAXIMUM POWER for battle competitions
     */
    fun setSafeMode(enabled: Boolean) {
        _safeMode.value = enabled
        if (enabled) {
            // Re-enable AGR - recalculate gain reduction
            recalculateGain()
            Log.i(TAG, "SAFE MODE ON - AGR enabled, clipping protection active")
        } else {
            // FULL SEND MODE - bypass AGR completely
            _gainReductionDb.value = 0f
            _isAGRActive.value = false
            _isClippingPrevented.value = false
            Log.w(TAG, "SAFE MODE OFF - FULL SEND! No gain reduction, maximum power!")
        }
    }

    // Gain sources (in dB)
    private var bassBoostGainDb = 0f
    private var eqPeakGainDb = 0f  // Peak EQ gain across all bands
    private var loudnessGainDb = 0f
    private var compressorMakeupGainDb = 0f
    private var exciterGainDb = 0f

    // Current state
    private val _totalGainDb = MutableStateFlow(0f)
    val totalGainDb: StateFlow<Float> = _totalGainDb.asStateFlow()

    private val _gainReductionDb = MutableStateFlow(0f)
    val gainReductionDb: StateFlow<Float> = _gainReductionDb.asStateFlow()

    private val _isClippingPrevented = MutableStateFlow(false)
    val isClippingPrevented: StateFlow<Boolean> = _isClippingPrevented.asStateFlow()

    private val _isAGRActive = MutableStateFlow(false)
    val isAGRActive: StateFlow<Boolean> = _isAGRActive.asStateFlow()

    /**
     * Update bass boost contribution
     * @param strength 0-1000 bass boost strength
     */
    fun updateBassBoost(strength: Int) {
        // Scale: 0-1000 maps to 0-12dB
        bassBoostGainDb = (strength / 1000f) * BASS_BOOST_MAX_GAIN_DB
        recalculateGain()
    }

    /**
     * Update EQ contribution (peak band gain)
     * @param peakBandLevelMb Peak EQ band level in millibels (can be negative)
     */
    fun updateEQGain(peakBandLevelMb: Int) {
        // Only count positive gains (boosts), not cuts
        eqPeakGainDb = if (peakBandLevelMb > 0) peakBandLevelMb / 100f else 0f
        recalculateGain()
    }

    /**
     * Update loudness enhancer contribution
     * @param gainMb Loudness gain in millibels (0-1000)
     */
    fun updateLoudnessGain(gainMb: Int) {
        // 1000 mB = 10 dB
        loudnessGainDb = gainMb / 100f
        recalculateGain()
    }

    /**
     * Update compressor makeup gain
     * @param gainDb Makeup gain in dB (0-24)
     */
    fun updateCompressorMakeup(gainDb: Float) {
        compressorMakeupGainDb = gainDb.coerceIn(0f, COMPRESSOR_MAKEUP_MAX_DB)
        recalculateGain()
    }

    /**
     * Update exciter contribution
     * @param drive Exciter drive 0-100
     * @param mix Exciter mix 0-100
     */
    fun updateExciterGain(drive: Int, mix: Int) {
        // Exciter adds harmonics which increase perceived loudness
        // Approximate as 0-6dB based on drive and mix
        exciterGainDb = (drive / 100f) * (mix / 100f) * EXCITER_MAX_GAIN_DB
        recalculateGain()
    }

    /**
     * Recalculate total gain and required reduction
     */
    private fun recalculateGain() {
        // Sum all gain sources
        // Note: These don't perfectly add in real audio, but this is a safe approximation
        val total = bassBoostGainDb + eqPeakGainDb + loudnessGainDb +
                   compressorMakeupGainDb + exciterGainDb

        _totalGainDb.value = total

        // SAFE MODE CHECK: If Safe Mode is OFF, skip all gain reduction
        if (!_safeMode.value) {
            // FULL SEND MODE - no gain reduction at all
            _gainReductionDb.value = 0f
            _isClippingPrevented.value = false
            _isAGRActive.value = false
            Log.d(TAG, "FULL SEND: total=${String.format("%.1f", total)}dB - NO REDUCTION")
            return
        }

        // Calculate required gain reduction (only when Safe Mode is ON)
        val excessGain = total - MAX_TOTAL_GAIN_DB
        if (excessGain > 0) {
            // Add safety margin to ensure we don't clip
            _gainReductionDb.value = min(excessGain + SAFETY_MARGIN_DB, MAX_REDUCTION_DB)
            _isClippingPrevented.value = true
            _isAGRActive.value = true
            Log.d(TAG, "AGR active: total=${String.format("%.1f", total)}dB, " +
                      "reduction=${String.format("%.1f", _gainReductionDb.value)}dB")
        } else {
            _gainReductionDb.value = 0f
            _isClippingPrevented.value = false
            _isAGRActive.value = false
        }
    }

    /**
     * Get adjusted loudness gain that accounts for other gain sources
     * This is the primary method for preventing clipping
     *
     * @param requestedGainMb Requested loudness gain in millibels
     * @return Adjusted gain in millibels that won't cause clipping
     */
    fun getAdjustedLoudnessGain(requestedGainMb: Int): Int {
        val reductionMb = (_gainReductionDb.value * 100).toInt()
        val adjusted = (requestedGainMb - reductionMb).coerceAtLeast(0)

        if (reductionMb > 0) {
            Log.d(TAG, "Loudness adjusted: requested=${requestedGainMb}mB, " +
                      "adjusted=${adjusted}mB (reduced by ${reductionMb}mB)")
        }
        return adjusted
    }

    /**
     * Get adjusted EQ level that accounts for other gain sources
     * Only applies reduction to positive (boost) values
     *
     * @param requestedLevelMb Requested EQ level in millibels
     * @return Adjusted level in millibels
     */
    fun getAdjustedEQLevel(requestedLevelMb: Int): Int {
        // Don't adjust cuts (negative values)
        if (requestedLevelMb <= 0) return requestedLevelMb

        // Apply half the reduction to EQ (it's less critical than loudness)
        val reductionMb = (_gainReductionDb.value * 100 * 0.5f).toInt()
        return (requestedLevelMb - reductionMb).coerceAtLeast(0)
    }

    /**
     * Get adjusted bass boost level
     *
     * @param requestedLevel Requested bass boost level (0-1000)
     * @return Adjusted level that won't cause clipping
     */
    fun getAdjustedBassBoost(requestedLevel: Int): Int {
        // Apply proportional reduction based on gain reduction
        val reductionFactor = 1f - (_gainReductionDb.value / MAX_REDUCTION_DB * 0.3f)
        return (requestedLevel * reductionFactor).toInt().coerceIn(0, 1000)
    }

    /**
     * Check if a given total gain would be safe
     * Useful for UI indicators
     */
    fun isGainSafe(additionalGainDb: Float = 0f): Boolean {
        return (_totalGainDb.value + additionalGainDb) <= MAX_TOTAL_GAIN_DB
    }

    /**
     * Get headroom remaining before AGR activates
     */
    fun getHeadroomDb(): Float {
        return (MAX_TOTAL_GAIN_DB - _totalGainDb.value).coerceAtLeast(0f)
    }

    /**
     * Reset all gain tracking to zero
     * Note: Does NOT reset safeMode - that's a user preference
     */
    fun reset() {
        bassBoostGainDb = 0f
        eqPeakGainDb = 0f
        loudnessGainDb = 0f
        compressorMakeupGainDb = 0f
        exciterGainDb = 0f
        _totalGainDb.value = 0f
        _gainReductionDb.value = 0f
        _isClippingPrevented.value = false
        _isAGRActive.value = false
        // Keep safeMode at current value - it's a user preference
        Log.d(TAG, "Gain staging reset (safeMode=${_safeMode.value})")
    }

    /**
     * Get a summary of current gain state for debugging
     */
    fun getGainSummary(): String {
        return buildString {
            append("Gain Summary:\n")
            append("  Bass Boost: ${String.format("%.1f", bassBoostGainDb)}dB\n")
            append("  EQ Peak: ${String.format("%.1f", eqPeakGainDb)}dB\n")
            append("  Loudness: ${String.format("%.1f", loudnessGainDb)}dB\n")
            append("  Compressor: ${String.format("%.1f", compressorMakeupGainDb)}dB\n")
            append("  Exciter: ${String.format("%.1f", exciterGainDb)}dB\n")
            append("  TOTAL: ${String.format("%.1f", _totalGainDb.value)}dB\n")
            append("  Reduction: ${String.format("%.1f", _gainReductionDb.value)}dB\n")
            append("  AGR Active: ${_isAGRActive.value}")
        }
    }
}
