package com.ultramusic.player.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FREQUENCY WARFARE SYSTEM
 * 
 * Advanced active tactics to DOMINATE the frequency spectrum!
 * 
 * TACTICS:
 * 1. MASKING - Boost frequencies that mask opponent's sound
 * 2. AVOIDANCE - Shift to frequencies opponent isn't using
 * 3. FLANKING - Attack from unexpected frequency ranges
 * 4. SATURATION - Fill all frequencies, leave no gaps
 * 5. SURGICAL STRIKE - Target specific problem frequencies
 * 6. FREQUENCY LOCK - Match opponent and overpower
 * 7. PHASE ATTACK - Use phase to cancel opponent (advanced)
 */
@Singleton
class FrequencyWarfare @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var warfareJob: Job? = null
    
    private var battleEngine: AudioBattleEngine? = null
    
    // ==================== STATE ====================
    
    private val _activeTactic = MutableStateFlow<WarfareTactic?>(null)
    val activeTactic: StateFlow<WarfareTactic?> = _activeTactic.asStateFlow()
    
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()
    
    private val _targetFrequencies = MutableStateFlow<List<TargetFrequency>>(emptyList())
    val targetFrequencies: StateFlow<List<TargetFrequency>> = _targetFrequencies.asStateFlow()
    
    private val _warfareLog = MutableStateFlow<List<WarfareAction>>(emptyList())
    val warfareLog: StateFlow<List<WarfareAction>> = _warfareLog.asStateFlow()
    
    private val _dominanceMap = MutableStateFlow<Map<FrequencyBand, DominanceStatus>>(emptyMap())
    val dominanceMap: StateFlow<Map<FrequencyBand, DominanceStatus>> = _dominanceMap.asStateFlow()
    
    // ==================== INITIALIZATION ====================
    
    fun initialize(engine: AudioBattleEngine) {
        battleEngine = engine
    }
    
    // ==================== TACTICS ====================
    
    /**
     * TACTIC 1: FREQUENCY MASKING
     * Boost frequencies that psychoacoustically mask opponent
     * 
     * Human hearing: Lower frequencies can mask higher frequencies
     * So if opponent is at 1kHz, we boost 500Hz to mask them!
     */
    fun executeMasking(opponentAnalysis: FrequencyAnalysis) {
        _activeTactic.value = WarfareTactic.MASKING
        _isActive.value = true
        
        val engine = battleEngine ?: return
        
        // Find opponent's strong frequencies and mask them
        opponentAnalysis.strongBands.forEach { band ->
            val maskingBand = getMaskingFrequency(band)
            val boostLevel = 1200 // +12dB
            
            when (maskingBand) {
                FrequencyBand.SUB_BASS -> engine.setEQBand(0, boostLevel)
                FrequencyBand.BASS -> engine.setEQBand(1, boostLevel)
                FrequencyBand.LOW_MID -> engine.setEQBand(2, boostLevel)
                FrequencyBand.MID -> engine.setEQBand(2, boostLevel)
                FrequencyBand.HIGH_MID -> engine.setEQBand(3, boostLevel)
                FrequencyBand.HIGH -> engine.setEQBand(4, boostLevel)
            }
            
            logAction(WarfareAction(
                tactic = WarfareTactic.MASKING,
                description = "Masking ${band.label} with ${maskingBand.label}",
                targetBand = band,
                actionBand = maskingBand,
                boost = 12
            ))
        }
        
        updateDominance()
    }
    
    /**
     * Get the frequency band that best masks the target
     */
    private fun getMaskingFrequency(target: FrequencyBand): FrequencyBand {
        // Lower frequencies mask higher frequencies more effectively
        return when (target) {
            FrequencyBand.HIGH -> FrequencyBand.HIGH_MID
            FrequencyBand.HIGH_MID -> FrequencyBand.MID
            FrequencyBand.MID -> FrequencyBand.LOW_MID
            FrequencyBand.LOW_MID -> FrequencyBand.BASS
            FrequencyBand.BASS -> FrequencyBand.SUB_BASS
            FrequencyBand.SUB_BASS -> FrequencyBand.SUB_BASS // Can't mask sub-bass
        }
    }
    
    /**
     * TACTIC 2: FREQUENCY AVOIDANCE
     * Shift to frequencies opponent isn't using
     * Our sound stands OUT instead of fighting
     */
    fun executeAvoidance(opponentAnalysis: FrequencyAnalysis) {
        _activeTactic.value = WarfareTactic.AVOIDANCE
        _isActive.value = true
        
        val engine = battleEngine ?: return
        
        // Cut where opponent is strong
        opponentAnalysis.strongBands.forEach { band ->
            val bandIndex = getBandIndex(band)
            engine.setEQBand(bandIndex, -600) // Cut 6dB
        }
        
        // Boost where opponent is weak
        opponentAnalysis.weakBands.forEach { band ->
            val bandIndex = getBandIndex(band)
            engine.setEQBand(bandIndex, 1000) // Boost 10dB
            
            logAction(WarfareAction(
                tactic = WarfareTactic.AVOIDANCE,
                description = "Avoiding conflict, boosting ${band.label}",
                targetBand = null,
                actionBand = band,
                boost = 10
            ))
        }
        
        updateDominance()
    }
    
    /**
     * TACTIC 3: FLANKING ATTACK
     * Hit from unexpected frequencies
     * If battle is in bass, attack with highs
     */
    fun executeFlanking(opponentAnalysis: FrequencyAnalysis) {
        _activeTactic.value = WarfareTactic.FLANKING
        _isActive.value = true
        
        val engine = battleEngine ?: return
        
        val battleZone = opponentAnalysis.strongBands.firstOrNull() ?: FrequencyBand.BASS
        
        // Attack from opposite end of spectrum
        when (battleZone) {
            FrequencyBand.SUB_BASS, FrequencyBand.BASS, FrequencyBand.LOW_MID -> {
                // Battle is low - flank with highs
                engine.setEQBand(3, 1200) // Presence
                engine.setEQBand(4, 1000) // Air
                logAction(WarfareAction(
                    tactic = WarfareTactic.FLANKING,
                    description = "Flanking low-end battle with highs!",
                    targetBand = battleZone,
                    actionBand = FrequencyBand.HIGH_MID,
                    boost = 12
                ))
            }
            FrequencyBand.MID, FrequencyBand.HIGH_MID, FrequencyBand.HIGH -> {
                // Battle is high - flank with lows
                engine.setEQBand(0, 1500) // Sub-bass
                engine.setEQBand(1, 1200) // Bass
                logAction(WarfareAction(
                    tactic = WarfareTactic.FLANKING,
                    description = "Flanking high-end battle with bass!",
                    targetBand = battleZone,
                    actionBand = FrequencyBand.SUB_BASS,
                    boost = 15
                ))
            }
        }
        
        updateDominance()
    }
    
    /**
     * TACTIC 4: FULL SATURATION
     * Fill EVERY frequency - leave no gaps
     * Overwhelming presence across spectrum
     */
    fun executeSaturation() {
        _activeTactic.value = WarfareTactic.SATURATION
        _isActive.value = true
        
        val engine = battleEngine ?: return
        
        // Boost everything
        engine.setEQBand(0, 1200)  // Sub-bass
        engine.setEQBand(1, 1000)  // Bass
        engine.setEQBand(2, 800)   // Low-mid (less to avoid mud)
        engine.setEQBand(3, 1000)  // Presence
        engine.setEQBand(4, 900)   // Air
        
        // Max bass and loudness
        engine.setBassBoost(1000)
        engine.setLoudness(900)
        
        logAction(WarfareAction(
            tactic = WarfareTactic.SATURATION,
            description = "FULL SPECTRUM SATURATION!",
            targetBand = null,
            actionBand = null,
            boost = 12
        ))
        
        updateDominance()
    }
    
    /**
     * TACTIC 5: SURGICAL STRIKE
     * Target ONE specific frequency with extreme precision
     */
    fun executeSurgicalStrike(targetBand: FrequencyBand, boostDb: Int = 15) {
        _activeTactic.value = WarfareTactic.SURGICAL_STRIKE
        _isActive.value = true
        
        val engine = battleEngine ?: return
        
        // Reset all bands
        for (i in 0..4) {
            engine.setEQBand(i, 0)
        }
        
        // Massive boost on single target
        val bandIndex = getBandIndex(targetBand)
        engine.setEQBand(bandIndex, boostDb * 100)
        
        logAction(WarfareAction(
            tactic = WarfareTactic.SURGICAL_STRIKE,
            description = "SURGICAL STRIKE on ${targetBand.label}!",
            targetBand = targetBand,
            actionBand = targetBand,
            boost = boostDb
        ))
        
        _targetFrequencies.value = listOf(
            TargetFrequency(
                band = targetBand,
                boost = boostDb,
                reason = "Surgical precision target"
            )
        )
        
        updateDominance()
    }
    
    /**
     * TACTIC 6: FREQUENCY LOCK
     * Match opponent's frequency and OVERPOWER
     */
    fun executeFrequencyLock(opponentAnalysis: FrequencyAnalysis) {
        _activeTactic.value = WarfareTactic.FREQUENCY_LOCK
        _isActive.value = true
        
        val engine = battleEngine ?: return
        
        // Find opponent's dominant and match it LOUDER
        opponentAnalysis.strongBands.forEach { band ->
            val bandIndex = getBandIndex(band)
            engine.setEQBand(bandIndex, 1500) // +15dB - LOUDER than them
        }
        
        // Max loudness to ensure we win
        engine.setLoudness(1000)
        
        logAction(WarfareAction(
            tactic = WarfareTactic.FREQUENCY_LOCK,
            description = "LOCKED onto ${opponentAnalysis.strongBands.firstOrNull()?.label}!",
            targetBand = opponentAnalysis.strongBands.firstOrNull(),
            actionBand = opponentAnalysis.strongBands.firstOrNull(),
            boost = 15
        ))
        
        updateDominance()
    }
    
    /**
     * TACTIC 7: ADAPTIVE WARFARE
     * Continuously adapts to opponent in real-time
     */
    fun startAdaptiveWarfare(analysisProvider: () -> FrequencyAnalysis) {
        _activeTactic.value = WarfareTactic.ADAPTIVE
        _isActive.value = true
        
        warfareJob = scope.launch {
            while (isActive && _isActive.value) {
                val analysis = analysisProvider()
                
                // Choose best tactic based on situation
                when {
                    // Opponent is well-rounded - use saturation
                    analysis.strongBands.size >= 4 -> executeSaturation()
                    
                    // Opponent has clear weakness - exploit it
                    analysis.weakBands.size >= 2 -> executeAvoidance(analysis)
                    
                    // Opponent is focused - flank them
                    analysis.strongBands.size == 1 -> executeFlanking(analysis)
                    
                    // Default - frequency lock
                    else -> executeFrequencyLock(analysis)
                }
                
                delay(500) // Adapt every 500ms
            }
        }
    }
    
    fun stopWarfare() {
        _isActive.value = false
        _activeTactic.value = null
        warfareJob?.cancel()
    }
    
    // ==================== DOMINANCE TRACKING ====================
    
    private fun updateDominance() {
        val engine = battleEngine ?: return
        val bands = engine.eqBands.value
        
        val dominance = mutableMapOf<FrequencyBand, DominanceStatus>()
        
        bands.forEachIndexed { index, band ->
            val freqBand = when (index) {
                0 -> FrequencyBand.SUB_BASS
                1 -> FrequencyBand.BASS
                2 -> FrequencyBand.LOW_MID
                3 -> FrequencyBand.HIGH_MID
                4 -> FrequencyBand.HIGH
                else -> return@forEachIndexed
            }
            
            dominance[freqBand] = when {
                band.currentLevel >= 1000 -> DominanceStatus.DOMINATING
                band.currentLevel >= 500 -> DominanceStatus.STRONG
                band.currentLevel >= 0 -> DominanceStatus.NEUTRAL
                else -> DominanceStatus.RETREATING
            }
        }
        
        _dominanceMap.value = dominance
    }
    
    // ==================== HELPERS ====================
    
    private fun getBandIndex(band: FrequencyBand): Int {
        return when (band) {
            FrequencyBand.SUB_BASS -> 0
            FrequencyBand.BASS -> 1
            FrequencyBand.LOW_MID -> 2
            FrequencyBand.MID -> 2
            FrequencyBand.HIGH_MID -> 3
            FrequencyBand.HIGH -> 4
        }
    }
    
    private fun logAction(action: WarfareAction) {
        _warfareLog.value = (_warfareLog.value + action).takeLast(20)
    }
    
    // ==================== COMBO ATTACKS ====================
    
    /**
     * Execute a combo of tactics in sequence
     */
    fun executeCombo(combo: WarfareCombo, analysisProvider: () -> FrequencyAnalysis) {
        scope.launch {
            combo.tactics.forEach { tactic ->
                when (tactic) {
                    WarfareTactic.MASKING -> executeMasking(analysisProvider())
                    WarfareTactic.AVOIDANCE -> executeAvoidance(analysisProvider())
                    WarfareTactic.FLANKING -> executeFlanking(analysisProvider())
                    WarfareTactic.SATURATION -> executeSaturation()
                    WarfareTactic.SURGICAL_STRIKE -> executeSurgicalStrike(FrequencyBand.SUB_BASS)
                    WarfareTactic.FREQUENCY_LOCK -> executeFrequencyLock(analysisProvider())
                    WarfareTactic.ADAPTIVE -> {} // Handled separately
                }
                delay(combo.delayBetweenMs)
            }
        }
    }
    
    companion object {
        val COMBOS = listOf(
            WarfareCombo(
                name = "🔥 Bass Assault",
                description = "Lock bass, then saturate",
                tactics = listOf(
                    WarfareTactic.FREQUENCY_LOCK,
                    WarfareTactic.SATURATION
                ),
                delayBetweenMs = 2000
            ),
            WarfareCombo(
                name = "🎯 Sniper",
                description = "Avoid, then surgical strike",
                tactics = listOf(
                    WarfareTactic.AVOIDANCE,
                    WarfareTactic.SURGICAL_STRIKE
                ),
                delayBetweenMs = 3000
            ),
            WarfareCombo(
                name = "⚔️ Pincer",
                description = "Flank from both sides",
                tactics = listOf(
                    WarfareTactic.FLANKING,
                    WarfareTactic.MASKING
                ),
                delayBetweenMs = 1500
            ),
            WarfareCombo(
                name = "💀 Total War",
                description = "Everything at once",
                tactics = listOf(
                    WarfareTactic.SATURATION,
                    WarfareTactic.FREQUENCY_LOCK,
                    WarfareTactic.MASKING
                ),
                delayBetweenMs = 1000
            )
        )
    }
}

// ==================== DATA CLASSES ====================

enum class WarfareTactic(val emoji: String, val description: String) {
    MASKING("🔇", "Mask opponent's frequencies"),
    AVOIDANCE("🏃", "Avoid conflict, exploit gaps"),
    FLANKING("⚔️", "Attack from unexpected direction"),
    SATURATION("🌊", "Fill entire spectrum"),
    SURGICAL_STRIKE("🎯", "Precise single-frequency attack"),
    FREQUENCY_LOCK("🔒", "Match and overpower"),
    ADAPTIVE("🤖", "AI adapts in real-time")
}

data class WarfareAction(
    val tactic: WarfareTactic,
    val description: String,
    val targetBand: FrequencyBand?,
    val actionBand: FrequencyBand?,
    val boost: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class TargetFrequency(
    val band: FrequencyBand,
    val boost: Int,
    val reason: String
)

enum class DominanceStatus(val emoji: String) {
    DOMINATING("👑"),
    STRONG("💪"),
    NEUTRAL("➡️"),
    RETREATING("🏳️")
}

data class WarfareCombo(
    val name: String,
    val description: String,
    val tactics: List<WarfareTactic>,
    val delayBetweenMs: Long
)
