package com.ultramusic.player.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Venue Profiler
 * 
 * KILLER FEATURE #3: Detect venue acoustics and auto-optimize
 * 
 * Uses test tones to analyze:
 * - Venue size (reverb time)
 * - Bass response
 * - Problem frequencies
 * - Optimal EQ for venue
 * 
 * Professional DJs do this manually - we automate it!
 */
@Singleton
class VenueProfiler @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // ==================== STATE ====================
    
    private val _isProfileing = MutableStateFlow(false)
    val isProfiling: StateFlow<Boolean> = _isProfileing.asStateFlow()
    
    private val _profileProgress = MutableStateFlow(0f)
    val profileProgress: StateFlow<Float> = _profileProgress.asStateFlow()
    
    private val _currentVenue = MutableStateFlow<VenueProfile?>(null)
    val currentVenue: StateFlow<VenueProfile?> = _currentVenue.asStateFlow()
    
    private val _savedVenues = MutableStateFlow<List<VenueProfile>>(emptyList())
    val savedVenues: StateFlow<List<VenueProfile>> = _savedVenues.asStateFlow()
    
    // ==================== PROFILING ====================
    
    /**
     * Quick venue profile using ambient sound
     * No test tones needed - analyzes background noise
     */
    suspend fun quickProfile(): VenueProfile {
        _isProfileing.value = true
        _profileProgress.value = 0f
        
        return withContext(Dispatchers.IO) {
            val profile = analyzeAmbient()
            _currentVenue.value = profile
            _isProfileing.value = false
            profile
        }
    }
    
    /**
     * Full venue profile with test tones
     * More accurate but requires playing sounds
     */
    suspend fun fullProfile(
        playTestTone: (frequency: Int, durationMs: Long) -> Unit
    ): VenueProfile {
        _isProfileing.value = true
        _profileProgress.value = 0f
        
        return withContext(Dispatchers.IO) {
            val measurements = mutableListOf<FrequencyMeasurement>()
            
            // Test frequencies (sub-bass to high)
            val testFrequencies = listOf(40, 60, 100, 200, 500, 1000, 2000, 4000, 8000, 12000)
            
            testFrequencies.forEachIndexed { index, freq ->
                // Play test tone
                withContext(Dispatchers.Main) {
                    playTestTone(freq, 500)
                }
                
                delay(600) // Wait for tone to play
                
                // Measure response
                val response = measureFrequencyResponse(freq)
                measurements.add(response)
                
                _profileProgress.value = (index + 1).toFloat() / testFrequencies.size
            }
            
            // Analyze reverb time
            val reverbTime = measureReverbTime()
            
            // Create profile
            val profile = analyzeResults(measurements, reverbTime)
            _currentVenue.value = profile
            _isProfileing.value = false
            profile
        }
    }
    
    private suspend fun analyzeAmbient(): VenueProfile {
        // Record ambient for 3 seconds
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            return createDefaultProfile("Unknown Venue")
        }
        
        try {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            audioRecord.startRecording()
            val buffer = ShortArray(sampleRate * 3) // 3 seconds
            var totalRead = 0
            
            while (totalRead < buffer.size) {
                val read = audioRecord.read(buffer, totalRead, minOf(bufferSize, buffer.size - totalRead))
                if (read > 0) totalRead += read
                _profileProgress.value = totalRead.toFloat() / buffer.size
            }
            
            audioRecord.stop()
            audioRecord.release()
            
            // Analyze ambient noise
            return analyzeAmbientBuffer(buffer, totalRead)
            
        } catch (e: Exception) {
            return createDefaultProfile("Unknown Venue")
        }
    }
    
    private fun analyzeAmbientBuffer(buffer: ShortArray, size: Int): VenueProfile {
        // Calculate overall SPL
        var sum = 0.0
        for (i in 0 until size) {
            sum += buffer[i] * buffer[i]
        }
        val rms = sqrt(sum / size)
        val spl = 20 * log10(rms / 32768f) + 90
        
        // Estimate venue size based on noise characteristics
        val noiseFloor = spl.toFloat()
        val venueSize = when {
            noiseFloor < 40 -> VenueSize.SMALL      // Quiet = small room
            noiseFloor < 55 -> VenueSize.MEDIUM    // Moderate = medium venue
            noiseFloor < 70 -> VenueSize.LARGE     // Louder = larger venue
            else -> VenueSize.OUTDOOR              // Very loud = outdoor/massive
        }
        
        // Estimate reverb (very rough from ambient)
        val reverbTime = when (venueSize) {
            VenueSize.SMALL -> 0.3f
            VenueSize.MEDIUM -> 0.6f
            VenueSize.LARGE -> 1.2f
            VenueSize.OUTDOOR -> 0.1f
        }
        
        // Generate EQ corrections based on venue size
        val corrections = generateVenueCorrections(venueSize, noiseFloor)
        
        return VenueProfile(
            name = "${venueSize.label} Venue",
            size = venueSize,
            reverbTime = reverbTime,
            noiseFloor = noiseFloor,
            bassResponse = when (venueSize) {
                VenueSize.SMALL -> 0.7f    // Small rooms have bass buildup
                VenueSize.MEDIUM -> 0.9f   // Good bass
                VenueSize.LARGE -> 1.0f    // Needs bass boost
                VenueSize.OUTDOOR -> 0.6f  // Bass dissipates outdoors
            },
            highFreqResponse = when (venueSize) {
                VenueSize.SMALL -> 1.1f    // Can be harsh
                VenueSize.MEDIUM -> 1.0f   // Normal
                VenueSize.LARGE -> 0.8f    // Highs get absorbed
                VenueSize.OUTDOOR -> 0.7f  // Highs dissipate
            },
            problemFrequencies = when (venueSize) {
                VenueSize.SMALL -> listOf(200, 400)   // Room modes
                VenueSize.MEDIUM -> listOf(100, 250)  // Bass buildup
                VenueSize.LARGE -> emptyList()
                VenueSize.OUTDOOR -> emptyList()
            },
            suggestedCorrections = corrections,
            suggestedBattleMode = when (venueSize) {
                VenueSize.SMALL -> BattleMode.CLARITY_STRIKE   // Don't overpower small room
                VenueSize.MEDIUM -> BattleMode.FULL_ASSAULT
                VenueSize.LARGE -> BattleMode.BASS_WARFARE     // Need that bass to travel
                VenueSize.OUTDOOR -> BattleMode.SPL_MONSTER    // Need raw power
            },
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun measureFrequencyResponse(frequency: Int): FrequencyMeasurement {
        // Placeholder - in production, would measure actual response
        return FrequencyMeasurement(
            frequency = frequency,
            inputLevel = 0f,
            measuredLevel = 0f,
            response = 1.0f
        )
    }
    
    private fun measureReverbTime(): Float {
        // Placeholder - would use impulse response analysis
        return 0.5f
    }
    
    private fun analyzeResults(
        measurements: List<FrequencyMeasurement>,
        reverbTime: Float
    ): VenueProfile {
        // Analyze measurements to create profile
        val bassResponse = measurements
            .filter { it.frequency <= 200 }
            .map { it.response }
            .average()
            .toFloat()
        
        val highResponse = measurements
            .filter { it.frequency >= 4000 }
            .map { it.response }
            .average()
            .toFloat()
        
        val venueSize = when {
            reverbTime < 0.4f -> VenueSize.SMALL
            reverbTime < 0.8f -> VenueSize.MEDIUM
            reverbTime < 1.5f -> VenueSize.LARGE
            else -> VenueSize.OUTDOOR
        }
        
        val problemFreqs = measurements
            .filter { it.response < 0.7f || it.response > 1.3f }
            .map { it.frequency }
        
        val corrections = generateMeasuredCorrections(measurements)
        
        return VenueProfile(
            name = "${venueSize.label} (Measured)",
            size = venueSize,
            reverbTime = reverbTime,
            noiseFloor = 50f,
            bassResponse = bassResponse,
            highFreqResponse = highResponse,
            problemFrequencies = problemFreqs,
            suggestedCorrections = corrections,
            suggestedBattleMode = when (venueSize) {
                VenueSize.SMALL -> BattleMode.CLARITY_STRIKE
                VenueSize.MEDIUM -> BattleMode.FULL_ASSAULT
                VenueSize.LARGE -> BattleMode.BASS_WARFARE
                VenueSize.OUTDOOR -> BattleMode.SPL_MONSTER
            },
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun generateVenueCorrections(size: VenueSize, noiseFloor: Float): List<VenueCorrection> {
        val corrections = mutableListOf<VenueCorrection>()
        
        when (size) {
            VenueSize.SMALL -> {
                corrections.add(VenueCorrection(0, -3, "Reduce sub-bass for small room"))
                corrections.add(VenueCorrection(2, -2, "Cut low-mids to reduce mud"))
                corrections.add(VenueCorrection(3, 2, "Boost presence for clarity"))
            }
            VenueSize.MEDIUM -> {
                corrections.add(VenueCorrection(0, 3, "Slight bass boost"))
                corrections.add(VenueCorrection(3, 2, "Boost presence"))
            }
            VenueSize.LARGE -> {
                corrections.add(VenueCorrection(0, 6, "Strong bass boost for large space"))
                corrections.add(VenueCorrection(1, 4, "Bass boost"))
                corrections.add(VenueCorrection(3, 5, "Strong presence for reach"))
                corrections.add(VenueCorrection(4, 4, "High boost for clarity"))
            }
            VenueSize.OUTDOOR -> {
                corrections.add(VenueCorrection(0, 10, "Maximum sub-bass - sound dissipates"))
                corrections.add(VenueCorrection(1, 8, "Heavy bass boost"))
                corrections.add(VenueCorrection(3, 6, "Strong presence"))
                corrections.add(VenueCorrection(4, 6, "High boost - sound travels poorly"))
            }
        }
        
        // Noise floor adjustments
        if (noiseFloor > 65) {
            corrections.add(VenueCorrection(-1, 10, "Noisy venue - boost overall loudness"))
        }
        
        return corrections
    }
    
    private fun generateMeasuredCorrections(measurements: List<FrequencyMeasurement>): List<VenueCorrection> {
        return measurements
            .filter { it.response != 1.0f }
            .map { m ->
                val correction = when {
                    m.response < 1.0f -> ((1.0f - m.response) * 12).toInt()  // Boost weak freqs
                    else -> (-(m.response - 1.0f) * 6).toInt()               // Cut peaks
                }
                
                val band = when (m.frequency) {
                    in 20..80 -> 0
                    in 81..300 -> 1
                    in 301..1000 -> 2
                    in 1001..5000 -> 3
                    else -> 4
                }
                
                VenueCorrection(
                    eqBand = band,
                    correction = correction,
                    reason = if (correction > 0) 
                        "Boost ${m.frequency}Hz - weak in venue" 
                    else 
                        "Cut ${m.frequency}Hz - resonant in venue"
                )
            }
    }
    
    private fun createDefaultProfile(name: String): VenueProfile {
        return VenueProfile(
            name = name,
            size = VenueSize.MEDIUM,
            reverbTime = 0.5f,
            noiseFloor = 50f,
            bassResponse = 1.0f,
            highFreqResponse = 1.0f,
            problemFrequencies = emptyList(),
            suggestedCorrections = emptyList(),
            suggestedBattleMode = BattleMode.FULL_ASSAULT,
            timestamp = System.currentTimeMillis()
        )
    }
    
    // ==================== PROFILE MANAGEMENT ====================
    
    fun saveProfile(profile: VenueProfile, customName: String) {
        val named = profile.copy(name = customName)
        _savedVenues.value = _savedVenues.value + named
    }
    
    fun loadProfile(profile: VenueProfile) {
        _currentVenue.value = profile
    }
    
    fun deleteProfile(profile: VenueProfile) {
        _savedVenues.value = _savedVenues.value.filter { it.name != profile.name }
    }
    
    /**
     * Apply venue corrections to battle engine
     */
    fun applyToEngine(battleEngine: AudioBattleEngine) {
        val profile = _currentVenue.value ?: return
        
        for (correction in profile.suggestedCorrections) {
            if (correction.eqBand >= 0) {
                battleEngine.setEQBand(correction.eqBand, correction.correction * 100)
            } else if (correction.eqBand == -1) {
                // Overall loudness
                battleEngine.setLoudness(correction.correction * 100)
            }
        }
        
        // Set suggested mode
        battleEngine.setBattleMode(profile.suggestedBattleMode)
    }
}

// ==================== DATA CLASSES ====================

data class VenueProfile(
    val name: String,
    val size: VenueSize,
    val reverbTime: Float,           // seconds
    val noiseFloor: Float,           // dB SPL
    val bassResponse: Float,         // 0-2, 1 = neutral
    val highFreqResponse: Float,     // 0-2, 1 = neutral
    val problemFrequencies: List<Int>,
    val suggestedCorrections: List<VenueCorrection>,
    val suggestedBattleMode: BattleMode,
    val timestamp: Long
) {
    val sizeEmoji: String
        get() = when (size) {
            VenueSize.SMALL -> "🏠"
            VenueSize.MEDIUM -> "🏢"
            VenueSize.LARGE -> "🏟️"
            VenueSize.OUTDOOR -> "🌳"
        }
}

enum class VenueSize(val label: String) {
    SMALL("Small Room"),
    MEDIUM("Medium Hall"),
    LARGE("Large Venue"),
    OUTDOOR("Outdoor")
}

data class VenueCorrection(
    val eqBand: Int,      // -1 = overall loudness
    val correction: Int,   // dB
    val reason: String
)

data class FrequencyMeasurement(
    val frequency: Int,
    val inputLevel: Float,
    val measuredLevel: Float,
    val response: Float    // measured/input ratio
)
