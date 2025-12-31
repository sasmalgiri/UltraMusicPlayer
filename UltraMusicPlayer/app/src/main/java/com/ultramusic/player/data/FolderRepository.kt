package com.ultramusic.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a folder containing music files
 */
data class MusicFolder(
    val name: String,
    val path: String,
    val songCount: Int,
    val subFolders: List<MusicFolder> = emptyList()
) {
    // Display name without full path
    val displayName: String
        get() = name.ifEmpty { "Music" }
}

/**
 * Represents the browsing state - either viewing folders or songs in a folder
 */
sealed class BrowseItem {
    data class Folder(val folder: MusicFolder) : BrowseItem()
    data class SongItem(val song: Song) : BrowseItem()
}

/**
 * Repository for unified folder browsing across all storage sources
 * Hides the actual storage location from users - they just see folders and songs
 */
@Singleton
class FolderRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var allSongs: List<Song> = emptyList()
    private var folderMap: Map<String, List<Song>> = emptyMap()
    
    /**
     * Scan all music and organize by folders
     * Returns a unified view - no "Internal" vs "SD Card" distinction
     */
    fun scanMusicWithFolders(): Flow<Pair<List<Song>, Map<String, List<Song>>>> = flow {
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
            MediaStore.Audio.Media.SIZE
        )
        
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
                        size = size
                    )
                )
            }
        }
        
        allSongs = songs
        
        // Group songs by their parent folder (simplified path)
        folderMap = songs.groupBy { song ->
            getSimplifiedFolderPath(song.path)
        }
        
        emit(Pair(songs, folderMap))
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get simplified folder path - removes storage prefix to unify view
     * "/storage/emulated/0/Music/Rock" -> "Music/Rock"
     * "/storage/1234-ABCD/MyMusic/Pop" -> "MyMusic/Pop"
     */
    private fun getSimplifiedFolderPath(filePath: String): String {
        val file = File(filePath)
        val parentPath = file.parent ?: return "Music"
        
        // Common storage prefixes to remove
        val prefixesToRemove = listOf(
            "/storage/emulated/0/",
            "/storage/emulated/legacy/",
            "/sdcard/",
            "/mnt/sdcard/",
            Environment.getExternalStorageDirectory().absolutePath + "/"
        )
        
        var simplified = parentPath
        
        // Remove common prefixes
        for (prefix in prefixesToRemove) {
            if (simplified.startsWith(prefix)) {
                simplified = simplified.removePrefix(prefix)
                break
            }
        }
        
        // Handle SD card paths like /storage/1234-ABCD/
        if (simplified.startsWith("/storage/")) {
            val parts = simplified.split("/")
            if (parts.size > 2) {
                // Skip "/storage/XXXX-XXXX/" part
                simplified = parts.drop(3).joinToString("/")
            }
        }
        
        // If still has leading slash, remove it
        simplified = simplified.trimStart('/')
        
        // If empty, default to "Music"
        return simplified.ifEmpty { "Music" }
    }
    
    /**
     * Get root level folders (top-level music folders)
     */
    fun getRootFolders(): List<MusicFolder> {
        val rootFolders = mutableMapOf<String, MutableList<Song>>()
        
        folderMap.forEach { (path, songs) ->
            val rootName = path.split("/").firstOrNull() ?: "Music"
            rootFolders.getOrPut(rootName) { mutableListOf() }.addAll(songs)
        }
        
        return rootFolders.map { (name, songs) ->
            MusicFolder(
                name = name,
                path = name,
                songCount = songs.size
            )
        }.sortedBy { it.name.lowercase() }
    }
    
    /**
     * Get contents of a folder - returns subfolders and songs
     */
    fun getFolderContents(folderPath: String): List<BrowseItem> {
        val items = mutableListOf<BrowseItem>()
        val subFolders = mutableMapOf<String, MutableList<Song>>()
        val songsInThisFolder = mutableListOf<Song>()
        
        folderMap.forEach { (path, songs) ->
            when {
                // Exact match - songs are directly in this folder
                path == folderPath -> {
                    songsInThisFolder.addAll(songs)
                }
                // Subfolder - path starts with folderPath/
                path.startsWith("$folderPath/") -> {
                    val relativePath = path.removePrefix("$folderPath/")
                    val subFolderName = relativePath.split("/").first()
                    subFolders.getOrPut(subFolderName) { mutableListOf() }.addAll(songs)
                }
            }
        }
        
        // Add subfolders first
        subFolders.forEach { (name, songs) ->
            items.add(
                BrowseItem.Folder(
                    MusicFolder(
                        name = name,
                        path = "$folderPath/$name",
                        songCount = songs.size
                    )
                )
            )
        }
        
        // Sort folders by name
        val sortedItems = items.sortedBy { 
            (it as BrowseItem.Folder).folder.name.lowercase() 
        }.toMutableList()
        
        // Add songs after folders
        songsInThisFolder.sortedBy { it.title.lowercase() }.forEach { song ->
            sortedItems.add(BrowseItem.SongItem(song))
        }
        
        return sortedItems
    }
    
    /**
     * Get all songs in a folder and its subfolders
     */
    fun getAllSongsInFolder(folderPath: String): List<Song> {
        val songs = mutableListOf<Song>()
        
        folderMap.forEach { (path, folderSongs) ->
            if (path == folderPath || path.startsWith("$folderPath/")) {
                songs.addAll(folderSongs)
            }
        }
        
        return songs.sortedBy { it.title.lowercase() }
    }
    
    /**
     * Get breadcrumb path for navigation
     */
    fun getBreadcrumbs(currentPath: String): List<Pair<String, String>> {
        if (currentPath.isEmpty()) return listOf("Home" to "")
        
        val breadcrumbs = mutableListOf("Home" to "")
        val parts = currentPath.split("/")
        var path = ""
        
        parts.forEach { part ->
            path = if (path.isEmpty()) part else "$path/$part"
            breadcrumbs.add(part to path)
        }
        
        return breadcrumbs
    }
    
    /**
     * Search songs by query
     */
    fun searchSongs(query: String): List<Song> {
        if (query.isBlank()) return emptyList()
        
        val lowerQuery = query.lowercase()
        return allSongs.filter { song ->
            song.title.lowercase().contains(lowerQuery) ||
            song.artist.lowercase().contains(lowerQuery) ||
            song.album.lowercase().contains(lowerQuery)
        }
    }
    
    fun getAllSongs(): List<Song> = allSongs
}
