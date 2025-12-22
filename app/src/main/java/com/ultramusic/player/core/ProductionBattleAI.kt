package com.ultramusic.player.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.ultramusic.player.audio.AudioBattleEngine
import com.ultramusic.player.audio.BattleMode
import com.ultramusic.player.data.Song
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * PRODUCTION-GRADE BATTLE AI
 * 
 * Combines real audio analysis with intelligent battle strategies.
 * Uses actual DSP for opponent detection and auto-counter.
 * 
 * Features:
 * - Real-time opponent BPM detection
 * - Key-based song recommendations
 * - Intelligent EQ counter-attack
 * - Crowd energy tracking
 * - Battle momentum system
 * - Automatic attack timing
 */
@Singleton
class ProductionBattleAI @Inject constructor(
    private val context: Context,
    private val audioAnalyzer: RealAudioAnalyzer,
    private val errorHandler: AppErrorHandler
) {
    companion object {
        private const val TAG = "ProductionBattleAI"
        private const val ANALYSIS_INTERVAL_MS = 100L
        private const val MOMENTUM_DECAY_RATE = 0.995f
        private const val ATTACK_COOLDOWN_MS = 3000L
    }
    
    private val scope = CoroutineScope(Dispatchers.Default)
    private var battleJob: Job? = null
    private var vibrator: Vibrator? = null
    
    // Dependencies (set when battle starts)
    private var battleEngine: AudioBattleEngine? = null
    private var songLibrary: List<Song> = emptyList()
    private var analyzedSongs: Map<Long, SongBattleProfile> = emptyMap()
    
    // ==================== STATE ====================
    
    private val _battleState = MutableStateFlow(BattleState.IDLE)
    val battleState: StateFlow<BattleState> = _battleState.asStateFlow()
    
    private val _battleStrategy = MutableStateFlow(BattleStrategy.BALANCED)
    val battleStrategy: StateFlow<BattleStrategy> = _battleStrategy.asStateFlow()
    
    private val _momentum = MutableStateFlow(50f) // 0-100, 50 = even
    val momentum: StateFlow<Float> = _momentum.asStateFlow()
    
    private val _opponentAnalysis = MutableStateFlow(OpponentStatus())
    val opponentAnalysis: StateFlow<OpponentStatus> = _opponentAnalysis.asStateFlow()
    
    private val _battleStats = MutableStateFlow(BattleStats())
    val battleStats: StateFlow<BattleStats> = _battleStats.asStateFlow()
    
    private val _attackOpportunity = MutableStateFlow<AttackOpportunity?>(null)
    val attackOpportunity: StateFlow<AttackOpportunity?> = _attackOpportunity.asStateFlow()
    
    private val _suggestedSongs = MutableStateFlow<List<SongSuggestion>>(emptyList())
    val suggestedSongs: StateFlow<List<SongSuggestion>> = _suggestedSongs.asStateFlow()
    
    private val _battleLog = MutableStateFlow<List<BattleEvent>>(emptyList())
    val battleLog: StateFlow<List<BattleEvent>> = _battleLog.asStateFlow()
    
    // Auto-features
    private val _autoCounterEnabled = MutableStateFlow(false)
    val autoCounterEnabled: StateFlow<Boolean> = _autoCounterEnabled.asStateFlow()
    
    private val _autoVolumeEnabled = MutableStateFlow(false)
    val autoVolumeEnabled: StateFlow<Boolean> = _autoVolumeEnabled.asStateFlow()
    
    // Internal tracking
    private var lastAttackTime = 0L
    private var peakMomentum = 50f
    private var opponentBpmHistory = mutableListOf<Double>()
    private var ourBpmHistory = mutableListOf<Double>()
    
    // ==================== INITIALIZATION ====================
    
    fun initialize(
        engine: AudioBattleEngine,
        songs: List<Song>,
        profiles: Map<Long, SongBattleProfile>
    ) {
        battleEngine = engine
        songLibrary = songs
        analyzedSongs = profiles
        
        // Get vibrator for haptic feedback
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        Log.i(TAG, "Battle AI initialized with ${songs.size} songs")
    }
    
    // ==================== BATTLE CONTROL ====================
    
    /**
     * Start the battle AI
     */
    fun startBattle(strategy: BattleStrategy = BattleStrategy.BALANCED) {
        if (_battleState.value == BattleState.ACTIVE) {
            Log.w(TAG, "Battle already active")
            return
        }
        
        errorHandler.logInfo(TAG, "Starting battle with strategy: $strategy")
        
        _battleStrategy.value = strategy
        _battleState.value = BattleState.ACTIVE
        _momentum.value = 50f
        peakMomentum = 50f
        opponentBpmHistory.clear()
        ourBpmHistory.clear()
        
        addBattleEvent(BattleEventType.BATTLE_START, "Battle started - $strategy mode")
        
        // Start audio analysis
        audioAnalyzer.startRealTimeAnalysis()
        
        // Start battle loop
        battleJob = scope.launch {
            while (isActive && _battleState.value == BattleState.ACTIVE) {
                try {
                    battleLoop()
                } catch (e: Exception) {
                    errorHandler.handleException(e, "BattleLoop")
                }
                delay(ANALYSIS_INTERVAL_MS)
            }
        }
        
        // Apply strategy-specific settings
        applyStrategy(strategy)
    }
    
    /**
     * Pause the battle
     */
    fun pauseBattle() {
        _battleState.value = BattleState.PAUSED
        addBattleEvent(BattleEventType.BATTLE_PAUSE, "Battle paused")
    }
    
    /**
     * Resume the battle
     */
    fun resumeBattle() {
        _battleState.value = BattleState.ACTIVE
        addBattleEvent(BattleEventType.BATTLE_RESUME, "Battle resumed")
    }
    
    /**
     * End the battle
     */
    fun endBattle() {
        battleJob?.cancel()
        audioAnalyzer.stopAnalysis()
        
        val result = when {
            _momentum.value >= 70 -> BattleResult.VICTORY
            _momentum.value <= 30 -> BattleResult.DEFEAT
            else -> BattleResult.DRAW
        }
        
        _battleState.value = BattleState.ENDED
        addBattleEvent(BattleEventType.BATTLE_END, "Battle ended: $result (Peak momentum: ${peakMomentum.roundToInt()}%)")
        
        errorHandler.logInfo(TAG, "Battle ended: $result")
    }
    
    /**
     * Change battle strategy mid-battle
     */
    fun setStrategy(strategy: BattleStrategy) {
        _battleStrategy.value = strategy
        applyStrategy(strategy)
        addBattleEvent(BattleEventType.STRATEGY_CHANGE, "Strategy changed to: $strategy")
    }
    
    // ==================== AUTO FEATURES ====================
    
    fun toggleAutoCounter(enabled: Boolean) {
        _autoCounterEnabled.value = enabled
        addBattleEvent(
            BattleEventType.FEATURE_TOGGLE,
            "Auto-Counter ${if (enabled) "enabled" else "disabled"}"
        )
    }
    
    fun toggleAutoVolume(enabled: Boolean) {
        _autoVolumeEnabled.value = enabled
        addBattleEvent(
            BattleEventType.FEATURE_TOGGLE,
            "Auto-Volume ${if (enabled) "enabled" else "disabled"}"
        )
    }
    
    // ==================== MAIN BATTLE LOOP ====================
    
    private suspend fun battleLoop() {
        // Collect real-time analysis data
        val loudness = audioAnalyzer.loudnessDb.value
        val energy = audioAnalyzer.energy.value
        val spectralCentroid = audioAnalyzer.spectralCentroid.value
        val detectedKey = audioAnalyzer.detectedKey.value
        val currentBpm = audioAnalyzer.currentBPM.value
        val onsetDetected = audioAnalyzer.onsetDetected.value
        
        // Update opponent status
        val opponent = OpponentStatus(
            loudnessDb = loudness,
            energy = energy,
            spectralCentroid = spectralCentroid,
            estimatedBpm = currentBpm,
            detectedKey = detectedKey,
            hasOnset = onsetDetected,
            timestamp = System.currentTimeMillis()
        )
        _opponentAnalysis.value = opponent
        
        // Track BPM history for better estimation
        if (currentBpm > 0) {
            opponentBpmHistory.add(currentBpm)
            if (opponentBpmHistory.size > 50) opponentBpmHistory.removeAt(0)
        }
        
        // Perform battle actions based on strategy
        when (_battleStrategy.value) {
            BattleStrategy.AGGRESSIVE -> aggressiveAction(opponent)
            BattleStrategy.DEFENSIVE -> defensiveAction(opponent)
            BattleStrategy.BALANCED -> balancedAction(opponent)
            BattleStrategy.COUNTER -> counterAction(opponent)
            BattleStrategy.STEALTH -> stealthAction(opponent)
        }
        
        // Auto-counter EQ if enabled
        if (_autoCounterEnabled.value) {
            autoCounterEQ(opponent)
        }
        
        // Auto-volume matching if enabled
        if (_autoVolumeEnabled.value) {
            autoVolumeMatch(opponent)
        }
        
        // Detect attack opportunities
        detectAttackOpportunity(opponent)
        
        // Update momentum
        updateMomentum(opponent)
        
        // Update song suggestions based on opponent
        if (opponentBpmHistory.size >= 20 && opponentBpmHistory.size % 20 == 0) {
            updateSongSuggestions(opponent)
        }
        
        // Update stats
        updateStats(opponent)
    }
    
    // ==================== STRATEGY IMPLEMENTATIONS ====================
    
    private fun applyStrategy(strategy: BattleStrategy) {
        val engine = battleEngine ?: return
        
        when (strategy) {
            BattleStrategy.AGGRESSIVE -> {
                engine.setBattleMode(BattleMode.MAXIMUM_IMPACT)
                engine.setLoudness(800)
                engine.setBassBoost(900)
                _autoCounterEnabled.value = true
                _autoVolumeEnabled.value = true
            }
            BattleStrategy.DEFENSIVE -> {
                engine.setBattleMode(BattleMode.CLARITY_STRIKE)
                engine.setLoudness(500)
                engine.setClarity(80)
                _autoCounterEnabled.value = true
            }
            BattleStrategy.BALANCED -> {
                engine.setBattleMode(BattleMode.FULL_ASSAULT)
                engine.setLoudness(600)
                engine.setBassBoost(600)
            }
            BattleStrategy.COUNTER -> {
                engine.setBattleMode(BattleMode.BALANCED_BATTLE)
                _autoCounterEnabled.value = true
            }
            BattleStrategy.STEALTH -> {
                engine.setBattleMode(BattleMode.INDOOR_BATTLE)
                engine.setLoudness(400)
            }
        }
    }
    
    private fun aggressiveAction(opponent: OpponentStatus) {
        val engine = battleEngine ?: return
        
        // When opponent is quiet, ATTACK
        if (opponent.loudnessDb < -20f) {
            if (canAttack()) {
                engine.goNuclear()
                addBattleEvent(BattleEventType.ATTACK, "Aggressive attack during opponent's quiet moment")
                recordAttack()
            }
        }
        
        // If opponent is bass-heavy, compete directly
        if (opponent.spectralCentroid < 500f) {
            engine.setBassBoost(1000)
        }
    }
    
    private fun defensiveAction(opponent: OpponentStatus) {
        val engine = battleEngine ?: return
        
        // Focus on clarity and cut-through rather than raw power
        if (opponent.loudnessDb > -10f) {
            engine.setClarity(90)
            engine.setEQBand(3, 1200) // Boost presence
        }
        
        // Stay above opponent but don't waste energy
        val targetLoudness = ((opponent.loudnessDb + 60) * 10 + 100).toInt().coerceIn(400, 700)
        engine.setLoudness(targetLoudness)
    }
    
    private fun balancedAction(opponent: OpponentStatus) {
        val engine = battleEngine ?: return
        
        // Adaptive approach - match opponent energy then exceed slightly
        when {
            opponent.energy > 0.7f -> {
                engine.setBassBoost(800)
                engine.setLoudness(700)
            }
            opponent.energy > 0.4f -> {
                engine.setBassBoost(600)
                engine.setLoudness(600)
            }
            else -> {
                // Build up for next attack
                engine.setBassBoost(400)
                engine.setLoudness(500)
            }
        }
    }
    
    private fun counterAction(opponent: OpponentStatus) {
        // Pure counter - only react, don't initiate
        autoCounterEQ(opponent)
    }
    
    private fun stealthAction(opponent: OpponentStatus) {
        val engine = battleEngine ?: return
        
        // Stay quiet, wait for perfect moment
        if (opponent.loudnessDb > -15f && _momentum.value > 60) {
            // Opponent is confident, strike!
            if (canAttack()) {
                engine.goNuclear()
                addBattleEvent(BattleEventType.ATTACK, "Stealth attack!")
                recordAttack()
            }
        } else {
            // Stay low
            engine.setLoudness(300)
        }
    }
    
    // ==================== AUTO FEATURES IMPLEMENTATION ====================
    
    private fun autoCounterEQ(opponent: OpponentStatus) {
        val engine = battleEngine ?: return
        
        // Counter-EQ: Boost where opponent is weak, compete where they're strong
        val centroid = opponent.spectralCentroid
        
        when {
            // Opponent is bass-heavy (low centroid) - boost our mids/highs for clarity
            centroid < 500f -> {
                engine.setEQBand(3, 1000) // Presence
                engine.setEQBand(4, 800)  // Air
            }
            // Opponent is bright (high centroid) - boost bass for power
            centroid > 2000f -> {
                engine.setEQBand(0, 1200) // Sub bass
                engine.setEQBand(1, 1000) // Bass
            }
            // Balanced opponent - full spectrum boost
            else -> {
                engine.setEQBand(1, 800)
                engine.setEQBand(2, 600)
                engine.setEQBand(3, 800)
            }
        }
    }
    
    private fun autoVolumeMatch(opponent: OpponentStatus) {
        val engine = battleEngine ?: return
        
        // Always stay louder than opponent
        // Convert opponent dB to loudness setting (rough mapping)
        val opponentLoudness = ((opponent.loudnessDb + 60) * 15).toInt().coerceIn(0, 800)
        val ourTarget = (opponentLoudness + 150).coerceIn(400, 1000)
        
        engine.setLoudness(ourTarget)
    }
    
    // ==================== ATTACK DETECTION ====================
    
    private fun detectAttackOpportunity(opponent: OpponentStatus) {
        val now = System.currentTimeMillis()
        
        // Check for various attack opportunities
        val opportunity: AttackOpportunity? = when {
            // Opponent is very quiet - perfect time to attack
            opponent.loudnessDb < -30f && opponent.energy < 0.2f -> {
                AttackOpportunity(
                    type = AttackType.SILENCE_EXPLOIT,
                    strength = 90,
                    message = "🎯 Opponent is silent - ATTACK NOW!"
                )
            }
            // Opponent energy dropping - they're losing steam
            _battleStats.value.opponentEnergyTrend < -0.1f -> {
                AttackOpportunity(
                    type = AttackType.ENERGY_DROP,
                    strength = 70,
                    message = "⚡ Opponent energy dropping - Push harder!"
                )
            }
            // We have momentum advantage
            _momentum.value > 70f -> {
                AttackOpportunity(
                    type = AttackType.MOMENTUM_ADVANTAGE,
                    strength = 60,
                    message = "🔥 High momentum - Keep the pressure!"
                )
            }
            else -> null
        }
        
        // Only update if new opportunity or significantly different
        if (opportunity != null && opportunity != _attackOpportunity.value) {
            _attackOpportunity.value = opportunity
            
            // Haptic feedback for high-strength opportunities
            if (opportunity.strength >= 80) {
                vibrateAttackAlert()
            }
        } else if (opportunity == null && _attackOpportunity.value != null) {
            _attackOpportunity.value = null
        }
    }
    
    private fun canAttack(): Boolean {
        return System.currentTimeMillis() - lastAttackTime > ATTACK_COOLDOWN_MS
    }
    
    private fun recordAttack() {
        lastAttackTime = System.currentTimeMillis()
    }
    
    // ==================== MOMENTUM SYSTEM ====================
    
    private fun updateMomentum(opponent: OpponentStatus) {
        var delta = 0f
        
        // Our loudness vs opponent's
        val loudnessAdvantage = -opponent.loudnessDb - 15f // We're always above ambient
        delta += loudnessAdvantage * 0.01f
        
        // Energy comparison (we estimate ours, compare to theirs)
        val energyAdvantage = 0.6f - opponent.energy
        delta += energyAdvantage * 5f
        
        // Onset detection (beats landing well)
        if (opponent.hasOnset && _momentum.value < 60) {
            delta -= 0.5f // Opponent is hitting beats
        }
        
        // Natural decay toward 50
        val currentMomentum = _momentum.value
        val decayTarget = if (currentMomentum > 50) currentMomentum * MOMENTUM_DECAY_RATE else currentMomentum / MOMENTUM_DECAY_RATE
        
        val newMomentum = (decayTarget + delta).coerceIn(0f, 100f)
        _momentum.value = newMomentum
        
        // Track peak
        if (newMomentum > peakMomentum) {
            peakMomentum = newMomentum
        }
    }
    
    // ==================== SONG SUGGESTIONS ====================
    
    private fun updateSongSuggestions(opponent: OpponentStatus) {
        if (songLibrary.isEmpty()) return
        
        val opponentBpm = if (opponentBpmHistory.isNotEmpty()) {
            opponentBpmHistory.average()
        } else 120.0
        
        val opponentKey = opponent.detectedKey
        
        // Score songs based on battle compatibility
        val suggestions = analyzedSongs.mapNotNull { (songId, profile) ->
            val song = songLibrary.find { it.id == songId } ?: return@mapNotNull null
            
            var score = 0f
            val reasons = mutableListOf<String>()
            
            // BPM compatibility (same BPM or harmonic multiple)
            if (profile.bpm != null) {
                val bpmDiff = abs(profile.bpm - opponentBpm)
                when {
                    bpmDiff < 2 -> {
                        score += 30
                        reasons.add("Perfect BPM match")
                    }
                    bpmDiff < 5 -> {
                        score += 20
                        reasons.add("Close BPM")
                    }
                    abs(profile.bpm * 2 - opponentBpm) < 5 || abs(profile.bpm / 2 - opponentBpm) < 5 -> {
                        score += 15
                        reasons.add("Harmonic BPM")
                    }
                }
            }
            
            // Key compatibility
            if (profile.key != null && opponentKey != null) {
                if (profile.key.isCompatibleWith(opponentKey)) {
                    score += 25
                    reasons.add("Key compatible (${profile.key.camelotCode})")
                }
            }
            
            // Energy for strategy
            when (_battleStrategy.value) {
                BattleStrategy.AGGRESSIVE -> {
                    if (profile.energy > 0.7f) {
                        score += 20
                        reasons.add("High energy")
                    }
                }
                BattleStrategy.STEALTH -> {
                    if (profile.energy < 0.5f) {
                        score += 15
                        reasons.add("Build-up potential")
                    }
                }
                else -> {
                    score += profile.energy * 10
                }
            }
            
            // Counter potential
            if (profile.energy > opponent.energy + 0.2f) {
                score += 15
                reasons.add("Energy advantage")
            }
            
            if (score > 20) {
                SongSuggestion(
                    song = song,
                    score = score,
                    reasons = reasons,
                    profile = profile
                )
            } else null
        }.sortedByDescending { it.score }.take(5)
        
        _suggestedSongs.value = suggestions
    }
    
    // ==================== STATS & LOGGING ====================
    
    private var previousEnergy = 0f
    
    private fun updateStats(opponent: OpponentStatus) {
        val energyTrend = opponent.energy - previousEnergy
        previousEnergy = opponent.energy
        
        val avgBpm = if (opponentBpmHistory.isNotEmpty()) opponentBpmHistory.average() else 0.0
        
        _battleStats.value = BattleStats(
            duration = if (_battleState.value == BattleState.ACTIVE) {
                System.currentTimeMillis() - (_battleStats.value.startTime ?: System.currentTimeMillis())
            } else 0,
            peakMomentum = peakMomentum,
            averageOpponentBpm = avgBpm,
            opponentEnergyTrend = energyTrend,
            attacksLaunched = _battleLog.value.count { it.type == BattleEventType.ATTACK },
            startTime = _battleStats.value.startTime ?: System.currentTimeMillis()
        )
    }
    
    private fun addBattleEvent(type: BattleEventType, message: String) {
        val event = BattleEvent(
            timestamp = System.currentTimeMillis(),
            type = type,
            message = message
        )
        _battleLog.value = (_battleLog.value + event).takeLast(100)
    }
    
    // ==================== HAPTIC FEEDBACK ====================
    
    private fun vibrateAttackAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }
}

