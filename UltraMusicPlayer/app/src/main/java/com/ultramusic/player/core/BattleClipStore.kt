package com.ultramusic.player.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BATTLE CLIP STORE
 * 
 * Internal store of battle-ready clip templates and presets.
 * Works with AutoClipDetector to provide smart suggestions.
 * 
 * Features:
 * - Pre-defined clip templates (bass drop, energy peak, etc.)
 * - User favorites store
 * - Recently used clips
 * - Battle session history
 * - Export/Import capability
 */
@Singleton
class BattleClipStore @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "BattleClipStore"
        private const val PREFS_NAME = "battle_clip_store"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_RECENT = "recent"
        private const val KEY_SESSION_HISTORY = "session_history"
        private const val KEY_CUSTOM_TEMPLATES = "custom_templates"
        private const val MAX_RECENT = 50
        private const val MAX_SESSION_HISTORY = 20
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // ==================== FAVORITES ====================
    private val _favorites = MutableStateFlow<List<StoredClip>>(emptyList())
    val favorites: StateFlow<List<StoredClip>> = _favorites.asStateFlow()
    
    // ==================== RECENT CLIPS ====================
    private val _recentClips = MutableStateFlow<List<StoredClip>>(emptyList())
    val recentClips: StateFlow<List<StoredClip>> = _recentClips.asStateFlow()
    
    // ==================== SESSION HISTORY ====================
    private val _sessionHistory = MutableStateFlow<List<BattleSession>>(emptyList())
    val sessionHistory: StateFlow<List<BattleSession>> = _sessionHistory.asStateFlow()
    
    // ==================== CUSTOM TEMPLATES ====================
    private val _customTemplates = MutableStateFlow<List<ClipTemplate>>(emptyList())
    val customTemplates: StateFlow<List<ClipTemplate>> = _customTemplates.asStateFlow()
    
    // ==================== BUILT-IN TEMPLATES ====================
    val builtInTemplates: List<ClipTemplate> = listOf(
        ClipTemplate(
            id = "bass_drop",
            name = "Bass Drop",
            emoji = "🔊",
            purpose = ClipPurpose.BASS_DESTROYER,
            description = "Find heavy bass drops for bass battles",
            detectionHint = "Low → High bass transition",
            idealDuration = 15000L,
            minEnergy = 0.6f,
            minBass = 0.5f
        ),
        ClipTemplate(
            id = "energy_peak",
            name = "Energy Peak",
            emoji = "💣",
            purpose = ClipPurpose.ENERGY_BOMB,
            description = "Maximum energy sections",
            detectionHint = "Top 10% loudness",
            idealDuration = 20000L,
            minEnergy = 0.8f
        ),
        ClipTemplate(
            id = "drop_killer",
            name = "Drop Killer",
            emoji = "🎯",
            purpose = ClipPurpose.DROP_KILLER,
            description = "Silence → Impact transitions",
            detectionHint = "Quiet to loud moment",
            idealDuration = 10000L,
            hasSilenceStart = true
        ),
        ClipTemplate(
            id = "mid_cutter",
            name = "Mid Cutter",
            emoji = "🔪",
            purpose = ClipPurpose.MID_CUTTER,
            description = "Vocal/mid-heavy for cutting bass",
            detectionHint = "Strong 500Hz-2kHz",
            idealDuration = 15000L,
            minMid = 0.5f
        ),
        ClipTemplate(
            id = "crowd_hype",
            name = "Crowd Hype",
            emoji = "🙌",
            purpose = ClipPurpose.CROWD_HYPER,
            description = "Crowd participation moments",
            detectionHint = "Sing-along/call-response",
            idealDuration = 20000L,
            minEnergy = 0.7f
        ),
        ClipTemplate(
            id = "sub_shaker",
            name = "Sub Shaker",
            emoji = "💥",
            purpose = ClipPurpose.SUB_SHAKER,
            description = "Deep sub-bass sections",
            detectionHint = "Below 60Hz dominance",
            idealDuration = 15000L,
            minBass = 0.7f
        ),
        ClipTemplate(
            id = "full_spectrum",
            name = "Full Spectrum",
            emoji = "🌊",
            purpose = ClipPurpose.FREQUENCY_FILLER,
            description = "Balanced all frequencies",
            detectionHint = "Bass + Mids + Highs present",
            idealDuration = 15000L,
            minBass = 0.25f,
            minMid = 0.25f,
            minHigh = 0.2f
        ),
        ClipTemplate(
            id = "silence_breaker",
            name = "Silence Breaker",
            emoji = "💀",
            purpose = ClipPurpose.SILENCE_BREAKER,
            description = "Immediate impact start",
            detectionHint = "No buildup, instant hit",
            idealDuration = 10000L,
            hasImmediateStart = true
        )
    )
    
    init {
        loadFromStorage()
    }
    
    // ==================== FAVORITES MANAGEMENT ====================
    
    fun addToFavorites(clip: StoredClip) {
        if (_favorites.value.none { it.id == clip.id }) {
            _favorites.value = listOf(clip) + _favorites.value
            saveToStorage()
            Log.i(TAG, "Added to favorites: ${clip.name}")
        }
    }
    
    fun removeFromFavorites(clipId: String) {
        _favorites.value = _favorites.value.filter { it.id != clipId }
        saveToStorage()
    }
    
    fun isFavorite(clipId: String): Boolean {
        return _favorites.value.any { it.id == clipId }
    }
    
    // ==================== RECENT CLIPS ====================
    
    fun addToRecent(clip: StoredClip) {
        val updated = listOf(clip) + _recentClips.value.filter { it.id != clip.id }
        _recentClips.value = updated.take(MAX_RECENT)
        saveToStorage()
    }
    
    fun clearRecent() {
        _recentClips.value = emptyList()
        saveToStorage()
    }
    
    // ==================== SESSION HISTORY ====================
    
    fun startSession(): BattleSession {
        val session = BattleSession(
            id = System.currentTimeMillis().toString(),
            startTime = System.currentTimeMillis(),
            clips = emptyList()
        )
        _sessionHistory.value = listOf(session) + _sessionHistory.value.take(MAX_SESSION_HISTORY - 1)
        saveToStorage()
        return session
    }
    
    fun addClipToSession(sessionId: String, clip: StoredClip, wasEffective: Boolean) {
        _sessionHistory.value = _sessionHistory.value.map { session ->
            if (session.id == sessionId) {
                session.copy(
                    clips = session.clips + SessionClipUse(
                        clip = clip,
                        usedAt = System.currentTimeMillis(),
                        wasEffective = wasEffective
                    )
                )
            } else session
        }
        saveToStorage()
    }
    
    fun endSession(sessionId: String, won: Boolean) {
        _sessionHistory.value = _sessionHistory.value.map { session ->
            if (session.id == sessionId) {
                session.copy(
                    endTime = System.currentTimeMillis(),
                    won = won
                )
            } else session
        }
        saveToStorage()
    }
    
    // ==================== CUSTOM TEMPLATES ====================
    
    fun createTemplate(
        name: String,
        purpose: ClipPurpose,
        description: String,
        idealDuration: Long = 15000L,
        minEnergy: Float = 0f,
        minBass: Float = 0f,
        minMid: Float = 0f,
        minHigh: Float = 0f
    ): ClipTemplate {
        val template = ClipTemplate(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            emoji = purpose.emoji,
            purpose = purpose,
            description = description,
            detectionHint = "Custom template",
            idealDuration = idealDuration,
            minEnergy = minEnergy,
            minBass = minBass,
            minMid = minMid,
            minHigh = minHigh,
            isCustom = true
        )
        _customTemplates.value = _customTemplates.value + template
        saveToStorage()
        return template
    }
    
    fun deleteTemplate(templateId: String) {
        _customTemplates.value = _customTemplates.value.filter { it.id != templateId }
        saveToStorage()
    }
    
    // ==================== BULK OPERATIONS ====================
    
    fun saveAllClips(clips: List<DetectedClip>, toArmory: BattleArmory): Int {
        var saved = 0
        clips.forEach { clip ->
            try {
                // Convert to StoredClip
                val stored = StoredClip(
                    id = "${clip.songId}_${clip.startMs}",
                    songId = clip.songId,
                    songTitle = clip.songTitle,
                    songArtist = clip.songArtist,
                    songPath = clip.songPath,
                    startMs = clip.startMs,
                    endMs = clip.endMs,
                    name = clip.suggestedName,
                    purpose = clip.purpose,
                    qualityScore = clip.qualityScore,
                    reason = clip.reason,
                    createdAt = System.currentTimeMillis()
                )
                
                // Add to recent
                addToRecent(stored)
                
                // Save to armory (need Song object)
                // This would be done by the caller
                saved++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save clip: ${clip.suggestedName}", e)
            }
        }
        return saved
    }
    
    // ==================== STATISTICS ====================
    
    fun getStatistics(): ClipStoreStats {
        val allSessions = _sessionHistory.value
        val allClipsUsed = allSessions.flatMap { it.clips }
        
        return ClipStoreStats(
            totalFavorites = _favorites.value.size,
            totalRecent = _recentClips.value.size,
            totalSessions = allSessions.size,
            sessionsWon = allSessions.count { it.won == true },
            totalClipsUsed = allClipsUsed.size,
            effectiveClips = allClipsUsed.count { it.wasEffective },
            mostUsedPurpose = allClipsUsed
                .groupBy { it.clip.purpose }
                .maxByOrNull { it.value.size }
                ?.key,
            winRate = if (allSessions.isNotEmpty()) {
                allSessions.count { it.won == true }.toFloat() / allSessions.size
            } else 0f
        )
    }
    
    // ==================== PERSISTENCE ====================
    
    private fun saveToStorage() {
        try {
            // Save favorites
            val favoritesJson = JSONArray()
            _favorites.value.forEach { clip ->
                favoritesJson.put(clip.toJson())
            }
            
            // Save recent
            val recentJson = JSONArray()
            _recentClips.value.forEach { clip ->
                recentJson.put(clip.toJson())
            }
            
            // Save session history
            val sessionsJson = JSONArray()
            _sessionHistory.value.forEach { session ->
                sessionsJson.put(session.toJson())
            }
            
            // Save custom templates
            val templatesJson = JSONArray()
            _customTemplates.value.forEach { template ->
                templatesJson.put(template.toJson())
            }
            
            prefs.edit()
                .putString(KEY_FAVORITES, favoritesJson.toString())
                .putString(KEY_RECENT, recentJson.toString())
                .putString(KEY_SESSION_HISTORY, sessionsJson.toString())
                .putString(KEY_CUSTOM_TEMPLATES, templatesJson.toString())
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save", e)
        }
    }
    
    private fun loadFromStorage() {
        try {
            // Load favorites
            prefs.getString(KEY_FAVORITES, null)?.let { json ->
                val array = JSONArray(json)
                _favorites.value = (0 until array.length()).map { 
                    StoredClip.fromJson(array.getJSONObject(it)) 
                }
            }
            
            // Load recent
            prefs.getString(KEY_RECENT, null)?.let { json ->
                val array = JSONArray(json)
                _recentClips.value = (0 until array.length()).map { 
                    StoredClip.fromJson(array.getJSONObject(it)) 
                }
            }
            
            // Load session history
            prefs.getString(KEY_SESSION_HISTORY, null)?.let { json ->
                val array = JSONArray(json)
                _sessionHistory.value = (0 until array.length()).map { 
                    BattleSession.fromJson(array.getJSONObject(it)) 
                }
            }
            
            // Load custom templates
            prefs.getString(KEY_CUSTOM_TEMPLATES, null)?.let { json ->
                val array = JSONArray(json)
                _customTemplates.value = (0 until array.length()).map { 
                    ClipTemplate.fromJson(array.getJSONObject(it)) 
                }
            }
            
            Log.i(TAG, "Loaded ${_favorites.value.size} favorites, ${_recentClips.value.size} recent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load", e)
        }
    }
}

