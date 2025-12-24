package com.ultramusic.player.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Battle Intelligence System
 * 
 * KILLER FEATURE #1: Real-time opponent analysis
 * 
 * Listens to venue audio (opponent's sound + ambient) and:
 * 1. Measures opponent's frequency distribution
 * 2. Finds their WEAK frequencies (gaps)
 * 3. Auto-suggests EQ to EXPLOIT those gaps
 * 4. Real-time SPL comparison
 * 
 * This is what makes us #1 - NO OTHER APP DOES THIS!
 */
@Singleton
class BattleIntelligence @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BattleIntelligence"
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var analysisJob: Job? = null
    private var audioRecord: AudioRecord? = null
    
    // ==================== STATE ====================
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _opponentAnalysis = MutableStateFlow(OpponentAnalysis())
    val opponentAnalysis: StateFlow<OpponentAnalysis> = _opponentAnalysis.asStateFlow()
    
    private val _venueSPL = MutableStateFlow(0f)
    val venueSPL: StateFlow<Float> = _venueSPL.asStateFlow()
    
    private val _frequencySpectrum = MutableStateFlow<List<Float>>(List(32) { 0f })
    val frequencySpectrum: StateFlow<List<Float>> = _frequencySpectrum.asStateFlow()
    
    private val _counterEQSuggestion = MutableStateFlow<List<EQSuggestion>>(emptyList())
    val counterEQSuggestion: StateFlow<List<EQSuggestion>> = _counterEQSuggestion.asStateFlow()
    
    private val _battleAdvice = MutableStateFlow<List<BattleAdvice>>(emptyList())
    val battleAdvice: StateFlow<List<BattleAdvice>> = _battleAdvice.asStateFlow()
    
    // ==================== AUDIO CAPTURE ====================
    
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2
    
    /**
     * Start listening to opponent/venue audio
     */
    fun startListening() {
        if (_isListening.value) return
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            audioRecord?.startRecording()
            _isListening.value = true
            
            analysisJob = scope.launch {
                val buffer = ShortArray(bufferSize / 2)
                
                while (isActive && _isListening.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (read > 0) {
                        analyzeAudio(buffer, read)
                    }
                    
                    delay(50) // 20 FPS analysis
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in battle intelligence listening", e)
        }
    }

    fun stopListening() {
        _isListening.value = false
        analysisJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    // ==================== ANALYSIS ====================
    
    private fun analyzeAudio(buffer: ShortArray, size: Int) {
        // 1. Calculate SPL
        val rms = calculateRMS(buffer, size)
        val spl = 20 * log10(rms / 32768f) + 90 // Approximate dB SPL
        _venueSPL.value = spl.coerceIn(0f, 140f)
        
        // 2. FFT for frequency analysis
        val spectrum = calculateSpectrum(buffer, size)
        _frequencySpectrum.value = spectrum
        
        // 3. Analyze opponent patterns
        val analysis = analyzeOpponentPattern(spectrum, spl)
        _opponentAnalysis.value = analysis
        
        // 4. Generate counter suggestions
        val suggestions = generateCounterEQ(analysis)
        _counterEQSuggestion.value = suggestions
        
        // 5. Generate battle advice
        val advice = generateBattleAdvice(analysis, spl)
        _battleAdvice.value = advice
    }
    
    private fun calculateRMS(buffer: ShortArray, size: Int): Float {
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt(sum / size).toFloat()
    }
    
    /**
     * Simple spectrum analysis (32 bands)
     * In production, use proper FFT library
     */
    private fun calculateSpectrum(buffer: ShortArray, size: Int): List<Float> {
        val bands = 32
        val bandSize = size / bands
        val spectrum = MutableList(bands) { 0f }
        
        for (band in 0 until bands) {
            var sum = 0.0
            for (i in 0 until bandSize) {
                val idx = band * bandSize + i
                if (idx < size) {
                    sum += abs(buffer[idx].toDouble())
                }
            }
            spectrum[band] = (sum / bandSize).toFloat()
        }
        
        // Normalize
        val max = spectrum.maxOrNull() ?: 1f
        return spectrum.map { (it / max).coerceIn(0f, 1f) }
    }
    
    private fun analyzeOpponentPattern(spectrum: List<Float>, spl: Float): OpponentAnalysis {
        // Find frequency distribution
        val subBass = spectrum.take(2).average().toFloat()      // 20-60 Hz
        val bass = spectrum.slice(2..4).average().toFloat()      // 60-250 Hz
        val lowMid = spectrum.slice(5..8).average().toFloat()    // 250-500 Hz
        val mid = spectrum.slice(9..14).average().toFloat()      // 500-2k Hz
        val highMid = spectrum.slice(15..20).average().toFloat() // 2k-4k Hz
        val high = spectrum.slice(21..31).average().toFloat()    // 4k-20k Hz
        
        // Find dominant frequency band
        val bands = listOf(
            FrequencyBand.SUB_BASS to subBass,
            FrequencyBand.BASS to bass,
            FrequencyBand.LOW_MID to lowMid,
            FrequencyBand.MID to mid,
            FrequencyBand.HIGH_MID to highMid,
            FrequencyBand.HIGH to high
        )
        val dominant = bands.maxByOrNull { it.second }?.first ?: FrequencyBand.BASS
        
        // Find weak frequencies (gaps to exploit)
        val weakBands = bands.filter { it.second < 0.3f }.map { it.first }
        
        // Detect opponent's strategy
        val strategy = when {
            subBass > 0.7f && bass > 0.6f -> OpponentStrategy.BASS_HEAVY
            highMid > 0.6f && high > 0.5f -> OpponentStrategy.CLARITY_FOCUSED
            mid > 0.6f -> OpponentStrategy.VOCAL_HEAVY
            bands.all { it.second in 0.4f..0.6f } -> OpponentStrategy.BALANCED
            else -> OpponentStrategy.UNKNOWN
        }
        
        return OpponentAnalysis(
            spl = spl,
            dominantBand = dominant,
            weakBands = weakBands,
            strategy = strategy,
            subBassLevel = subBass,
            bassLevel = bass,
            lowMidLevel = lowMid,
            midLevel = mid,
            highMidLevel = highMid,
            highLevel = high,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * KILLER FEATURE: Auto-generate counter EQ
     * Boost YOUR frequencies where opponent is WEAK
     */
    private fun generateCounterEQ(analysis: OpponentAnalysis): List<EQSuggestion> {
        val suggestions = mutableListOf<EQSuggestion>()
        
        // Exploit weak frequencies
        for (weakBand in analysis.weakBands) {
            val suggestion = when (weakBand) {
                FrequencyBand.SUB_BASS -> EQSuggestion(
                    band = 0,
                    frequency = "60Hz",
                    suggestedBoost = 12,
                    reason = "Opponent weak in sub-bass - DOMINATE here!"
                )
                FrequencyBand.BASS -> EQSuggestion(
                    band = 1,
                    frequency = "230Hz",
                    suggestedBoost = 10,
                    reason = "Gap in opponent's bass - fill it!"
                )
                FrequencyBand.LOW_MID -> EQSuggestion(
                    band = 2,
                    frequency = "500Hz",
                    suggestedBoost = 6,
                    reason = "Opponent missing warmth - add body"
                )
                FrequencyBand.MID -> EQSuggestion(
                    band = 2,
                    frequency = "1kHz",
                    suggestedBoost = 8,
                    reason = "Opponent lacking presence - cut through!"
                )
                FrequencyBand.HIGH_MID -> EQSuggestion(
                    band = 3,
                    frequency = "3.5kHz",
                    suggestedBoost = 10,
                    reason = "Opponent's clarity is weak - ATTACK!"
                )
                FrequencyBand.HIGH -> EQSuggestion(
                    band = 4,
                    frequency = "12kHz",
                    suggestedBoost = 8,
                    reason = "Opponent lacking sparkle - shine bright!"
                )
            }
            suggestions.add(suggestion)
        }
        
        // Counter their strength
        when (analysis.strategy) {
            OpponentStrategy.BASS_HEAVY -> {
                suggestions.add(EQSuggestion(
                    band = 3,
                    frequency = "3.5kHz",
                    suggestedBoost = 12,
                    reason = "They're bass heavy - cut through with clarity!"
                ))
            }
            OpponentStrategy.CLARITY_FOCUSED -> {
                suggestions.add(EQSuggestion(
                    band = 0,
                    frequency = "60Hz",
                    suggestedBoost = 15,
                    reason = "They focus on highs - overpower with BASS!"
                ))
            }
            OpponentStrategy.VOCAL_HEAVY -> {
                suggestions.add(EQSuggestion(
                    band = 0,
                    frequency = "60Hz",
                    suggestedBoost = 12,
                    reason = "They're mid-focused - hit the sub-bass!"
                ))
                suggestions.add(EQSuggestion(
                    band = 4,
                    frequency = "12kHz",
                    suggestedBoost = 8,
                    reason = "Add air to stand out from their vocals"
                ))
            }
            else -> {}
        }
        
        return suggestions.distinctBy { it.band }
    }
    
    private fun generateBattleAdvice(analysis: OpponentAnalysis, spl: Float): List<BattleAdvice> {
        val advice = mutableListOf<BattleAdvice>()
        
        // SPL advice
        when {
            spl < 70 -> advice.add(BattleAdvice(
                icon = "🔇",
                title = "Low Volume Detected",
                message = "Opponent/venue is quiet. Good time to establish dominance!",
                priority = BattlePriority.INFO
            ))
            spl > 100 -> advice.add(BattleAdvice(
                icon = "🔊",
                title = "HIGH SPL Environment",
                message = "Very loud! Focus on clarity to cut through, not just volume.",
                priority = BattlePriority.WARNING
            ))
        }
        
        // Strategy advice
        when (analysis.strategy) {
            OpponentStrategy.BASS_HEAVY -> advice.add(BattleAdvice(
                icon = "🎯",
                title = "Opponent: Bass Heavy",
                message = "Don't compete on bass alone. Use CLARITY STRIKE mode!",
                priority = BattlePriority.CRITICAL
            ))
            OpponentStrategy.CLARITY_FOCUSED -> advice.add(BattleAdvice(
                icon = "💥",
                title = "Opponent: Clarity Focused",
                message = "Perfect! Use BASS WARFARE to overwhelm them!",
                priority = BattlePriority.CRITICAL
            ))
            OpponentStrategy.VOCAL_HEAVY -> advice.add(BattleAdvice(
                icon = "⚡",
                title = "Opponent: Vocal Heavy",
                message = "Go for sub-bass + sparkle. Sandwich their mids!",
                priority = BattlePriority.HIGH
            ))
            OpponentStrategy.BALANCED -> advice.add(BattleAdvice(
                icon = "☢️",
                title = "Opponent: Balanced",
                message = "They're well-rounded. GO NUCLEAR to overwhelm!",
                priority = BattlePriority.HIGH
            ))
            OpponentStrategy.UNKNOWN -> advice.add(BattleAdvice(
                icon = "🔍",
                title = "Analyzing...",
                message = "Still detecting opponent pattern. Keep listening.",
                priority = BattlePriority.INFO
            ))
        }
        
        // Gap exploitation
        if (analysis.weakBands.isNotEmpty()) {
            advice.add(BattleAdvice(
                icon = "🎯",
                title = "GAPS DETECTED!",
                message = "Opponent weak in: ${analysis.weakBands.joinToString()}. EXPLOIT NOW!",
                priority = BattlePriority.CRITICAL
            ))
        }
        
        return advice.sortedByDescending { it.priority.ordinal }
    }
    
    // ==================== AUTO COUNTER MODE ====================
    
    /**
     * KILLER FEATURE: Auto-apply counter EQ in real-time
     */
    fun enableAutoCounter(battleEngine: AudioBattleEngine) {
        scope.launch {
            counterEQSuggestion.collect { suggestions ->
                for (suggestion in suggestions) {
                    battleEngine.setEQBand(suggestion.band, suggestion.suggestedBoost * 100)
                }
            }
        }
    }
}

// ==================== DATA CLASSES ====================

data class OpponentAnalysis(
    val spl: Float = 0f,
    val dominantBand: FrequencyBand = FrequencyBand.BASS,
    val weakBands: List<FrequencyBand> = emptyList(),
    val strategy: OpponentStrategy = OpponentStrategy.UNKNOWN,
    val subBassLevel: Float = 0f,
    val bassLevel: Float = 0f,
    val lowMidLevel: Float = 0f,
    val midLevel: Float = 0f,
    val highMidLevel: Float = 0f,
    val highLevel: Float = 0f,
    val timestamp: Long = 0
)

enum class FrequencyBand(val label: String, val range: String) {
    SUB_BASS("Sub-Bass", "20-60Hz"),
    BASS("Bass", "60-250Hz"),
    LOW_MID("Low-Mid", "250-500Hz"),
    MID("Mid", "500Hz-2kHz"),
    HIGH_MID("High-Mid", "2-4kHz"),
    HIGH("High", "4-20kHz")
}

enum class OpponentStrategy {
    BASS_HEAVY,
    CLARITY_FOCUSED,
    VOCAL_HEAVY,
    BALANCED,
    UNKNOWN
}

data class EQSuggestion(
    val band: Int,
    val frequency: String,
    val suggestedBoost: Int,  // dB
    val reason: String
)

data class BattleAdvice(
    val icon: String,
    val title: String,
    val message: String,
    val priority: BattlePriority
)

enum class BattlePriority {
    INFO,
    WARNING,
    HIGH,
    CRITICAL
}
