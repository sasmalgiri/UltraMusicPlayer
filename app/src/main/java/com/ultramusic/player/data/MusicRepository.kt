package com.ultramusic.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for accessing music files from device storage
 */
@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Scan all audio files from device storage
     */
    fun scanMusic(): Flow<List<Song>> = flow {
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
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        // Include all audio files with duration > 10 seconds
        // This covers: MP3, WAV, FLAC, AAC, OGG, OPUS, M4A, WMA, AIFF, etc.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(dataColumn) ?: ""
                val dateAdded = cursor.getLong(dateAddedColumn)
                val size = cursor.getLong(sizeColumn)
                val mimeType = cursor.getString(mimeTypeColumn) ?: ""

                // Estimate bitrate from file size and duration
                val bitrate = if (duration > 0 && size > 0) {
                    ((size * 8) / (duration / 1000) / 1000).toInt()
                } else 0
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                
                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uri = contentUri,
                        albumArtUri = albumArtUri,
                        path = path,
                        dateAdded = dateAdded,
                        size = size,
                        mimeType = mimeType,
                        bitrate = bitrate
                    )
                )
            }
        }
        
        emit(songs)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Search songs by query
     */
    fun searchSongs(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) return songs
        
        val lowerQuery = query.lowercase()
        return songs.filter { song ->
            song.title.lowercase().contains(lowerQuery) ||
            song.artist.lowercase().contains(lowerQuery) ||
            song.album.lowercase().contains(lowerQuery)
        }
    }
    
    /**
     * Sort songs by option
     */
    fun sortSongs(songs: List<Song>, sortOption: SortOption): List<Song> {
        return when (sortOption) {
            SortOption.TITLE -> songs.sortedBy { it.title.lowercase() }
            SortOption.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM -> songs.sortedBy { it.album.lowercase() }
            SortOption.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
            SortOption.DURATION -> songs.sortedByDescending { it.duration }
            SortOption.FORMAT -> songs.sortedBy { it.format.displayName }
            SortOption.SIZE -> songs.sortedByDescending { it.size }
        }
    }

    /**
     * Get format statistics for the library
     */
    fun getFormatStats(songs: List<Song>): Map<AudioFormat, Int> {
        return songs.groupingBy { it.format }.eachCount()
    }

    /**
     * Get songs by format
     */
    fun getSongsByFormat(songs: List<Song>, format: AudioFormat): List<Song> {
        return songs.filter { it.format == format }
    }

    /**
     * Get lossless songs only
     */
    fun getLosslessSongs(songs: List<Song>): List<Song> {
        return songs.filter { it.format.isLossless }
    }
    
    /**
     * Get songs by album
     */
    fun getSongsByAlbum(songs: List<Song>, album: String): List<Song> {
        return songs.filter { it.album == album }
    }
    
    /**
     * Get songs by artist
     */
    fun getSongsByArtist(songs: List<Song>, artist: String): List<Song> {
        return songs.filter { it.artist == artist }
    }
    
    /**
     * Get unique albums
     */
    fun getAlbums(songs: List<Song>): List<String> {
        return songs.map { it.album }.distinct().sorted()
    }
    
    /**
     * Get unique artists
     */
    fun getArtists(songs: List<Song>): List<String> {
        return songs.map { it.artist }.distinct().sorted()
    }
}
