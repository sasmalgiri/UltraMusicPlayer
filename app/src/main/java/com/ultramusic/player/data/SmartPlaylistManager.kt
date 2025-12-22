package com.ultramusic.player.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Smart Active Playlist Manager
 * 
 * A dynamic playlist system that:
 * - Can be updated ANYTIME (even while playing)
 * - Supports real-time search while typing
 * - Uses fuzzy matching for typos
 * - Accepts voice commands to add songs
 * - Shows narrowing suggestions as user types
 * - Never panics - always has next song ready
 */
@Singleton
class SmartPlaylistManager @Inject constructor(
    private val smartSearchEngine: SmartSearchEngine
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    // ==================== PLAYLIST STATE ====================
    
    private val _activePlaylist = MutableStateFlow(ActivePlaylist())
    val activePlaylist: StateFlow<ActivePlaylist> = _activePlaylist.asStateFlow()
    
    private val _searchState = MutableStateFlow(PlaylistSearchState())
    val searchState: StateFlow<PlaylistSearchState> = _searchState.asStateFlow()
    
    private val _isAddingMode = MutableStateFlow(false)
    val isAddingMode: StateFlow<Boolean> = _isAddingMode.asStateFlow()
    
    // All available songs for searching
    private var allSongs: List<Song> = emptyList()
    
    // ==================== INITIALIZATION ====================
    
    fun initialize(songs: List<Song>) {
        allSongs = songs
    }
    
    // ==================== PLAYLIST OPERATIONS ====================
    
    /**
     * Add song to playlist at specific position or end
     */
    fun addSong(song: Song, position: Int? = null) {
        val current = _activePlaylist.value
        val newQueue = current.queue.toMutableList()
        
        if (position != null && position in 0..newQueue.size) {
            newQueue.add(position, song)
        } else {
            newQueue.add(song)
        }
        
        _activePlaylist.value = current.copy(
            queue = newQueue,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Add song to play next (after current song)
     */
    fun addToPlayNext(song: Song) {
        val current = _activePlaylist.value
        val nextPosition = current.currentIndex + 1
        addSong(song, nextPosition)
    }
    
    /**
     * Add multiple songs at once
     */
    fun addSongs(songs: List<Song>, position: Int? = null) {
        val current = _activePlaylist.value
        val newQueue = current.queue.toMutableList()
        
        val insertPosition = position ?: newQueue.size
        newQueue.addAll(insertPosition.coerceIn(0, newQueue.size), songs)
        
        _activePlaylist.value = current.copy(
            queue = newQueue,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Remove song from playlist
     */
    fun removeSong(index: Int) {
        val current = _activePlaylist.value
        if (index !in current.queue.indices) return
        
        val newQueue = current.queue.toMutableList()
        newQueue.removeAt(index)
        
        // Adjust current index if needed
        val newCurrentIndex = when {
            index < current.currentIndex -> current.currentIndex - 1
            index == current.currentIndex && newQueue.isEmpty() -> -1
            index == current.currentIndex -> current.currentIndex.coerceAtMost(newQueue.size - 1)
            else -> current.currentIndex
        }
        
        _activePlaylist.value = current.copy(
            queue = newQueue,
            currentIndex = newCurrentIndex,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Remove song by ID
     */
    fun removeSongById(songId: Long) {
        val index = _activePlaylist.value.queue.indexOfFirst { it.id == songId }
        if (index >= 0) removeSong(index)
    }
    
    /**
     * Move song to new position
     */
    fun moveSong(fromIndex: Int, toIndex: Int) {
        val current = _activePlaylist.value
        if (fromIndex !in current.queue.indices || toIndex !in current.queue.indices) return
        
        val newQueue = current.queue.toMutableList()
        val song = newQueue.removeAt(fromIndex)
        newQueue.add(toIndex, song)
        
        // Adjust current index
        val newCurrentIndex = when {
            current.currentIndex == fromIndex -> toIndex
            fromIndex < current.currentIndex && toIndex >= current.currentIndex -> current.currentIndex - 1
            fromIndex > current.currentIndex && toIndex <= current.currentIndex -> current.currentIndex + 1
            else -> current.currentIndex
        }
        
        _activePlaylist.value = current.copy(
            queue = newQueue,
            currentIndex = newCurrentIndex,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Clear entire playlist
     */
    fun clearPlaylist() {
        _activePlaylist.value = ActivePlaylist()
    }
    
    /**
     * Set current playing index
     */
    fun setCurrentIndex(index: Int) {
        val current = _activePlaylist.value
        if (index in current.queue.indices) {
            _activePlaylist.value = current.copy(currentIndex = index)
        }
    }
    
    /**
     * Move to next song
     */
    fun moveToNext(): Song? {
        val current = _activePlaylist.value
        val nextIndex = current.currentIndex + 1
        
        return if (nextIndex < current.queue.size) {
            _activePlaylist.value = current.copy(currentIndex = nextIndex)
            current.queue[nextIndex]
        } else if (current.isLooping && current.queue.isNotEmpty()) {
            _activePlaylist.value = current.copy(currentIndex = 0)
            current.queue[0]
        } else {
            null
        }
    }
    
    /**
     * Move to previous song
     */
    fun moveToPrevious(): Song? {
        val current = _activePlaylist.value
        val prevIndex = current.currentIndex - 1
        
        return if (prevIndex >= 0) {
            _activePlaylist.value = current.copy(currentIndex = prevIndex)
            current.queue[prevIndex]
        } else if (current.isLooping && current.queue.isNotEmpty()) {
            val lastIndex = current.queue.size - 1
            _activePlaylist.value = current.copy(currentIndex = lastIndex)
            current.queue[lastIndex]
        } else {
            null
        }
    }
    
    /**
     * Shuffle remaining songs (keeps current and played songs)
     */
    fun shuffleRemaining() {
        val current = _activePlaylist.value
        if (current.queue.size <= current.currentIndex + 2) return
        
        val played = current.queue.take(current.currentIndex + 1)
        val remaining = current.queue.drop(current.currentIndex + 1).shuffled()
        
        _activePlaylist.value = current.copy(
            queue = played + remaining,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * Toggle loop mode
     */
    fun toggleLoop() {
        val current = _activePlaylist.value
        _activePlaylist.value = current.copy(isLooping = !current.isLooping)
    }
    
    // ==================== REAL-TIME SEARCH ====================
    
    /**
     * Start adding mode - enables search UI
     */
    fun startAddingMode() {
        _isAddingMode.value = true
        _searchState.value = PlaylistSearchState()
    }
    
    /**
     * End adding mode
     */
    fun endAddingMode() {
        _isAddingMode.value = false
        _searchState.value = PlaylistSearchState()
    }
    
    /**
     * Update search query with real-time results
     * Shows narrowing suggestions as user types
     */
    fun updateSearchQuery(query: String) {
        scope.launch {
            val trimmedQuery = query.trim()
            
            if (trimmedQuery.isEmpty()) {
                _searchState.value = PlaylistSearchState(
                    query = "",
                    suggestions = emptyList(),
                    exactMatches = emptyList(),
                    fuzzyMatches = emptyList(),
                    similarMatches = emptyList()
                )
                return@launch
            }
            
            // Get search results with scoring
            val searchResults = smartSearchEngine.searchSongs(allSongs, trimmedQuery)
            
            // Categorize results
            val exactMatches = mutableListOf<SongMatch>()
            val fuzzyMatches = mutableListOf<SongMatch>()
            val similarMatches = mutableListOf<SongMatch>()
            
            for (result in searchResults) {
                val match = SongMatch(
                    song = result.song,
                    matchType = result.matchType,
                    score = result.score,
                    highlightedTitle = highlightMatch(result.song.title, trimmedQuery),
                    highlightedArtist = highlightMatch(result.song.artist, trimmedQuery)
                )
                
                when (result.matchType) {
                    MatchType.EXACT, MatchType.STRONG -> exactMatches.add(match)
                    MatchType.GOOD -> fuzzyMatches.add(match)
                    MatchType.PARTIAL, MatchType.WEAK -> similarMatches.add(match)
                }
            }
            
            // Get suggestions for autocomplete
            val suggestions = smartSearchEngine.getSuggestions(allSongs, trimmedQuery, 5)
            
            // Check if query narrowed down results
            val previousCount = _searchState.value.totalResults
            val currentCount = searchResults.size
            val isNarrowing = query.length > _searchState.value.query.length && 
                             currentCount < previousCount
            
            _searchState.value = PlaylistSearchState(
                query = query,
                suggestions = suggestions,
                exactMatches = exactMatches.take(10),
                fuzzyMatches = fuzzyMatches.take(10),
                similarMatches = similarMatches.take(10),
                totalResults = currentCount,
                isNarrowing = isNarrowing,
                isSearching = false
            )
        }
    }
    
    /**
     * Highlight matching parts in text
     */
    private fun highlightMatch(text: String, query: String): HighlightedText {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        
        val segments = mutableListOf<TextSegment>()
        var currentIndex = 0
        
        // Find all occurrences of query words
        val queryWords = lowerQuery.split(" ").filter { it.isNotBlank() }
        val matchRanges = mutableListOf<IntRange>()
        
        for (word in queryWords) {
            var searchFrom = 0
            while (true) {
                val index = lowerText.indexOf(word, searchFrom)
                if (index < 0) break
                matchRanges.add(index until (index + word.length))
                searchFrom = index + 1
            }
        }
        
        // Merge overlapping ranges
        val mergedRanges = mergeRanges(matchRanges.sortedBy { it.first })
        
        // Create segments
        for (range in mergedRanges) {
            if (currentIndex < range.first) {
                segments.add(TextSegment(text.substring(currentIndex, range.first), false))
            }
            segments.add(TextSegment(text.substring(range), true))
            currentIndex = range.last + 1
        }
        
        if (currentIndex < text.length) {
            segments.add(TextSegment(text.substring(currentIndex), false))
        }
        
        return HighlightedText(segments.ifEmpty { listOf(TextSegment(text, false)) })
    }
    
    private fun mergeRanges(ranges: List<IntRange>): List<IntRange> {
        if (ranges.isEmpty()) return emptyList()
        
        val merged = mutableListOf<IntRange>()
        var current = ranges[0]
        
        for (i in 1 until ranges.size) {
            val next = ranges[i]
            if (next.first <= current.last + 1) {
                current = current.first..maxOf(current.last, next.last)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        
        return merged
    }
    
    /**
     * Add song from search result to playlist
     */
    fun addFromSearch(song: Song, playNext: Boolean = false) {
        if (playNext) {
            addToPlayNext(song)
        } else {
            addSong(song)
        }
    }
    
    /**
     * Add first matching song from voice input
     */
    fun addFromVoice(recognizedText: String): AddResult {
        val results = smartSearchEngine.searchSongs(allSongs, recognizedText)
        
        return if (results.isNotEmpty()) {
            val bestMatch = results.first()
            addSong(bestMatch.song)
            AddResult.Success(bestMatch.song, bestMatch.matchType)
        } else {
            AddResult.NoMatch(recognizedText)
        }
    }
    
    /**
     * Quick add by partial name - adds best match
     */
    fun quickAdd(partialName: String): AddResult {
        return addFromVoice(partialName)
    }
    
    // ==================== PLAYLIST INFO ====================
    
    fun getCurrentSong(): Song? {
        val current = _activePlaylist.value
        return current.queue.getOrNull(current.currentIndex)
    }
    
    fun getNextSong(): Song? {
        val current = _activePlaylist.value
        return current.queue.getOrNull(current.currentIndex + 1)
    }
    
    fun getUpcoming(count: Int = 5): List<Song> {
        val current = _activePlaylist.value
        val startIndex = current.currentIndex + 1
        return current.queue.drop(startIndex).take(count)
    }
    
    fun getRemainingCount(): Int {
        val current = _activePlaylist.value
        return (current.queue.size - current.currentIndex - 1).coerceAtLeast(0)
    }
    
    fun getTotalDuration(): Long {
        return _activePlaylist.value.queue.sumOf { it.duration }
    }
    
    fun getRemainingDuration(): Long {
        val current = _activePlaylist.value
        return current.queue.drop(current.currentIndex).sumOf { it.duration }
    }
    
    // ==================== SAVE/LOAD PLAYLIST ====================
    
    fun saveAsPlaylist(name: String): SavedPlaylist {
        val current = _activePlaylist.value
        return SavedPlaylist(
            name = name,
            songIds = current.queue.map { it.id },
            createdAt = System.currentTimeMillis()
        )
    }
    
    fun loadPlaylist(savedPlaylist: SavedPlaylist) {
        val songs = savedPlaylist.songIds.mapNotNull { id ->
            allSongs.find { it.id == id }
        }
        
        _activePlaylist.value = ActivePlaylist(
            queue = songs,
            currentIndex = 0,
            lastModified = System.currentTimeMillis()
        )
    }
}

// ==================== DATA CLASSES ====================

/**
 * Active playlist state
 */
data class ActivePlaylist(
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isLooping: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
) {
    val isEmpty: Boolean get() = queue.isEmpty()
    val size: Int get() = queue.size
    val currentSong: Song? get() = queue.getOrNull(currentIndex)
    val nextSong: Song? get() = queue.getOrNull(currentIndex + 1)
    val hasNext: Boolean get() = currentIndex < queue.size - 1 || isLooping
    val hasPrevious: Boolean get() = currentIndex > 0 || isLooping
}

/**
 * Search state for playlist adding
 */
data class PlaylistSearchState(
    val query: String = "",
    val suggestions: List<String> = emptyList(),
    val exactMatches: List<SongMatch> = emptyList(),
    val fuzzyMatches: List<SongMatch> = emptyList(),
    val similarMatches: List<SongMatch> = emptyList(),
    val totalResults: Int = 0,
    val isNarrowing: Boolean = false,
    val isSearching: Boolean = false
) {
    val hasResults: Boolean get() = exactMatches.isNotEmpty() || 
                                    fuzzyMatches.isNotEmpty() || 
                                    similarMatches.isNotEmpty()
    
    val allMatches: List<SongMatch> get() = exactMatches + fuzzyMatches + similarMatches
}

/**
 * Song match with highlighting info
 */
data class SongMatch(
    val song: Song,
    val matchType: MatchType,
    val score: Int,
    val highlightedTitle: HighlightedText,
    val highlightedArtist: HighlightedText
)

/**
 * Text with highlighted segments
 */
data class HighlightedText(
    val segments: List<TextSegment>
) {
    val plainText: String get() = segments.joinToString("") { it.text }
}

data class TextSegment(
    val text: String,
    val isHighlighted: Boolean
)

/**
 * Result of adding a song
 */
sealed class AddResult {
    data class Success(val song: Song, val matchType: MatchType) : AddResult()
    data class NoMatch(val query: String) : AddResult()
}

/**
 * Saved playlist for persistence
 */
data class SavedPlaylist(
    val name: String,
    val songIds: List<Long>,
    val createdAt: Long
)
