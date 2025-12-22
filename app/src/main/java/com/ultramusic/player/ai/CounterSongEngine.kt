package com.ultramusic.player.ai

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.ultramusic.player.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Counter Song Engine
 * 
 * AI-powered system to predict the perfect counter song against opponent.
 * 
 * Strategy Modes:
 * 1. CONTRAST - Pick opposite mood/energy (sad → happy)
 * 2. ESCALATE - Pick higher energy version of same style
 * 3. SURPRISE - Pick unexpected genre shift
 * 4. CROWD_PLEASER - Pick most popular song that fits
 * 5. SMOOTH_TRANSITION - Pick song with compatible key/BPM
 * 
 * No RAG needed for basic version - uses feature extraction + rules.
 * RAG version available for advanced strategy with LLM.
 */
@Singleton
class CounterSongEngine @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // ==================== STATE ====================
    
    private val _state = MutableStateFlow<CounterEngineState>(CounterEngineState.Idle)
    val state: StateFlow<CounterEngineState> = _state.asStateFlow()
    
    private val _recommendations = MutableStateFlow<List<CounterRecommendation>>(emptyList())
    val recommendations: StateFlow<List<CounterRecommendation>> = _recommendations.asStateFlow()
    
    // Your music library with pre-extracted features
    private var libraryWithFeatures: List<SongWithFeatures> = emptyList()
    
    // ==================== INITIALIZATION ====================
    
    /**
     * Index your library with audio features for fast counter matching
     * Call this once when app starts or library changes
     */
    suspend fun indexLibrary(songs: List<Song>) {
        _state.value = CounterEngineState.Indexing(0, songs.size)
        
        withContext(Dispatchers.Default) {
            libraryWithFeatures = songs.mapIndexed { index, song ->
                _state.value = CounterEngineState.Indexing(index + 1, songs.size)
                
                // Extract features from song metadata and filename
                // In production, use Essentia or audio analysis
                SongWithFeatures(
                    song = song,
                    features = extractFeaturesFromMetadata(song)
                )
            }
        }
        
        _state.value = CounterEngineState.Ready
    }
    
    // ==================== COUNTER SONG PREDICTION ====================
    
    /**
     * Find counter songs for a known opponent song (by title/artist)
     */
    fun findCounterByName(
        opponentSongTitle: String,
        opponentArtist: String? = null,
        strategy: CounterStrategy = CounterStrategy.AUTO
    ) {
        scope.launch {
            _state.value = CounterEngineState.Analyzing
            
            // Estimate opponent song features from title/artist
            val opponentFeatures = estimateFeaturesFromName(opponentSongTitle, opponentArtist)
            
            val counters = findBestCounters(opponentFeatures, strategy)
            _recommendations.value = counters
            
            _state.value = CounterEngineState.Ready
        }
    }
    
    /**
     * Find counter songs by listening to opponent's audio
     * Uses audio fingerprinting + feature extraction
     */
    fun findCounterByListening(
        strategy: CounterStrategy = CounterStrategy.AUTO
    ) {
        scope.launch {
            _state.value = CounterEngineState.Listening
            
            // Capture audio and extract features
            val opponentFeatures = captureAndAnalyzeAudio()
            
            if (opponentFeatures != null) {
                _state.value = CounterEngineState.Analyzing
                val counters = findBestCounters(opponentFeatures, strategy)
                _recommendations.value = counters
            } else {
                _state.value = CounterEngineState.Error("Could not analyze opponent's song")
            }
            
            _state.value = CounterEngineState.Ready
        }
    }
    
    /**
     * Real-time counter suggestions as opponent's song plays
     */
    fun startRealtimeCounterMode(
        strategy: CounterStrategy = CounterStrategy.AUTO,
        onUpdate: (List<CounterRecommendation>) -> Unit
    ) {
        scope.launch {
            _state.value = CounterEngineState.RealtimeMode
            
            // Continuously analyze and update recommendations
            // In production, use streaming audio analysis
            while (_state.value == CounterEngineState.RealtimeMode) {
                val features = captureAndAnalyzeAudio(durationMs = 3000)
                if (features != null) {
                    val counters = findBestCounters(features, strategy)
                    _recommendations.value = counters
                    onUpdate(counters)
                }
                kotlinx.coroutines.delay(2000) // Update every 2 seconds
            }
        }
    }
    
    fun stopRealtimeMode() {
        _state.value = CounterEngineState.Ready
    }
    
    // ==================== CORE MATCHING LOGIC ====================
    
    private fun findBestCounters(
        opponentFeatures: AudioFeatures,
        strategy: CounterStrategy
    ): List<CounterRecommendation> {
        
        val effectiveStrategy = if (strategy == CounterStrategy.AUTO) {
            determineOptimalStrategy(opponentFeatures)
        } else {
            strategy
        }
        
        val scored = libraryWithFeatures.map { songWithFeatures ->
            val score = calculateCounterScore(
                opponent = opponentFeatures,
                candidate = songWithFeatures.features,
                strategy = effectiveStrategy
            )
            
            CounterRecommendation(
                song = songWithFeatures.song,
                features = songWithFeatures.features,
                score = score,
                strategy = effectiveStrategy,
                reasoning = generateReasoning(opponentFeatures, songWithFeatures.features, effectiveStrategy)
            )
        }
        
        return scored
            .sortedByDescending { it.score }
            .take(5)
    }
    
    private fun calculateCounterScore(
        opponent: AudioFeatures,
        candidate: AudioFeatures,
        strategy: CounterStrategy
    ): Float {
        return when (strategy) {
            CounterStrategy.CONTRAST -> calculateContrastScore(opponent, candidate)
            CounterStrategy.ESCALATE -> calculateEscalateScore(opponent, candidate)
            CounterStrategy.SURPRISE -> calculateSurpriseScore(opponent, candidate)
            CounterStrategy.CROWD_PLEASER -> calculateCrowdPleaserScore(opponent, candidate)
            CounterStrategy.SMOOTH_TRANSITION -> calculateTransitionScore(opponent, candidate)
            CounterStrategy.AUTO -> calculateContrastScore(opponent, candidate) // fallback
        }
    }
    
    /**
     * CONTRAST: Pick opposite mood/energy
     * If they play sad, you play happy
     * If they play slow, you play fast
     */
    private fun calculateContrastScore(opponent: AudioFeatures, candidate: AudioFeatures): Float {
        var score = 0f
        
        // Energy contrast (higher is better if opponent is low)
        val energyContrast = abs(opponent.energy - candidate.energy)
        score += energyContrast * 30f
        
        // Mood contrast
        val moodContrast = if (opponent.mood != candidate.mood) 1f else 0f
        score += moodContrast * 25f
        
        // Tempo contrast (but not too extreme)
        val tempoRatio = candidate.bpm / opponent.bpm.coerceAtLeast(1f)
        val tempoScore = when {
            tempoRatio > 1.3f -> 20f  // Faster
            tempoRatio < 0.7f -> 15f  // Slower
            else -> 5f
        }
        score += tempoScore
        
        // Popularity bonus
        score += candidate.popularity * 10f
        
        // Regional bonus if opponent played English
        if (opponent.language == "english" && candidate.language != "english") {
            score += 15f
        }
        
        return score
    }
    
    /**
     * ESCALATE: Pick higher energy version of same style
     * Beat them at their own game
     */
    private fun calculateEscalateScore(opponent: AudioFeatures, candidate: AudioFeatures): Float {
        var score = 0f
        
        // Same genre bonus
        if (opponent.genre == candidate.genre) {
            score += 25f
        }
        
        // Higher energy required
        if (candidate.energy > opponent.energy) {
            score += (candidate.energy - opponent.energy) * 40f
        }
        
        // Faster tempo bonus
        if (candidate.bpm > opponent.bpm) {
            score += 15f
        }
        
        // Popularity must be higher or equal
        if (candidate.popularity >= opponent.popularity) {
            score += 20f
        }
        
        return score
    }
    
    /**
     * SURPRISE: Unexpected genre shift
     * Catch them off guard
     */
    private fun calculateSurpriseScore(opponent: AudioFeatures, candidate: AudioFeatures): Float {
        var score = 0f
        
        // Different genre is key
        if (opponent.genre != candidate.genre) {
            score += 35f
        }
        
        // Drastic tempo change
        val tempoDiff = abs(candidate.bpm - opponent.bpm)
        if (tempoDiff > 40) {
            score += 20f
        }
        
        // Different language
        if (opponent.language != candidate.language) {
            score += 15f
        }
        
        // Still needs to be good
        score += candidate.popularity * 15f
        score += candidate.energy * 10f
        
        return score
    }
    
    /**
     * CROWD_PLEASER: Pick most popular song that fits
     * Safe choice that everyone knows
     */
    private fun calculateCrowdPleaserScore(opponent: AudioFeatures, candidate: AudioFeatures): Float {
        var score = 0f
        
        // Popularity is king
        score += candidate.popularity * 50f
        
        // High energy preferred
        score += candidate.energy * 20f
        
        // Danceability
        score += candidate.danceability * 20f
        
        // Not too different from opponent (smooth experience)
        val similarity = 1f - abs(opponent.energy - candidate.energy)
        score += similarity * 10f
        
        return score
    }
    
    /**
     * SMOOTH_TRANSITION: Compatible key/BPM for DJ mixing
     * Professional seamless transition
     */
    private fun calculateTransitionScore(opponent: AudioFeatures, candidate: AudioFeatures): Float {
        var score = 0f
        
        // Key compatibility (Camelot wheel logic)
        val keyScore = calculateKeyCompatibility(opponent.musicalKey, candidate.musicalKey)
        score += keyScore * 35f
        
        // BPM within mixable range (±8 BPM)
        val bpmDiff = abs(candidate.bpm - opponent.bpm)
        val bpmScore = when {
            bpmDiff <= 4 -> 30f
            bpmDiff <= 8 -> 20f
            bpmDiff <= 16 -> 10f
            else -> 0f
        }
        score += bpmScore
        
        // Similar energy for smooth transition
        val energyDiff = abs(opponent.energy - candidate.energy)
        score += (1f - energyDiff) * 20f
        
        // Popularity still matters
        score += candidate.popularity * 15f
        
        return score
    }
    
    /**
     * Calculate key compatibility using Camelot wheel
     */
    private fun calculateKeyCompatibility(key1: String, key2: String): Float {
        // Simplified Camelot wheel compatibility
        // In production, use full Camelot wheel logic
        val camelotMap = mapOf(
            "C" to "8B", "Am" to "8A",
            "G" to "9B", "Em" to "9A",
            "D" to "10B", "Bm" to "10A",
            "A" to "11B", "F#m" to "11A",
            "E" to "12B", "C#m" to "12A",
            "B" to "1B", "G#m" to "1A",
            "F#" to "2B", "D#m" to "2A",
            "Db" to "3B", "Bbm" to "3A",
            "Ab" to "4B", "Fm" to "4A",
            "Eb" to "5B", "Cm" to "5A",
            "Bb" to "6B", "Gm" to "6A",
            "F" to "7B", "Dm" to "7A"
        )
        
        val c1 = camelotMap[key1] ?: return 0.5f
        val c2 = camelotMap[key2] ?: return 0.5f
        
        // Same key = perfect
        if (c1 == c2) return 1f
        
        // Adjacent on wheel = good
        val num1 = c1.dropLast(1).toIntOrNull() ?: return 0.5f
        val num2 = c2.dropLast(1).toIntOrNull() ?: return 0.5f
        val numDiff = minOf(abs(num1 - num2), 12 - abs(num1 - num2))
        
        return when (numDiff) {
            0 -> 0.9f  // Same number, different mode (8A/8B)
            1 -> 0.8f  // Adjacent
            2 -> 0.5f  // 2 steps
            else -> 0.2f
        }
    }
    
    /**
     * Determine best strategy based on opponent's song
     */
    private fun determineOptimalStrategy(opponent: AudioFeatures): CounterStrategy {
        return when {
            // If opponent played slow sad song, contrast with energy
            opponent.energy < 0.4f && opponent.mood == Mood.SAD -> CounterStrategy.CONTRAST
            
            // If opponent played high energy, try to escalate
            opponent.energy > 0.7f -> CounterStrategy.ESCALATE
            
            // If opponent played very popular song, surprise them
            opponent.popularity > 0.8f -> CounterStrategy.SURPRISE
            
            // If it's a dance/party context, go crowd pleaser
            opponent.danceability > 0.7f -> CounterStrategy.CROWD_PLEASER
            
            // Default to contrast
            else -> CounterStrategy.CONTRAST
        }
    }
    
    private fun generateReasoning(
        opponent: AudioFeatures,
        candidate: AudioFeatures,
        strategy: CounterStrategy
    ): String {
        return when (strategy) {
            CounterStrategy.CONTRAST -> {
                val energyChange = if (candidate.energy > opponent.energy) "higher energy" else "different vibe"
                "Counter with $energyChange to shift the mood"
            }
            CounterStrategy.ESCALATE -> {
                "Beat them at their own game with more intensity"
            }
            CounterStrategy.SURPRISE -> {
                "Unexpected ${candidate.genre} to catch them off guard"
            }
            CounterStrategy.CROWD_PLEASER -> {
                "Popular choice that everyone will love"
            }
            CounterStrategy.SMOOTH_TRANSITION -> {
                "Key-compatible for seamless DJ transition"
            }
            CounterStrategy.AUTO -> "AI-selected best strategy"
        }
    }
    
    // ==================== FEATURE EXTRACTION ====================
    
    /**
     * Extract features from song metadata
     * In production, replace with actual audio analysis using Essentia
     */
    private fun extractFeaturesFromMetadata(song: Song): AudioFeatures {
        val title = song.title.lowercase()
        val artist = song.artist.lowercase()
        
        // Estimate BPM from duration and title keywords
        val estimatedBpm = when {
            title.contains("slow") || title.contains("ballad") -> 70f
            title.contains("dance") || title.contains("party") -> 128f
            title.contains("rock") || title.contains("metal") -> 140f
            else -> 100f + (song.duration % 50)
        }
        
        // Estimate mood from title
        val mood = when {
            title.contains("sad") || title.contains("cry") || title.contains("pain") -> Mood.SAD
            title.contains("happy") || title.contains("joy") || title.contains("love") -> Mood.HAPPY
            title.contains("angry") || title.contains("rage") -> Mood.ANGRY
            title.contains("chill") || title.contains("relax") -> Mood.CALM
            else -> Mood.NEUTRAL
        }
        
        // Estimate genre from artist/title
        val genre = when {
            artist.contains("arijit") || artist.contains("shreya") -> "bollywood"
            title.contains("bengali") || artist.contains("bengali") -> "bengali"
            title.contains("edm") || title.contains("remix") -> "electronic"
            title.contains("rock") -> "rock"
            title.contains("hip hop") || title.contains("rap") -> "hiphop"
            else -> "pop"
        }
        
        // Estimate language
        val language = when {
            artist.contains("arijit") || artist.contains("shreya") || 
            artist.contains("kumar") || title.contains("hindi") -> "hindi"
            title.contains("bengali") -> "bengali"
            else -> "english"
        }
        
        // Estimate energy (0-1)
        val energy = when {
            title.contains("slow") || title.contains("sad") -> 0.3f
            title.contains("party") || title.contains("dance") -> 0.9f
            title.contains("rock") || title.contains("metal") -> 0.85f
            else -> 0.6f
        }
        
        // Popularity estimate (based on common songs)
        val popularity = estimatePopularity(title, artist)
        
        return AudioFeatures(
            bpm = estimatedBpm,
            musicalKey = "C", // Would need actual audio analysis
            energy = energy,
            danceability = if (genre == "electronic" || title.contains("dance")) 0.8f else 0.5f,
            mood = mood,
            genre = genre,
            language = language,
            popularity = popularity
        )
    }
    
    /**
     * Estimate features from opponent song name (when you don't have the audio)
     */
    private fun estimateFeaturesFromName(title: String, artist: String?): AudioFeatures {
        val lowerTitle = title.lowercase()
        val lowerArtist = artist?.lowercase() ?: ""
        
        // Check known popular songs database
        val knownSong = KNOWN_SONGS_FEATURES[lowerTitle]
        if (knownSong != null) return knownSong
        
        // Estimate from name
        return AudioFeatures(
            bpm = 120f,
            musicalKey = "C",
            energy = when {
                lowerTitle.contains("slow") -> 0.3f
                lowerTitle.contains("party") || lowerTitle.contains("dance") -> 0.85f
                else -> 0.6f
            },
            danceability = 0.6f,
            mood = when {
                lowerTitle.contains("sad") || lowerTitle.contains("broken") -> Mood.SAD
                lowerTitle.contains("happy") || lowerTitle.contains("love") -> Mood.HAPPY
                else -> Mood.NEUTRAL
            },
            genre = when {
                lowerArtist.contains("arijit") -> "bollywood"
                lowerTitle.contains("edm") -> "electronic"
                else -> "pop"
            },
            language = if (lowerArtist.contains("arijit") || lowerArtist.contains("shreya")) "hindi" else "english",
            popularity = estimatePopularity(lowerTitle, lowerArtist)
        )
    }
    
    private fun estimatePopularity(title: String, artist: String): Float {
        // Known popular songs get high scores
        val popularKeywords = listOf(
            "kesariya", "tum hi ho", "channa mereya", "shape of you",
            "despacito", "believer", "raataan lambiyan", "pasoori"
        )
        
        return if (popularKeywords.any { title.contains(it) }) 0.9f else 0.5f
    }
    
    /**
     * Capture and analyze opponent's audio
     * In production, use audio fingerprinting (ACRCloud/Chromaprint)
     */
    private suspend fun captureAndAnalyzeAudio(durationMs: Long = 5000): AudioFeatures? {
        return withContext(Dispatchers.IO) {
            try {
                // This is a placeholder - in production:
                // 1. Use AudioRecord to capture audio
                // 2. Send to ACRCloud for song identification
                // 3. Or use local Chromaprint + Essentia for analysis
                
                // Simulated analysis
                kotlinx.coroutines.delay(durationMs)
                
                // Return estimated features
                AudioFeatures(
                    bpm = 120f,
                    musicalKey = "Am",
                    energy = 0.7f,
                    danceability = 0.6f,
                    mood = Mood.NEUTRAL,
                    genre = "pop",
                    language = "hindi",
                    popularity = 0.7f
                )
            } catch (e: Exception) {
                null
            }
        }
    }
    
    companion object {
        /**
         * Known songs with pre-defined features
         * Expand this database for better accuracy
         */
        val KNOWN_SONGS_FEATURES = mapOf(
            "kesariya" to AudioFeatures(
                bpm = 98f, musicalKey = "Bb", energy = 0.65f, danceability = 0.55f,
                mood = Mood.HAPPY, genre = "bollywood", language = "hindi", popularity = 0.95f
            ),
            "tum hi ho" to AudioFeatures(
                bpm = 70f, musicalKey = "C#m", energy = 0.4f, danceability = 0.3f,
                mood = Mood.SAD, genre = "bollywood", language = "hindi", popularity = 0.98f
            ),
            "channa mereya" to AudioFeatures(
                bpm = 85f, musicalKey = "D", energy = 0.5f, danceability = 0.4f,
                mood = Mood.SAD, genre = "bollywood", language = "hindi", popularity = 0.92f
            ),
            "pasoori" to AudioFeatures(
                bpm = 105f, musicalKey = "Am", energy = 0.75f, danceability = 0.8f,
                mood = Mood.HAPPY, genre = "punjabi", language = "punjabi", popularity = 0.9f
            ),
            "raataan lambiyan" to AudioFeatures(
                bpm = 92f, musicalKey = "G", energy = 0.6f, danceability = 0.5f,
                mood = Mood.HAPPY, genre = "bollywood", language = "hindi", popularity = 0.88f
            ),
            "shape of you" to AudioFeatures(
                bpm = 96f, musicalKey = "C#m", energy = 0.8f, danceability = 0.85f,
                mood = Mood.HAPPY, genre = "pop", language = "english", popularity = 0.99f
            ),
            "despacito" to AudioFeatures(
                bpm = 89f, musicalKey = "Bm", energy = 0.75f, danceability = 0.9f,
                mood = Mood.HAPPY, genre = "latin", language = "spanish", popularity = 0.99f
            ),
            "believer" to AudioFeatures(
                bpm = 125f, musicalKey = "Bb", energy = 0.9f, danceability = 0.7f,
                mood = Mood.ANGRY, genre = "rock", language = "english", popularity = 0.95f
            )
        )
    }
}

