package com.ultramusic.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ultramusic.player.data.ActivePlaylist
import com.ultramusic.player.data.HighlightedText
import com.ultramusic.player.data.MatchType
import com.ultramusic.player.data.PlaylistSearchState
import com.ultramusic.player.data.Song
import com.ultramusic.player.data.SongMatch

/**
 * Smart Playlist Panel for NowPlayingScreen
 *
 * Features:
 * - Playlist display with current song highlighted
 * - Drag handles for reordering (visual only, actual drag uses library)
 * - Remove button for each song
 * - Inline search bar for adding songs
 * - Real-time fuzzy search results
 * - Shuffle and Loop toggles
 */
@Composable
fun SmartPlaylistPanel(
    playlist: ActivePlaylist,
    searchState: PlaylistSearchState,
    isSearchMode: Boolean,
    isMinimized: Boolean,
    onToggleMinimize: () -> Unit,
    onPlaySong: (Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onMoveSong: (from: Int, to: Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddFromSearch: (Song, Boolean) -> Unit,
    onToggleSearchMode: () -> Unit,
    onToggleLoop: () -> Unit,
    onShuffleRemaining: () -> Unit,
    showMinimizeToggle: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header with controls
        SmartPlaylistHeader(
            songCount = playlist.size,
            isLooping = playlist.isLooping,
            isMinimized = isMinimized,
            onAddClick = onToggleSearchMode,
            onShuffleClick = onShuffleRemaining,
            onLoopClick = onToggleLoop,
            onToggleMinimize = onToggleMinimize,
            showMinimizeToggle = showMinimizeToggle
        )

        AnimatedVisibility(
            visible = !isMinimized,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            // Search mode or playlist view
            if (isSearchMode) {
                // Search overlay
                SearchModeContent(
                    searchState = searchState,
                    onQueryChange = onSearchQueryChange,
                    onAddSong = onAddFromSearch,
                    onClose = onToggleSearchMode,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Playlist content
                if (playlist.isEmpty) {
                    EmptyPlaylistContent(
                        onAddClick = onToggleSearchMode,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    PlaylistContent(
                        playlist = playlist,
                        onPlaySong = onPlaySong,
                        onRemoveSong = onRemoveSong,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Bottom search bar (when not in search mode)
                QuickSearchBar(
                    onClick = onToggleSearchMode
                )
            }
        }
    }
}

@Composable
private fun SmartPlaylistHeader(
    songCount: Int,
    isLooping: Boolean,
    isMinimized: Boolean,
    onAddClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onLoopClick: () -> Unit,
    onToggleMinimize: () -> Unit,
    showMinimizeToggle: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and count
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Playlist",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$songCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Add button
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add songs",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Shuffle button
            IconButton(
                onClick = onShuffleClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Loop button
            IconButton(
                onClick = onLoopClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Loop",
                    tint = if (isLooping) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (showMinimizeToggle) {
                IconButton(
                    onClick = onToggleMinimize,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isMinimized) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isMinimized) "Expand playlist" else "Minimize playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlaylistContent(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No songs yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap + or search to add",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PlaylistContent(
    playlist: ActivePlaylist,
    onPlaySong: (Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        itemsIndexed(
            items = playlist.queue,
            key = { index, song -> "${song.id}_$index" }
        ) { index, song ->
            val isCurrentSong = index == playlist.currentIndex
            val isPastSong = index < playlist.currentIndex

            PlaylistSongItem(
                song = song,
                index = index,
                isPlaying = isCurrentSong,
                isPastSong = isPastSong,
                onPlay = { onPlaySong(index) },
                onRemove = { onRemoveSong(index) }
            )
        }
    }
}

@Composable
private fun PlaylistSongItem(
    song: Song,
    index: Int,
    isPlaying: Boolean,
    isPastSong: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .background(
                when {
                    isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    isPastSong -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index or playing indicator
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPastSong)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Album art
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Playing overlay
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isPlaying -> MaterialTheme.colorScheme.primary
                    isPastSong -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isPastSong) 0.4f else 0.7f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }

        // Duration
        Text(
            text = song.durationFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )

        // Drag handle
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(14.dp)
        )

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun QuickSearchBar(
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Search to add songs...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SearchModeContent(
    searchState: PlaylistSearchState,
    onQueryChange: (String) -> Unit,
    onAddSong: (Song, Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Search header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchState.query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        "Type song name...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchState.query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close search",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Results info
        if (searchState.query.isNotEmpty()) {
            Text(
                text = "${searchState.totalResults} results",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // Search results
        if (searchState.hasResults) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                // Best matches
                if (searchState.exactMatches.isNotEmpty()) {
                    items(searchState.exactMatches.size) { index ->
                        SearchResultItem(
                            match = searchState.exactMatches[index],
                            onAdd = { onAddSong(searchState.exactMatches[index].song, false) },
                            onPlayNext = { onAddSong(searchState.exactMatches[index].song, true) }
                        )
                    }
                }

                // Fuzzy matches
                if (searchState.fuzzyMatches.isNotEmpty()) {
                    items(searchState.fuzzyMatches.size) { index ->
                        SearchResultItem(
                            match = searchState.fuzzyMatches[index],
                            onAdd = { onAddSong(searchState.fuzzyMatches[index].song, false) },
                            onPlayNext = { onAddSong(searchState.fuzzyMatches[index].song, true) }
                        )
                    }
                }

                // Similar matches
                if (searchState.similarMatches.isNotEmpty()) {
                    items(searchState.similarMatches.size) { index ->
                        SearchResultItem(
                            match = searchState.similarMatches[index],
                            onAdd = { onAddSong(searchState.similarMatches[index].song, false) },
                            onPlayNext = { onAddSong(searchState.similarMatches[index].song, true) }
                        )
                    }
                }
            }
        } else if (searchState.query.isNotEmpty()) {
            // No results
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Empty search state
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type to search",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    match: SongMatch,
    onAdd: () -> Unit,
    onPlayNext: () -> Unit
) {
    var showAdded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onAdd()
                showAdded = true
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (match.song.albumArtUri != null) {
                AsyncImage(
                    model = match.song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            HighlightedTextView(match.highlightedTitle)
            Text(
                text = match.song.artist,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
        }

        // Play Next button
        IconButton(
            onClick = {
                onPlayNext()
                showAdded = true
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Queue,
                contentDescription = "Play next",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        // Add to end button
        IconButton(
            onClick = {
                onAdd()
                showAdded = true
            },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                if (showAdded) Icons.Default.PlaylistPlay else Icons.Default.PlaylistAdd,
                contentDescription = "Add to playlist",
                tint = if (showAdded) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
        }
    }

    // Reset added state
    LaunchedEffect(showAdded) {
        if (showAdded) {
            kotlinx.coroutines.delay(1000)
            showAdded = false
        }
    }
}

@Composable
private fun HighlightedTextView(
    highlightedText: HighlightedText
) {
    val annotatedString = buildAnnotatedString {
        for (segment in highlightedText.segments) {
            if (segment.isHighlighted) {
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(segment.text)
                }
            } else {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    append(segment.text)
                }
            }
        }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontSize = 12.sp
    )
}
