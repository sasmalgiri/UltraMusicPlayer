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
    val size: Long = 0,
    val mimeType: String = "",
    val bitrate: Int = 0,
    val sampleRate: Int = 0
) {
    val durationFormatted: String
        get() {
            val minutes = (duration / 1000) / 60
            val seconds = (duration / 1000) % 60
            return "%d:%02d".format(minutes, seconds)
        }

    /**
     * Audio format derived from MIME type or file extension
     */
    val format: AudioFormat
        get() = AudioFormat.fromMimeType(mimeType, path)

    /**
     * Human-readable format string (e.g., "MP3 320kbps")
     */
    val formatInfo: String
        get() {
            val formatName = format.displayName
            return when {
                bitrate > 0 -> "$formatName ${bitrate}kbps"
                else -> formatName
            }
        }

    /**
     * File size formatted (e.g., "3.5 MB")
     */
    val sizeFormatted: String
        get() {
            return when {
                size >= 1_073_741_824 -> "%.1f GB".format(size / 1_073_741_824.0)
                size >= 1_048_576 -> "%.1f MB".format(size / 1_048_576.0)
                size >= 1024 -> "%.1f KB".format(size / 1024.0)
                else -> "$size B"
            }
        }

    /**
     * Quality indicator based on format and bitrate
     */
    val qualityTier: QualityTier
        get() = when {
            format.isLossless -> QualityTier.LOSSLESS
            format == AudioFormat.AAC && bitrate >= 256 -> QualityTier.HIGH
            format == AudioFormat.MP3 && bitrate >= 320 -> QualityTier.HIGH
            bitrate >= 192 -> QualityTier.GOOD
            bitrate > 0 -> QualityTier.STANDARD
            format.isLossless -> QualityTier.LOSSLESS
            else -> QualityTier.UNKNOWN
        }
}

/**
 * Supported audio formats with metadata
 */
enum class AudioFormat(
    val displayName: String,
    val extensions: List<String>,
    val mimeTypes: List<String>,
    val isLossless: Boolean = false
) {
    MP3("MP3", listOf("mp3"), listOf("audio/mpeg", "audio/mp3")),
    AAC("AAC", listOf("aac", "m4a"), listOf("audio/aac", "audio/mp4", "audio/x-m4a")),
    FLAC("FLAC", listOf("flac"), listOf("audio/flac", "audio/x-flac"), isLossless = true),
    WAV("WAV", listOf("wav"), listOf("audio/wav", "audio/x-wav", "audio/vnd.wave"), isLossless = true),
    OGG("OGG", listOf("ogg", "oga"), listOf("audio/ogg", "application/ogg")),
    OPUS("Opus", listOf("opus"), listOf("audio/opus")),
    ALAC("ALAC", listOf("m4a"), listOf("audio/x-alac"), isLossless = true),
    AIFF("AIFF", listOf("aiff", "aif"), listOf("audio/aiff", "audio/x-aiff"), isLossless = true),
    WMA("WMA", listOf("wma"), listOf("audio/x-ms-wma")),
    AMR("AMR", listOf("amr"), listOf("audio/amr")),
    MIDI("MIDI", listOf("mid", "midi"), listOf("audio/midi", "audio/x-midi")),
    WEBM("WebM", listOf("webm"), listOf("audio/webm")),
    MKA("MKA", listOf("mka"), listOf("audio/x-matroska")),
    APE("APE", listOf("ape"), listOf("audio/ape", "audio/x-ape"), isLossless = true),
    DSD("DSD", listOf("dsf", "dff"), listOf("audio/dsd", "audio/x-dsd"), isLossless = true),
    UNKNOWN("Audio", emptyList(), emptyList());

    companion object {
        /**
         * All supported audio extensions for file filtering
         */
        val SUPPORTED_EXTENSIONS = entries.flatMap { it.extensions }.distinct()

        /**
         * All supported MIME types
         */
        val SUPPORTED_MIME_TYPES = entries.flatMap { it.mimeTypes }.distinct()

        fun fromMimeType(mimeType: String, path: String = ""): AudioFormat {
            // Try MIME type first
            val byMime = entries.find { format ->
                format.mimeTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
            if (byMime != null && byMime != UNKNOWN) return byMime

            // Fallback to extension
            val extension = path.substringAfterLast('.', "").lowercase()
            return entries.find { format ->
                format.extensions.contains(extension)
            } ?: UNKNOWN
        }

        fun fromExtension(extension: String): AudioFormat {
            val ext = extension.lowercase().removePrefix(".")
            return entries.find { format ->
                format.extensions.contains(ext)
            } ?: UNKNOWN
        }
    }
}

/**
 * Audio quality tier for display
 */
enum class QualityTier(val displayName: String, val emoji: String) {
    LOSSLESS("Lossless", "🎯"),
    HIGH("High Quality", "⭐"),
    GOOD("Good", "👍"),
    STANDARD("Standard", "📻"),
    UNKNOWN("Unknown", "❓")
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
    DURATION,
    FORMAT,
    SIZE
}

/**
 * Storage type for browsing
 */
enum class StorageType {
    ALL,
    INTERNAL,
    SD_CARD
}
