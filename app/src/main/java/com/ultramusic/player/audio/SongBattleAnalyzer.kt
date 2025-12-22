package com.ultramusic.player.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.ultramusic.player.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Song Battle Analyzer
 * 
 * KILLER FEATURE #2: Pre-analyze songs for battle potential
 * 
 * Rates each song by:
 * - Bass impact score
 * - Clarity/presence score
 * - Energy/hype factor
 * - Drop potential
 * - Overall battle rating
 * 
 * Helps users pick the RIGHT song for battle!
 */
@Singleton
class SongBattleAnalyzer @Inject constructor(
    private val context: Context
) {
    private val _analyzedSongs = MutableStateFlow<Map<Long, SongBattleRating>>(emptyMap())
    val analyzedSongs: StateFlow<Map<Long, SongBattleRating>> = _analyzedSongs.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress: StateFlow<Float> = _analysisProgress.asStateFlow()
    
    /**
     * Analyze a single song for battle potential
     */
    suspend fun analyzeSong(song: Song): SongBattleRating {
        return withContext(Dispatchers.IO) {
            try {
                // Extract audio properties
                val extractor = MediaExtractor()
                extractor.setDataSource(context, song.uri, null)
                
                var sampleRate = 44100
                var bitrate = 320000
                var channels = 2
                
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("audio/") == true) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
                        bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE, 320000)
                        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
                        break
                    }
                }
                extractor.release()
                
                // Analyze based on metadata and heuristics
                val rating = calculateBattleRating(song, sampleRate, bitrate, channels)
                
                // Cache result
                val updatedMap = _analyzedSongs.value.toMutableMap()
                updatedMap[song.id] = rating
                _analyzedSongs.value = updatedMap
                
                rating
            } catch (e: Exception) {
                // Return default rating on error
                SongBattleRating(
                    songId = song.id,
                    overallRating = 50,
                    bassImpact = 50,
                    clarity = 50,
                    energy = 50,
                    dropPotential = 50,
                    crowdAppeal = 50,
                    battleTier = BattleTier.B,
                    strengths = listOf("Unknown"),
                    weaknesses = listOf("Could not analyze"),
                    bestFor = listOf(BattleScenario.GENERAL),
                    suggestedMode = BattleMode.FULL_ASSAULT
                )
            }
        }
    }
    
    /**
     * Batch analyze entire library
     */
    suspend fun analyzeLibrary(songs: List<Song>, onProgress: (Float) -> Unit = {}) {
        _isAnalyzing.value = true
        
        songs.forEachIndexed { index, song ->
            analyzeSong(song)
            val progress = (index + 1).toFloat() / songs.size
            _analysisProgress.value = progress
            onProgress(progress)
        }
        
        _isAnalyzing.value = false
    }
    
    /**
     * Get top battle songs from library
     */
    fun getTopBattleSongs(count: Int = 20): List<Pair<Long, SongBattleRating>> {
        return _analyzedSongs.value
            .entries
            .sortedByDescending { it.value.overallRating }
            .take(count)
            .map { it.key to it.value }
    }
    
    /**
     * Get songs best for specific scenario
     */
    fun getSongsForScenario(scenario: BattleScenario): List<Pair<Long, SongBattleRating>> {
        return _analyzedSongs.value
            .entries
            .filter { scenario in it.value.bestFor }
            .sortedByDescending { it.value.overallRating }
            .map { it.key to it.value }
    }
    
    /**
     * Get songs by battle tier
     */
    fun getSongsByTier(tier: BattleTier): List<Long> {
        return _analyzedSongs.value
            .filter { it.value.battleTier == tier }
            .keys
            .toList()
    }
    
    // ==================== ANALYSIS LOGIC ====================
    
    private fun calculateBattleRating(
        song: Song,
        sampleRate: Int,
        bitrate: Int,
        channels: Int
    ): SongBattleRating {
        val title = song.title.lowercase()
        val artist = song.artist.lowercase()
        val duration = song.duration
        
        // Base scores from audio quality
        val qualityBonus = when {
            bitrate >= 320000 -> 10
            bitrate >= 256000 -> 5
            bitrate >= 192000 -> 0
            else -> -5
        }
        
        // Analyze based on title/artist keywords
        val bassKeywords = listOf("bass", "sub", "low", "boom", "drop", "heavy", "808", "trap")
        val energyKeywords = listOf("party", "dance", "hype", "turn up", "lit", "fire", "rage", "hard")
        val crowdKeywords = listOf("hit", "remix", "popular", "viral", "anthem", "banger")
        val clarityKeywords = listOf("vocal", "acoustic", "clear", "live", "unplugged")
        
        // Calculate component scores
        var bassImpact = 50 + qualityBonus
        var energy = 50 + qualityBonus
        var clarity = 50 + qualityBonus
        var crowdAppeal = 50
        var dropPotential = 40
        
        // Keyword boosts
        if (bassKeywords.any { title.contains(it) || artist.contains(it) }) {
            bassImpact += 25
            dropPotential += 20
        }
        
        if (energyKeywords.any { title.contains(it) || artist.contains(it) }) {
            energy += 25
            crowdAppeal += 15
        }
        
        if (crowdKeywords.any { title.contains(it) || artist.contains(it) }) {
            crowdAppeal += 30
        }
        
        if (clarityKeywords.any { title.contains(it) }) {
            clarity += 20
            bassImpact -= 10
        }
        
        // Genre detection from artist
        val bollywoodArtists = listOf("arijit", "shreya", "badshah", "honey", "yo yo", "diljit", "ap dhillon")
        val edmArtists = listOf("martin garrix", "marshmello", "skrillex", "deadmau5", "avicii", "alan walker")
        val rapArtists = listOf("eminem", "drake", "kendrick", "divine", "raftaar", "emiway")
        
        when {
            bollywoodArtists.any { artist.contains(it) } -> {
                crowdAppeal += 20  // Very popular in India
                if (artist.contains("badshah") || artist.contains("honey singh")) {
                    bassImpact += 15
                    energy += 15
                }
            }
            edmArtists.any { artist.contains(it) } -> {
                bassImpact += 20
                energy += 25
                dropPotential += 30
            }
            rapArtists.any { artist.contains(it) } -> {
                bassImpact += 15
                energy += 20
                clarity += 10
            }
        }
        
        // Duration factor
        when {
            duration in 150000..240000 -> crowdAppeal += 10  // 2.5-4 min ideal
            duration > 360000 -> crowdAppeal -= 10  // Too long
            duration < 120000 -> dropPotential += 10  // Short = punchy
        }
        
        // Cap scores
        bassImpact = bassImpact.coerceIn(0, 100)
        energy = energy.coerceIn(0, 100)
        clarity = clarity.coerceIn(0, 100)
        crowdAppeal = crowdAppeal.coerceIn(0, 100)
        dropPotential = dropPotential.coerceIn(0, 100)
        
        // Calculate overall rating (weighted)
        val overallRating = (
            bassImpact * 0.25f +
            energy * 0.25f +
            clarity * 0.15f +
            crowdAppeal * 0.20f +
            dropPotential * 0.15f
        ).toInt()
        
        // Determine tier
        val tier = when {
            overallRating >= 85 -> BattleTier.S
            overallRating >= 70 -> BattleTier.A
            overallRating >= 55 -> BattleTier.B
            overallRating >= 40 -> BattleTier.C
            else -> BattleTier.D
        }
        
        // Determine strengths/weaknesses
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        
        if (bassImpact >= 70) strengths.add("💥 Heavy Bass")
        if (energy >= 70) strengths.add("⚡ High Energy")
        if (clarity >= 70) strengths.add("🎯 Great Clarity")
        if (crowdAppeal >= 70) strengths.add("👥 Crowd Favorite")
        if (dropPotential >= 70) strengths.add("🔥 Epic Drop")
        
        if (bassImpact < 40) weaknesses.add("🔇 Weak Bass")
        if (energy < 40) weaknesses.add("😴 Low Energy")
        if (clarity < 40) weaknesses.add("🌫️ Muddy Sound")
        if (crowdAppeal < 40) weaknesses.add("❓ Unknown Song")
        
        if (strengths.isEmpty()) strengths.add("Balanced")
        if (weaknesses.isEmpty()) weaknesses.add("No major weaknesses")
        
        // Determine best scenarios
        val bestFor = mutableListOf<BattleScenario>()
        if (bassImpact >= 70) bestFor.add(BattleScenario.BASS_BATTLE)
        if (crowdAppeal >= 70) bestFor.add(BattleScenario.CROWD_HYPE)
        if (dropPotential >= 70) bestFor.add(BattleScenario.DROP_MOMENT)
        if (energy >= 70) bestFor.add(BattleScenario.OPENER)
        if (clarity >= 70) bestFor.add(BattleScenario.CLARITY_COUNTER)
        if (bestFor.isEmpty()) bestFor.add(BattleScenario.GENERAL)
        
        // Suggest battle mode
        val suggestedMode = when {
            bassImpact >= 80 -> BattleMode.BASS_WARFARE
            clarity >= 80 -> BattleMode.CLARITY_STRIKE
            energy >= 80 -> BattleMode.FULL_ASSAULT
            crowdAppeal >= 80 -> BattleMode.CROWD_REACH
            else -> BattleMode.FULL_ASSAULT
        }
        
        return SongBattleRating(
            songId = song.id,
            overallRating = overallRating,
            bassImpact = bassImpact,
            clarity = clarity,
            energy = energy,
            dropPotential = dropPotential,
            crowdAppeal = crowdAppeal,
            battleTier = tier,
            strengths = strengths,
            weaknesses = weaknesses,
            bestFor = bestFor,
            suggestedMode = suggestedMode
        )
    }
}