// ==================== DATA CLASSES ====================

/**
 * A stored clip (saved to internal store)
 */
data class StoredClip(
    val id: String,
    val songId: Long,
    val songTitle: String,
    val songArtist: String,
    val songPath: String,
    val startMs: Long,
    val endMs: Long,
    val name: String,
    val purpose: ClipPurpose,
    val qualityScore: Float,
    val reason: String,
    val createdAt: Long,
    val useCount: Int = 0,
    val winCount: Int = 0,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList()
) {
    val durationMs: Long get() = endMs - startMs
    val winRate: Float get() = if (useCount > 0) winCount.toFloat() / useCount else 0f
    
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("songId", songId)
        put("songTitle", songTitle)
        put("songArtist", songArtist)
        put("songPath", songPath)
        put("startMs", startMs)
        put("endMs", endMs)
        put("name", name)
        put("purpose", purpose.name)
        put("qualityScore", qualityScore)
        put("reason", reason)
        put("createdAt", createdAt)
        put("useCount", useCount)
        put("winCount", winCount)
        put("isFavorite", isFavorite)
        put("tags", JSONArray(tags))
    }
    
    companion object {
        fun fromJson(json: JSONObject): StoredClip {
            val tagsArray = json.optJSONArray("tags")
            val tags = if (tagsArray != null) {
                (0 until tagsArray.length()).map { tagsArray.getString(it) }
            } else emptyList()
            
            return StoredClip(
                id = json.getString("id"),
                songId = json.getLong("songId"),
                songTitle = json.getString("songTitle"),
                songArtist = json.getString("songArtist"),
                songPath = json.getString("songPath"),
                startMs = json.getLong("startMs"),
                endMs = json.getLong("endMs"),
                name = json.getString("name"),
                purpose = try { ClipPurpose.valueOf(json.getString("purpose")) } 
                         catch (e: Exception) { ClipPurpose.ALL_ROUNDER },
                qualityScore = json.optDouble("qualityScore", 0.0).toFloat(),
                reason = json.optString("reason", ""),
                createdAt = json.optLong("createdAt", 0),
                useCount = json.optInt("useCount", 0),
                winCount = json.optInt("winCount", 0),
                isFavorite = json.optBoolean("isFavorite", false),
                tags = tags
            )
        }
    }
}

