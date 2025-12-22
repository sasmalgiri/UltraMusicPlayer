package com.ultramusic.player.ai

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.ultramusic.player.core.ClipPurpose
import com.ultramusic.player.core.FrequencyRange
import com.ultramusic.player.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GROK AI SERVICE
 * 
 * Free AI-powered battle intelligence using xAI's Grok API.
 * 
 * Features:
 * - Counter song recommendations based on opponent analysis
 * - Battle strategy suggestions
 * - Crowd reaction predictions
 * - Real-time battle commentary
 * 
 * Grok has a FREE tier with generous limits!
 * Get your API key at: https://console.x.ai/
 * 
 * Alternative free options also supported:
 * - Groq (very fast, free tier)
 * - Together AI (free credits)
 * - OpenRouter (pay as you go, cheap)
 */
@Singleton
class GrokAIService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "GrokAIService"
        private const val PREFS_NAME = "grok_ai_settings"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PROVIDER = "provider"
        
        // API Endpoints
        private const val GROK_API_URL = "https://api.x.ai/v1/chat/completions"
        private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions"
        
        // Models
        private const val GROK_MODEL = "grok-beta"
        private const val GROQ_MODEL = "llama-3.1-70b-versatile" // Free!
        private const val OPENROUTER_MODEL = "meta-llama/llama-3.1-8b-instruct:free" // Free!
    }
    
    enum class AIProvider {
        GROK,       // xAI's Grok - Free tier available
        GROQ,       // Groq - Very fast, free tier
        OPENROUTER  // OpenRouter - Many free models
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    // State
    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()
    
    private val _lastResponse = MutableStateFlow<String?>(null)
    val lastResponse: StateFlow<String?> = _lastResponse.asStateFlow()
    
    private var currentProvider: AIProvider = AIProvider.GROQ // Default to free Groq
    private var apiKey: String? = null
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        apiKey = prefs.getString(KEY_API_KEY, null)
        currentProvider = try {
            AIProvider.valueOf(prefs.getString(KEY_PROVIDER, AIProvider.GROQ.name) ?: AIProvider.GROQ.name)
        } catch (e: Exception) {
            AIProvider.GROQ
        }
        _isConfigured.value = !apiKey.isNullOrBlank()
    }
    
    /**
     * Configure API key and provider
     */
    fun configure(apiKey: String, provider: AIProvider = AIProvider.GROQ) {
        this.apiKey = apiKey
        this.currentProvider = provider
        
        prefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_PROVIDER, provider.name)
            .apply()
        
        _isConfigured.value = apiKey.isNotBlank()
        Log.i(TAG, "Configured with provider: $provider")
    }
    
    /**
     * Get counter song recommendation based on opponent analysis
     */
    suspend fun getCounterRecommendation(
        opponentBpm: Int,
        opponentDominantFrequency: FrequencyRange,
        opponentEnergy: Float,
        availableSongs: List<Song>
    ): AICounterRecommendation? = withContext(Dispatchers.IO) {
        
        if (!_isConfigured.value) {
            Log.w(TAG, "AI not configured, using fallback")
            return@withContext fallbackRecommendation(opponentDominantFrequency, availableSongs)
        }
        
        val songList = availableSongs.take(20).mapIndexed { i, song ->
            "${i + 1}. ${song.title} - ${song.artist}"
        }.joinToString("\n")
        
        val prompt = """
            You are a sound system battle AI assistant. Analyze the opponent and recommend a counter song.
            
            OPPONENT ANALYSIS:
            - BPM: $opponentBpm
            - Dominant Frequency: ${opponentDominantFrequency.displayName}
            - Energy Level: ${(opponentEnergy * 100).toInt()}%
            
            AVAILABLE SONGS:
            $songList
            
            STRATEGY RULES:
            1. If opponent is bass-heavy (SUB_BASS/BASS), counter with MID-heavy song OR even HEAVIER bass
            2. If opponent is mid-heavy, counter with BASS-heavy or HIGH-heavy
            3. Match or exceed their BPM for energy battles
            4. If they're low energy, hit them with maximum energy
            
            Respond in this EXACT JSON format:
            {
                "songIndex": <number 1-20>,
                "reason": "<brief strategy explanation>",
                "counterStrategy": "<OVERPOWER_BASS|CUT_THROUGH_MIDS|ENERGY_BOMB|FREQUENCY_GAP>",
                "confidence": <0.0-1.0>
            }
        """.trimIndent()
        
        try {
            val response = callAI(prompt)
            parseCounterRecommendation(response, availableSongs)
        } catch (e: Exception) {
            Log.e(TAG, "AI call failed", e)
            fallbackRecommendation(opponentDominantFrequency, availableSongs)
        }
    }
    
    /**
     * Get battle strategy advice
     */
    suspend fun getBattleStrategy(
        opponentProfile: String,
        currentScore: String,
        crowdMood: String
    ): BattleStrategy? = withContext(Dispatchers.IO) {
        
        if (!_isConfigured.value) return@withContext null
        
        val prompt = """
            You are a sound system battle strategist. Give tactical advice.
            
            SITUATION:
            - Opponent Style: $opponentProfile
            - Current Score: $currentScore
            - Crowd Mood: $crowdMood
            
            Provide battle strategy in this JSON format:
            {
                "nextMove": "<aggressive|defensive|surprise|crowd_pleaser>",
                "frequencyFocus": "<sub_bass|bass|low_mid|mid|high_mid|high>",
                "energyLevel": "<build_up|maintain|peak|drop>",
                "timing": "<immediate|wait_for_drop|counter_after_opponent>",
                "advice": "<brief tactical advice>"
            }
        """.trimIndent()
        
        try {
            val response = callAI(prompt)
            parseBattleStrategy(response)
        } catch (e: Exception) {
            Log.e(TAG, "Strategy call failed", e)
            null
        }
    }
    
    /**
     * Get clip purpose suggestion for auto-detected clip
     */
    suspend fun suggestClipPurpose(
        songTitle: String,
        startTime: String,
        endTime: String,
        bassStrength: Float,
        midStrength: Float,
        highStrength: Float,
        energy: Float
    ): ClipPurpose = withContext(Dispatchers.IO) {
        
        if (!_isConfigured.value) {
            // Fallback logic
            return@withContext when {
                bassStrength > 0.6f -> ClipPurpose.BASS_DESTROYER
                energy > 0.8f -> ClipPurpose.ENERGY_BOMB
                midStrength > 0.5f -> ClipPurpose.MID_CUTTER
                else -> ClipPurpose.ALL_ROUNDER
            }
        }
        
        val prompt = """
            Classify this audio clip for sound system battle use.
            
            CLIP INFO:
            - Song: $songTitle
            - Time: $startTime - $endTime
            - Bass: ${(bassStrength * 100).toInt()}%
            - Mids: ${(midStrength * 100).toInt()}%
            - Highs: ${(highStrength * 100).toInt()}%
            - Energy: ${(energy * 100).toInt()}%
            
            CATEGORIES:
            1. BASS_DESTROYER - Heavy sub-bass, speaker shaking
            2. SUB_SHAKER - Deep rumbling bass
            3. MID_CUTTER - Cutting mids that slice through bass
            4. HIGH_PIERCER - Sharp highs that cut through
            5. ENERGY_BOMB - Maximum energy explosion
            6. DROP_KILLER - Perfect drop/transition moment
            7. CROWD_HYPER - Gets crowd moving
            8. SILENCE_BREAKER - Instant impact from silence
            9. FREQUENCY_FILLER - Fills gaps in spectrum
            10. ALL_ROUNDER - General purpose
            
            Respond with ONLY the category name, nothing else.
        """.trimIndent()
        
        try {
            val response = callAI(prompt)
            ClipPurpose.values().find { 
                response.uppercase().contains(it.name) 
            } ?: ClipPurpose.ALL_ROUNDER
        } catch (e: Exception) {
            ClipPurpose.ALL_ROUNDER
        }
    }
    
    /**
     * Generate battle commentary
     */
    suspend fun generateCommentary(
        event: String,
        yourMove: String?,
        opponentMove: String?,
        crowdReaction: String?
    ): String = withContext(Dispatchers.IO) {
        
        if (!_isConfigured.value) {
            return@withContext when (event) {
                "battle_start" -> "🔥 BATTLE BEGINS! May the best sound system win!"
                "your_turn" -> "💪 Your turn to shake the ground!"
                "opponent_turn" -> "👀 Opponent is making their move..."
                "crowd_hype" -> "🙌 The crowd is going WILD!"
                else -> "⚔️ The battle continues!"
            }
        }
        
        val prompt = """
            You are a hype sound system battle MC. Generate exciting commentary (max 50 words).
            
            EVENT: $event
            YOUR MOVE: ${yourMove ?: "none"}
            OPPONENT: ${opponentMove ?: "none"}
            CROWD: ${crowdReaction ?: "watching"}
            
            Be energetic, use battle slang, make it exciting! Include emojis.
        """.trimIndent()
        
        try {
            callAI(prompt)
        } catch (e: Exception) {
            "⚔️ The battle is ON!"
        }
    }
    
    /**
     * Call the AI API
     */
    private suspend fun callAI(prompt: String): String = withContext(Dispatchers.IO) {
        val key = apiKey ?: throw IllegalStateException("API key not configured")
        
        val (url, model) = when (currentProvider) {
            AIProvider.GROK -> GROK_API_URL to GROK_MODEL
            AIProvider.GROQ -> GROQ_API_URL to GROQ_MODEL
            AIProvider.OPENROUTER -> OPENROUTER_API_URL to OPENROUTER_MODEL
        }
        
        val requestBody = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "user", content = prompt)
            ),
            temperature = 0.7f,
            maxTokens = 500
        )
        
        val json = gson.toJson(requestBody)
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = httpClient.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("API call failed: ${response.code} ${response.message}")
        }
        
        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
        
        val content = chatResponse.choices?.firstOrNull()?.message?.content
            ?: throw Exception("No content in response")
        
        _lastResponse.value = content
        Log.d(TAG, "AI Response: $content")
        
        content
    }
    
    private fun parseCounterRecommendation(json: String, songs: List<Song>): AICounterRecommendation? {
        return try {
            // Extract JSON from response (might have extra text)
            val jsonStart = json.indexOf('{')
            val jsonEnd = json.lastIndexOf('}') + 1
            val cleanJson = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                json.substring(jsonStart, jsonEnd)
            } else json
            
            val parsed = gson.fromJson(cleanJson, CounterRecommendationJson::class.java)
            val songIndex = (parsed.songIndex ?: 1) - 1
            
            if (songIndex in songs.indices) {
                AICounterRecommendation(
                    song = songs[songIndex],
                    reason = parsed.reason ?: "AI recommended",
                    strategy = parsed.counterStrategy ?: "OVERPOWER",
                    confidence = parsed.confidence ?: 0.5f
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse recommendation", e)
            null
        }
    }
    
    private fun parseBattleStrategy(json: String): BattleStrategy? {
        return try {
            val jsonStart = json.indexOf('{')
            val jsonEnd = json.lastIndexOf('}') + 1
            val cleanJson = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                json.substring(jsonStart, jsonEnd)
            } else json
            
            gson.fromJson(cleanJson, BattleStrategy::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse strategy", e)
            null
        }
    }
    
    private fun fallbackRecommendation(
        opponentFreq: FrequencyRange,
        songs: List<Song>
    ): AICounterRecommendation? {
        if (songs.isEmpty()) return null
        
        // Simple heuristic: pick a random song with counter strategy
        val song = songs.random()
        val strategy = when (opponentFreq) {
            FrequencyRange.SUB_BASS, FrequencyRange.BASS -> "CUT_THROUGH_MIDS"
            FrequencyRange.LOW_MID, FrequencyRange.MID -> "OVERPOWER_BASS"
            else -> "ENERGY_BOMB"
        }
        
        return AICounterRecommendation(
            song = song,
            reason = "Counter ${opponentFreq.displayName} with contrasting frequency",
            strategy = strategy,
            confidence = 0.6f
        )
    }
}

// ==================== DATA CLASSES ====================

data class AICounterRecommendation(
    val song: Song,
    val reason: String,
    val strategy: String,
    val confidence: Float
)

data class BattleStrategy(
    @SerializedName("nextMove") val nextMove: String? = null,
    @SerializedName("frequencyFocus") val frequencyFocus: String? = null,
    @SerializedName("energyLevel") val energyLevel: String? = null,
    @SerializedName("timing") val timing: String? = null,
    @SerializedName("advice") val advice: String? = null
)

private data class CounterRecommendationJson(
    @SerializedName("songIndex") val songIndex: Int? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("counterStrategy") val counterStrategy: String? = null,
    @SerializedName("confidence") val confidence: Float? = null
)

// API Request/Response classes
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 500
)

private data class ChatMessage(
    val role: String,
    val content: String
)

private data class ChatResponse(
    val choices: List<ChatChoice>? = null
)

private data class ChatChoice(
    val message: ChatMessage? = null
)