// ==================== DATA CLASSES ====================

enum class BattleState {
    IDLE, ACTIVE, PAUSED, ENDED
}

enum class BattleStrategy {
    AGGRESSIVE,  // Maximum power, constant attack
    DEFENSIVE,   // Clarity-focused, efficient
    BALANCED,    // Adaptive to opponent
    COUNTER,     // Only react to opponent
    STEALTH      // Low profile until perfect moment
}

enum class BattleResult {
    VICTORY, DEFEAT, DRAW
}

data class OpponentStatus(
    val loudnessDb: Float = -60f,
    val energy: Float = 0f,
    val spectralCentroid: Float = 0f,
    val estimatedBpm: Double = 0.0,
    val detectedKey: MusicalKey? = null,
    val hasOnset: Boolean = false,
    val timestamp: Long = 0
)

data class BattleStats(
    val duration: Long = 0,
    val peakMomentum: Float = 50f,
    val averageOpponentBpm: Double = 0.0,
    val opponentEnergyTrend: Float = 0f,
    val attacksLaunched: Int = 0,
    val startTime: Long? = null
)

enum class AttackType {
    SILENCE_EXPLOIT,
    ENERGY_DROP,
    MOMENTUM_ADVANTAGE,
    BPM_SYNC,
    KEY_ADVANTAGE
}

data class AttackOpportunity(
    val type: AttackType,
    val strength: Int, // 0-100
    val message: String
)

data class SongSuggestion(
    val song: Song,
    val score: Float,
    val reasons: List<String>,
    val profile: SongBattleProfile
)

data class SongBattleProfile(
    val songId: Long,
    val bpm: Double?,
    val key: MusicalKey?,
    val energy: Float,
    val danceability: Float,
    val valence: Float,
    val tags: List<String> = emptyList()
)

enum class BattleEventType {
    BATTLE_START,
    BATTLE_END,
    BATTLE_PAUSE,
    BATTLE_RESUME,
    STRATEGY_CHANGE,
    FEATURE_TOGGLE,
    ATTACK,
    COUNTER,
    OPPORTUNITY_DETECTED,
    SONG_SUGGESTION
}

data class BattleEvent(
    val timestamp: Long,
    val type: BattleEventType,
    val message: String
)