/**
 * A clip template for auto-detection
 */
data class ClipTemplate(
    val id: String,
    val name: String,
    val emoji: String,
    val purpose: ClipPurpose,
    val description: String,
    val detectionHint: String,
    val idealDuration: Long = 15000L,
    val minEnergy: Float = 0f,
    val minBass: Float = 0f,
    val minMid: Float = 0f,
    val minHigh: Float = 0f,
    val hasSilenceStart: Boolean = false,
    val hasImmediateStart: Boolean = false,
    val isCustom: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("emoji", emoji)
        put("purpose", purpose.name)
        put("description", description)
        put("detectionHint", detectionHint)
        put("idealDuration", idealDuration)
        put("minEnergy", minEnergy)
        put("minBass", minBass)
        put("minMid", minMid)
        put("minHigh", minHigh)
        put("hasSilenceStart", hasSilenceStart)
        put("hasImmediateStart", hasImmediateStart)
        put("isCustom", isCustom)
    }
    
    companion object {
        fun fromJson(json: JSONObject): ClipTemplate = ClipTemplate(
            id = json.getString("id"),
            name = json.getString("name"),
            emoji = json.optString("emoji", "🎵"),
            purpose = try { ClipPurpose.valueOf(json.getString("purpose")) } 
                     catch (e: Exception) { ClipPurpose.ALL_ROUNDER },
            description = json.optString("description", ""),
            detectionHint = json.optString("detectionHint", ""),
            idealDuration = json.optLong("idealDuration", 15000),
            minEnergy = json.optDouble("minEnergy", 0.0).toFloat(),
            minBass = json.optDouble("minBass", 0.0).toFloat(),
            minMid = json.optDouble("minMid", 0.0).toFloat(),
            minHigh = json.optDouble("minHigh", 0.0).toFloat(),
            hasSilenceStart = json.optBoolean("hasSilenceStart", false),
            hasImmediateStart = json.optBoolean("hasImmediateStart", false),
            isCustom = json.optBoolean("isCustom", false)
        )
    }
}

