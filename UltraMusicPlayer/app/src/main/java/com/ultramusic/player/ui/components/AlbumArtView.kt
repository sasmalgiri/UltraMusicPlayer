package com.ultramusic.player.ui.components

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ALBUM ART VIEW
 * 
 * Loads album art from:
 * 1. Embedded metadata (MediaMetadataRetriever)
 * 2. MediaStore album art URI
 * 3. Fallback to music note icon
 * 
 * Uses Coil for efficient async loading and caching.
 */
@Composable
fun AlbumArtView(
    songPath: String?,
    albumId: Long? = null,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    cornerRadius: Dp = 16.dp,
    showGradientOverlay: Boolean = false
) {
    val context = LocalContext.current
    var embeddedArt by remember(songPath) { mutableStateOf<Bitmap?>(null) }
    var loadAttempted by remember(songPath) { mutableStateOf(false) }
    
    // Try to load embedded album art
    LaunchedEffect(songPath) {
        if (songPath != null && !loadAttempted) {
            loadAttempted = true
            embeddedArt = withContext(Dispatchers.IO) {
                extractEmbeddedArt(songPath)
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2A2A2A),
                        Color(0xFF1A1A1A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            // 1. Try embedded art first
            embeddedArt != null -> {
                Image(
                    bitmap = embeddedArt!!.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // 2. Try MediaStore album art
            albumId != null && albumId > 0 -> {
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = {
                        // Will show fallback below
                    }
                )
            }
            
            // 3. Fallback placeholder
            else -> {
                AlbumArtPlaceholder(size = size * 0.4f)
            }
        }
        
        // Optional gradient overlay for text readability
        if (showGradientOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }
    }
}

/**
 * Placeholder when no album art is available
 */
@Composable
fun AlbumArtPlaceholder(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    iconColor: Color = Color(0xFF666666),
    backgroundColor: Color = Color(0xFF2A2A2A)
) {
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = "No Album Art",
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Small album art for list items
 */
@Composable
fun SmallAlbumArt(
    songPath: String?,
    albumId: Long? = null,
    modifier: Modifier = Modifier
) {
    AlbumArtView(
        songPath = songPath,
        albumId = albumId,
        modifier = modifier,
        size = 56.dp,
        cornerRadius = 8.dp
    )
}

/**
 * Large album art for now playing screen
 */
@Composable
fun LargeAlbumArt(
    songPath: String?,
    albumId: Long? = null,
    modifier: Modifier = Modifier
) {
    AlbumArtView(
        songPath = songPath,
        albumId = albumId,
        modifier = modifier,
        size = 300.dp,
        cornerRadius = 24.dp,
        showGradientOverlay = false
    )
}

/**
 * Extract embedded album art from audio file
 */
private fun extractEmbeddedArt(path: String): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(path)
        val artBytes = retriever.embeddedPicture
        if (artBytes != null) {
            BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (e: Exception) { }
    }
}

/**
 * Dominant color extraction from album art
 * (For dynamic theming)
 */
fun extractDominantColor(bitmap: Bitmap): Color {
    val scaled = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
    
    var redSum = 0L
    var greenSum = 0L
    var blueSum = 0L
    var count = 0
    
    for (x in 0 until scaled.width) {
        for (y in 0 until scaled.height) {
            val pixel = scaled.getPixel(x, y)
            val red = android.graphics.Color.red(pixel)
            val green = android.graphics.Color.green(pixel)
            val blue = android.graphics.Color.blue(pixel)
            
            // Skip very dark or very light pixels
            val brightness = (red + green + blue) / 3
            if (brightness in 30..220) {
                redSum += red
                greenSum += green
                blueSum += blue
                count++
            }
        }
    }
    
    return if (count > 0) {
        Color(
            red = (redSum / count).toInt(),
            green = (greenSum / count).toInt(),
            blue = (blueSum / count).toInt()
        )
    } else {
        Color(0xFF4CAF50) // Default green
    }
}
