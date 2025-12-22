package com.ultramusic.player.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ultramusic.player.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BATTLE ARMORY
 * 
 * Your pre-prepared arsenal for sound system battles!
 * 
 * Key Concepts:
 * 1. COUNTER CLIPS - A-B sections of songs that are perfect counters
 *    (Not the full song, just the killer part!)
 * 
 * 2. BATTLE CATEGORIES - Songs organized by their counter purpose
 *    - Bass Destroyers (counter weak bass)
 *    - Mid Cutters (cut through bass-heavy opponents)
 *    - Energy Bombs (overpower low-energy opponents)
 *    - Drop Killers (silence-to-bass transitions)
 *    - Frequency Fillers (fill opponent's gaps)
 * 
 * 3. QUICK FIRE CLIPS - One-tap access to your best counter sections
 * 
 * Prepare these BEFORE the battle - not during!
 */
@Singleton
class BattleArmory @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BattleArmory"
        private const val PREFS_NAME = "battle_armory"
        private const val KEY_COUNTER_CLIPS = "counter_clips"
        private const val KEY_QUICK_FIRE = "quick_fire"
        private const val KEY_BATTLE_SETS = "battle_sets"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // ==================== COUNTER CLIPS ====================
    // Pre-marked A-B sections that are perfect counters
    
    private val _counterClips = MutableStateFlow<List<CounterClip>>(emptyList())
    val counterClips: StateFlow<List<CounterClip>> = _counterClips.asStateFlow()
    
    // Organized by purpose
    private val _clipsByPurpose = MutableStateFlow<Map<ClipPurpose, List<CounterClip>>>(emptyMap())
    val clipsByPurpose: StateFlow<Map<ClipPurpose, List<CounterClip>>> = _clipsByPurpose.asStateFlow()
    
    // ==================== QUICK FIRE ====================
    // Your top 10 instant-access counter clips
    
    private val _quickFireClips = MutableStateFlow<List<CounterClip>>(emptyList())
    val quickFireClips: StateFlow<List<CounterClip>> = _quickFireClips.asStateFlow()
    
    // ==================== BATTLE SETS ====================
    // Pre-organized sets for different battle situations
    
    private val _battleSets = MutableStateFlow<List<BattleSet>>(emptyList())
    val battleSets: StateFlow<List<BattleSet>> = _battleSets.asStateFlow()
    
    init {
        loadFromStorage()
    }
    
    // ==================== CLIP MANAGEMENT ====================
    
    /**
     * Create a counter clip from a song section
     * 
     * @param song The source song
     * @param startMs A-point in milliseconds
     * @param endMs B-point in milliseconds
     * @param name Custom name for this clip
     * @param purpose What this clip counters
     * @param notes Your battle notes
     */
    fun createCounterClip(
        song: Song,
        startMs: Long,
        endMs: Long,
        name: String,
        purpose: ClipPurpose,
        notes: String = ""
    ): CounterClip {
        val clip = CounterClip(
            id = System.currentTimeMillis(),
            songId = song.id,
            songTitle = song.title,
            songArtist = song.artist,
            songPath = song.path,
            startMs = startMs,
            endMs = endMs,
            durationMs = endMs - startMs,
            name = name,
            purpose = purpose,
            notes = notes,
            useCount = 0,
            winCount = 0,
            createdAt = System.currentTimeMillis()
        )
        
        val updated = _counterClips.value + clip
        _counterClips.value = updated
        updateClipsByPurpose()
        saveToStorage()
        
        Log.i(TAG, "Created counter clip: $name (${formatDuration(clip.durationMs)})")
        return clip
    }
    
    /**
     * Update clip after using it in battle
     */
    fun recordClipUsage(clipId: Long, won: Boolean) {
        val updated = _counterClips.value.map { clip ->
            if (clip.id == clipId) {
                clip.copy(
                    useCount = clip.useCount + 1,
                    winCount = if (won) clip.winCount + 1 else clip.winCount
                )
            } else clip
        }
        _counterClips.value = updated
        saveToStorage()
    }
    
    /**
     * Delete a counter clip
     */
    fun deleteClip(clipId: Long) {
        _counterClips.value = _counterClips.value.filter { it.id != clipId }
        _quickFireClips.value = _quickFireClips.value.filter { it.id != clipId }
        updateClipsByPurpose()
        saveToStorage()
    }
    
    /**
     * Add clip to Quick Fire (top 10 instant access)
     */
    fun addToQuickFire(clip: CounterClip) {
        if (_quickFireClips.value.size >= 10) {
            Log.w(TAG, "Quick Fire is full (max 10). Remove one first.")
            return
        }
        if (_quickFireClips.value.any { it.id == clip.id }) {
            Log.w(TAG, "Clip already in Quick Fire")
            return
        }
        _quickFireClips.value = _quickFireClips.value + clip
        saveToStorage()
    }
    
    /**
     * Remove from Quick Fire
     */
    fun removeFromQuickFire(clipId: Long) {
        _quickFireClips.value = _quickFireClips.value.filter { it.id != clipId }
        saveToStorage()
    }
    
    /**
     * Reorder Quick Fire clips
     */
    fun reorderQuickFire(fromIndex: Int, toIndex: Int) {
        val list = _quickFireClips.value.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _quickFireClips.value = list
        saveToStorage()
    }
    
    // ==================== INSTANT QUERIES ====================
    
    /**
     * Get clips that counter bass-heavy opponents
     */
    fun getAntiBassClips(): List<CounterClip> {
        return _clipsByPurpose.value[ClipPurpose.MID_CUTTER].orEmpty() +
               _clipsByPurpose.value[ClipPurpose.HIGH_PIERCER].orEmpty()
    }
    
    /**
     * Get clips that counter mid-heavy opponents
     */
    fun getAntiMidClips(): List<CounterClip> {
        return _clipsByPurpose.value[ClipPurpose.BASS_DESTROYER].orEmpty() +
               _clipsByPurpose.value[ClipPurpose.SUB_SHAKER].orEmpty()
    }
    
    /**
     * Get clips for energy domination
     */
    fun getEnergyClips(): List<CounterClip> {
        return _clipsByPurpose.value[ClipPurpose.ENERGY_BOMB].orEmpty() +
               _clipsByPurpose.value[ClipPurpose.DROP_KILLER].orEmpty()
    }
    
    /**
     * Get best counter for opponent profile
     */
    fun getBestCounter(
        opponentDominant: FrequencyRange,
        opponentWeak: FrequencyRange,
        opponentEnergy: Float
    ): CounterClip? {
        val candidates = when {
            // Opponent is bass-heavy with weak mids
            opponentDominant in listOf(FrequencyRange.SUB_BASS, FrequencyRange.BASS) &&
            opponentWeak in listOf(FrequencyRange.MID, FrequencyRange.HIGH_MID) -> {
                getAntiBassClips()
            }
            // Opponent is mid-heavy
            opponentDominant == FrequencyRange.MID -> {
                getAntiMidClips()
            }
            // Opponent is low energy
            opponentEnergy < 0.5f -> {
                getEnergyClips()
            }
            // General counter
            else -> {
                _clipsByPurpose.value[ClipPurpose.ALL_ROUNDER].orEmpty()
            }
        }
        
        // Return highest win-rate clip
        return candidates.maxByOrNull { 
            if (it.useCount > 0) it.winCount.toFloat() / it.useCount else 0.5f 
        }
    }
    
    // ==================== BATTLE SETS ====================
    
    /**
     * Create a battle set (organized collection of clips)
     */
    fun createBattleSet(
        name: String,
        description: String,
        clipIds: List<Long>
    ): BattleSet {
        val clips = _counterClips.value.filter { it.id in clipIds }
        val set = BattleSet(
            id = System.currentTimeMillis(),
            name = name,
            description = description,
            clips = clips,
            createdAt = System.currentTimeMillis()
        )
        _battleSets.value = _battleSets.value + set
        saveToStorage()
        return set
    }
    
    /**
     * Delete a battle set
     */
    fun deleteBattleSet(setId: Long) {
        _battleSets.value = _battleSets.value.filter { it.id != setId }
        saveToStorage()
    }
    
    // ==================== RECOMMENDED CLIPS TO PREPARE ====================
    
    /**
     * Get recommendations for clips you should prepare
     * Based on what's missing from your armory
     */
    fun getPreparationRecommendations(): List<ClipRecommendation> {
        val recommendations = mutableListOf<ClipRecommendation>()
        val current = _clipsByPurpose.value
        
        // Check each purpose and recommend if missing
        ClipPurpose.values().forEach { purpose ->
            val count = current[purpose]?.size ?: 0
            val minRecommended = purpose.minRecommended
            
            if (count < minRecommended) {
                recommendations.add(
                    ClipRecommendation(
                        purpose = purpose,
                        currentCount = count,
                        recommendedCount = minRecommended,
                        priority = when {
                            count == 0 -> RecommendationPriority.CRITICAL
                            count < minRecommended / 2 -> RecommendationPriority.HIGH
                            else -> RecommendationPriority.MEDIUM
                        },
                        suggestion = purpose.preparationTip
                    )
                )
            }
        }
        
        return recommendations.sortedBy { it.priority.ordinal }
    }
    
    // ==================== HELPERS ====================
    
    private fun updateClipsByPurpose() {
        _clipsByPurpose.value = _counterClips.value.groupBy { it.purpose }
    }
    
    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        return "${minutes}:${secs.toString().padStart(2, '0')}"
    }
    
    // ==================== PERSISTENCE ====================
    
    private fun saveToStorage() {
        try {
            // Save counter clips
            val clipsJson = JSONArray()
            _counterClips.value.forEach { clip ->
                clipsJson.put(clip.toJson())
            }
            
            // Save quick fire
            val quickFireJson = JSONArray()
            _quickFireClips.value.forEach { clip ->
                quickFireJson.put(clip.id)
            }
            
            // Save battle sets
            val setsJson = JSONArray()
            _battleSets.value.forEach { set ->
                setsJson.put(set.toJson())
            }
            
            prefs.edit()
                .putString(KEY_COUNTER_CLIPS, clipsJson.toString())
                .putString(KEY_QUICK_FIRE, quickFireJson.toString())
                .putString(KEY_BATTLE_SETS, setsJson.toString())
                .apply()
                
            Log.i(TAG, "Saved ${_counterClips.value.size} clips, ${_quickFireClips.value.size} quick fire")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save", e)
        }
    }
    
    private fun loadFromStorage() {
        try {
            // Load counter clips
            val clipsJson = prefs.getString(KEY_COUNTER_CLIPS, null)
            if (clipsJson != null) {
                val array = JSONArray(clipsJson)
                val clips = mutableListOf<CounterClip>()
                for (i in 0 until array.length()) {
                    clips.add(CounterClip.fromJson(array.getJSONObject(i)))
                }
                _counterClips.value = clips
                updateClipsByPurpose()
            }
            
            // Load quick fire
            val quickFireJson = prefs.getString(KEY_QUICK_FIRE, null)
            if (quickFireJson != null) {
                val array = JSONArray(quickFireJson)
                val quickFireIds = (0 until array.length()).map { array.getLong(it) }
                _quickFireClips.value = _counterClips.value.filter { it.id in quickFireIds }
            }
            
            // Load battle sets
            val setsJson = prefs.getString(KEY_BATTLE_SETS, null)
            if (setsJson != null) {
                val array = JSONArray(setsJson)
                val sets = mutableListOf<BattleSet>()
                for (i in 0 until array.length()) {
                    sets.add(BattleSet.fromJson(array.getJSONObject(i), _counterClips.value))
                }
                _battleSets.value = sets
            }
            
            Log.i(TAG, "Loaded ${_counterClips.value.size} clips")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load", e)
        }
    }
}

