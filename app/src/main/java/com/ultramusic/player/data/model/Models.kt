package com.ultramusic.player.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Storage source indicator
 */
enum class StorageSource {
    INTERNAL,   // Internal storage
    SD_CARD,    // External SD card
    CLOUD       // Cloud storage (future)
}

/**
 * Song data model
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val duration: Long,         // Duration in milliseconds
    val filePath: String,
    val fileSize: Long,         // Size in bytes
    val mimeType: String,
    val dateAdded: Long,        // Timestamp in milliseconds
    val year: Int,
    val trackNumber: Int,
    val storageSource: StorageSource,
    val uri: Uri
) {
    /**
     * Get formatted duration (MM:SS or HH:MM:SS)
     */
    fun getFormattedDuration(): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = duration / (1000 * 60 * 60)
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Get formatted file size
     */
    fun getFormattedSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
        }
    }
    
    /**
     * Get file extension
     */
    fun getFileExtension(): String {
        return filePath.substringAfterLast('.', "").uppercase()
    }
    
    /**
     * Get storage indicator icon
     */
    fun getStorageIcon(): String {
        return when (storageSource) {
            StorageSource.INTERNAL -> "📱"
            StorageSource.SD_CARD -> "💾"
            StorageSource.CLOUD -> "☁️"
        }
    }
}

/**
 * Album data model
 */
@Entity(tableName = "albums")
data class Album(
    @PrimaryKey
    val id: Long,
    val name: String,
    val artist: String,
    val artistId: Long,
    val songCount: Int,
    val duration: Long,
    val year: Int,
    val albumArtUri: Uri?
) {
    fun getFormattedDuration(): String {
        val minutes = duration / (1000 * 60)
        return "$minutes min"
    }
}

/**
 * Artist data model
 */
@Entity(tableName = "artists")
data class Artist(
    @PrimaryKey
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

/**
 * Playlist data model
 */
@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
    val duration: Long = 0
)

/**
 * Playlist-Song relationship
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSong(
    val playlistId: Long,
    val songId: Long,
    val orderIndex: Int,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Folder structure for browsing
 */
data class MusicFolder(
    val path: String,
    val name: String,
    val songCount: Int,
    val storageSource: StorageSource,
    val subFolders: List<MusicFolder> = emptyList()
)

/**
 * Audio preset for quick settings
 */
data class AudioPreset(
    val id: Long = 0,
    val name: String,
    val speed: Float = 1.0f,
    val pitchSemitones: Float = 0.0f,
    val pitchCents: Float = 0.0f,
    val formantShift: Float = 0.0f,
    val preserveFormants: Boolean = true,
    val isBuiltIn: Boolean = false
) {
    companion object {
        // Built-in presets
        val DEFAULT = AudioPreset(
            id = -1,
            name = "Default",
            isBuiltIn = true
        )
        
        val NIGHTCORE = AudioPreset(
            id = -2,
            name = "Nightcore",
            speed = 1.25f,
            pitchSemitones = 4f,
            isBuiltIn = true
        )
        
        val SLOWED = AudioPreset(
            id = -3,
            name = "Slowed + Reverb",
            speed = 0.8f,
            pitchSemitones = -3f,
            isBuiltIn = true
        )
        
        val VAPORWAVE = AudioPreset(
            id = -4,
            name = "Vaporwave",
            speed = 0.7f,
            pitchSemitones = -5f,
            isBuiltIn = true
        )
        
        val CHIPMUNK = AudioPreset(
            id = -5,
            name = "Chipmunk",
            speed = 1.0f,
            pitchSemitones = 12f,
            preserveFormants = false,
            isBuiltIn = true
        )
        
        val DEEP_VOICE = AudioPreset(
            id = -6,
            name = "Deep Voice",
            speed = 1.0f,
            pitchSemitones = -12f,
            preserveFormants = false,
            isBuiltIn = true
        )
        
        val ULTRA_SLOW = AudioPreset(
            id = -7,
            name = "Ultra Slow",
            speed = 0.1f,
            isBuiltIn = true
        )
        
        val DOUBLE_TIME = AudioPreset(
            id = -8,
            name = "Double Time",
            speed = 2.0f,
            isBuiltIn = true
        )
        
        val PRACTICE_SLOW = AudioPreset(
            id = -9,
            name = "Practice (Slow)",
            speed = 0.5f,
            isBuiltIn = true
        )
        
        val ALL_BUILT_IN = listOf(
            DEFAULT, NIGHTCORE, SLOWED, VAPORWAVE, 
            CHIPMUNK, DEEP_VOICE, ULTRA_SLOW, DOUBLE_TIME, PRACTICE_SLOW
        )
    }
}

/**
 * Loop region for A-B repeat
 */
data class LoopRegion(
    val startMs: Long,
    val endMs: Long,
    val name: String = ""
) {
    val durationMs: Long get() = endMs - startMs
    
    fun contains(positionMs: Long): Boolean {
        return positionMs in startMs..endMs
    }
}

/**
 * Search result item
 */
sealed class SearchResult {
    data class SongResult(val song: Song) : SearchResult()
    data class AlbumResult(val album: Album) : SearchResult()
    data class ArtistResult(val artist: Artist) : SearchResult()
    data class FolderResult(val folder: MusicFolder) : SearchResult()
}
