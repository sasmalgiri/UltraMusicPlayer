package com.ultramusic.player.data

import android.net.Uri

/**
 * Represents a song in the music library
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val path: String,
    val dateAdded: Long = 0,
    val size: Long = 0
) {
    val durationFormatted: String
        get() {
            val minutes = (duration / 1000) / 60
            val seconds = (duration / 1000) % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

/**
 * Audio preset for quick speed/pitch settings
 */
data class AudioPreset(
    val name: String,
    val speed: Float,
    val pitch: Float, // in semitones
    val icon: String = "🎵"
) {
    companion object {
        val PRESETS = listOf(
            AudioPreset("Normal", 1.0f, 0f, "🎵"),
            AudioPreset("Nightcore", 1.25f, 4f, "⚡"),
            AudioPreset("Slowed", 0.85f, -2f, "🌙"),
            AudioPreset("Vaporwave", 0.8f, -3f, "🌴"),
            AudioPreset("Chipmunk", 1.0f, 12f, "🐿️"),
            AudioPreset("Deep Voice", 1.0f, -12f, "🎸"),
            AudioPreset("2x Speed", 2.0f, 0f, "⏩"),
            AudioPreset("Half Speed", 0.5f, 0f, "🐢"),
            AudioPreset("Study Mode", 0.9f, 0f, "📚"),
            AudioPreset("Practice Slow", 0.7f, 0f, "🎹"),
            AudioPreset("Dance Mix", 1.15f, 2f, "💃"),
            AudioPreset("Bass Boost", 0.95f, -4f, "🔊"),
            AudioPreset("High Energy", 1.3f, 3f, "🔥"),
            AudioPreset("Chill", 0.9f, -1f, "😌"),
            AudioPreset("Extreme Slow", 0.25f, -6f, "🦥"),
            AudioPreset("Ultra Fast", 3.0f, 0f, "🚀")
        )
    }
}

/**
 * Playback state
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentSong: Song? = null,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val speed: Float = 1.0f,
    val pitch: Float = 0f, // in semitones
    val isLooping: Boolean = false,
    val isShuffling: Boolean = false,
    val abLoopStart: Long? = null,
    val abLoopEnd: Long? = null,
    val repeatMode: Int = 0,  // 0=off, 1=all, 2=one
    val battleModeEnabled: Boolean = false,
    val bassBoostDb: Float = 0f
) {
    // Aliases for compatibility
    val position: Long get() = currentPosition
    val loopStartPosition: Long? get() = abLoopStart
    val loopEndPosition: Long? get() = abLoopEnd
    val isShuffleEnabled: Boolean get() = isShuffling
    
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f
    
    val positionFormatted: String
        get() {
            val minutes = (currentPosition / 1000) / 60
            val seconds = (currentPosition / 1000) % 60
            return "%d:%02d".format(minutes, seconds)
        }
    
    val durationFormatted: String
        get() {
            val minutes = (duration / 1000) / 60
            val seconds = (duration / 1000) % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

/**
 * Sort options for song list
 */
enum class SortOption {
    TITLE,
    ARTIST,
    ALBUM,
    DATE_ADDED,
    DURATION
}

/**
 * Storage type for browsing
 */
enum class StorageType {
    ALL,
    INTERNAL,
    SD_CARD
}