// ==================== DATA CLASSES ====================

/**
 * A counter clip - a specific A-B section of a song ready for battle
 */
data class CounterClip(
    val id: Long,
    val songId: Long,
    val songTitle: String,
    val songArtist: String,
    val songPath: String,
    val startMs: Long,           // A point
    val endMs: Long,             // B point
    val durationMs: Long,        // Clip length
    val name: String,            // Custom name like "Bass Drop #1"
    val purpose: ClipPurpose,    // What it counters
    val notes: String,           // Battle notes
    val useCount: Int,           // Times used
    val winCount: Int,           // Times it worked
    val createdAt: Long
) {
    val winRate: Float get() = if (useCount > 0) winCount.toFloat() / useCount else 0f
    
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("songId", songId)
        put("songTitle", songTitle)
        put("songArtist", songArtist)
        put("songPath", songPath)
        put("startMs", startMs)
        put("endMs", endMs)
        put("durationMs", durationMs)
        put("name", name)
        put("purpose", purpose.name)
        put("notes", notes)
        put("useCount", useCount)
        put("winCount", winCount)
        put("createdAt", createdAt)
    }
    
    companion object {
        fun fromJson(json: JSONObject): CounterClip = CounterClip(
            id = json.getLong("id"),
            songId = json.getLong("songId"),
            songTitle = json.getString("songTitle"),
            songArtist = json.getString("songArtist"),
            songPath = json.getString("songPath"),
            startMs = json.getLong("startMs"),
            endMs = json.getLong("endMs"),
            durationMs = json.getLong("durationMs"),
            name = json.getString("name"),
            purpose = try { ClipPurpose.valueOf(json.getString("purpose")) } catch (e: Exception) { ClipPurpose.ALL_ROUNDER },
            notes = json.optString("notes", ""),
            useCount = json.optInt("useCount", 0),
            winCount = json.optInt("winCount", 0),
            createdAt = json.optLong("createdAt", 0)
        )
    }
}