// ==================== DATA CLASSES ====================

sealed class CounterEngineState {
    object Idle : CounterEngineState()
    data class Indexing(val current: Int, val total: Int) : CounterEngineState()
    object Ready : CounterEngineState()
    object Listening : CounterEngineState()
    object Analyzing : CounterEngineState()
    object RealtimeMode : CounterEngineState()
    data class Error(val message: String) : CounterEngineState()
}

enum class CounterStrategy {
    AUTO,           // AI picks best strategy
    CONTRAST,       // Opposite mood/energy
    ESCALATE,       // Beat them at their game
    SURPRISE,       // Unexpected genre shift
    CROWD_PLEASER,  // Popular safe choice
    SMOOTH_TRANSITION // DJ-friendly key/BPM match
}

enum class Mood {
    HAPPY, SAD, ANGRY, CALM, NEUTRAL
}

data class AudioFeatures(
    val bpm: Float,
    val musicalKey: String,
    val energy: Float,        // 0-1
    val danceability: Float,  // 0-1
    val mood: Mood,
    val genre: String,
    val language: String,
    val popularity: Float     // 0-1
)

data class SongWithFeatures(
    val song: Song,
    val features: AudioFeatures
)

data class CounterRecommendation(
    val song: Song,
    val features: AudioFeatures,
    val score: Float,
    val strategy: CounterStrategy,
    val reasoning: String
)