// ==================== DATA CLASSES ====================

data class SongBattleRating(
    val songId: Long,
    val overallRating: Int,      // 0-100
    val bassImpact: Int,         // 0-100
    val clarity: Int,            // 0-100
    val energy: Int,             // 0-100
    val dropPotential: Int,      // 0-100
    val crowdAppeal: Int,        // 0-100
    val battleTier: BattleTier,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val bestFor: List<BattleScenario>,
    val suggestedMode: BattleMode
) {
    val ratingEmoji: String
        get() = when (battleTier) {
            BattleTier.S -> "🏆"
            BattleTier.A -> "⭐"
            BattleTier.B -> "👍"
            BattleTier.C -> "👌"
            BattleTier.D -> "👎"
        }
    
    val ratingColor: Long
        get() = when (battleTier) {
            BattleTier.S -> 0xFFFFD700  // Gold
            BattleTier.A -> 0xFF4CAF50  // Green
            BattleTier.B -> 0xFF2196F3  // Blue
            BattleTier.C -> 0xFFFF9800  // Orange
            BattleTier.D -> 0xFF9E9E9E  // Gray
        }
}

enum class BattleTier(val label: String) {
    S("S-Tier 🏆"),
    A("A-Tier ⭐"),
    B("B-Tier 👍"),
    C("C-Tier 👌"),
    D("D-Tier 👎")
}

enum class BattleScenario(val label: String) {
    OPENER("🎬 Opener"),
    BASS_BATTLE("🔊 Bass Battle"),
    CLARITY_COUNTER("⚔️ Clarity Counter"),
    DROP_MOMENT("🔥 Drop Moment"),
    CROWD_HYPE("👥 Crowd Hype"),
    CLOSER("🎯 Closer"),
    GENERAL("🎵 General")
}