/**
 * What a clip is designed to counter
 */
enum class ClipPurpose(
    val displayName: String,
    val emoji: String,
    val description: String,
    val minRecommended: Int,
    val preparationTip: String
) {
    BASS_DESTROYER(
        "Bass Destroyer",
        "🔊",
        "Overpower opponent's bass with MORE bass",
        5,
        "Find sections with DEEP sub-bass (30-60Hz). Mark the heaviest drops."
    ),
    SUB_SHAKER(
        "Sub Shaker",
        "💥",
        "Extreme sub-bass to shake the ground",
        3,
        "808s, sub drops, anything below 50Hz that vibrates chests"
    ),
    MID_CUTTER(
        "Mid Cutter",
        "🔪",
        "Cut through bass-heavy opponents with strong mids",
        5,
        "Vocal hooks, synth leads, horns - anything 500Hz-2kHz that pierces"
    ),
    HIGH_PIERCER(
        "High Piercer",
        "⚡",
        "Bright highs to fill opponent's gaps",
        3,
        "Hi-hats, cymbals, high synths that sparkle above the mix"
    ),
    ENERGY_BOMB(
        "Energy Bomb",
        "💣",
        "Maximum energy to overpower quiet opponents",
        5,
        "The loudest, most energetic sections - big drops, builds, peaks"
    ),
    DROP_KILLER(
        "Drop Killer",
        "🎯",
        "Silence-to-bass transitions, perfect for attack moments",
        5,
        "Find build-ups that lead to massive drops. Mark from buildup start."
    ),
    FREQUENCY_FILLER(
        "Frequency Filler",
        "🌊",
        "Full spectrum sound to fill all gaps",
        3,
        "Balanced sections with bass, mids, AND highs all present"
    ),
    CROWD_HYPER(
        "Crowd Hyper",
        "🙌",
        "Gets the crowd jumping - singalongs, call-response",
        3,
        "Anthemic moments, crowd participation sections, big hooks"
    ),
    SILENCE_BREAKER(
        "Silence Breaker",
        "💀",
        "First hit after opponent stops - make it count",
        3,
        "Clean, impactful starts - no build up, just immediate hit"
    ),
    ALL_ROUNDER(
        "All Rounder",
        "🎵",
        "General purpose counter that works in many situations",
        5,
        "Solid sections that work against most opponents"
    )
}

