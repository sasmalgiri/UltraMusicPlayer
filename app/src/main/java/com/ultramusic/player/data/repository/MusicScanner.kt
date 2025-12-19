package com.ultramusic.player.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import com.ultramusic.player.data.model.Song
import com.ultramusic.player.data.model.Album
import com.ultramusic.player.data.model.Artist
import com.ultramusic.player.data.model.StorageSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Music Scanner
 * 
 * Scans for music files on:
 * - Internal storage
 * - External SD card (if present)
 * 
 * Provides detailed information about each track including:
 * - File location (internal/external)
 * - Metadata (title, artist, album, etc.)
 * - Audio properties (duration, bitrate, sample rate)
 */
@Singleton
class MusicScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Get all storage volumes (internal + SD cards)
     */
    fun getStorageVolumes(): List<StorageVolume> {
        val volumes = mutableListOf<StorageVolume>()
        
        // Internal storage
        val internal = Environment.getExternalStorageDirectory()
        volumes.add(
            StorageVolume(
                path = internal.absolutePath,
                name = "Internal Storage",
                source = StorageSource.INTERNAL,
                isRemovable = false,
                totalSpace = internal.totalSpace,
                freeSpace = internal.freeSpace
            )
        )
        
        // External SD cards
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            storageManager.storageVolumes.forEach { volume ->
                if (volume.isRemovable) {
                    volume.directory?.let { dir ->
                        volumes.add(
                            StorageVolume(
                                path = dir.absolutePath,
                                name = volume.getDescription(context) ?: "SD Card",
                                source = StorageSource.SD_CARD,
                                isRemovable = true,
                                totalSpace = dir.totalSpace,
                                freeSpace = dir.freeSpace
                            )
                        )
                    }
                }
            }
        } else {
            // Fallback for older devices
            findExternalSDCards().forEach { path ->
                val dir = File(path)
                volumes.add(
                    StorageVolume(
                        path = path,
                        name = "SD Card",
                        source = StorageSource.SD_CARD,
                        isRemovable = true,
                        totalSpace = dir.totalSpace,
                        freeSpace = dir.freeSpace
                    )
                )
            }
        }
        
        return volumes
    }
    
    /**
     * Scan all music files from all storage volumes
     */
    suspend fun scanAllMusic(): Flow<ScanProgress> = flow {
        emit(ScanProgress.Started)
        
        val songs = mutableListOf<Song>()
        val volumes = getStorageVolumes()
        
        volumes.forEachIndexed { index, volume ->
            emit(ScanProgress.ScanningVolume(volume.name, index + 1, volumes.size))
            
            val volumeSongs = scanMusicFromVolume(volume)
            songs.addAll(volumeSongs)
            
            emit(ScanProgress.VolumeComplete(volume.name, volumeSongs.size))
        }
        
        emit(ScanProgress.Complete(songs))
    }
    
    /**
     * Scan music from a specific storage volume
     */
    suspend fun scanMusicFromVolume(volume: StorageVolume): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
                "${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("${volume.path}%")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val filePath = cursor.getString(dataColumn)
                
                // Determine storage source
                val storageSource = if (filePath.startsWith(volume.path)) {
                    volume.source
                } else {
                    StorageSource.INTERNAL
                }
                
                val song = Song(
                    id = id,
                    title = cursor.getString(titleColumn) ?: "Unknown",
                    artist = cursor.getString(artistColumn) ?: "Unknown Artist",
                    artistId = cursor.getLong(artistIdColumn),
                    album = cursor.getString(albumColumn) ?: "Unknown Album",
                    albumId = cursor.getLong(albumIdColumn),
                    duration = cursor.getLong(durationColumn),
                    filePath = filePath,
                    fileSize = cursor.getLong(sizeColumn),
                    mimeType = cursor.getString(mimeTypeColumn) ?: "audio/*",
                    dateAdded = cursor.getLong(dateAddedColumn) * 1000,
                    year = cursor.getInt(yearColumn),
                    trackNumber = cursor.getInt(trackColumn),
                    storageSource = storageSource,
                    uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                )
                
                songs.add(song)
            }
        }
        
        songs
    }
    
    /**
     * Get albums from songs
     */
    fun getAlbumsFromSongs(songs: List<Song>): List<Album> {
        return songs
            .groupBy { it.albumId }
            .map { (albumId, albumSongs) ->
                val firstSong = albumSongs.first()
                Album(
                    id = albumId,
                    name = firstSong.album,
                    artist = firstSong.artist,
                    artistId = firstSong.artistId,
                    songCount = albumSongs.size,
                    duration = albumSongs.sumOf { it.duration },
                    year = firstSong.year,
                    albumArtUri = getAlbumArtUri(albumId)
                )
            }
            .sortedBy { it.name }
    }
    
    /**
     * Get artists from songs
     */
    fun getArtistsFromSongs(songs: List<Song>): List<Artist> {
        return songs
            .groupBy { it.artistId }
            .map { (artistId, artistSongs) ->
                val albums = artistSongs.distinctBy { it.albumId }
                Artist(
                    id = artistId,
                    name = artistSongs.first().artist,
                    songCount = artistSongs.size,
                    albumCount = albums.size
                )
            }
            .sortedBy { it.name }
    }
    
    /**
     * Get songs grouped by folder
     */
    fun getSongsByFolder(songs: List<Song>): Map<String, List<Song>> {
        return songs.groupBy { song ->
            File(song.filePath).parent ?: "Unknown"
        }
    }
    
    /**
     * Get album art URI
     */
    private fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }
    
    /**
     * Find external SD card paths (for older Android versions)
     */
    private fun findExternalSDCards(): List<String> {
        val sdCards = mutableListOf<String>()
        
        // Check common SD card mount points
        val possiblePaths = listOf(
            "/storage/sdcard1",
            "/storage/extSdCard",
            "/storage/external_SD",
            "/mnt/extSdCard",
            "/mnt/sdcard/external_sd",
            "/mnt/external_sd",
            "/mnt/media_rw/sdcard1"
        )
        
        possiblePaths.forEach { path ->
            val file = File(path)
            if (file.exists() && file.isDirectory && file.canRead()) {
                sdCards.add(path)
            }
        }
        
        // Also check /storage for any removable storage
        File("/storage").listFiles()?.forEach { file ->
            if (file.isDirectory && 
                file.name != "emulated" && 
                file.name != "self" &&
                file.canRead()) {
                sdCards.add(file.absolutePath)
            }
        }
        
        return sdCards.distinct()
    }
    
    /**
     * Check if SD card is mounted
     */
    fun isSDCardMounted(): Boolean {
        return getStorageVolumes().any { it.source == StorageSource.SD_CARD }
    }
    
    /**
     * Get detailed audio file info
     */
    suspend fun getAudioFileInfo(filePath: String): AudioFileInfo? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            
            // Use MediaMetadataRetriever for detailed info
            android.media.MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(filePath)
                
                AudioFileInfo(
                    filePath = filePath,
                    fileName = file.name,
                    fileSize = file.length(),
                    format = file.extension.uppercase(),
                    bitrate = retriever.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE
                    )?.toIntOrNull() ?: 0,
                    sampleRate = 44100, // Need additional library for this
                    channels = 2, // Need additional library for this
                    duration = retriever.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Storage volume information
 */
data class StorageVolume(
    val path: String,
    val name: String,
    val source: StorageSource,
    val isRemovable: Boolean,
    val totalSpace: Long,
    val freeSpace: Long
)

/**
 * Audio file detailed information
 */
data class AudioFileInfo(
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val format: String,
    val bitrate: Int,
    val sampleRate: Int,
    val channels: Int,
    val duration: Long
)

/**
 * Scan progress states
 */
sealed class ScanProgress {
    object Started : ScanProgress()
    data class ScanningVolume(val volumeName: String, val current: Int, val total: Int) : ScanProgress()
    data class VolumeComplete(val volumeName: String, val songCount: Int) : ScanProgress()
    data class Complete(val songs: List<Song>) : ScanProgress()
    data class Error(val message: String) : ScanProgress()
}
