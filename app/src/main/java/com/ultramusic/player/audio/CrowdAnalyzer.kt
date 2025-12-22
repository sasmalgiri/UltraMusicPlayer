package com.ultramusic.player.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import kotlin.math.sqrt

/**
 * CROWD ANALYZER
 * 
 * Listens to crowd reactions and helps you understand:
 * - Are they cheering for you or opponent?
 * - Is energy rising or falling?
 * - When to drop the bass?
 * - Perfect timing for transitions
 * 
 * The crowd TELLS you who's winning - we just listen!
 */
@Singleton
class CrowdAnalyzer @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var analysisJob: Job? = null
    
    // ==================== STATE ====================
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _crowdEnergy = MutableStateFlow(50) // 0-100
    val crowdEnergy: StateFlow<Int> = _crowdEnergy.asStateFlow()
    
    private val _crowdTrend = MutableStateFlow(CrowdTrend.STABLE)
    val crowdTrend: StateFlow<CrowdTrend> = _crowdTrend.asStateFlow()
    
    private val _crowdMood = MutableStateFlow(CrowdMood.NEUTRAL)
    val crowdMood: StateFlow<CrowdMood> = _crowdMood.asStateFlow()
    
    private val _peakMoments = MutableStateFlow<List<PeakMoment>>(emptyList())
    val peakMoments: StateFlow<List<PeakMoment>> = _peakMoments.asStateFlow()
    
    private val _dropRecommendation = MutableStateFlow<DropRecommendation?>(null)
    val dropRecommendation: StateFlow<DropRecommendation?> = _dropRecommendation.asStateFlow()
    
    private val _crowdHistory = MutableStateFlow<List<CrowdSnapshot>>(emptyList())
    val crowdHistory: StateFlow<List<CrowdSnapshot>> = _crowdHistory.asStateFlow()
    
    // Energy tracking for trend detection
    private val energyWindow = mutableListOf<Int>()
    private val maxWindowSize = 30 // 3 seconds at 100ms intervals
    
    // ==================== CONTROL ====================
    
    fun startAnalyzing() {
        if (_isAnalyzing.value) return
        _isAnalyzing.value = true
        
        analysisJob = scope.launch {
            while (isActive && _isAnalyzing.value) {
                analyzeCrowd()
                delay(100) // 10 FPS
            }
        }
    }
    
    fun stopAnalyzing() {
        _isAnalyzing.value = false
        analysisJob?.cancel()
    }
    
    // ==================== ANALYSIS ====================
    
    private suspend fun analyzeCrowd() {
        // In production, this would use the microphone
        // For now, we simulate based on patterns
        
        val currentEnergy = detectCrowdEnergy()
        _crowdEnergy.value = currentEnergy
        
        // Track energy for trend detection
        energyWindow.add(currentEnergy)
        if (energyWindow.size > maxWindowSize) {
            energyWindow.removeAt(0)
        }
        
        // Detect trend
        _crowdTrend.value = detectTrend()
        
        // Detect mood
        _crowdMood.value = detectMood(currentEnergy)
        
        // Detect peak moments
        detectPeakMoment(currentEnergy)
        
        // Generate drop recommendation
        updateDropRecommendation()
        
        // Save snapshot
        saveSnapshot(currentEnergy)
    }
    
    private fun detectCrowdEnergy(): Int {
        // In production: Analyze audio for cheering, clapping, singing along
        // Indicators of high energy:
        // - High SPL in voice frequencies (300Hz - 3kHz)
        // - Rhythmic patterns (clapping)
        // - Sustained noise (cheering)
        
        // Simulated for now - would use real audio analysis
        val baseEnergy = _crowdEnergy.value
        val variation = (-5..5).random()
        return (baseEnergy + variation).coerceIn(0, 100)
    }
    
    private fun detectTrend(): CrowdTrend {
        if (energyWindow.size < 10) return CrowdTrend.STABLE
        
        val recent = energyWindow.takeLast(10).average()
        val earlier = energyWindow.take(10).average()
        
        val diff = recent - earlier
        
        return when {
            diff > 15 -> CrowdTrend.SURGING
            diff > 5 -> CrowdTrend.RISING
            diff < -15 -> CrowdTrend.CRASHING
            diff < -5 -> CrowdTrend.FALLING
            else -> CrowdTrend.STABLE
        }
    }
    
    private fun detectMood(energy: Int): CrowdMood {
        val trend = _crowdTrend.value
        
        return when {
            energy >= 80 && trend == CrowdTrend.SURGING -> CrowdMood.ECSTATIC
            energy >= 70 -> CrowdMood.HYPED
            energy >= 50 -> CrowdMood.ENGAGED
            energy >= 30 -> CrowdMood.WAITING
            trend == CrowdTrend.FALLING -> CrowdMood.LOSING_INTEREST
            else -> CrowdMood.NEUTRAL
        }
    }
    
    private fun detectPeakMoment(currentEnergy: Int) {
        // Detect if we just hit a peak
        if (energyWindow.size < 5) return
        
        val recent = energyWindow.takeLast(5)
        val peak = recent.maxOrNull() ?: 0
        val isPeak = currentEnergy == peak && currentEnergy >= 75
        
        if (isPeak) {
            val moment = PeakMoment(
                timestamp = System.currentTimeMillis(),
                energy = currentEnergy,
                type = when {
                    currentEnergy >= 90 -> PeakType.MASSIVE
                    currentEnergy >= 80 -> PeakType.BIG
                    else -> PeakType.NORMAL
                }
            )
            _peakMoments.value = (_peakMoments.value + moment).takeLast(20)
        }
    }
    
    private fun updateDropRecommendation() {
        val energy = _crowdEnergy.value
        val trend = _crowdTrend.value
        val mood = _crowdMood.value
        
        val recommendation = when {
            // Perfect drop moment: Energy building, crowd waiting
            trend == CrowdTrend.RISING && energy in 60..75 -> {
                DropRecommendation(
                    timing = DropTiming.PERFECT,
                    message = "🎯 PERFECT DROP MOMENT!",
                    countdown = 3,
                    confidence = 95
                )
            }
            
            // Good drop moment: High energy, crowd hyped
            mood == CrowdMood.HYPED && energy >= 70 -> {
                DropRecommendation(
                    timing = DropTiming.GOOD,
                    message = "👍 Good time to drop!",
                    countdown = 5,
                    confidence = 80
                )
            }
            
            // Build more: Energy too low
            energy < 50 -> {
                DropRecommendation(
                    timing = DropTiming.BUILD_MORE,
                    message = "📈 Build more energy first",
                    countdown = 10,
                    confidence = 60
                )
            }
            
            // Wait: Trend falling
            trend == CrowdTrend.FALLING -> {
                DropRecommendation(
                    timing = DropTiming.WAIT,
                    message = "⏳ Wait for energy to return",
                    countdown = 15,
                    confidence = 50
                )
            }
            
            // Ready: Stable high energy
            energy >= 65 && trend == CrowdTrend.STABLE -> {
                DropRecommendation(
                    timing = DropTiming.READY,
                    message = "✅ Ready when you are",
                    countdown = 5,
                    confidence = 75
                )
            }
            
            else -> null
        }
        
        _dropRecommendation.value = recommendation
    }
    
    private fun saveSnapshot(energy: Int) {
        val snapshot = CrowdSnapshot(
            timestamp = System.currentTimeMillis(),
            energy = energy,
            trend = _crowdTrend.value,
            mood = _crowdMood.value
        )
        _crowdHistory.value = (_crowdHistory.value + snapshot).takeLast(600) // 1 minute
    }
    
    // ==================== HELPERS ====================
    
    /**
     * Get crowd reaction score for last N seconds
     */
    fun getReactionScore(lastSeconds: Int): Int {
        val snapshots = _crowdHistory.value
            .filter { System.currentTimeMillis() - it.timestamp < lastSeconds * 1000 }
        
        return if (snapshots.isEmpty()) 50
        else snapshots.map { it.energy }.average().toInt()
    }
    
    /**
     * Predict best time for next drop
     */
    fun predictNextDropWindow(): Long {
        val trend = _crowdTrend.value
        val energy = _crowdEnergy.value
        
        // Estimate milliseconds until good drop moment
        return when {
            trend == CrowdTrend.RISING && energy > 60 -> 2000  // 2 seconds
            trend == CrowdTrend.RISING -> 5000                  // 5 seconds
            trend == CrowdTrend.STABLE && energy > 50 -> 3000  // 3 seconds
            else -> 10000                                       // 10 seconds
        }
    }
    
    /**
     * Check if crowd is responding to your music
     */
    fun isRespondingToUs(): Boolean {
        // Compare energy during our play vs opponent's
        // Would need timing info from battle system
        return _crowdEnergy.value > 60 && _crowdTrend.value != CrowdTrend.FALLING
    }
}

