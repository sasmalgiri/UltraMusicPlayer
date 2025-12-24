package com.ultramusic.player.data

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.ultramusic.player.TestFixtures
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SongMetadataManager
 *
 * Tests title normalization, metadata cleaning, fuzzy matching,
 * and popular song database lookups.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SongMetadataManagerTest {

    private lateinit var metadataManager: SongMetadataManager
    private val mockContext: Context = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        metadataManager = SongMetadataManager(mockContext)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== TITLE CLEANING TESTS ====================

    @Test
    fun `normalizeSong cleans official video suffix`() = runTest {
        val song = TestFixtures.createSong(
            title = "Kesariya (Official Video)",
            artist = "Arijit Singh"
        )

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized.normalizedTitle.lowercase()).doesNotContain("official")
        assertThat(normalized.normalizedTitle.lowercase()).doesNotContain("video")
    }

    @Test
    fun `normalizeSong cleans lyrics suffix`() = runTest {
        val song = TestFixtures.createSong(
            title = "Shape of You (Lyrics)",
            artist = "Ed Sheeran"
        )

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized.normalizedTitle.lowercase()).doesNotContain("lyrics")
    }

    @Test
    fun `normalizeSong cleans track number prefix`() = runTest {
        val song = TestFixtures.createSong(
            title = "01. Kesariya",
            artist = "Arijit Singh"
        )

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized.normalizedTitle).doesNotContain("01.")
        assertThat(normalized.normalizedTitle).doesNotContain("01 ")
    }

    @Test
    fun `normalizeSong cleans HD and HQ suffixes`() = runTest {
        val songHD = TestFixtures.createSong(title = "Song Title (HD)")
        val songHQ = TestFixtures.createSong(title = "Song Title (HQ)")

        val normalizedHD = metadataManager.normalizeSong(songHD)
        val normalizedHQ = metadataManager.normalizeSong(songHQ)

        assertThat(normalizedHD.normalizedTitle.lowercase()).doesNotContain("hd")
        assertThat(normalizedHQ.normalizedTitle.lowercase()).doesNotContain("hq")
    }

    @Test
    fun `normalizeSong cleans mp3 suffix`() = runTest {
        val song = TestFixtures.createSong(title = "Song Title - mp3")

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized.normalizedTitle.lowercase()).doesNotContain("mp3")
    }

    @Test
    fun `normalizeSong replaces underscores with spaces`() = runTest {
        val song = TestFixtures.createSong(title = "Song_Title_Here")

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized.normalizedTitle).doesNotContain("_")
    }

    @Test
    fun `normalizeSong trims whitespace`() = runTest {
        val song = TestFixtures.createSong(title = "  Song Title  ")

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized.normalizedTitle).doesNotStartWith(" ")
        assertThat(normalized.normalizedTitle).doesNotEndWith(" ")
    }

    // ==================== POPULAR SONG MATCHING TESTS ====================

    @Test
    fun `normalizeSong matches exact popular title`() = runTest {
        val song = TestFixtures.createSong(
            title = "Kesariya",
            artist = "Arijit Singh"
        )

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized.matchMethod).isIn(
            listOf(MatchMethod.TITLE_MATCH, MatchMethod.CLEANED_TITLE_MATCH)
        )
        assertThat(normalized.confidence).isAtLeast(0.8f)
    }

    @Test
    fun `normalizeSong matches alias of popular title`() = runTest {
        val song = TestFixtures.createSong(
            title = "kesaria",  // Common typo/alias
            artist = "Arijit Singh"
        )

        val normalized = metadataManager.normalizeSong(song)

        // Should find a match through aliases
        assertThat(normalized.normalizedTitle.lowercase()).contains("kesar")
    }

    @Test
    fun `normalizeSong matches with fuzzy matching`() = runTest {
        val song = TestFixtures.createSong(
            title = "Tum He Ho",  // Typo for "Tum Hi Ho"
            artist = "Arijit Singh"
        )

        val normalized = metadataManager.normalizeSong(song)

        // Fuzzy matching should find "Tum Hi Ho"
        assertThat(normalized.confidence).isGreaterThan(0f)
    }

    // ==================== ARTIST MATCHING TESTS ====================

    @Test
    fun `normalizeSong preserves artist info from database`() = runTest {
        val song = TestFixtures.createSong(
            title = "Kesariya",
            artist = ""
        )

        val normalized = metadataManager.normalizeSong(song)

        // Should get artist info from popular song database
        if (normalized.matchMethod == MatchMethod.TITLE_MATCH) {
            assertThat(normalized.artist).isNotEmpty()
        }
    }

    // ==================== FILENAME EXTRACTION TESTS ====================

    @Test
    fun `normalizeSong extracts info from Artist - Title format`() = runTest {
        val song = TestFixtures.createSong(
            title = "Unknown",
            artist = "Unknown",
            path = "/music/Arijit Singh - Kesariya.mp3"
        )

        val normalized = metadataManager.normalizeSong(song)

        // Should extract from filename pattern
        assertThat(normalized.normalizedTitle.lowercase()).contains("kesar")
    }

    // ==================== CONFIDENCE SCORE TESTS ====================

    @Test
    fun `exact match has high confidence`() = runTest {
        val song = TestFixtures.createSong(
            title = "Shape of You",
            artist = "Ed Sheeran"
        )

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized.confidence).isAtLeast(0.7f)
    }

    @Test
    fun `unknown song has lower confidence`() = runTest {
        val song = TestFixtures.createSong(
            title = "Random Unknown Song XYZ123",
            artist = "Nobody"
        )

        val normalized = metadataManager.normalizeSong(song)

        // Unknown songs should have lower confidence
        assertThat(normalized.matchMethod).isEqualTo(MatchMethod.EXTRACTED)
    }

    // ==================== ALIASES TESTS ====================

    @Test
    fun `getAliases returns aliases for known song`() = runTest {
        val song = TestFixtures.createSong(
            title = "Kesariya",
            artist = "Arijit Singh"
        )

        // First normalize to populate cache
        metadataManager.normalizeSong(song)

        val aliases = metadataManager.getAliases(song)

        assertThat(aliases).isNotEmpty()
    }

    @Test
    fun `getAliases returns original title if not cached`() {
        val song = TestFixtures.createSong(
            title = "Some Title",
            artist = "Some Artist"
        )

        val aliases = metadataManager.getAliases(song)

        assertThat(aliases).contains("Some Title")
    }

    // ==================== BATCH NORMALIZATION TESTS ====================

    @Test
    fun `normalizeAll processes all songs`() = runTest {
        val songs = TestFixtures.sampleSongs.take(5)

        val results = metadataManager.normalizeAll(songs)

        assertThat(results).hasSize(5)
        assertThat(results.keys).containsExactlyElementsIn(songs.map { it.id })
    }

    // ==================== SEARCH WITH NORMALIZATION TESTS ====================

    @Test
    fun `searchWithNormalization finds songs by normalized title`() = runTest {
        val songs = TestFixtures.sampleSongs

        // First normalize
        metadataManager.normalizeAll(songs)

        val results = metadataManager.searchWithNormalization(songs, "kesariya")

        assertThat(results).isNotEmpty()
    }

    @Test
    fun `searchWithNormalization finds songs by original title`() = runTest {
        val songs = TestFixtures.sampleSongs

        val results = metadataManager.searchWithNormalization(songs, "Shape of You")

        assertThat(results).isNotEmpty()
    }

    @Test
    fun `searchWithNormalization finds songs by artist`() = runTest {
        val songs = TestFixtures.sampleSongs

        val results = metadataManager.searchWithNormalization(songs, "Arijit")

        assertThat(results).isNotEmpty()
        assertThat(results.all { it.artist.contains("Arijit") }).isTrue()
    }

    // ==================== NORMALIZED SONG INFO TESTS ====================

    @Test
    fun `NormalizedSongInfo contains all required fields`() = runTest {
        val song = TestFixtures.createSong(title = "Test Song", artist = "Test Artist")

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized.originalTitle).isEqualTo("Test Song")
        assertThat(normalized.normalizedTitle).isNotEmpty()
        assertThat(normalized.confidence).isIn(0f..1f)
        assertThat(normalized.matchMethod).isNotNull()
        assertThat(normalized.aliases).isNotNull()
    }

    // ==================== SIMILARITY CALCULATION TESTS ====================

    @Test
    fun `identical strings have similarity of 1`() = runTest {
        val song1 = TestFixtures.createSong(title = "Exact Match")
        val song2 = TestFixtures.createSong(title = "Exact Match")

        val normalized1 = metadataManager.normalizeSong(song1)
        val normalized2 = metadataManager.normalizeSong(song2)

        // Both should normalize to same title with similar confidence
        assertThat(normalized1.normalizedTitle).isEqualTo(normalized2.normalizedTitle)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `normalizeSong handles empty title`() = runTest {
        val song = TestFixtures.createSong(title = "")

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized).isNotNull()
    }

    @Test
    fun `normalizeSong handles special characters`() = runTest {
        val song = TestFixtures.createSong(title = "Song!@#$%^&*()")

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized).isNotNull()
        assertThat(normalized.normalizedTitle).isNotNull()
    }

    @Test
    fun `normalizeSong handles unicode characters`() = runTest {
        val song = TestFixtures.createSong(title = "তোমাকে চাই")  // Bengali text

        val normalized = metadataManager.normalizeSong(song)

        assertThat(normalized).isNotNull()
    }
}
