package com.ultramusic.player.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.PresetReverb
import android.os.Build
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
            e.printStackTrace()
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
            e.printStackTrace()
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