// ==================== DATA CLASSES ====================

enum class CrowdTrend(val emoji: String, val description: String) {
    SURGING("🚀", "Energy exploding!"),
    RISING("📈", "Energy building"),
    STABLE("➡️", "Energy steady"),
    FALLING("📉", "Energy dropping"),
    CRASHING("💥", "Crowd losing interest")
}

enum class CrowdMood(val emoji: String, val description: String) {
    ECSTATIC("🤩", "Crowd going crazy!"),
    HYPED("🔥", "Crowd is hyped!"),
    ENGAGED("😊", "Crowd is into it"),
    NEUTRAL("😐", "Crowd is neutral"),
    WAITING("⏳", "Crowd waiting for something"),
    LOSING_INTEREST("😴", "Crowd getting bored")
}

data class PeakMoment(
    val timestamp: Long,
    val energy: Int,
    val type: PeakType
)

enum class PeakType {
    NORMAL,
    BIG,
    MASSIVE
}

data class DropRecommendation(
    val timing: DropTiming,
    val message: String,
    val countdown: Int,  // seconds
    val confidence: Int  // 0-100
)

enum class DropTiming {
    PERFECT,
    GOOD,
    READY,
    BUILD_MORE,
    WAIT
}

data class CrowdSnapshot(
    val timestamp: Long,
    val energy: Int,
    val trend: CrowdTrend,
    val mood: CrowdMood
)
