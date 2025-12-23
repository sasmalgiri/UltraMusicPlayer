package com.ultramusic.player.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
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
import kotlin.math.sqrt

/**
 * ACTIVE BATTLE SYSTEM
 * 
 * This is the AI that ACTIVELY fights in the battle!
 * Not passive controls - ACTIVE participation!
 * 
 * ACTIVE FEATURES:
 * 1. Auto-Counter EQ - Automatically adjusts EQ to exploit opponent weakness
 * 2. Attack Timing - Detects opponent's quiet moments and ATTACKS
 * 3. Frequency Warfare - Actively boosts frequencies that MASK opponent
 * 4. Smart Queue - Auto-selects next song to counter opponent
 * 5. Volume Matching - Always stays louder than opponent
 * 6. Beat Drop Sync - Drops bass at perfect psychological moments
 * 7. Crowd Momentum - Tracks battle momentum and adjusts strategy
 * 8. Weakness Hunter - Continuously finds and exploits weak frequencies
 * 9. Counter-Attack - When opponent attacks, we counter harder
 * 10. Battle Scripts - Pre-programmed attack sequences
 */
@Singleton
@UnstableApi
class ActiveBattleSystem @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var battleJob: Job? = null
    private var audioRecord: AudioRecord? = null
    
    // Dependencies (injected when battle starts)
    private var battleEngine: AudioBattleEngine? = null
    private var musicController: MusicController? = null
    private var songLibrary: List<Song> = emptyList()
    private var songRatings: Map<Long, SongBattleRating> = emptyMap()
    
    // ==================== STATE ====================
    
    private val _battleState = MutableStateFlow(ActiveBattleState.IDLE)
    val battleState: StateFlow<ActiveBattleState> = _battleState.asStateFlow()
    
    private val _battleMode = MutableStateFlow(ActiveBattleMode.BALANCED)
    val battleMode: StateFlow<ActiveBattleMode> = _battleMode.asStateFlow()
    
    private val _momentum = MutableStateFlow(50) // 0-100, 50 = even
    val momentum: StateFlow<Int> = _momentum.asStateFlow()
    
    private val _opponentSPL = MutableStateFlow(0f)
    val opponentSPL: StateFlow<Float> = _opponentSPL.asStateFlow()
    
    private val _ourSPL = MutableStateFlow(0f)
    val ourSPL: StateFlow<Float> = _ourSPL.asStateFlow()
    
    private val _activeAttacks = MutableStateFlow<List<ActiveAttack>>(emptyList())
    val activeAttacks: StateFlow<List<ActiveAttack>> = _activeAttacks.asStateFlow()
    
    private val _battleLog = MutableStateFlow<List<BattleLogEntry>>(emptyList())
    val battleLog: StateFlow<List<BattleLogEntry>> = _battleLog.asStateFlow()
    
    private val _nextSongSuggestion = MutableStateFlow<Song?>(null)
    val nextSongSuggestion: StateFlow<Song?> = _nextSongSuggestion.asStateFlow()
    
    private val _attackOpportunity = MutableStateFlow<AttackOpportunity?>(null)
    val attackOpportunity: StateFlow<AttackOpportunity?> = _attackOpportunity.asStateFlow()
    
    // Real-time frequency analysis
    private val _opponentFrequencies = MutableStateFlow(FrequencyAnalysis())
    val opponentFrequencies: StateFlow<FrequencyAnalysis> = _opponentFrequencies.asStateFlow()
    
    // Auto-features enabled
    private val _autoCounterEnabled = MutableStateFlow(false)
    val autoCounterEnabled: StateFlow<Boolean> = _autoCounterEnabled.asStateFlow()
    
    private val _autoVolumeEnabled = MutableStateFlow(false)
    val autoVolumeEnabled: StateFlow<Boolean> = _autoVolumeEnabled.asStateFlow()
    
    private val _autoQueueEnabled = MutableStateFlow(false)
    val autoQueueEnabled: StateFlow<Boolean> = _autoQueueEnabled.asStateFlow()
    
    private val _attackAlertsEnabled = MutableStateFlow(true)
    val attackAlertsEnabled: StateFlow<Boolean> = _attackAlertsEnabled.asStateFlow()
    
    // ==================== INITIALIZATION ====================
    
    fun initialize(
        engine: AudioBattleEngine,
        controller: MusicController,
        songs: List<Song>,
        ratings: Map<Long, SongBattleRating>
    ) {
        battleEngine = engine
        musicController = controller
        songLibrary = songs
        songRatings = ratings
    }
    
    // ==================== BATTLE CONTROL ====================
    
    /**
     * START ACTIVE BATTLE - The AI takes over!
     */
    fun startBattle(mode: ActiveBattleMode = ActiveBattleMode.BALANCED) {
        if (_battleState.value == ActiveBattleState.ACTIVE) return
        
        _battleMode.value = mode
        _battleState.value = ActiveBattleState.ACTIVE
        _momentum.value = 50
        
        logEvent("🔥 BATTLE STARTED", "Mode: ${mode.name}")
        
        battleJob = scope.launch {
            startAudioCapture()
            
            while (isActive && _battleState.value == ActiveBattleState.ACTIVE) {
                // Main battle loop - runs every 100ms
                analyzeAndAct()
                delay(100)
            }
        }
    }
    
    fun pauseBattle() {
        _battleState.value = ActiveBattleState.PAUSED
        logEvent("⏸️ BATTLE PAUSED", "Waiting...")
    }
    
    fun resumeBattle() {
        _battleState.value = ActiveBattleState.ACTIVE
        logEvent("▶️ BATTLE RESUMED", "Let's go!")
    }
    
    fun endBattle() {
        _battleState.value = ActiveBattleState.ENDED
        battleJob?.cancel()
        stopAudioCapture()
        
        val result = when {
            _momentum.value >= 70 -> "🏆 VICTORY!"
            _momentum.value <= 30 -> "😔 Defeat"
            else -> "🤝 Draw"
        }
        logEvent("🏁 BATTLE ENDED", result)
    }
    
    fun setBattleMode(mode: ActiveBattleMode) {
        _battleMode.value = mode
        logEvent("🔄 MODE CHANGED", mode.name)
        
        // Apply mode-specific settings
        when (mode) {
            ActiveBattleMode.AGGRESSIVE -> {
                battleEngine?.goNuclear()
                _autoCounterEnabled.value = true
                _autoVolumeEnabled.value = true
            }
            ActiveBattleMode.DEFENSIVE -> {
                battleEngine?.setBattleMode(BattleMode.CLARITY_STRIKE)
                _autoCounterEnabled.value = true
            }
            ActiveBattleMode.BALANCED -> {
                battleEngine?.setBattleMode(BattleMode.FULL_ASSAULT)
            }
            ActiveBattleMode.STEALTH -> {
                // Start quiet, build up
                battleEngine?.setLoudness(300)
            }
            ActiveBattleMode.COUNTER_ONLY -> {
                _autoCounterEnabled.value = true
                _autoVolumeEnabled.value = false
            }
        }
    }
    
    // ==================== AUTO FEATURES ====================
    
    fun toggleAutoCounter(enabled: Boolean) {
        _autoCounterEnabled.value = enabled
        logEvent(
            if (enabled) "🎯 AUTO-COUNTER ON" else "🎯 AUTO-COUNTER OFF",
            "Automatic frequency exploitation"
        )
    }
    
    fun toggleAutoVolume(enabled: Boolean) {
        _autoVolumeEnabled.value = enabled
        logEvent(
            if (enabled) "📢 AUTO-VOLUME ON" else "📢 AUTO-VOLUME OFF",
            "Always louder than opponent"
        )
    }
    
    fun toggleAutoQueue(enabled: Boolean) {
        _autoQueueEnabled.value = enabled
        logEvent(
            if (enabled) "🎵 AUTO-QUEUE ON" else "🎵 AUTO-QUEUE OFF",
            "AI picks counter songs"
        )
    }
    
    fun toggleAttackAlerts(enabled: Boolean) {
        _attackAlertsEnabled.value = enabled
    }
    
    // ==================== MAIN BATTLE LOOP ====================
    
    private suspend fun analyzeAndAct() {
        val analysis = captureAndAnalyze()
        _opponentFrequencies.value = analysis
        
        // 1. AUTO-COUNTER EQ
        if (_autoCounterEnabled.value) {
            autoCounterEQ(analysis)
        }
        
        // 2. AUTO-VOLUME MATCHING
        if (_autoVolumeEnabled.value) {
            autoVolumeMatch(analysis)
        }
        
        // 3. DETECT ATTACK OPPORTUNITIES
        detectAttackOpportunities(analysis)
        
        // 4. UPDATE MOMENTUM
        updateMomentum(analysis)
        
        // 5. AUTO-QUEUE NEXT SONG
        if (_autoQueueEnabled.value) {
            suggestNextSong(analysis)
        }
        
        // 6. EXECUTE ACTIVE ATTACKS
        executeActiveAttacks(analysis)
    }
    
    // ==================== FEATURE 1: AUTO-COUNTER EQ ====================
    
    /**
     * Automatically adjusts EQ to exploit opponent's weak frequencies
     * Runs continuously during battle
     */
    private fun autoCounterEQ(analysis: FrequencyAnalysis) {
        val engine = battleEngine ?: return
        
        // Find opponent's weak bands and BOOST ours there
        analysis.weakBands.forEach { weakBand ->
            val boostAmount = when (weakBand) {
                FrequencyBand.SUB_BASS -> 1500  // +15dB
                FrequencyBand.BASS -> 1200     // +12dB
                FrequencyBand.LOW_MID -> 800   // +8dB
                FrequencyBand.MID -> 1000      // +10dB
                FrequencyBand.HIGH_MID -> 1200 // +12dB
                FrequencyBand.HIGH -> 1000     // +10dB
            }
            
            val bandIndex = when (weakBand) {
                FrequencyBand.SUB_BASS -> 0
                FrequencyBand.BASS -> 1
                FrequencyBand.LOW_MID -> 2
                FrequencyBand.MID -> 2
                FrequencyBand.HIGH_MID -> 3
                FrequencyBand.HIGH -> 4
            }
            
            engine.setEQBand(bandIndex, boostAmount)
        }
        
        // Find opponent's STRONG bands and either:
        // Option A: Boost ours higher to compete
        // Option B: Boost different frequencies to stand out
        
        if (_battleMode.value == ActiveBattleMode.AGGRESSIVE) {
            // Compete directly - boost same frequencies LOUDER
            analysis.strongBands.forEach { strongBand ->
                val bandIndex = when (strongBand) {
                    FrequencyBand.SUB_BASS -> 0
                    FrequencyBand.BASS -> 1
                    FrequencyBand.LOW_MID -> 2
                    FrequencyBand.MID -> 2
                    FrequencyBand.HIGH_MID -> 3
                    FrequencyBand.HIGH -> 4
                }
                engine.setEQBand(bandIndex, 1500) // MAX boost
            }
        } else {
            // Stand out - boost complementary frequencies
            // If they're bass heavy, we go clarity
            if (FrequencyBand.BASS in analysis.strongBands || 
                FrequencyBand.SUB_BASS in analysis.strongBands) {
                engine.setEQBand(3, 1200) // Presence
                engine.setEQBand(4, 1000) // Air
            }
        }
        
        // Log significant changes
        if (analysis.weakBands.isNotEmpty()) {
            addAttack(ActiveAttack(
                type = AttackType.FREQUENCY_EXPLOIT,
                target = analysis.weakBands.joinToString { it.label },
                intensity = 80,
                timestamp = System.currentTimeMillis()
            ))
        }
    }
    
    // ==================== FEATURE 2: AUTO-VOLUME MATCH ====================
    
    /**
     * Automatically stays louder than opponent
     * Adjusts loudness enhancer based on opponent's SPL
     */
    private fun autoVolumeMatch(analysis: FrequencyAnalysis) {
        val engine = battleEngine ?: return
        val opponentSpl = analysis.overallSPL
        _opponentSPL.value = opponentSpl
        
        // Target: Be 3-6dB louder than opponent
        val targetLoudnessBoost = when {
            opponentSpl < 70 -> 300    // Quiet - moderate boost
            opponentSpl < 85 -> 600    // Medium - good boost
            opponentSpl < 95 -> 800    // Loud - strong boost
            opponentSpl < 105 -> 1000  // Very loud - max boost
            else -> 1000               // Extreme - max everything
        }
        
        engine.setLoudness(targetLoudnessBoost)
        
        // Also boost bass if opponent is loud
        if (opponentSpl > 90) {
            engine.setBassBoost(1000)
        }
        
        // Estimate our SPL (based on settings)
        _ourSPL.value = opponentSpl + (targetLoudnessBoost / 100f)
    }
    
    // ==================== FEATURE 3: ATTACK OPPORTUNITY DETECTION ====================
    
    /**
     * Detects when opponent has quiet moments or transitions
     * Perfect time to DROP!
     */
    private fun detectAttackOpportunities(analysis: FrequencyAnalysis) {
        val opportunity = when {
            // Opponent just got quiet - ATTACK NOW!
            analysis.overallSPL < 60 && _opponentSPL.value > 80 -> {
                AttackOpportunity(
                    type = OpportunityType.SILENCE_DETECTED,
                    message = "🎯 OPPONENT QUIET - DROP NOW!",
                    urgency = Urgency.CRITICAL,
                    suggestedAction = "Hit BASS DROP button!"
                )
            }
            
            // Opponent's bass dropped - we can dominate low end
            analysis.subBassLevel < 0.2f && analysis.overallSPL > 70 -> {
                AttackOpportunity(
                    type = OpportunityType.BASS_GAP,
                    message = "🔊 BASS GAP - DOMINATE LOW END!",
                    urgency = Urgency.HIGH,
                    suggestedAction = "Max sub-bass now!"
                )
            }
            
            // Opponent lacks clarity - cut through!
            analysis.highMidLevel < 0.3f -> {
                AttackOpportunity(
                    type = OpportunityType.CLARITY_GAP,
                    message = "⚔️ CLARITY GAP - CUT THROUGH!",
                    urgency = Urgency.MEDIUM,
                    suggestedAction = "Boost presence frequencies"
                )
            }
            
            // Opponent is transitioning between songs
            analysis.isTransitioning -> {
                AttackOpportunity(
                    type = OpportunityType.TRANSITION,
                    message = "🔄 OPPONENT TRANSITIONING - ATTACK!",
                    urgency = Urgency.CRITICAL,
                    suggestedAction = "Drop your biggest hit NOW!"
                )
            }
            
            else -> null
        }
        
        if (opportunity != null && opportunity != _attackOpportunity.value) {
            _attackOpportunity.value = opportunity
            
            // Vibrate phone for critical opportunities
            if (_attackAlertsEnabled.value && opportunity.urgency == Urgency.CRITICAL) {
                vibrateAlert()
            }
            
            // Auto-execute in aggressive mode
            if (_battleMode.value == ActiveBattleMode.AGGRESSIVE) {
                executeOpportunity(opportunity)
            }
            
            logEvent("🎯 OPPORTUNITY!", opportunity.message)
        } else if (opportunity == null) {
            _attackOpportunity.value = null
        }
    }
    
    private fun executeOpportunity(opportunity: AttackOpportunity) {
        val engine = battleEngine ?: return
        
        when (opportunity.type) {
            OpportunityType.SILENCE_DETECTED -> {
                engine.emergencyBassBoost()
                addAttack(ActiveAttack(
                    type = AttackType.BASS_DROP,
                    target = "Silence",
                    intensity = 100,
                    timestamp = System.currentTimeMillis()
                ))
            }
            OpportunityType.BASS_GAP -> {
                engine.setBassBoost(1000)
                engine.setEQBand(0, 1500)
                engine.setEQBand(1, 1200)
            }
            OpportunityType.CLARITY_GAP -> {
                engine.cutThrough()
            }
            OpportunityType.TRANSITION -> {
                engine.goNuclear()
            }
            OpportunityType.CROWD_FADING -> {
                // Play crowd pleaser
            }
        }
    }
    
    // ==================== FEATURE 4: MOMENTUM TRACKING ====================
    
    /**
     * Tracks who's winning the battle
     * 0 = We're losing badly
     * 50 = Even
     * 100 = We're dominating
     */
    private fun updateMomentum(analysis: FrequencyAnalysis) {
        var delta = 0
        
        // SPL comparison
        val splDiff = _ourSPL.value - analysis.overallSPL
        delta += when {
            splDiff > 6 -> 2   // We're much louder
            splDiff > 3 -> 1   // We're louder
            splDiff < -6 -> -2 // They're much louder
            splDiff < -3 -> -1 // They're louder
            else -> 0
        }
        
        // Frequency dominance
        if (analysis.weakBands.size >= 2) delta += 1  // They have gaps
        if (analysis.strongBands.size >= 3) delta -= 1 // They're well-rounded
        
        // Transitions (we're more stable)
        if (analysis.isTransitioning) delta += 2
        
        // Apply delta with smoothing
        val newMomentum = (_momentum.value + delta).coerceIn(0, 100)
        _momentum.value = newMomentum
        
        // Log significant momentum shifts
        if (newMomentum >= 70 && _momentum.value < 70) {
            logEvent("🔥 MOMENTUM SHIFT!", "We're taking over!")
        } else if (newMomentum <= 30 && _momentum.value > 30) {
            logEvent("⚠️ LOSING MOMENTUM", "Fight back!")
        }
    }
    
    // ==================== FEATURE 5: SMART SONG QUEUE ====================
    
    /**
     * AI picks the next song based on battle situation
     */
    private fun suggestNextSong(analysis: FrequencyAnalysis) {
        if (songLibrary.isEmpty() || songRatings.isEmpty()) return
        
        val currentMomentum = _momentum.value
        val opponentStrategy = detectOpponentStrategy(analysis)
        
        // Pick song based on situation
        val idealSong = when {
            // We're losing - need a crowd pleaser
            currentMomentum < 30 -> {
                songRatings.entries
                    .filter { it.value.crowdAppeal >= 80 }
                    .maxByOrNull { it.value.crowdAppeal }
                    ?.key
            }
            
            // We're winning - keep pressure with high energy
            currentMomentum > 70 -> {
                songRatings.entries
                    .filter { it.value.energy >= 80 }
                    .maxByOrNull { it.value.energy }
                    ?.key
            }
            
            // Opponent is bass heavy - counter with clarity
            opponentStrategy == OpponentStrategy.BASS_HEAVY -> {
                songRatings.entries
                    .filter { it.value.clarity >= 70 }
                    .maxByOrNull { it.value.clarity }
                    ?.key
            }
            
            // Opponent is clarity focused - overpower with bass
            opponentStrategy == OpponentStrategy.CLARITY_FOCUSED -> {
                songRatings.entries
                    .filter { it.value.bassImpact >= 80 }
                    .maxByOrNull { it.value.bassImpact }
                    ?.key
            }
            
            // Default - highest overall rating
            else -> {
                songRatings.entries
                    .maxByOrNull { it.value.overallRating }
                    ?.key
            }
        }
        
        idealSong?.let { songId ->
            val song = songLibrary.find { it.id == songId }
            if (song != _nextSongSuggestion.value) {
                _nextSongSuggestion.value = song
                song?.let {
                    logEvent("🎵 NEXT SONG", "${it.title} - ${songRatings[songId]?.battleTier?.label}")
                }
            }
        }
    }
    
    /**
     * Auto-play the suggested song (if enabled)
     */
    fun playNextSuggestion() {
        _nextSongSuggestion.value?.let { song ->
            musicController?.playSong(song)
            logEvent("▶️ PLAYING", song.title)
        }
    }
    
    // ==================== FEATURE 6: ACTIVE ATTACKS ====================
    
    /**
     * Execute pre-programmed attack sequences
     */
    fun executeBattleScript(script: BattleScript) {
        scope.launch {
            logEvent("📜 SCRIPT: ${script.name}", "Executing...")
            
            for (action in script.actions) {
                when (action) {
                    is ScriptAction.Wait -> delay(action.durationMs)
                    is ScriptAction.SetBass -> battleEngine?.setBassBoost(action.level)
                    is ScriptAction.SetLoudness -> battleEngine?.setLoudness(action.level)
                    is ScriptAction.SetClarity -> battleEngine?.setClarity(action.level)
                    is ScriptAction.SetEQ -> battleEngine?.setEQBand(action.band, action.level)
                    is ScriptAction.BassDropSequence -> {
                        // Dramatic bass drop
                        battleEngine?.setBassBoost(0)
                        delay(500)
                        battleEngine?.emergencyBassBoost()
                        vibrateAlert()
                    }
                    is ScriptAction.BuildUp -> {
                        // Gradual build
                        for (i in 0..10) {
                            battleEngine?.setBassBoost(i * 100)
                            battleEngine?.setLoudness(i * 100)
                            delay(100)
                        }
                    }
                    is ScriptAction.Nuclear -> {
                        battleEngine?.goNuclear()
                    }
                }
            }
            
            logEvent("✅ SCRIPT COMPLETE", script.name)
        }
    }
    
    private fun executeActiveAttacks(analysis: FrequencyAnalysis) {
        // Clean up old attacks
        val now = System.currentTimeMillis()
        _activeAttacks.value = _activeAttacks.value.filter { 
            now - it.timestamp < 5000 // Keep for 5 seconds
        }
    }
    
    private fun addAttack(attack: ActiveAttack) {
        _activeAttacks.value = _activeAttacks.value + attack
    }
    
    // ==================== FEATURE 7: COUNTER-ATTACK ====================
    
    /**
     * When opponent makes a big move, we counter harder
     */
    fun enableCounterAttackMode() {
        scope.launch {
            var lastOpponentSPL = _opponentSPL.value
            
            while (_battleState.value == ActiveBattleState.ACTIVE) {
                val currentSPL = _opponentSPL.value
                
                // Opponent just got louder - COUNTER!
                if (currentSPL - lastOpponentSPL > 10) {
                    logEvent("⚡ COUNTER-ATTACK!", "Opponent boosted - countering!")
                    battleEngine?.goNuclear()
                    vibrateAlert()
                    
                    addAttack(ActiveAttack(
                        type = AttackType.COUNTER_ATTACK,
                        target = "Volume Spike",
                        intensity = 100,
                        timestamp = System.currentTimeMillis()
                    ))
                }
                
                lastOpponentSPL = currentSPL
                delay(200)
            }
        }
    }
    
    // ==================== AUDIO CAPTURE ====================

    private fun startAudioCapture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            val sampleRate = 44100
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2
            )
            audioRecord?.startRecording()
        } catch (e: SecurityException) {
            // Permission can still be revoked at runtime
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopAudioCapture() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    private fun captureAndAnalyze(): FrequencyAnalysis {
        val record = audioRecord ?: return FrequencyAnalysis()
        
        val buffer = ShortArray(4096)
        val read = record.read(buffer, 0, buffer.size)
        
        if (read <= 0) return FrequencyAnalysis()
        
        // Calculate SPL
        var sum = 0.0
        for (i in 0 until read) {
            sum += buffer[i] * buffer[i]
        }
        val rms = sqrt(sum / read)
        val spl = (20 * kotlin.math.log10(rms / 32768.0) + 90).toFloat().coerceIn(0f, 140f)
        
        // Simple frequency band analysis
        val bandSize = read / 6
        val bands = (0 until 6).map { band ->
            var bandSum = 0.0
            for (i in 0 until bandSize) {
                val idx = band * bandSize + i
                if (idx < read) bandSum += abs(buffer[idx].toDouble())
            }
            (bandSum / bandSize / 32768.0).toFloat()
        }
        
        // Detect transitions (sudden level changes)
        val isTransitioning = abs(spl - _opponentSPL.value) > 15
        
        // Find weak and strong bands
        val weakBands = mutableListOf<FrequencyBand>()
        val strongBands = mutableListOf<FrequencyBand>()
        
        val allBands = listOf(
            FrequencyBand.SUB_BASS to bands.getOrElse(0) { 0f },
            FrequencyBand.BASS to bands.getOrElse(1) { 0f },
            FrequencyBand.LOW_MID to bands.getOrElse(2) { 0f },
            FrequencyBand.MID to bands.getOrElse(3) { 0f },
            FrequencyBand.HIGH_MID to bands.getOrElse(4) { 0f },
            FrequencyBand.HIGH to bands.getOrElse(5) { 0f }
        )
        
        allBands.forEach { (band, level) ->
            when {
                level < 0.3f -> weakBands.add(band)
                level > 0.7f -> strongBands.add(band)
            }
        }
        
        return FrequencyAnalysis(
            overallSPL = spl,
            subBassLevel = bands.getOrElse(0) { 0f },
            bassLevel = bands.getOrElse(1) { 0f },
            lowMidLevel = bands.getOrElse(2) { 0f },
            midLevel = bands.getOrElse(3) { 0f },
            highMidLevel = bands.getOrElse(4) { 0f },
            highLevel = bands.getOrElse(5) { 0f },
            weakBands = weakBands,
            strongBands = strongBands,
            isTransitioning = isTransitioning,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun detectOpponentStrategy(analysis: FrequencyAnalysis): OpponentStrategy {
        return when {
            analysis.subBassLevel > 0.7f && analysis.bassLevel > 0.6f -> OpponentStrategy.BASS_HEAVY
            analysis.highMidLevel > 0.6f && analysis.highLevel > 0.5f -> OpponentStrategy.CLARITY_FOCUSED
            analysis.midLevel > 0.6f -> OpponentStrategy.VOCAL_HEAVY
            else -> OpponentStrategy.BALANCED
        }
    }
    
    // ==================== UTILITIES ====================
    
    private fun vibrateAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun logEvent(title: String, message: String) {
        val entry = BattleLogEntry(
            timestamp = System.currentTimeMillis(),
            title = title,
            message = message
        )
        _battleLog.value = (_battleLog.value + entry).takeLast(50)
    }
    
    // ==================== BATTLE SCRIPTS ====================
    
    companion object {
        val SCRIPTS = listOf(
            BattleScript(
                name = "💥 BASS DROP",
                description = "Dramatic silence then BOOM",
                actions = listOf(
                    ScriptAction.SetBass(0),
                    ScriptAction.SetLoudness(200),
                    ScriptAction.Wait(1000),
                    ScriptAction.BassDropSequence
                )
            ),
            BattleScript(
                name = "📈 BUILD UP",
                description = "Gradual intensity increase",
                actions = listOf(
                    ScriptAction.BuildUp,
                    ScriptAction.Nuclear
                )
            ),
            BattleScript(
                name = "⚡ SHOCK ATTACK",
                description = "Sudden full power",
                actions = listOf(
                    ScriptAction.Nuclear,
                    ScriptAction.Wait(5000),
                    ScriptAction.SetBass(800),
                    ScriptAction.SetLoudness(700)
                )
            ),
            BattleScript(
                name = "🌊 BASS WAVE",
                description = "Pulsing bass attack",
                actions = listOf(
                    ScriptAction.SetBass(1000),
                    ScriptAction.Wait(500),
                    ScriptAction.SetBass(300),
                    ScriptAction.Wait(500),
                    ScriptAction.SetBass(1000),
                    ScriptAction.Wait(500),
                    ScriptAction.SetBass(300),
                    ScriptAction.Wait(500),
                    ScriptAction.SetBass(1000)
                )
            ),
            BattleScript(
                name = "🎯 PRECISION STRIKE",
                description = "Target clarity frequencies",
                actions = listOf(
                    ScriptAction.SetEQ(3, 1500),
                    ScriptAction.SetEQ(4, 1200),
                    ScriptAction.SetClarity(100),
                    ScriptAction.Wait(3000)
                )
            )
        )
    }
}

// ==================== DATA CLASSES ====================

enum class ActiveBattleState {
    IDLE,
    ACTIVE,
    PAUSED,
    ENDED
}

enum class ActiveBattleMode(val description: String) {
    AGGRESSIVE("All-out attack, counter everything"),
    DEFENSIVE("Protect our frequencies, smart counters"),
    BALANCED("Mix of attack and defense"),
    STEALTH("Start quiet, build up, surprise attack"),
    COUNTER_ONLY("Only counter, no auto-volume")
}

data class FrequencyAnalysis(
    val overallSPL: Float = 0f,
    val subBassLevel: Float = 0f,
    val bassLevel: Float = 0f,
    val lowMidLevel: Float = 0f,
    val midLevel: Float = 0f,
    val highMidLevel: Float = 0f,
    val highLevel: Float = 0f,
    val weakBands: List<FrequencyBand> = emptyList(),
    val strongBands: List<FrequencyBand> = emptyList(),
    val isTransitioning: Boolean = false,
    val timestamp: Long = 0
) {
    val dominantBand: FrequencyBand
        get() = strongBands.firstOrNull() ?: FrequencyBand.MID
}

data class ActiveAttack(
    val type: AttackType,
    val target: String,
    val intensity: Int,  // 0-100
    val timestamp: Long
)

enum class AttackType {
    FREQUENCY_EXPLOIT,
    BASS_DROP,
    CLARITY_STRIKE,
    VOLUME_SURGE,
    COUNTER_ATTACK,
    SCRIPT_EXECUTION
}

data class AttackOpportunity(
    val type: OpportunityType,
    val message: String,
    val urgency: Urgency,
    val suggestedAction: String
)

enum class OpportunityType {
    SILENCE_DETECTED,
    BASS_GAP,
    CLARITY_GAP,
    TRANSITION,
    CROWD_FADING
}

enum class Urgency {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class BattleLogEntry(
    val timestamp: Long,
    val title: String,
    val message: String
)

data class BattleScript(
    val name: String,
    val description: String,
    val actions: List<ScriptAction>
)

sealed class ScriptAction {
    data class Wait(val durationMs: Long) : ScriptAction()
    data class SetBass(val level: Int) : ScriptAction()
    data class SetLoudness(val level: Int) : ScriptAction()
    data class SetClarity(val level: Int) : ScriptAction()
    data class SetEQ(val band: Int, val level: Int) : ScriptAction()
    object BassDropSequence : ScriptAction()
    object BuildUp : ScriptAction()
    object Nuclear : ScriptAction()
}
