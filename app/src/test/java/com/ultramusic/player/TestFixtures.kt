package com.ultramusic.player

import android.net.Uri
import com.ultramusic.player.data.Song

/**
 * Test fixtures for UltraMusic Player unit tests
 */
object TestFixtures {

    /**
     * Create a test Song with default values
     */
    fun createSong(
        id: Long = 1L,
        title: String = "Test Song",
        artist: String = "Test Artist",
        album: String = "Test Album",
        duration: Long = 180000L, // 3 minutes
        path: String = "/storage/music/test.mp3",
        dateAdded: Long = System.currentTimeMillis(),
        size: Long = 5_000_000L,
        mimeType: String = "audio/mpeg",
        bitrate: Int = 320,
        sampleRate: Int = 44100
    ): Song {
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            uri = Uri.parse("content://media/external/audio/media/$id"),
            albumArtUri = null,
            path = path,
            dateAdded = dateAdded,
            size = size,
            mimeType = mimeType,
            bitrate = bitrate,
            sampleRate = sampleRate
        )
    }

    /**
     * Sample songs for testing
     */
    val sampleSongs = listOf(
        createSong(
            id = 1,
            title = "Kesariya",
            artist = "Arijit Singh",
            album = "Brahmastra",
            duration = 270000,
            path = "/storage/music/kesariya.mp3"
        ),
        createSong(
            id = 2,
            title = "Tum Hi Ho",
            artist = "Arijit Singh",
            album = "Aashiqui 2",
            duration = 260000,
            path = "/storage/music/tum_hi_ho.mp3"
        ),
        createSong(
            id = 3,
            title = "Shape of You",
            artist = "Ed Sheeran",
            album = "Divide",
            duration = 235000,
            path = "/storage/music/shape_of_you.mp3"
        ),
        createSong(
            id = 4,
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            duration = 200000,
            path = "/storage/music/blinding_lights.mp3"
        ),
        createSong(
            id = 5,
            title = "Channa Mereya",
            artist = "Arijit Singh",
            album = "Ae Dil Hai Mushkil",
            duration = 290000,
            path = "/storage/music/channa_mereya.mp3"
        ),
        createSong(
            id = 6,
            title = "Pasoori",
            artist = "Ali Sethi",
            album = "Coke Studio",
            duration = 240000,
            path = "/storage/music/pasoori.mp3"
        ),
        createSong(
            id = 7,
            title = "Excuses",
            artist = "AP Dhillon",
            album = "Hidden Gems",
            duration = 180000,
            path = "/storage/music/excuses.mp3"
        ),
        createSong(
            id = 8,
            title = "Raataan Lambiyan",
            artist = "Jubin Nautiyal",
            album = "Shershaah",
            duration = 250000,
            path = "/storage/music/raataan_lambiyan.mp3"
        ),
        createSong(
            id = 9,
            title = "Believer",
            artist = "Imagine Dragons",
            album = "Evolve",
            duration = 204000,
            path = "/storage/music/believer.mp3"
        ),
        createSong(
            id = 10,
            title = "Despacito",
            artist = "Luis Fonsi",
            album = "Vida",
            duration = 282000,
            path = "/storage/music/despacito.mp3"
        )
    )

    /**
     * Songs with various audio formats for format testing
     */
    val formatTestSongs = listOf(
        createSong(id = 101, title = "MP3 Song", mimeType = "audio/mpeg", path = "/music/song.mp3", bitrate = 320),
        createSong(id = 102, title = "FLAC Song", mimeType = "audio/flac", path = "/music/song.flac", bitrate = 1411),
        createSong(id = 103, title = "AAC Song", mimeType = "audio/aac", path = "/music/song.m4a", bitrate = 256),
        createSong(id = 104, title = "WAV Song", mimeType = "audio/wav", path = "/music/song.wav", bitrate = 1411),
        createSong(id = 105, title = "OGG Song", mimeType = "audio/ogg", path = "/music/song.ogg", bitrate = 192),
        createSong(id = 106, title = "Unknown", mimeType = "", path = "/music/song.xyz", bitrate = 0)
    )

    /**
     * Songs with Bengali titles for transliteration testing
     */
    val bengaliSongs = listOf(
        createSong(id = 201, title = "Tomake Chai", artist = "Arijit Singh", album = "Gangster"),
        createSong(id = 202, title = "Mon Majhi Re", artist = "Arijit Singh", album = "Boss"),
        createSong(id = 203, title = "Bolte Bolte Cholte Cholte", artist = "Imran Mahmudul"),
        createSong(id = 204, title = "Tumi Ashbe Bole", artist = "Nachiketa"),
        createSong(id = 205, title = "Keno Emon Hoy", artist = "Ash King")
    )

    /**
     * Songs with typos/variations for fuzzy search testing
     */
    val typoVariationSongs = listOf(
        createSong(id = 301, title = "Kesaria", artist = "Arijit Singh"),  // typo
        createSong(id = 302, title = "Tum He Ho", artist = "Arijit Singh"),  // typo
        createSong(id = 303, title = "Chana Mereya", artist = "Arijit Singh"),  // typo
        createSong(id = 304, title = "Pasuri", artist = "Ali Sethi"),  // typo
        createSong(id = 305, title = "Ratan Lambiyan", artist = "Jubin Nautiyal")  // typo
    )
}
