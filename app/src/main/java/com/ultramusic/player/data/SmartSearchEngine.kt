package com.ultramusic.player.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Smart Search Engine
 * 
 * Advanced search with:
 * - Fuzzy matching (typo tolerance)
 * - Phonetic matching (sounds-like search)
 * - Bengali transliteration support
 * - Partial word matching
 * - Common variations handling
 */
@Singleton
class SmartSearchEngine @Inject constructor() {
    
    // Common Bengali to English transliterations for song names
    private val bengaliTransliterations = mapOf(
        // Common words in Bengali songs
        "প্রেম" to listOf("prem", "pram"),
        "ভালোবাসা" to listOf("bhalobasa", "valobasha", "bhalobasha"),
        "গান" to listOf("gaan", "gan"),
        "মন" to listOf("mon", "mone"),
        "আমি" to listOf("ami", "aami"),
        "তুমি" to listOf("tumi", "toomi"),
        "আকাশ" to listOf("akash", "aakash"),
        "বৃষ্টি" to listOf("brishti", "bristy", "bristi"),
        "সূর্য" to listOf("surjo", "surya", "shurjo"),
        "চাঁদ" to listOf("chand", "chaand"),
        "রাত" to listOf("raat", "rat"),
        "দিন" to listOf("din", "deen"),
        "স্বপ্ন" to listOf("swapno", "shopno", "swopno"),
        "জীবন" to listOf("jibon", "jeebon", "jibon"),
        "মরণ" to listOf("moron", "maran"),
        "কষ্ট" to listOf("koshto", "kosto", "kashto"),
        "সুখ" to listOf("sukh", "shukh"),
        "দুঃখ" to listOf("dukkho", "dukho", "dukhkho"),
        
        // Popular song words
        "কেন" to listOf("keno", "kano"),
        "কি" to listOf("ki", "kee"),
        "না" to listOf("na", "naa"),
        "হ্যাঁ" to listOf("haan", "ha", "hya"),
        "এক" to listOf("ek", "aek"),
        "দুই" to listOf("dui", "dooi"),
    )
    
    // Common song title variations
    private val commonVariations = mapOf(
        "kesariya" to listOf("kesaria", "kasariya", "kesriya", "kesariyaa"),
        "tum hi ho" to listOf("tum he ho", "tumhi ho", "tum hiho"),
        "channa mereya" to listOf("chana mereya", "channa meriya", "chana meriya"),
        "tera ban" to listOf("tere baan", "tera baan"),
        "raataan lambiyan" to listOf("ratan lambiyan", "raatan lambiya", "ratan lambiya"),
        "pasoori" to listOf("pasuri", "pasoori", "pasori"),
        "excuses" to listOf("excusez", "excuse"),
        "apna bana le" to listOf("apna banale", "apna bana"),
    )
    
    // Bengali-accented English phonetic mappings
    private val bengaliAccentPhonetics = mapOf(
        // V/W confusion
        "v" to "w",
        "w" to "v",
        // Th sounds
        "th" to "t",
        "t" to "th",
        // J/Z confusion
        "j" to "z",
        "z" to "j",
        // F/Ph confusion
        "f" to "ph",
        "ph" to "f",
        // S/Sh confusion
        "s" to "sh",
        "sh" to "s",
        // Ch/S confusion  
        "ch" to "s",
        // Common endings
        "tion" to "shon",
        "sion" to "shon",
    )
    
    /**
     * Search songs with smart matching
     * Returns list of (Song, MatchScore) sorted by relevance
     */
    fun searchSongs(songs: List<Song>, query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        
        val normalizedQuery = normalizeText(query)
        val queryWords = normalizedQuery.split(" ").filter { it.isNotBlank() }
        
        val results = songs.mapNotNull { song ->
            val score = calculateMatchScore(song, normalizedQuery, queryWords)
            if (score > 0) SearchResult(song, score, getMatchType(score)) else null
        }
        
        return results.sortedByDescending { it.score }
    }
    