/**
 * A battle session record
 */
data class BattleSession(
    val id: String,
    val startTime: Long,
    val endTime: Long? = null,
    val clips: List<SessionClipUse> = emptyList(),
    val won: Boolean? = null,
    val notes: String = ""
) {
    val duration: Long? get() = endTime?.let { it - startTime }
    
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("startTime", startTime)
        endTime?.let { put("endTime", it) }
        put("clips", JSONArray().apply {
            clips.forEach { put(it.toJson()) }
        })
        won?.let { put("won", it) }
        put("notes", notes)
    }
    
    companion object {
        fun fromJson(json: JSONObject): BattleSession {
            val clipsArray = json.optJSONArray("clips")
            val clips = if (clipsArray != null) {
                (0 until clipsArray.length()).map { 
                    SessionClipUse.fromJson(clipsArray.getJSONObject(it)) 
                }
            } else emptyList()
            
            return BattleSession(
                id = json.getString("id"),
                startTime = json.getLong("startTime"),
                endTime = if (json.has("endTime")) json.getLong("endTime") else null,
                clips = clips,
                won = if (json.has("won")) json.getBoolean("won") else null,
                notes = json.optString("notes", "")
            )
        }
    }
}

/**
 * Record of a clip used in a session
 */
data class SessionClipUse(
    val clip: StoredClip,
    val usedAt: Long,
    val wasEffective: Boolean
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("clip", clip.toJson())
        put("usedAt", usedAt)
        put("wasEffective", wasEffective)
    }
    
    companion object {
        fun fromJson(json: JSONObject): SessionClipUse = SessionClipUse(
            clip = StoredClip.fromJson(json.getJSONObject("clip")),
            usedAt = json.getLong("usedAt"),
            wasEffective = json.getBoolean("wasEffective")
        )
    }
}

/**
 * Clip store statistics
 */
data class ClipStoreStats(
    val totalFavorites: Int,
    val totalRecent: Int,
    val totalSessions: Int,
    val sessionsWon: Int,
    val totalClipsUsed: Int,
    val effectiveClips: Int,
    val mostUsedPurpose: ClipPurpose?,
    val winRate: Float
)