/**
 * A battle set - organized collection of clips for a specific situation
 */
data class BattleSet(
    val id: Long,
    val name: String,
    val description: String,
    val clips: List<CounterClip>,
    val createdAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("description", description)
        put("clipIds", JSONArray(clips.map { it.id }))
        put("createdAt", createdAt)
    }
    
    companion object {
        fun fromJson(json: JSONObject, allClips: List<CounterClip>): BattleSet {
            val clipIds = json.getJSONArray("clipIds")
            val ids = (0 until clipIds.length()).map { clipIds.getLong(it) }
            return BattleSet(
                id = json.getLong("id"),
                name = json.getString("name"),
                description = json.optString("description", ""),
                clips = allClips.filter { it.id in ids },
                createdAt = json.optLong("createdAt", 0)
            )
        }
    }
}

/**
 * Recommendation for clips you should prepare
 */
data class ClipRecommendation(
    val purpose: ClipPurpose,
    val currentCount: Int,
    val recommendedCount: Int,
    val priority: RecommendationPriority,
    val suggestion: String
)

enum class RecommendationPriority {
    CRITICAL,  // You have NONE of these!
    HIGH,      // You need more
    MEDIUM,    // Could use a few more
    LOW        // Nice to have
}

// ==================== RECOMMENDED SONG TYPES ====================

/**
 * Types of custom songs you should prepare for battle
 * 
 * These are recommendations for what to have in your library:
 */
object BattleSongRecommendations {
    