    /**
     * Calculate match score for a song against query
     */
    private fun calculateMatchScore(
        song: Song,
        normalizedQuery: String,
        queryWords: List<String>
    ): Int {
        var totalScore = 0
        
        val titleNorm = normalizeText(song.title)
        val artistNorm = normalizeText(song.artist)
        val albumNorm = normalizeText(song.album)
        
        // Exact match (highest priority)
        if (titleNorm == normalizedQuery) totalScore += 1000
        if (artistNorm == normalizedQuery) totalScore += 800
        
        // Contains match
        if (titleNorm.contains(normalizedQuery)) totalScore += 500
        if (artistNorm.contains(normalizedQuery)) totalScore += 400
        if (albumNorm.contains(normalizedQuery)) totalScore += 300
        
        // Word-by-word matching
        for (word in queryWords) {
            if (titleNorm.contains(word)) totalScore += 100
            if (artistNorm.contains(word)) totalScore += 80
            if (albumNorm.contains(word)) totalScore += 60
        }
        
        // Fuzzy matching (typo tolerance)
        val fuzzyTitleScore = fuzzyMatch(titleNorm, normalizedQuery)
        val fuzzyArtistScore = fuzzyMatch(artistNorm, normalizedQuery)
        totalScore += (fuzzyTitleScore * 200).toInt()
        totalScore += (fuzzyArtistScore * 150).toInt()
        
        // Phonetic matching (sounds-like)
        val phoneticTitleScore = phoneticMatch(titleNorm, normalizedQuery)
        val phoneticArtistScore = phoneticMatch(artistNorm, normalizedQuery)
        totalScore += (phoneticTitleScore * 150).toInt()
        totalScore += (phoneticArtistScore * 100).toInt()
        
        // Bengali transliteration matching
        val translitScore = bengaliTransliterationMatch(song, normalizedQuery)
        totalScore += translitScore
        
        // Common variation matching
        val variationScore = variationMatch(titleNorm, normalizedQuery)
        totalScore += variationScore
        
        // Starts-with bonus (important for quick search)
        if (titleNorm.startsWith(normalizedQuery)) totalScore += 200
        if (artistNorm.startsWith(normalizedQuery)) totalScore += 150
        
        // Word starts-with bonus
        for (word in queryWords) {
            val titleWords = titleNorm.split(" ")
            val artistWords = artistNorm.split(" ")
            
            if (titleWords.any { it.startsWith(word) }) totalScore += 50
            if (artistWords.any { it.startsWith(word) }) totalScore += 40
        }
        
        return totalScore
    }
    
    /**
     * Fuzzy matching using Levenshtein distance
     * Returns similarity score (0.0 to 1.0)
     */
    private fun fuzzyMatch(text: String, query: String): Float {
        if (text.isEmpty() || query.isEmpty()) return 0f
        
        // Check each word in text
        val textWords = text.split(" ")
        var bestMatch = 0f
        
        for (word in textWords) {
            val distance = levenshteinDistance(word, query)
            val maxLen = maxOf(word.length, query.length)
            val similarity = 1f - (distance.toFloat() / maxLen)
            
            if (similarity > bestMatch) {
                bestMatch = similarity
            }
        }
        
        // Also check full text similarity
        val fullDistance = levenshteinDistance(text, query)
        val fullSimilarity = 1f - (fullDistance.toFloat() / maxOf(text.length, query.length))
        
        return maxOf(bestMatch, fullSimilarity * 0.8f) // Weight word matches higher
    }
    
    /**
     * Levenshtein distance calculation
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        
        if (m == 0) return n
        if (n == 0) return m
        
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        
        return dp[m][n]
    }
    
    /**
     * Phonetic matching using simplified Soundex-like algorithm
     * with Bengali accent considerations
     */
    private fun phoneticMatch(text: String, query: String): Float {
        val textPhonetic = toPhonetic(text)
        val queryPhonetic = toPhonetic(query)
        
        if (textPhonetic == queryPhonetic) return 1.0f
        if (textPhonetic.contains(queryPhonetic)) return 0.8f
        
        // Check with Bengali accent variations
        val queryWithAccent = applyBengaliAccent(query)
        for (variation in queryWithAccent) {
            val varPhonetic = toPhonetic(variation)
            if (textPhonetic.contains(varPhonetic)) return 0.7f
        }
        
        return 0f
    }
    
