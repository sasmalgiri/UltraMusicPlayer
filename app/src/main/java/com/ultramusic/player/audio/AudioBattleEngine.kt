package com.ultramusic.player.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.PresetReverb
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio Battle Engine
 * 
 * Designed for SOUND SYSTEM COMPETITIONS where you need to:
 * 1. OVERPOWER opponent's bass
 * 2. MAXIMIZE loudness without distortion
 * 3. ENHANCE clarity so YOUR sound reaches audience
 * 4. CREATE presence that fills the venue
 * 
 * DSP Chain:
 * Input → EQ → Bass Boost → Loudness → Compressor → Virtualizer → Output
 */
@Singleton
class AudioBattleEngine @Inject constructor(
    private val context: Context
) {
    // ==================== AUDIO EFFECTS ====================
    
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var virtualizer: Virtualizer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    
    // ==================== STATE ====================
    
    private val _battleMode = MutableStateFlow(BattleMode.OFF)
    val battleMode: StateFlow<BattleMode> = _battleMode.asStateFlow()
    
    private val _bassLevel = MutableStateFlow(500) // 0-1000
    val bassLevel: StateFlow<Int> = _bassLevel.asStateFlow()
    
    private val _loudnessGain = MutableStateFlow(0) // 0-1000 mB
    val loudnessGain: StateFlow<Int> = _loudnessGain.asStateFlow()
    
    private val _clarityLevel = MutableStateFlow(50) // 0-100
    val clarityLevel: StateFlow<Int> = _clarityLevel.asStateFlow()
    
    private val _spatialLevel = MutableStateFlow(500) // 0-1000
    val spatialLevel: StateFlow<Int> = _spatialLevel.asStateFlow()
    
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    private val _currentPreset = MutableStateFlow<BattlePreset?>(null)
    val currentPreset: StateFlow<BattlePreset?> = _currentPreset.asStateFlow()
    
    // EQ bands state
    private val _eqBands = MutableStateFlow<List<EQBand>>(emptyList())
    val eqBands: StateFlow<List<EQBand>> = _eqBands.asStateFlow()

    // ==================== ADVANCED MANUAL CONTROLS ====================

    // Compressor settings
    private val _compressorEnabled = MutableStateFlow(true)
    val compressorEnabled: StateFlow<Boolean> = _compressorEnabled.asStateFlow()

    private val _compressorThreshold = MutableStateFlow(-12f) // dB
    val compressorThreshold: StateFlow<Float> = _compressorThreshold.asStateFlow()

    private val _compressorRatio = MutableStateFlow(4f) // ratio
    val compressorRatio: StateFlow<Float> = _compressorRatio.asStateFlow()

    private val _compressorAttack = MutableStateFlow(10f) // ms
    val compressorAttack: StateFlow<Float> = _compressorAttack.asStateFlow()

    private val _compressorRelease = MutableStateFlow(100f) // ms
    val compressorRelease: StateFlow<Float> = _compressorRelease.asStateFlow()

    private val _compressorMakeupGain = MutableStateFlow(6f) // dB
    val compressorMakeupGain: StateFlow<Float> = _compressorMakeupGain.asStateFlow()

    // Limiter settings
    private val _limiterEnabled = MutableStateFlow(true)
    val limiterEnabled: StateFlow<Boolean> = _limiterEnabled.asStateFlow()

    private val _limiterThreshold = MutableStateFlow(-1f) // dB
    val limiterThreshold: StateFlow<Float> = _limiterThreshold.asStateFlow()

    private val _limiterCeiling = MutableStateFlow(-0.1f) // dB
    val limiterCeiling: StateFlow<Float> = _limiterCeiling.asStateFlow()

    private val _limiterAttack = MutableStateFlow(1f) // ms
    val limiterAttack: StateFlow<Float> = _limiterAttack.asStateFlow()

    private val _limiterRelease = MutableStateFlow(50f) // ms
    val limiterRelease: StateFlow<Float> = _limiterRelease.asStateFlow()

    // Bass Boost advanced
    private val _bassFrequency = MutableStateFlow(80f) // Hz - center frequency
    val bassFrequency: StateFlow<Float> = _bassFrequency.asStateFlow()

    // Stereo Widener
    private val _stereoWidthEnabled = MutableStateFlow(false)
    val stereoWidthEnabled: StateFlow<Boolean> = _stereoWidthEnabled.asStateFlow()

    private val _stereoWidth = MutableStateFlow(100) // 0-200, 100 = normal
    val stereoWidth: StateFlow<Int> = _stereoWidth.asStateFlow()

    // Harmonic Exciter
    private val _exciterEnabled = MutableStateFlow(false)
    val exciterEnabled: StateFlow<Boolean> = _exciterEnabled.asStateFlow()

    private val _exciterDrive = MutableStateFlow(30) // 0-100
    val exciterDrive: StateFlow<Int> = _exciterDrive.asStateFlow()

    private val _exciterMix = MutableStateFlow(50) // 0-100 dry/wet
    val exciterMix: StateFlow<Int> = _exciterMix.asStateFlow()

    // Reverb settings
    private val _reverbEnabled = MutableStateFlow(false)
    val reverbEnabled: StateFlow<Boolean> = _reverbEnabled.asStateFlow()

    private val _reverbPreset = MutableStateFlow(0) // PresetReverb presets
    val reverbPreset: StateFlow<Int> = _reverbPreset.asStateFlow()

    private var presetReverb: PresetReverb? = null

    // ==================== DANGER MODE (LIMITER BYPASS) ====================

    private val _dangerModeEnabled = MutableStateFlow(false)
    val dangerModeEnabled: StateFlow<Boolean> = _dangerModeEnabled.asStateFlow()

    // ==================== PEAK dB MONITORING ====================

    private val _currentPeakDb = MutableStateFlow(-60f)
    val currentPeakDb: StateFlow<Float> = _currentPeakDb.asStateFlow()

    private val _isClipping = MutableStateFlow(false)
    val isClipping: StateFlow<Boolean> = _isClipping.asStateFlow()

    // ==================== QUICK PROFILE SLOTS (A/B/C) ====================

    private val _profileSlotA = MutableStateFlow<QuickProfile?>(null)
    val profileSlotA: StateFlow<QuickProfile?> = _profileSlotA.asStateFlow()

    private val _profileSlotB = MutableStateFlow<QuickProfile?>(null)
    val profileSlotB: StateFlow<QuickProfile?> = _profileSlotB.asStateFlow()

    private val _profileSlotC = MutableStateFlow<QuickProfile?>(null)
    val profileSlotC: StateFlow<QuickProfile?> = _profileSlotC.asStateFlow()

    private val _activeProfileSlot = MutableStateFlow<Char?>(null)
    val activeProfileSlot: StateFlow<Char?> = _activeProfileSlot.asStateFlow()
    
    // ==================== INITIALIZATION ====================
    
    /**
     * Initialize audio effects for the given audio session
     */
    fun initialize(audioSessionId: Int) {
        try {
            // Bass Boost - for that chest-hitting low end
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
                setStrength(500.toShort())
            }
            
            // Equalizer - for frequency sculpting
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
                // Get band info
                val bands = (0 until numberOfBands).map { band ->
                    EQBand(
                        index = band,
                        centerFreq = getCenterFreq(band.toShort()) / 1000, // Hz
                        minLevel = bandLevelRange[0].toInt(),
                        maxLevel = bandLevelRange[1].toInt(),
                        currentLevel = getBandLevel(band.toShort()).toInt()
                    )
                }
                _eqBands.value = bands
            }
            
            // Loudness Enhancer - for maximum SPL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                    enabled = true
                    setTargetGain(500) // 500 mB = 5 dB boost
                }
            }
            
            // Virtualizer - for spatial presence
            virtualizer = Virtualizer(0, audioSessionId).apply {
                enabled = true
                setStrength(500.toShort())
            }
            
            // Dynamics Processing (API 28+) - for compression/limiting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                initDynamicsProcessing(audioSessionId)
            }
            
            _isEnabled.value = true

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing battle engine", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun initDynamicsProcessing(audioSessionId: Int) {
        try {
            val config = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                1,  // channel count
                true,  // pre-EQ
                6,     // pre-EQ bands
                true,  // multi-band compressor
                6,     // MBC bands
                true,  // post-EQ
                6,     // post-EQ bands
                true   // limiter
            ).build()
            
            dynamicsProcessing = DynamicsProcessing(0, audioSessionId, config).apply {
                enabled = true
                
                // Configure limiter to prevent clipping
                val limiter = getLimiterByChannelIndex(0)
                limiter.isEnabled = true
                limiter.attackTime = 1f      // Fast attack
                limiter.releaseTime = 50f    // Medium release
                limiter.ratio = 10f          // Heavy limiting
                limiter.threshold = -1f      // Just below 0 dB
                limiter.postGain = 0f
                setLimiterByChannelIndex(0, limiter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing dynamics processing", e)
        }
    }

    // ==================== BATTLE MODES ====================
    
    /**
     * Set battle mode - pre-configured settings for different scenarios
     */
    fun setBattleMode(mode: BattleMode) {
        _battleMode.value = mode
        
        when (mode) {
            BattleMode.OFF -> {
                disableAll()
            }
            BattleMode.BASS_WARFARE -> {
                // Maximum bass, opponent's speakers will cry
                setBassBoost(1000)
                setLoudness(800)
                applyBassWarfareEQ()
                setVirtualizer(300)
            }
            BattleMode.CLARITY_STRIKE -> {
                // Cut through opponent's muddy sound
                setBassBoost(400)
                setLoudness(600)
                applyClarityEQ()
                setVirtualizer(700)
            }
            BattleMode.FULL_ASSAULT -> {
                // Everything maxed - total domination
                setBassBoost(1000)
                setLoudness(1000)
                applyFullAssaultEQ()
                setVirtualizer(800)
            }
            BattleMode.SPL_MONSTER -> {
                // Pure loudness for SPL meters
                setBassBoost(800)
                setLoudness(1000)
                applySPLEQ()
                setVirtualizer(200)
            }
            BattleMode.CROWD_REACH -> {
                // Optimized for large venue coverage
                setBassBoost(600)
                setLoudness(700)
                applyCrowdReachEQ()
                setVirtualizer(1000)
            }
            BattleMode.MAXIMUM_IMPACT -> {
                // Maximum power - everything at peak
                setBassBoost(1000)
                setLoudness(1000)
                applyFullAssaultEQ()
                setVirtualizer(1000)
            }
            BattleMode.BALANCED_BATTLE -> {
                // Balanced approach for general battles
                setBassBoost(700)
                setLoudness(700)
                applyBassWarfareEQ()
                setVirtualizer(500)
            }
            BattleMode.INDOOR_BATTLE -> {
                // Indoor optimized - less bass reflection
                setBassBoost(600)
                setLoudness(800)
                applyClarityEQ()
                setVirtualizer(400)
            }
        }
    }
    
    // ==================== BASS CONTROL ====================
    
    /**
     * Set bass boost level (0-1000)
     * Higher = more chest-hitting bass
     */
    fun setBassBoost(level: Int) {
        val clamped = level.coerceIn(0, 1000)
        _bassLevel.value = clamped
        
        bassBoost?.let {
            if (it.strengthSupported) {
                it.setStrength(clamped.toShort())
            }
        }
        
        // Also boost low EQ bands for extra impact
        boostLowFrequencies(clamped)
    }
    
    private fun boostLowFrequencies(level: Int) {
        equalizer?.let { eq ->
            val boost = (level / 1000f * 10).toInt() // 0-10 dB boost
            
            // Boost sub-bass (band 0, ~60Hz)
            if (eq.numberOfBands > 0) {
                val subBassBoost = (boost * 1.5).toInt().coerceAtMost(15)
                val maxLevel = eq.bandLevelRange[1]
                eq.setBandLevel(0, (subBassBoost * 100).toShort().coerceAtMost(maxLevel))
            }
            
            // Boost bass (band 1, ~230Hz)
            if (eq.numberOfBands > 1) {
                val maxLevel = eq.bandLevelRange[1]
                eq.setBandLevel(1, (boost * 100).toShort().coerceAtMost(maxLevel))
            }
        }
    }
    
    // ==================== LOUDNESS CONTROL ====================
    
    /**
     * Set loudness enhancement (0-1000 mB)
     * 1000 mB = 10 dB boost
     * WARNING: Can cause distortion at high levels
     */
    fun setLoudness(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1000)
        _loudnessGain.value = clamped
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            loudnessEnhancer?.setTargetGain(clamped)
        }
    }
    
    // ==================== CLARITY CONTROL ====================
    
    /**
     * Set clarity level (0-100)
     * Boosts mid-high frequencies for vocal/instrument presence
     */
    fun setClarity(level: Int) {
        val clamped = level.coerceIn(0, 100)
        _clarityLevel.value = clamped
        
        equalizer?.let { eq ->
            val boost = (clamped / 100f * 8).toInt() // 0-8 dB
            
            // Boost presence (band 3, ~3.6kHz)
            if (eq.numberOfBands > 3) {
                val maxLevel = eq.bandLevelRange[1]
                eq.setBandLevel(3, (boost * 100).toShort().coerceAtMost(maxLevel))
            }
            
            // Boost air (band 4, ~14kHz)
            if (eq.numberOfBands > 4) {
                val maxLevel = eq.bandLevelRange[1]
                eq.setBandLevel(4, (boost * 80).toShort().coerceAtMost(maxLevel))
            }
        }
    }
    
    // ==================== SPATIAL CONTROL ====================
    
    /**
     * Set virtualizer/spatial level (0-1000)
     * Creates wider soundstage to fill the venue
     */
    fun setVirtualizer(level: Int) {
        val clamped = level.coerceIn(0, 1000)
        _spatialLevel.value = clamped
        
        virtualizer?.let {
            if (it.strengthSupported) {
                it.setStrength(clamped.toShort())
            }
        }
    }
    
    // ==================== EQUALIZER PRESETS ====================
    
    private fun applyBassWarfareEQ() {
        // Massive low end, reduced mids to avoid muddiness
        equalizer?.let { eq ->
            val max = eq.bandLevelRange[1]
            val min = eq.bandLevelRange[0]
            
            // Sub-bass: MAX
            if (eq.numberOfBands > 0) eq.setBandLevel(0, max)
            // Bass: HIGH
            if (eq.numberOfBands > 1) eq.setBandLevel(1, (max * 0.8).toInt().toShort())
            // Low-mids: REDUCED (avoid muddiness)
            if (eq.numberOfBands > 2) eq.setBandLevel(2, (min * 0.3).toInt().toShort())
            // Mids: NEUTRAL
            if (eq.numberOfBands > 3) eq.setBandLevel(3, 0)
            // Highs: SLIGHT BOOST (for definition)
            if (eq.numberOfBands > 4) eq.setBandLevel(4, (max * 0.3).toInt().toShort())
        }
        updateEQState()
    }
    
    private fun applyClarityEQ() {
        // Cuts through opponent's sound
        equalizer?.let { eq ->
            val max = eq.bandLevelRange[1]
            
            // Sub-bass: MODERATE
            if (eq.numberOfBands > 0) eq.setBandLevel(0, (max * 0.5).toInt().toShort())
            // Bass: MODERATE
            if (eq.numberOfBands > 1) eq.setBandLevel(1, (max * 0.4).toInt().toShort())
            // Low-mids: CUT (remove mud)
            if (eq.numberOfBands > 2) eq.setBandLevel(2, (-300).toShort())
            // Presence: BOOST (cut through)
            if (eq.numberOfBands > 3) eq.setBandLevel(3, (max * 0.7).toInt().toShort())
            // Air: BOOST (sparkle)
            if (eq.numberOfBands > 4) eq.setBandLevel(4, (max * 0.6).toInt().toShort())
        }
        updateEQState()
    }
    
    private fun applyFullAssaultEQ() {
        // Everything boosted strategically
        equalizer?.let { eq ->
            val max = eq.bandLevelRange[1]
            
            // Sub-bass: MAX
            if (eq.numberOfBands > 0) eq.setBandLevel(0, max)
            // Bass: HIGH
            if (eq.numberOfBands > 1) eq.setBandLevel(1, (max * 0.9).toInt().toShort())
            // Low-mids: SLIGHT CUT
            if (eq.numberOfBands > 2) eq.setBandLevel(2, (-200).toShort())
            // Presence: HIGH
            if (eq.numberOfBands > 3) eq.setBandLevel(3, (max * 0.8).toInt().toShort())
            // Air: HIGH
            if (eq.numberOfBands > 4) eq.setBandLevel(4, (max * 0.7).toInt().toShort())
        }
        updateEQState()
    }
    
    private fun applySPLEQ() {
        // Optimized for SPL meter readings
        equalizer?.let { eq ->
            val max = eq.bandLevelRange[1]
            
            // Sub-bass: MAXIMUM (SPL meters love this)
            if (eq.numberOfBands > 0) eq.setBandLevel(0, max)
            // Bass: MAXIMUM
            if (eq.numberOfBands > 1) eq.setBandLevel(1, max)
            // Low-mids: HIGH
            if (eq.numberOfBands > 2) eq.setBandLevel(2, (max * 0.6).toInt().toShort())
            // Presence: MODERATE
            if (eq.numberOfBands > 3) eq.setBandLevel(3, (max * 0.3).toInt().toShort())
            // Air: LOW (doesn't help SPL)
            if (eq.numberOfBands > 4) eq.setBandLevel(4, 0)
        }
        updateEQState()
    }
    
    private fun applyCrowdReachEQ() {
        // Optimized for sound traveling across venue
        equalizer?.let { eq ->
            val max = eq.bandLevelRange[1]
            
            // Sub-bass: HIGH (travels far)
            if (eq.numberOfBands > 0) eq.setBandLevel(0, (max * 0.8).toInt().toShort())
            // Bass: MODERATE
            if (eq.numberOfBands > 1) eq.setBandLevel(1, (max * 0.5).toInt().toShort())
            // Low-mids: CUT (mud doesn't travel)
            if (eq.numberOfBands > 2) eq.setBandLevel(2, (-400).toShort())
            // Presence: HIGH (voices cut through)
            if (eq.numberOfBands > 3) eq.setBandLevel(3, (max * 0.9).toInt().toShort())
            // Air: HIGH (sparkle travels)
            if (eq.numberOfBands > 4) eq.setBandLevel(4, (max * 0.8).toInt().toShort())
        }
        updateEQState()
    }
    
    // ==================== MANUAL EQ CONTROL ====================
    
    /**
     * Set individual EQ band level
     */
    fun setEQBand(bandIndex: Int, level: Int) {
        equalizer?.let { eq ->
            if (bandIndex < eq.numberOfBands) {
                val clamped = level.coerceIn(
                    eq.bandLevelRange[0].toInt(),
                    eq.bandLevelRange[1].toInt()
                )
                eq.setBandLevel(bandIndex.toShort(), clamped.toShort())
                updateEQState()
            }
        }
    }
    
    private fun updateEQState() {
        equalizer?.let { eq ->
            val bands = (0 until eq.numberOfBands).map { band ->
                EQBand(
                    index = band,
                    centerFreq = eq.getCenterFreq(band.toShort()) / 1000,
                    minLevel = eq.bandLevelRange[0].toInt(),
                    maxLevel = eq.bandLevelRange[1].toInt(),
                    currentLevel = eq.getBandLevel(band.toShort()).toInt()
                )
            }
            _eqBands.value = bands
        }
    }

    // ==================== ADVANCED MANUAL CONTROL METHODS ====================

    // ----- COMPRESSOR CONTROLS -----

    fun setCompressorEnabled(enabled: Boolean) {
        _compressorEnabled.value = enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsCompressor()
        }
    }

    fun setCompressorThreshold(thresholdDb: Float) {
        _compressorThreshold.value = thresholdDb.coerceIn(-60f, 0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsCompressor()
        }
    }

    fun setCompressorRatio(ratio: Float) {
        _compressorRatio.value = ratio.coerceIn(1f, 20f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsCompressor()
        }
    }

    fun setCompressorAttack(attackMs: Float) {
        _compressorAttack.value = attackMs.coerceIn(0.1f, 200f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsCompressor()
        }
    }

    fun setCompressorRelease(releaseMs: Float) {
        _compressorRelease.value = releaseMs.coerceIn(10f, 1000f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsCompressor()
        }
    }

    fun setCompressorMakeupGain(gainDb: Float) {
        _compressorMakeupGain.value = gainDb.coerceIn(0f, 24f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsCompressor()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateDynamicsCompressor() {
        dynamicsProcessing?.let { dp ->
            try {
                // Update MBC (Multi-band Compressor) for each band
                for (band in 0 until 6) {
                    val mbc = dp.getMbcBandByChannelIndex(0, band)
                    mbc.isEnabled = _compressorEnabled.value
                    mbc.attackTime = _compressorAttack.value
                    mbc.releaseTime = _compressorRelease.value
                    mbc.ratio = _compressorRatio.value
                    mbc.threshold = _compressorThreshold.value
                    mbc.postGain = _compressorMakeupGain.value
                    dp.setMbcBandByChannelIndex(0, band, mbc)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating compressor", e)
            }
        }
    }

    // ----- LIMITER CONTROLS -----

    fun setLimiterEnabled(enabled: Boolean) {
        _limiterEnabled.value = enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsLimiter()
        }
    }

    fun setLimiterThreshold(thresholdDb: Float) {
        _limiterThreshold.value = thresholdDb.coerceIn(-12f, 0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsLimiter()
        }
    }

    fun setLimiterCeiling(ceilingDb: Float) {
        _limiterCeiling.value = ceilingDb.coerceIn(-3f, 0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsLimiter()
        }
    }

    fun setLimiterAttack(attackMs: Float) {
        _limiterAttack.value = attackMs.coerceIn(0.1f, 10f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsLimiter()
        }
    }

    fun setLimiterRelease(releaseMs: Float) {
        _limiterRelease.value = releaseMs.coerceIn(10f, 500f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsLimiter()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun updateDynamicsLimiter() {
        dynamicsProcessing?.let { dp ->
            try {
                val limiter = dp.getLimiterByChannelIndex(0)
                limiter.isEnabled = _limiterEnabled.value
                limiter.attackTime = _limiterAttack.value
                limiter.releaseTime = _limiterRelease.value
                limiter.threshold = _limiterThreshold.value
                limiter.postGain = 0f // Ceiling is handled separately
                dp.setLimiterByChannelIndex(0, limiter)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating limiter", e)
            }
        }
    }

    // ----- BASS FREQUENCY CONTROL -----

    fun setBassFrequency(frequencyHz: Float) {
        _bassFrequency.value = frequencyHz.coerceIn(20f, 200f)
        // Re-apply bass boost with new frequency
        setBassBoost(_bassLevel.value)
    }

    // ----- STEREO WIDENER CONTROLS -----

    fun setStereoWidthEnabled(enabled: Boolean) {
        _stereoWidthEnabled.value = enabled
        if (enabled) {
            virtualizer?.enabled = true
            virtualizer?.let {
                if (it.strengthSupported) {
                    // Map stereo width to virtualizer strength
                    val strength = ((_stereoWidth.value - 100) * 10).coerceIn(0, 1000)
                    it.setStrength(strength.toShort())
                }
            }
        }
    }

    fun setStereoWidth(width: Int) {
        _stereoWidth.value = width.coerceIn(0, 200)
        if (_stereoWidthEnabled.value) {
            virtualizer?.let {
                if (it.strengthSupported) {
                    // 0-100 = narrow to normal, 100-200 = normal to wide
                    val strength = ((width - 100) * 10).coerceIn(0, 1000)
                    it.setStrength(strength.toShort())
                }
            }
        }
    }

    // ----- HARMONIC EXCITER CONTROLS -----

    fun setExciterEnabled(enabled: Boolean) {
        _exciterEnabled.value = enabled
        // Exciter is implemented via EQ boost in high frequencies + slight saturation
        if (enabled) {
            applyExciterEffect()
        }
    }

    fun setExciterDrive(drive: Int) {
        _exciterDrive.value = drive.coerceIn(0, 100)
        if (_exciterEnabled.value) {
            applyExciterEffect()
        }
    }

    fun setExciterMix(mix: Int) {
        _exciterMix.value = mix.coerceIn(0, 100)
        if (_exciterEnabled.value) {
            applyExciterEffect()
        }
    }

    private fun applyExciterEffect() {
        // Exciter adds harmonics to high frequencies for "presence"
        // We simulate this by boosting high-mid and high frequencies
        equalizer?.let { eq ->
            val drive = _exciterDrive.value / 100f
            val mix = _exciterMix.value / 100f
            val max = eq.bandLevelRange[1]

            // Boost presence (3-6kHz range, typically band 3)
            if (eq.numberOfBands > 3) {
                val boost = (drive * mix * max * 0.5f).toInt()
                eq.setBandLevel(3, boost.toShort().coerceAtMost(max))
            }
            // Boost air/brilliance (8-16kHz range, typically band 4)
            if (eq.numberOfBands > 4) {
                val boost = (drive * mix * max * 0.7f).toInt()
                eq.setBandLevel(4, boost.toShort().coerceAtMost(max))
            }
        }
        updateEQState()
    }

    // ----- REVERB CONTROLS -----

    fun setReverbEnabled(enabled: Boolean) {
        _reverbEnabled.value = enabled
        presetReverb?.enabled = enabled
    }

    fun setReverbPreset(preset: Int) {
        _reverbPreset.value = preset.coerceIn(0, 6)
        presetReverb?.preset = preset.toShort()
    }

    fun initReverb(audioSessionId: Int) {
        try {
            presetReverb = PresetReverb(0, audioSessionId).apply {
                enabled = _reverbEnabled.value
                preset = _reverbPreset.value.toShort()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing reverb", e)
        }
    }

    // ----- DANGER MODE CONTROLS -----

    /**
     * Enable DANGER MODE - bypasses limiter for maximum output
     * WARNING: This can cause clipping and speaker damage!
     * Use only when you need to overpower opponent at all costs
     */
    fun setDangerModeEnabled(enabled: Boolean) {
        _dangerModeEnabled.value = enabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dynamicsProcessing?.let { dp ->
                try {
                    val limiter = dp.getLimiterByChannelIndex(0)
                    if (enabled) {
                        // BYPASS LIMITER - dangerous!
                        limiter.isEnabled = false
                    } else {
                        // Re-enable limiter with current settings
                        limiter.isEnabled = _limiterEnabled.value
                        limiter.threshold = _limiterThreshold.value
                    }
                    dp.setLimiterByChannelIndex(0, limiter)
                } catch (e: Exception) {
                    Log.e(TAG, "Error toggling danger mode", e)
                }
            }
        }
    }

    // ----- PEAK dB MONITORING -----

    /**
     * Update peak dB level (called from audio processing callback)
     */
    fun updatePeakDb(peakDb: Float) {
        _currentPeakDb.value = peakDb
        _isClipping.value = peakDb >= -0.5f // Clipping threshold
    }

    /**
     * Reset peak dB monitor
     */
    fun resetPeakDb() {
        _currentPeakDb.value = -60f
        _isClipping.value = false
    }

    // ----- QUICK PROFILE SLOTS -----

    /**
     * Save current settings to a profile slot (A, B, or C)
     */
    fun saveToProfileSlot(slot: Char) {
        val profile = captureCurrentProfile()
        when (slot) {
            'A' -> _profileSlotA.value = profile
            'B' -> _profileSlotB.value = profile
            'C' -> _profileSlotC.value = profile
        }
    }

    /**
     * Load settings from a profile slot
     */
    fun loadFromProfileSlot(slot: Char) {
        val profile = when (slot) {
            'A' -> _profileSlotA.value
            'B' -> _profileSlotB.value
            'C' -> _profileSlotC.value
            else -> null
        }

        profile?.let {
            applyQuickProfile(it)
            _activeProfileSlot.value = slot
        }
    }

    /**
     * Clear a profile slot
     */
    fun clearProfileSlot(slot: Char) {
        when (slot) {
            'A' -> _profileSlotA.value = null
            'B' -> _profileSlotB.value = null
            'C' -> _profileSlotC.value = null
        }
        if (_activeProfileSlot.value == slot) {
            _activeProfileSlot.value = null
        }
    }

    private fun captureCurrentProfile(): QuickProfile {
        return QuickProfile(
            name = "Profile ${System.currentTimeMillis() % 1000}",
            bassLevel = _bassLevel.value,
            bassFrequency = _bassFrequency.value,
            loudnessGain = _loudnessGain.value,
            clarityLevel = _clarityLevel.value,
            spatialLevel = _spatialLevel.value,
            compressorEnabled = _compressorEnabled.value,
            compressorThreshold = _compressorThreshold.value,
            compressorRatio = _compressorRatio.value,
            compressorAttack = _compressorAttack.value,
            compressorRelease = _compressorRelease.value,
            compressorMakeupGain = _compressorMakeupGain.value,
            limiterEnabled = _limiterEnabled.value,
            limiterThreshold = _limiterThreshold.value,
            limiterCeiling = _limiterCeiling.value,
            stereoWidthEnabled = _stereoWidthEnabled.value,
            stereoWidth = _stereoWidth.value,
            exciterEnabled = _exciterEnabled.value,
            exciterDrive = _exciterDrive.value,
            exciterMix = _exciterMix.value,
            eqBands = _eqBands.value.map { it.currentLevel }
        )
    }

    private fun applyQuickProfile(profile: QuickProfile) {
        // Apply all settings from profile
        setBassBoost(profile.bassLevel)
        setBassFrequency(profile.bassFrequency)
        setLoudness(profile.loudnessGain)
        setClarity(profile.clarityLevel)
        setVirtualizer(profile.spatialLevel)

        setCompressorEnabled(profile.compressorEnabled)
        setCompressorThreshold(profile.compressorThreshold)
        setCompressorRatio(profile.compressorRatio)
        setCompressorAttack(profile.compressorAttack)
        setCompressorRelease(profile.compressorRelease)
        setCompressorMakeupGain(profile.compressorMakeupGain)

        setLimiterEnabled(profile.limiterEnabled)
        setLimiterThreshold(profile.limiterThreshold)
        setLimiterCeiling(profile.limiterCeiling)

        setStereoWidthEnabled(profile.stereoWidthEnabled)
        setStereoWidth(profile.stereoWidth)

        setExciterEnabled(profile.exciterEnabled)
        setExciterDrive(profile.exciterDrive)
        setExciterMix(profile.exciterMix)

        // Apply EQ bands
        profile.eqBands.forEachIndexed { index, level ->
            setEQBand(index, level)
        }
    }

    // ----- RESET ALL TO DEFAULTS -----

    fun resetAllToDefaults() {
        // Reset compressor
        _compressorEnabled.value = true
        _compressorThreshold.value = -12f
        _compressorRatio.value = 4f
        _compressorAttack.value = 10f
        _compressorRelease.value = 100f
        _compressorMakeupGain.value = 6f

        // Reset limiter
        _limiterEnabled.value = true
        _limiterThreshold.value = -1f
        _limiterCeiling.value = -0.1f
        _limiterAttack.value = 1f
        _limiterRelease.value = 50f

        // Reset bass
        _bassLevel.value = 500
        _bassFrequency.value = 80f

        // Reset stereo
        _stereoWidthEnabled.value = false
        _stereoWidth.value = 100

        // Reset exciter
        _exciterEnabled.value = false
        _exciterDrive.value = 30
        _exciterMix.value = 50

        // Reset reverb
        _reverbEnabled.value = false
        _reverbPreset.value = 0

        // Reset main controls
        _loudnessGain.value = 0
        _clarityLevel.value = 50
        _spatialLevel.value = 500

        // Apply resets
        setBassBoost(500)
        setLoudness(0)
        setClarity(50)
        setVirtualizer(500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            updateDynamicsCompressor()
            updateDynamicsLimiter()
        }

        // Reset EQ to flat
        equalizer?.let { eq ->
            for (band in 0 until eq.numberOfBands) {
                eq.setBandLevel(band.toShort(), 0)
            }
        }
        updateEQState()
    }

    // ==================== BATTLE PRESETS ====================
    
    /**
     * Apply a battle preset
     */
    fun applyPreset(preset: BattlePreset) {
        _currentPreset.value = preset
        
        setBassBoost(preset.bassLevel)
        setLoudness(preset.loudnessGain)
        setClarity(preset.clarityLevel)
        setVirtualizer(preset.spatialLevel)
        
        // Apply EQ
        preset.eqBands.forEachIndexed { index, level ->
            setEQBand(index, level)
        }
    }
    
    /**
     * Save current settings as preset
     */
    fun saveAsPreset(name: String): BattlePreset {
        return BattlePreset(
            name = name,
            bassLevel = _bassLevel.value,
            loudnessGain = _loudnessGain.value,
            clarityLevel = _clarityLevel.value,
            spatialLevel = _spatialLevel.value,
            eqBands = _eqBands.value.map { it.currentLevel }
        )
    }
    
    // ==================== QUICK ACTIONS ====================
    
    /**
     * Emergency bass drop - instant maximum bass
     */
    fun emergencyBassBoost() {
        setBassBoost(1000)
        setLoudness(800)
        
        equalizer?.let { eq ->
            val max = eq.bandLevelRange[1]
            if (eq.numberOfBands > 0) eq.setBandLevel(0, max)
            if (eq.numberOfBands > 1) eq.setBandLevel(1, max)
        }
    }
    
    /**
     * Cut through - when opponent is drowning you out
     */
    fun cutThrough() {
        setClarity(100)
        setVirtualizer(800)
        
        equalizer?.let { eq ->
            val max = eq.bandLevelRange[1]
            // Boost presence frequencies that cut through
            if (eq.numberOfBands > 3) eq.setBandLevel(3, max)
            if (eq.numberOfBands > 4) eq.setBandLevel(4, (max * 0.8).toInt().toShort())
        }
    }
    
    /**
     * Maximum everything - last resort
     */
    fun goNuclear() {
        setBassBoost(1000)
        setLoudness(1000)
        setClarity(100)
        setVirtualizer(1000)
        
        equalizer?.let { eq ->
            val max = eq.bandLevelRange[1]
            for (band in 0 until eq.numberOfBands) {
                eq.setBandLevel(band.toShort(), max)
            }
        }
    }
    
    // ==================== UTILITY ====================
    
    private fun disableAll() {
        bassBoost?.enabled = false
        equalizer?.enabled = false
        loudnessEnhancer?.enabled = false
        virtualizer?.enabled = false
        dynamicsProcessing?.enabled = false
        _isEnabled.value = false
    }
    
    fun enable() {
        bassBoost?.enabled = true
        equalizer?.enabled = true
        loudnessEnhancer?.enabled = true
        virtualizer?.enabled = true
        dynamicsProcessing?.enabled = true
        _isEnabled.value = true
    }
    
    fun release() {
        bassBoost?.release()
        equalizer?.release()
        loudnessEnhancer?.release()
        virtualizer?.release()
        dynamicsProcessing?.release()
        
        bassBoost = null
        equalizer = null
        loudnessEnhancer = null
        virtualizer = null
        dynamicsProcessing = null
    }
    
    companion object {
        private const val TAG = "AudioBattleEngine"

        val DEFAULT_PRESETS = listOf(
            BattlePreset(
                name = "🔊 Bass Destroyer",
                bassLevel = 1000,
                loudnessGain = 800,
                clarityLevel = 30,
                spatialLevel = 400,
                eqBands = listOf(1500, 1200, -200, 300, 200)
            ),
            BattlePreset(
                name = "⚔️ Cut Through",
                bassLevel = 400,
                loudnessGain = 600,
                clarityLevel = 100,
                spatialLevel = 700,
                eqBands = listOf(600, 400, -400, 1200, 1000)
            ),
            BattlePreset(
                name = "💀 Full Assault",
                bassLevel = 1000,
                loudnessGain = 1000,
                clarityLevel = 80,
                spatialLevel = 800,
                eqBands = listOf(1500, 1300, -200, 1100, 900)
            ),
            BattlePreset(
                name = "📊 SPL King",
                bassLevel = 1000,
                loudnessGain = 1000,
                clarityLevel = 20,
                spatialLevel = 200,
                eqBands = listOf(1500, 1500, 800, 400, 0)
            ),
            BattlePreset(
                name = "🎯 Crowd Reach",
                bassLevel = 600,
                loudnessGain = 700,
                clarityLevel = 90,
                spatialLevel = 1000,
                eqBands = listOf(1000, 600, -400, 1200, 1100)
            ),
            BattlePreset(
                name = "🌊 Sub Tsunami",
                bassLevel = 1000,
                loudnessGain = 900,
                clarityLevel = 10,
                spatialLevel = 300,
                eqBands = listOf(1500, 1400, 1000, -200, -300)
            )
        )
    }
}

// ==================== DATA CLASSES ====================

enum class BattleMode {
    OFF,
    BASS_WARFARE,    // Maximum bass impact
    CLARITY_STRIKE,  // Cut through opponent
    FULL_ASSAULT,    // Everything maxed
    SPL_MONSTER,     // Pure loudness
    CROWD_REACH,     // Fill the venue
    MAXIMUM_IMPACT,  // Maximum power
    BALANCED_BATTLE, // Balanced approach
    INDOOR_BATTLE    // Indoor optimized
}

data class EQBand(
    val index: Int,
    val centerFreq: Int,  // Hz
    val minLevel: Int,    // millibels
    val maxLevel: Int,    // millibels
    val currentLevel: Int // millibels
) {
    val frequencyLabel: String
        get() = when {
            centerFreq < 100 -> "${centerFreq}Hz"
            centerFreq < 1000 -> "${centerFreq}Hz"
            else -> "${centerFreq / 1000}kHz"
        }
    
    val levelDb: Float
        get() = currentLevel / 100f
}

data class BattlePreset(
    val name: String,
    val bassLevel: Int,      // 0-1000
    val loudnessGain: Int,   // 0-1000 mB
    val clarityLevel: Int,   // 0-100
    val spatialLevel: Int,   // 0-1000
    val eqBands: List<Int>   // millibels per band
)

/**
 * Quick Profile for instant A/B/C slot switching during battles
 * Stores ALL current audio settings for instant recall
 */
data class QuickProfile(
    val name: String,
    val bassLevel: Int,
    val bassFrequency: Float,
    val loudnessGain: Int,
    val clarityLevel: Int,
    val spatialLevel: Int,
    val compressorEnabled: Boolean,
    val compressorThreshold: Float,
    val compressorRatio: Float,
    val compressorAttack: Float,
    val compressorRelease: Float,
    val compressorMakeupGain: Float,
    val limiterEnabled: Boolean,
    val limiterThreshold: Float,
    val limiterCeiling: Float,
    val stereoWidthEnabled: Boolean,
    val stereoWidth: Int,
    val exciterEnabled: Boolean,
    val exciterDrive: Int,
    val exciterMix: Int,
    val eqBands: List<Int>
)
