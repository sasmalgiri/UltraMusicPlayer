package com.ultramusic.player.data

import com.google.common.truth.Truth.assertThat
import com.ultramusic.player.TestFixtures
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SmartPlaylistManager
 *
 * Tests playlist operations: add, remove, move, shuffle, loop, navigation
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class SmartPlaylistManagerTest {

    private lateinit var playlistManager: SmartPlaylistManager
    private lateinit var mockSearchEngine: SmartSearchEngine
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockSearchEngine = mockk(relaxed = true)
        playlistManager = SmartPlaylistManager(mockSearchEngine)
        playlistManager.initialize(TestFixtures.sampleSongs)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== ADD SONG TESTS ====================

    @Test
    fun `addSong adds song to end of playlist`() {
        val song = TestFixtures.sampleSongs[0]

        playlistManager.addSong(song)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue).contains(song)
        assertThat(playlist.queue.last()).isEqualTo(song)
    }

    @Test
    fun `addSong at specific position inserts correctly`() {
        val song1 = TestFixtures.sampleSongs[0]
        val song2 = TestFixtures.sampleSongs[1]
        val song3 = TestFixtures.sampleSongs[2]

        playlistManager.addSong(song1)
        playlistManager.addSong(song3)
        playlistManager.addSong(song2, position = 1)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue[0]).isEqualTo(song1)
        assertThat(playlist.queue[1]).isEqualTo(song2)
        assertThat(playlist.queue[2]).isEqualTo(song3)
    }

    @Test
    fun `addToPlayNext inserts after current index`() {
        val song1 = TestFixtures.sampleSongs[0]
        val song2 = TestFixtures.sampleSongs[1]
        val song3 = TestFixtures.sampleSongs[2]

        playlistManager.addSong(song1)
        playlistManager.addSong(song2)
        playlistManager.setCurrentIndex(0)
        playlistManager.addToPlayNext(song3)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue[1]).isEqualTo(song3)
    }

    @Test
    fun `addSongs adds multiple songs at once`() {
        val songs = TestFixtures.sampleSongs.take(3)

        playlistManager.addSongs(songs)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue).containsExactlyElementsIn(songs).inOrder()
    }

    // ==================== REMOVE SONG TESTS ====================

    @Test
    fun `removeSong removes song at index`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)

        playlistManager.removeSong(1)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue).hasSize(2)
        assertThat(playlist.queue).doesNotContain(songs[1])
    }

    @Test
    fun `removeSong adjusts currentIndex when removing before current`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(2)

        playlistManager.removeSong(0)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.currentIndex).isEqualTo(1)
    }

    @Test
    fun `removeSong does nothing for invalid index`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)

        playlistManager.removeSong(10)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue).hasSize(3)
    }

    @Test
    fun `removeSongById removes correct song`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)

        playlistManager.removeSongById(songs[1].id)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue.map { it.id }).doesNotContain(songs[1].id)
    }

    // ==================== MOVE SONG TESTS ====================

    @Test
    fun `moveSong moves song to new position`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)

        playlistManager.moveSong(0, 2)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue[2]).isEqualTo(songs[0])
    }

    @Test
    fun `moveSong updates currentIndex when moving current song`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(0)

        playlistManager.moveSong(0, 2)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.currentIndex).isEqualTo(2)
    }

    @Test
    fun `moveSong does nothing for invalid indices`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)

        playlistManager.moveSong(-1, 2)
        playlistManager.moveSong(0, 10)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue).containsExactlyElementsIn(songs).inOrder()
    }

    // ==================== CLEAR PLAYLIST TESTS ====================

    @Test
    fun `clearPlaylist removes all songs`() {
        playlistManager.addSongs(TestFixtures.sampleSongs)

        playlistManager.clearPlaylist()

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue).isEmpty()
        assertThat(playlist.currentIndex).isEqualTo(-1)
    }

    // ==================== NAVIGATION TESTS ====================

    @Test
    fun `setCurrentIndex updates current index`() {
        val songs = TestFixtures.sampleSongs.take(5)
        playlistManager.addSongs(songs)

        playlistManager.setCurrentIndex(3)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.currentIndex).isEqualTo(3)
    }

    @Test
    fun `setCurrentIndex ignores invalid index`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(1)

        playlistManager.setCurrentIndex(10)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.currentIndex).isEqualTo(1)
    }

    @Test
    fun `moveToNext advances to next song`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(0)

        val nextSong = playlistManager.moveToNext()

        assertThat(nextSong).isEqualTo(songs[1])
        assertThat(playlistManager.activePlaylist.value.currentIndex).isEqualTo(1)
    }

    @Test
    fun `moveToNext returns null at end without loop`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(2)

        val nextSong = playlistManager.moveToNext()

        assertThat(nextSong).isNull()
    }

    @Test
    fun `moveToNext loops when looping enabled`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(2)
        playlistManager.toggleLoop()

        val nextSong = playlistManager.moveToNext()

        assertThat(nextSong).isEqualTo(songs[0])
        assertThat(playlistManager.activePlaylist.value.currentIndex).isEqualTo(0)
    }

    @Test
    fun `moveToPrevious goes to previous song`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(2)

        val prevSong = playlistManager.moveToPrevious()

        assertThat(prevSong).isEqualTo(songs[1])
        assertThat(playlistManager.activePlaylist.value.currentIndex).isEqualTo(1)
    }

    @Test
    fun `moveToPrevious returns null at start without loop`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(0)

        val prevSong = playlistManager.moveToPrevious()

        assertThat(prevSong).isNull()
    }

    @Test
    fun `moveToPrevious loops when looping enabled`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(0)
        playlistManager.toggleLoop()

        val prevSong = playlistManager.moveToPrevious()

        assertThat(prevSong).isEqualTo(songs[2])
    }

    // ==================== SHUFFLE TESTS ====================

    @Test
    fun `shuffleRemaining shuffles songs after current`() {
        val songs = TestFixtures.sampleSongs.take(5)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(1)

        val before = playlistManager.activePlaylist.value.queue.toList()
        playlistManager.shuffleRemaining()
        val after = playlistManager.activePlaylist.value.queue

        // First two songs (played + current) should be unchanged
        assertThat(after[0]).isEqualTo(before[0])
        assertThat(after[1]).isEqualTo(before[1])

        // Remaining songs should be shuffled (may or may not be same order)
        assertThat(after.drop(2)).containsExactlyElementsIn(before.drop(2))
    }

    @Test
    fun `shuffleRemaining does nothing with less than 2 remaining songs`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(2)

        val before = playlistManager.activePlaylist.value.queue.toList()
        playlistManager.shuffleRemaining()
        val after = playlistManager.activePlaylist.value.queue

        assertThat(after).containsExactlyElementsIn(before).inOrder()
    }

    // ==================== LOOP TESTS ====================

    @Test
    fun `toggleLoop toggles looping state`() {
        assertThat(playlistManager.activePlaylist.value.isLooping).isFalse()

        playlistManager.toggleLoop()
        assertThat(playlistManager.activePlaylist.value.isLooping).isTrue()

        playlistManager.toggleLoop()
        assertThat(playlistManager.activePlaylist.value.isLooping).isFalse()
    }

    // ==================== GETTER TESTS ====================

    @Test
    fun `getCurrentSong returns current song`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(1)

        val currentSong = playlistManager.getCurrentSong()

        assertThat(currentSong).isEqualTo(songs[1])
    }

    @Test
    fun `getCurrentSong returns null when no current song`() {
        val currentSong = playlistManager.getCurrentSong()

        assertThat(currentSong).isNull()
    }

    @Test
    fun `getNextSong returns next song`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(0)

        val nextSong = playlistManager.getNextSong()

        assertThat(nextSong).isEqualTo(songs[1])
    }

    @Test
    fun `getUpcoming returns upcoming songs`() {
        val songs = TestFixtures.sampleSongs.take(5)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(1)

        val upcoming = playlistManager.getUpcoming(3)

        assertThat(upcoming).hasSize(3)
        assertThat(upcoming).containsExactly(songs[2], songs[3], songs[4]).inOrder()
    }

    @Test
    fun `getRemainingCount returns correct count`() {
        val songs = TestFixtures.sampleSongs.take(5)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(2)

        val remaining = playlistManager.getRemainingCount()

        assertThat(remaining).isEqualTo(2)
    }

    @Test
    fun `getTotalDuration returns sum of all durations`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)

        val totalDuration = playlistManager.getTotalDuration()

        assertThat(totalDuration).isEqualTo(songs.sumOf { it.duration })
    }

    // ==================== ACTIVE PLAYLIST DATA CLASS TESTS ====================

    @Test
    fun `ActivePlaylist isEmpty returns true when empty`() {
        val playlist = ActivePlaylist()

        assertThat(playlist.isEmpty).isTrue()
    }

    @Test
    fun `ActivePlaylist size returns correct count`() {
        val songs = TestFixtures.sampleSongs.take(5)
        playlistManager.addSongs(songs)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.size).isEqualTo(5)
    }

    @Test
    fun `ActivePlaylist hasNext returns correct value`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(1)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.hasNext).isTrue()

        playlistManager.setCurrentIndex(2)
        val playlistAtEnd = playlistManager.activePlaylist.value
        assertThat(playlistAtEnd.hasNext).isFalse()
    }

    @Test
    fun `ActivePlaylist hasPrevious returns correct value`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)
        playlistManager.setCurrentIndex(0)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.hasPrevious).isFalse()

        playlistManager.setCurrentIndex(1)
        val playlistAtMiddle = playlistManager.activePlaylist.value
        assertThat(playlistAtMiddle.hasPrevious).isTrue()
    }

    // ==================== SAVE/LOAD TESTS ====================

    @Test
    fun `saveAsPlaylist creates SavedPlaylist with correct data`() {
        val songs = TestFixtures.sampleSongs.take(3)
        playlistManager.addSongs(songs)

        val savedPlaylist = playlistManager.saveAsPlaylist("My Playlist")

        assertThat(savedPlaylist.name).isEqualTo("My Playlist")
        assertThat(savedPlaylist.songIds).containsExactlyElementsIn(songs.map { it.id })
    }

    @Test
    fun `loadPlaylist restores playlist correctly`() {
        val songs = TestFixtures.sampleSongs.take(3)
        val savedPlaylist = SavedPlaylist(
            name = "Test Playlist",
            songIds = songs.map { it.id },
            createdAt = System.currentTimeMillis()
        )

        playlistManager.loadPlaylist(savedPlaylist)

        val playlist = playlistManager.activePlaylist.value
        assertThat(playlist.queue.map { it.id }).containsExactlyElementsIn(songs.map { it.id })
        assertThat(playlist.currentIndex).isEqualTo(0)
    }
}