    val ESSENTIAL_CATEGORIES = listOf(
        SongTypeRecommendation(
            category = "BASS WEAPONS",
            emoji = "🔊",
            minCount = 10,
            description = "Songs with EXTREME bass for bass battles",
            examples = listOf(
                "808 heavy tracks",
                "Dub plates with boosted subs",
                "Reggae bass lines",
                "Trap with sub-bass drops"
            ),
            tips = listOf(
                "Find sections with bass below 60Hz",
                "Mark the heaviest bass drops as clips",
                "Have variety: some with sustained bass, some with punchy bass"
            )
        ),
        SongTypeRecommendation(
            category = "MID RANGE KILLERS",
            emoji = "🔪",
            minCount = 8,
            description = "Songs that CUT THROUGH heavy bass",
            examples = listOf(
                "Vocal-heavy tracks",
                "Horn sections (reggae, dancehall)",
                "Synth leads (1-3kHz)",
                "Guitar-driven tracks"
            ),
            tips = listOf(
                "Look for presence in 500Hz-2kHz range",
                "Vocals with clarity, not buried in mix",
                "Use these when opponent is all bass, no mids"
            )
        ),
        SongTypeRecommendation(
            category = "ENERGY BOMBS",
            emoji = "💣",
            minCount = 10,
            description = "Maximum energy tracks to overpower quiet opponents",
            examples = listOf(
                "Build-up → Drop sections",
                "Festival anthems",
                "High BPM bangers",
                "Crowd participation tracks"
            ),
            tips = listOf(
                "Mark the exact moment the energy peaks",
                "Include the build-up in your clip for maximum impact",
                "These are for when opponent is playing soft"
            )
        ),
        SongTypeRecommendation(
            category = "DROP KILLERS",
            emoji = "🎯",
            minCount = 8,
            description = "Silence → Massive impact transitions",
            examples = listOf(
                "Tracks that start with a big hit",
                "Post-breakdown drops",
                "Any section: quiet → LOUD"
            ),
            tips = listOf(
                "Perfect for attack opportunities",
                "When opponent ends a song, HIT them with this",
                "The contrast is the weapon"
            )
        ),
        SongTypeRecommendation(
            category = "CUSTOM DUBPLATES",
            emoji = "🏆",
            minCount = 5,
            description = "YOUR exclusive tracks no one else has",
            examples = listOf(
                "Tracks made for your sound system",
                "Remixes with your name",
                "Unreleased exclusives",
                "Custom versions of popular tracks"
            ),
            tips = listOf(
                "These are your secret weapons",
                "Opponent can't counter what they've never heard",
                "Save these for crucial moments"
            )
        ),
        SongTypeRecommendation(
            category = "BPM MATCHED SETS",
            emoji = "🎵",
            minCount = 15,
            description = "Songs at common battle BPMs for seamless mixing",
            examples = listOf(
                "90-100 BPM (Reggae, Hip-Hop)",
                "110-120 BPM (Dancehall)",
                "125-130 BPM (House, UK Garage)",
                "140-150 BPM (Jungle, D&B)"
            ),
            tips = listOf(
                "Match opponent's BPM for seamless takeover",
                "Or use double/half time for energy shift",
                "Pre-analyze all BPMs!"
            )
        ),
        SongTypeRecommendation(
            category = "CROWD PLEASERS",
            emoji = "🙌",
            minCount = 5,
            description = "Tracks that get the crowd on YOUR side",
            examples = listOf(
                "Anthems everyone knows",
                "Call and response tracks",
                "Singalong hooks",
                "Local favorites"
            ),
            tips = listOf(
                "Crowd reaction matters in battles",
                "Use these to turn the crowd",
                "Timing is everything"
            )
        ),
        SongTypeRecommendation(
            category = "FREQUENCY FILLERS",
            emoji = "🌊",
            minCount = 5,
            description = "Full spectrum tracks that fill ALL frequencies",
            examples = listOf(
                "Well-mastered commercial tracks",
                "Full band recordings",
                "Orchestral/cinematic pieces",
                "Live recordings with ambiance"
            ),
            tips = listOf(
                "Use when opponent has gaps in their frequency range",
                "Creates a 'wall of sound' effect",
                "Good for overwhelming specific weaknesses"
            )
        )
    )
    
    /**
     * Get total recommended songs
     */
    fun getTotalRecommended(): Int = ESSENTIAL_CATEGORIES.sumOf { it.minCount }
    
    /**
     * Get checklist for battle preparation
     */
    fun getBattleChecklist(): List<String> = listOf(
        "✓ At least 10 BASS WEAPONS with marked drop clips",
        "✓ At least 8 MID CUTTERS for bass-heavy opponents",
        "✓ At least 10 ENERGY BOMBS for quiet moments",
        "✓ At least 8 DROP KILLERS for attack opportunities",
        "✓ At least 5 CUSTOM DUBPLATES (your exclusives)",
        "✓ Songs at all common BPMs (90, 110, 120, 140)",
        "✓ Quick Fire loaded with your TOP 10 clips",
        "✓ All songs pre-indexed in Battle Database",
        "✓ A-B clips marked for best sections of each song",
        "✓ Battle Sets organized by opponent type"
    )
}

data class SongTypeRecommendation(
    val category: String,
    val emoji: String,
    val minCount: Int,
    val description: String,
    val examples: List<String>,
    val tips: List<String>
)
