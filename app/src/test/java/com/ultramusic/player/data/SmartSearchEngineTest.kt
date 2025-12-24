package com.ultramusic.player.data

import com.google.common.truth.Truth.assertThat
import com.ultramusic.player.TestFixtures
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SmartSearchEngine
 *
 * Tests fuzzy matching, phonetic search, Bengali transliteration,
 * and other search features.
 */
class SmartSearchEngineTest {

    private lateinit var searchEngine: SmartSearchEngine

    @Before
    fun setup() {
        searchEngine = SmartSearchEngine()
    }

    // ==================== EXACT MATCH TESTS ====================

    @Test
    fun `searchSongs returns exact match with highest score`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Kesariya")

        assertThat(results).isNotEmpty()
        assertThat(results.first().song.title).isEqualTo("Kesariya")
        assertThat(results.first().matchType).isEqualTo(MatchType.EXACT)
    }

    @Test
    fun `searchSongs is case insensitive`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "KESARIYA")

        assertThat(results).isNotEmpty()
        assertThat(results.first().song.title).isEqualTo("Kesariya")
    }

    @Test
    fun `searchSongs matches artist name`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Arijit Singh")

        assertThat(results).isNotEmpty()
        // Should return all songs by Arijit Singh
        assertThat(results.map { it.song.artist }).contains("Arijit Singh")
    }

    @Test
    fun `searchSongs matches album name`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Brahmastra")

        assertThat(results).isNotEmpty()
        assertThat(results.first().song.album).isEqualTo("Brahmastra")
    }

    // ==================== EMPTY/BLANK QUERY TESTS ====================

    @Test
    fun `searchSongs returns empty list for empty query`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "")

        assertThat(results).isEmpty()
    }

    @Test
    fun `searchSongs returns empty list for blank query`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "   ")

        assertThat(results).isEmpty()
    }

    @Test
    fun `searchSongs returns empty list when no songs provided`() {
        val results = searchEngine.searchSongs(emptyList(), "test")

        assertThat(results).isEmpty()
    }

    // ==================== FUZZY MATCHING TESTS ====================

    @Test
    fun `searchSongs handles typos with fuzzy matching`() {
        val songs = TestFixtures.sampleSongs
        // "Kesaria" is a common typo for "Kesariya"
        val results = searchEngine.searchSongs(songs, "Kesaria")

        assertThat(results).isNotEmpty()
        // Should still find the correct song
        assertThat(results.any { it.song.title == "Kesariya" }).isTrue()
    }

    @Test
    fun `searchSongs handles missing characters`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Tum Hi H")

        assertThat(results).isNotEmpty()
        assertThat(results.any { it.song.title.contains("Tum Hi Ho") }).isTrue()
    }

    @Test
    fun `searchSongs handles extra characters`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Kesariyaa")

        assertThat(results).isNotEmpty()
        assertThat(results.any { it.song.title == "Kesariya" }).isTrue()
    }

    // ==================== PARTIAL MATCHING TESTS ====================

    @Test
    fun `searchSongs matches partial title`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Shape")

        assertThat(results).isNotEmpty()
        assertThat(results.first().song.title).contains("Shape")
    }

    @Test
    fun `searchSongs matches word starts`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Blind")

        assertThat(results).isNotEmpty()
        assertThat(results.any { it.song.title.startsWith("Blind") }).isTrue()
    }

    @Test
    fun `searchSongs matches middle words`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Hi Ho")

        assertThat(results).isNotEmpty()
        assertThat(results.any { it.song.title.contains("Hi Ho") }).isTrue()
    }

    // ==================== SPECIAL CHARACTERS TESTS ====================

    @Test
    fun `searchSongs handles special characters`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Shape!@#of$%^You")

        // Should still find results after normalizing
        assertThat(results).isNotEmpty()
    }

    @Test
    fun `searchSongs handles numbers in query`() {
        val songs = listOf(
            TestFixtures.createSong(id = 1, title = "Song 2020", artist = "Artist")
        )
        val results = searchEngine.searchSongs(songs, "2020")

        assertThat(results).isNotEmpty()
    }

    // ==================== SCORING TESTS ====================

    @Test
    fun `searchSongs returns results sorted by relevance`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Arijit")

        // Results should be sorted by score (descending)
        for (i in 0 until results.size - 1) {
            assertThat(results[i].score).isAtLeast(results[i + 1].score)
        }
    }

    @Test
    fun `exact title match scores higher than partial match`() {
        val songs = listOf(
            TestFixtures.createSong(id = 1, title = "Test", artist = "Artist"),
            TestFixtures.createSong(id = 2, title = "Testing Song", artist = "Artist"),
            TestFixtures.createSong(id = 3, title = "A Test Song", artist = "Artist")
        )
        val results = searchEngine.searchSongs(songs, "Test")

        assertThat(results).isNotEmpty()
        // Exact match "Test" should be first
        assertThat(results.first().song.title).isEqualTo("Test")
    }

    @Test
    fun `title match scores higher than artist match`() {
        val songs = listOf(
            TestFixtures.createSong(id = 1, title = "Something Else", artist = "Kesariya Artist"),
            TestFixtures.createSong(id = 2, title = "Kesariya", artist = "Different Artist")
        )
        val results = searchEngine.searchSongs(songs, "Kesariya")

        assertThat(results).isNotEmpty()
        // Title match should score higher
        assertThat(results.first().song.title).isEqualTo("Kesariya")
    }

    // ==================== MULTI-WORD QUERY TESTS ====================

    @Test
    fun `searchSongs handles multi-word queries`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Tum Hi Ho Arijit")

        assertThat(results).isNotEmpty()
        assertThat(results.any {
            it.song.title.contains("Tum Hi Ho") && it.song.artist.contains("Arijit")
        }).isTrue()
    }

    @Test
    fun `searchSongs matches words in any order`() {
        val songs = TestFixtures.sampleSongs
        val results = searchEngine.searchSongs(songs, "Singh Arijit")

        assertThat(results).isNotEmpty()
        assertThat(results.any { it.song.artist == "Arijit Singh" }).isTrue()
    }

    // ==================== SUGGESTIONS TESTS ====================

    @Test
    fun `getSuggestions returns matching titles`() {
        val songs = TestFixtures.sampleSongs
        val suggestions = searchEngine.getSuggestions(songs, "Kes", 5)

        assertThat(suggestions).isNotEmpty()
        assertThat(suggestions.any { it.lowercase().startsWith("kes") }).isTrue()
    }

    @Test
    fun `getSuggestions returns empty for very short query`() {
        val songs = TestFixtures.sampleSongs
        val suggestions = searchEngine.getSuggestions(songs, "K", 5)

        // Minimum 2 characters required
        assertThat(suggestions).isEmpty()
    }

    @Test
    fun `getSuggestions respects limit`() {
        val songs = TestFixtures.sampleSongs
        val suggestions = searchEngine.getSuggestions(songs, "Ar", 3)

        assertThat(suggestions.size).isAtMost(3)
    }

    @Test
    fun `getSuggestions includes artist matches`() {
        val songs = TestFixtures.sampleSongs
        val suggestions = searchEngine.getSuggestions(songs, "Ari", 10)

        // Should include "Arijit Singh" as a suggestion
        assertThat(suggestions.any { it.contains("Arijit") }).isTrue()
    }

    // ==================== MATCH TYPE TESTS ====================

    @Test
    fun `searchSongs categorizes match types correctly`() {
        val songs = TestFixtures.sampleSongs

        // Exact match
        val exactResults = searchEngine.searchSongs(songs, "Kesariya")
        assertThat(exactResults.first().matchType).isIn(listOf(MatchType.EXACT, MatchType.STRONG))

        // Partial match
        val partialResults = searchEngine.searchSongs(songs, "Kes")
        if (partialResults.isNotEmpty()) {
            assertThat(partialResults.first().matchType).isNotNull()
        }
    }

    // ==================== COMMON VARIATIONS TESTS ====================

    @Test
    fun `searchSongs matches common song variations`() {
        val songs = TestFixtures.sampleSongs

        // "Pasoori" has variations like "pasuri", "pasori"
        val results = searchEngine.searchSongs(songs, "pasuri")

        assertThat(results).isNotEmpty()
        assertThat(results.any { it.song.title.lowercase().contains("pasoori") }).isTrue()
    }

    // ==================== BENGALI TRANSLITERATION TESTS ====================

    @Test
    fun `searchSongs handles Bengali transliterations`() {
        val songs = TestFixtures.bengaliSongs

        // Search with common romanization
        val results = searchEngine.searchSongs(songs, "mon")

        assertThat(results).isNotEmpty()
        assertThat(results.any { it.song.title.lowercase().contains("mon") }).isTrue()
    }

    // ==================== PHONETIC MATCHING TESTS ====================

    @Test
    fun `searchSongs handles phonetic variations`() {
        val songs = TestFixtures.sampleSongs

        // "Shape" sounds like "Shap"
        val results = searchEngine.searchSongs(songs, "shap")

        assertThat(results).isNotEmpty()
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `searchSongs handles very long query`() {
        val songs = TestFixtures.sampleSongs
        val longQuery = "This is a very long search query that probably does not match anything"

        val results = searchEngine.searchSongs(songs, longQuery)
        // Should not crash, may return empty or weak matches
        assertThat(results).isNotNull()
    }

    @Test
    fun `searchSongs handles unicode characters`() {
        val songs = listOf(
            TestFixtures.createSong(id = 1, title = "Song with \u2764", artist = "Artist")
        )
        val results = searchEngine.searchSongs(songs, "Song")

        assertThat(results).isNotEmpty()
    }
}
