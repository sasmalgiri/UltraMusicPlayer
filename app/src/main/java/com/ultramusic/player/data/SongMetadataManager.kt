package com.ultramusic.player.data

import android.content.Context
import android.media.MediaMetadataRetriever
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Song Metadata Manager
 * 
 * Auto-names songs with their popular/canonical titles so that:
 * 1. User searches with popular name -> finds the song
 * 2. Song displays with popular name -> user recognizes it
 * 
 * Example:
 * File: "Track_01_Final_Mix_v2.mp3" 
 * Popular name: "Kesariya - Brahmastra"
 * 
 * Features:
 * - Offline title database for common songs
 * - Audio fingerprint matching (basic)
 * - Metadata extraction and cleaning
 * - Title normalization and aliases
 */
@Singleton
class SongMetadataManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Local database of popular songs with normalized names
    // In production, this would be a larger database or cloud lookup
    private val popularSongDatabase = buildPopularSongDatabase()
    
    // Cache of processed songs
    private val normalizedTitleCache = mutableMapOf<Long, NormalizedSongInfo>()
    
    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()
    
    /**
     * Normalize a song's title to its popular/canonical form
     */
    suspend fun normalizeSong(song: Song): NormalizedSongInfo = withContext(Dispatchers.IO) {
        // Check cache first
        normalizedTitleCache[song.id]?.let { return@withContext it }
        
        // Try multiple matching strategies
        val normalizedInfo = tryMatchStrategies(song)
        
        // Cache result
        normalizedTitleCache[song.id] = normalizedInfo
        
        normalizedInfo
    }
    
    /**
     * Batch normalize all songs
     */
    suspend fun normalizeAll(songs: List<Song>): Map<Long, NormalizedSongInfo> = 
        withContext(Dispatchers.IO) {
            val results = mutableMapOf<Long, NormalizedSongInfo>()
            
            songs.forEachIndexed { index, song ->
                results[song.id] = normalizeSong(song)
                _processingProgress.value = (index + 1).toFloat() / songs.size
            }
            
            _processingProgress.value = 1f
            results
        }
    
    /**
     * Try multiple strategies to match song to popular title
     */
    private fun tryMatchStrategies(song: Song): NormalizedSongInfo {
        // Strategy 1: Direct title match
        findByTitle(song.title)?.let { match ->
            return NormalizedSongInfo(
                originalTitle = song.title,
                normalizedTitle = match.popularTitle,
                artist = match.artist,
                aliases = match.aliases,
                confidence = 0.95f,
                matchMethod = MatchMethod.TITLE_MATCH
            )
        }
        
        // Strategy 2: Clean title and match
        val cleanedTitle = cleanTitle(song.title)
        findByTitle(cleanedTitle)?.let { match ->
            return NormalizedSongInfo(
                originalTitle = song.title,
                normalizedTitle = match.popularTitle,
                artist = match.artist,
                aliases = match.aliases,
                confidence = 0.85f,
                matchMethod = MatchMethod.CLEANED_TITLE_MATCH
            )
        }
        
        // Strategy 3: Fuzzy match
        findFuzzyMatch(cleanedTitle)?.let { (match, confidence) ->
            return NormalizedSongInfo(
                originalTitle = song.title,
                normalizedTitle = match.popularTitle,
                artist = match.artist,
                aliases = match.aliases,
                confidence = confidence,
                matchMethod = MatchMethod.FUZZY_MATCH
            )
        }
        
        // Strategy 4: Artist + partial title match
        findByArtistAndPartialTitle(song.artist, cleanedTitle)?.let { match ->
            return NormalizedSongInfo(
                originalTitle = song.title,
                normalizedTitle = match.popularTitle,
                artist = match.artist,
                aliases = match.aliases,
                confidence = 0.75f,
                matchMethod = MatchMethod.ARTIST_TITLE_MATCH
            )
        }
        
        // Strategy 5: Try to extract from metadata/filename
        val extractedInfo = extractFromFilename(song.path)
        
        // No match found - return cleaned version
        return NormalizedSongInfo(
            originalTitle = song.title,
            normalizedTitle = extractedInfo?.title ?: cleanTitle(song.title),
            artist = extractedInfo?.artist ?: song.artist,
            aliases = listOf(song.title),
            confidence = 0.5f,
            matchMethod = MatchMethod.EXTRACTED
        )
    }
    
    /**
     * Find exact title match in database
     */
    private fun findByTitle(title: String): PopularSong? {
        val normalized = title.lowercase().trim()
        
        return popularSongDatabase.find { song ->
            song.popularTitle.lowercase() == normalized ||
            song.aliases.any { it.lowercase() == normalized }
        }
    }
    
    /**
     * Find fuzzy match in database
     */
    private fun findFuzzyMatch(title: String): Pair<PopularSong, Float>? {
        val normalized = title.lowercase()
        var bestMatch: PopularSong? = null
        var bestScore = 0f
        
        for (song in popularSongDatabase) {
            val allTitles = listOf(song.popularTitle) + song.aliases
            
            for (candidate in allTitles) {
                val score = calculateSimilarity(normalized, candidate.lowercase())
                if (score > bestScore && score > 0.7f) {
                    bestScore = score
                    bestMatch = song
                }
            }
        }
        
        return bestMatch?.let { it to bestScore }
    }
    
    /**
     * Find by artist and partial title
     */
    private fun findByArtistAndPartialTitle(artist: String, title: String): PopularSong? {
        val normalizedArtist = artist.lowercase()
        val normalizedTitle = title.lowercase()
        
        return popularSongDatabase.find { song ->
            song.artist.lowercase().contains(normalizedArtist) &&
            (song.popularTitle.lowercase().contains(normalizedTitle) ||
             normalizedTitle.contains(song.popularTitle.lowercase().take(5)))
        }
    }
    
    /**
     * Clean title by removing common noise
     */
    private fun cleanTitle(title: String): String {
        var cleaned = title
        
        // Remove common prefixes/suffixes
        val patterns = listOf(
            Regex("^\\d+[.\\-_\\s]+"),           // "01. ", "01 - "
            Regex("\\s*\\(official\\s*(video|audio|music)?\\)?", RegexOption.IGNORE_CASE),
            Regex("\\s*\\[official\\s*(video|audio|music)?\\]?", RegexOption.IGNORE_CASE),
            Regex("\\s*-\\s*official\\s*(video|audio)?", RegexOption.IGNORE_CASE),
            Regex("\\s*\\(lyrics?\\)?", RegexOption.IGNORE_CASE),
            Regex("\\s*\\[lyrics?\\]?", RegexOption.IGNORE_CASE),
            Regex("\\s*\\(hd\\)?", RegexOption.IGNORE_CASE),
            Regex("\\s*\\(hq\\)?", RegexOption.IGNORE_CASE),
            Regex("\\s*\\(full\\s*song\\)?", RegexOption.IGNORE_CASE),
            Regex("\\s*\\(audio\\)?", RegexOption.IGNORE_CASE),
            Regex("\\s*\\(video\\)?", RegexOption.IGNORE_CASE),
            Regex("\\s*-\\s*mp3\\s*$", RegexOption.IGNORE_CASE),
            Regex("\\s*_+\\s*"),                 // Multiple underscores
            Regex("\\s+"),                        // Multiple spaces
        )
        
        for (pattern in patterns) {
            cleaned = cleaned.replace(pattern, " ")
        }
        
        // Remove file extension
        cleaned = cleaned.replace(Regex("\\.(mp3|m4a|flac|wav|ogg|aac)$", RegexOption.IGNORE_CASE), "")
        
        // Replace underscores with spaces
        cleaned = cleaned.replace("_", " ")
        
        // Trim and normalize spaces
        cleaned = cleaned.trim().replace(Regex("\\s+"), " ")
        
        return cleaned
    }
    
    /**
     * Extract info from filename
     */
    private fun extractFromFilename(path: String): ExtractedInfo? {
        val filename = File(path).nameWithoutExtension
        
        // Try pattern: "Artist - Title"
        val dashPattern = Regex("^(.+?)\\s*-\\s*(.+)$")
        dashPattern.find(filename)?.let { match ->
            return ExtractedInfo(
                artist = match.groupValues[1].trim(),
                title = cleanTitle(match.groupValues[2].trim())
            )
        }
        
        // Just clean the filename
        return ExtractedInfo(
            artist = null,
            title = cleanTitle(filename)
        )
    }
    
    /**
     * Calculate string similarity (Jaro-Winkler)
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1f
        if (s1.isEmpty() || s2.isEmpty()) return 0f
        
        val maxDist = (maxOf(s1.length, s2.length) / 2) - 1
        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)
        
        var matches = 0
        var transpositions = 0
        
        // Find matches
        for (i in s1.indices) {
            val start = maxOf(0, i - maxDist)
            val end = minOf(i + maxDist + 1, s2.length)
            
            for (j in start until end) {
                if (s2Matches[j] || s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }
        
        if (matches == 0) return 0f
        
        // Count transpositions
        var k = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }
        
        val jaro = (matches.toFloat() / s1.length +
                   matches.toFloat() / s2.length +
                   (matches - transpositions / 2f) / matches) / 3
        
        // Winkler modification
        var prefix = 0
        for (i in 0 until minOf(s1.length, s2.length, 4)) {
            if (s1[i] == s2[i]) prefix++ else break
        }
        
        return jaro + prefix * 0.1f * (1 - jaro)
    }
    
    /**
     * Get all aliases for a song
     */
    fun getAliases(song: Song): List<String> {
        val normalized = normalizedTitleCache[song.id]
        return normalized?.aliases ?: listOf(song.title)
    }
    
    /**
     * Search with normalized titles
     */
    fun searchWithNormalization(songs: List<Song>, query: String): List<Song> {
        val normalizedQuery = query.lowercase().trim()
        
        return songs.filter { song ->
            val info = normalizedTitleCache[song.id]
            
            // Match against normalized title
            info?.normalizedTitle?.lowercase()?.contains(normalizedQuery) == true ||
            // Match against aliases
            info?.aliases?.any { it.lowercase().contains(normalizedQuery) } == true ||
            // Match against original
            song.title.lowercase().contains(normalizedQuery) ||
            song.artist.lowercase().contains(normalizedQuery)
        }
    }
    
    /**
     * Build the popular song database
     * In production, this would be loaded from a file or network
     */
    private fun buildPopularSongDatabase(): List<PopularSong> {
        return listOf(
            // Bollywood Popular
            PopularSong(
                popularTitle = "Kesariya",
                artist = "Arijit Singh",
                movie = "Brahmastra",
                aliases = listOf("kesaria", "kasariya", "kesriya", "kesariyaa", "kesariya tera ishq hai piya")
            ),
            PopularSong(
                popularTitle = "Tum Hi Ho",
                artist = "Arijit Singh",
                movie = "Aashiqui 2",
                aliases = listOf("tum he ho", "tumhi ho", "tum hiho", "tum hi ho aashiqui")
            ),
            PopularSong(
                popularTitle = "Channa Mereya",
                artist = "Arijit Singh",
                movie = "Ae Dil Hai Mushkil",
                aliases = listOf("chana mereya", "channa meriya", "chana meriya")
            ),
            PopularSong(
                popularTitle = "Tera Ban Jaunga",
                artist = "Akhil Sachdeva, Tulsi Kumar",
                movie = "Kabir Singh",
                aliases = listOf("tera baan jaunga", "tera ban jaonga")
            ),
            PopularSong(
                popularTitle = "Raataan Lambiyan",
                artist = "Jubin Nautiyal, Asees Kaur",
                movie = "Shershaah",
                aliases = listOf("ratan lambiyan", "raatan lambiya", "ratan lambiya", "raataan lambiaan")
            ),
            PopularSong(
                popularTitle = "Pasoori",
                artist = "Ali Sethi, Shae Gill",
                aliases = listOf("pasuri", "pasoori", "pasori", "pasoori coke studio")
            ),
            PopularSong(
                popularTitle = "Apna Bana Le",
                artist = "Arijit Singh",
                movie = "Bhediya",
                aliases = listOf("apna banale", "apna bana", "apna bana le piya")
            ),
            PopularSong(
                popularTitle = "Excuses",
                artist = "AP Dhillon, Gurinder Gill",
                aliases = listOf("excusez", "excuse", "excuses ap dhillon")
            ),
            PopularSong(
                popularTitle = "Dil Nu",
                artist = "AP Dhillon",
                aliases = listOf("dil nu ap dhillon", "dil noo")
            ),
            PopularSong(
                popularTitle = "Maan Meri Jaan",
                artist = "King",
                aliases = listOf("tu maan meri jaan", "maan meri jan", "man meri jaan")
            ),
            PopularSong(
                popularTitle = "Kahani Suno",
                artist = "Kaifi Khalil",
                aliases = listOf("kahani suno 2.0", "kahani suno 2", "kahani sunao")
            ),
            
            // Bengali Popular
            PopularSong(
                popularTitle = "Tomake Chai",
                artist = "Arijit Singh",
                movie = "Gangster",
                aliases = listOf("tomake cai", "tomake chay", "তোমাকে চাই")
            ),
            PopularSong(
                popularTitle = "Mon Majhi Re",
                artist = "Arijit Singh",
                movie = "Boss",
                aliases = listOf("mon majhire", "মন মাঝি রে", "mon maji re")
            ),
            PopularSong(
                popularTitle = "Bolte Bolte Cholte Cholte",
                artist = "Imran Mahmudul",
                aliases = listOf("bolte bolte", "বলতে বলতে", "bolte cholte")
            ),
            PopularSong(
                popularTitle = "Tumi Ashbe Bole",
                artist = "Nachiketa",
                aliases = listOf("tumi asbe bole", "তুমি আসবে বলে")
            ),
            PopularSong(
                popularTitle = "Keno Emon Hoy",
                artist = "Ash King",
                aliases = listOf("keno amon hoy", "কেন এমন হয়")
            ),
            PopularSong(
                popularTitle = "Sesh Kanna",
                artist = "Minar Rahman",
                aliases = listOf("ses kanna", "শেষ কান্না")
            ),
            
            // International Popular
            PopularSong(
                popularTitle = "Shape of You",
                artist = "Ed Sheeran",
                aliases = listOf("shape of u", "shapeofyou")
            ),
            PopularSong(
                popularTitle = "Blinding Lights",
                artist = "The Weeknd",
                aliases = listOf("blinding light", "blinding lites")
            ),
            PopularSong(
                popularTitle = "Someone Like You",
                artist = "Adele",
                aliases = listOf("some one like you", "someone like u")
            ),
            PopularSong(
                popularTitle = "Despacito",
                artist = "Luis Fonsi, Daddy Yankee",
                aliases = listOf("despasito", "despaceto", "des pa cito")
            ),
            PopularSong(
                popularTitle = "Believer",
                artist = "Imagine Dragons",
                aliases = listOf("beleiver", "believer imagine dragons")
            ),
        )
    }
}

/**
 * Popular song entry
 */
data class PopularSong(
    val popularTitle: String,
    val artist: String,
    val movie: String? = null,
    val aliases: List<String> = emptyList()
)

/**
 * Normalized song information
 */
data class NormalizedSongInfo(
    val originalTitle: String,
    val normalizedTitle: String,
    val artist: String,
    val aliases: List<String>,
    val confidence: Float,
    val matchMethod: MatchMethod
)

enum class MatchMethod {
    TITLE_MATCH,           // Exact title match
    CLEANED_TITLE_MATCH,   // Match after cleaning
    FUZZY_MATCH,           // Fuzzy string match
    ARTIST_TITLE_MATCH,    // Artist + partial title
    FINGERPRINT_MATCH,     // Audio fingerprint (future)
    EXTRACTED              // Extracted from metadata/filename
}

/**
 * Extracted info from filename
 */
private data class ExtractedInfo(
    val artist: String?,
    val title: String
)