    /**
     * Convert text to phonetic representation
     */
    private fun toPhonetic(text: String): String {
        var result = text.lowercase()
        
        // Remove vowels except first letter
        val firstChar = result.firstOrNull() ?: return ""
        result = firstChar + result.drop(1)
            .replace(Regex("[aeiou]"), "")
        
        // Normalize common phonetic equivalents
        result = result
            .replace("ph", "f")
            .replace("ck", "k")
            .replace("gh", "g")
            .replace("wh", "w")
            .replace("kn", "n")
            .replace("wr", "r")
            .replace("mb", "m")
            .replace("sc", "s")
            .replace("ce", "s")
            .replace("ci", "s")
            .replace("x", "ks")
            .replace("q", "k")
        
        // Remove duplicate consonants
        result = result.replace(Regex("(.)\\1+"), "$1")
        
        return result
    }
    
    /**
     * Apply Bengali accent variations to a word
     */
    private fun applyBengaliAccent(text: String): List<String> {
        val variations = mutableListOf(text)
        var current = text.lowercase()
        
        for ((from, to) in bengaliAccentPhonetics) {
            if (current.contains(from)) {
                variations.add(current.replace(from, to))
            }
            if (current.contains(to)) {
                variations.add(current.replace(to, from))
            }
        }
        
        return variations.distinct()
    }
    
    /**
     * Match against Bengali transliterations
     */
    private fun bengaliTransliterationMatch(song: Song, query: String): Int {
        var score = 0
        
        // Check if query matches any Bengali transliteration
        for ((_, translits) in bengaliTransliterations) {
            if (translits.any { it.contains(query) || query.contains(it) }) {
                // Check if song contains the Bengali word or its transliterations
                for (translit in translits) {
                    if (song.title.lowercase().contains(translit)) score += 100
                    if (song.artist.lowercase().contains(translit)) score += 80
                }
            }
        }
        
        return score
    }
    
    /**
     * Match against common song title variations
     */
    private fun variationMatch(title: String, query: String): Int {
        for ((canonical, variations) in commonVariations) {
            val allForms = listOf(canonical) + variations
            
            val queryMatchesVariation = allForms.any { 
                query.contains(it) || it.contains(query) 
            }
            val titleMatchesVariation = allForms.any { 
                title.contains(it) 
            }
            
            if (queryMatchesVariation && titleMatchesVariation) {
                return 150
            }
        }
        return 0
    }
    
    /**
     * Normalize text for comparison
     */
    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove special chars
            .replace(Regex("\\s+"), " ")        // Normalize spaces
            .trim()
    }
    
    /**
     * Determine match type based on score
     */
    private fun getMatchType(score: Int): MatchType {
        return when {
            score >= 1000 -> MatchType.EXACT
            score >= 500 -> MatchType.STRONG
            score >= 200 -> MatchType.GOOD
            score >= 100 -> MatchType.PARTIAL
            else -> MatchType.WEAK
        }
    }
    
    /**
     * Get search suggestions based on partial input
     */
    fun getSuggestions(songs: List<Song>, partialQuery: String, limit: Int = 5): List<String> {
        if (partialQuery.length < 2) return emptyList()
        
        val normalized = normalizeText(partialQuery)
        val suggestions = mutableSetOf<String>()
        
        // Collect matching titles
        for (song in songs) {
            val titleNorm = normalizeText(song.title)
            val artistNorm = normalizeText(song.artist)
            
            if (titleNorm.startsWith(normalized) || 
                titleNorm.split(" ").any { it.startsWith(normalized) }) {
                suggestions.add(song.title)
            }
            
            if (artistNorm.startsWith(normalized)) {
                suggestions.add(song.artist)
            }
            
            if (suggestions.size >= limit * 2) break
        }
        
        return suggestions.take(limit).toList()
    }
}

/**
 * Search result with score and match type
 */
data class SearchResult(
    val song: Song,
    val score: Int,
    val matchType: MatchType
)

enum class MatchType {
    EXACT,    // Perfect match
    STRONG,   // Very good match
    GOOD,     // Good match
    PARTIAL,  // Partial match
    WEAK      // Weak match
}
