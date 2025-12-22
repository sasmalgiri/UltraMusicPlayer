package com.ultramusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.ultramusic.player.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SmartPlaylistScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNowPlaying: () -> Unit
) {
    val playlist by viewModel.activePlaylist.collectAsState()
    val searchState by viewModel.playlistSearchState.collectAsState()
    val isAddingMode by viewModel.isPlaylistAddingMode.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    
    var showVoiceInput by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Playlist",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${playlist.size} songs • ${formatDuration(viewModel.getPlaylistTotalDuration())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Shuffle remaining
                    IconButton(onClick = { viewModel.shufflePlaylistRemaining() }) {
                        Icon(Icons.Default.Shuffle, "Shuffle remaining")
                    }
                    // Toggle loop
                    IconButton(onClick = { viewModel.togglePlaylistLoop() }) {
                        Icon(
                            Icons.Default.Repeat,
                            "Loop",
                            tint = if (playlist.isLooping) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startPlaylistAddingMode() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add songs")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (playlist.isEmpty) {
                // Empty state
                EmptyPlaylistState(
                    onAddSongs = { viewModel.startPlaylistAddingMode() }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Now playing indicator
                    playlist.currentSong?.let { currentSong ->
                        NowPlayingCard(
                            song = currentSong,
                            nextSong = playlist.nextSong,
                            remainingCount = viewModel.getPlaylistRemainingCount(),
                            onClick = onNavigateToNowPlaying
                        )
                    }
                    
                    // Playlist queue
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(
                            items = playlist.queue,
                            key = { index, song -> "${song.id}_$index" }
                        ) { index, song ->
                            val isCurrentSong = index == playlist.currentIndex
                            val isPastSong = index < playlist.currentIndex
                            
                            SwipeablePlaylistItem(
                                song = song,
                                index = index,
                                isCurrentSong = isCurrentSong,
                                isPastSong = isPastSong,
                                onRemove = { viewModel.removeFromPlaylist(index) },
                                onPlay = { viewModel.playFromPlaylistIndex(index) },
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }
            }
            
            // Adding mode overlay
            AnimatedVisibility(
                visible = isAddingMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                AddSongsOverlay(
                    searchState = searchState,
                    onQueryChange = { viewModel.updatePlaylistSearchQuery(it) },
                    onSongAdd = { song, playNext -> 
                        viewModel.addToPlaylistFromSearch(song, playNext)
                    },
                    onVoiceInput = { showVoiceInput = true },
                    onClose = { viewModel.endPlaylistAddingMode() }
                )
            }
            
            // Voice input overlay
            if (showVoiceInput) {
                VoiceAddOverlay(
                    onVoiceResult = { text ->
                        viewModel.addToPlaylistFromVoice(text)
                        showVoiceInput = false
                    },
                    onClose = { showVoiceInput = false },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun EmptyPlaylistState(onAddSongs: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QueueMusic,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Your playlist is empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Add songs anytime - even while playing!\nUse search or voice to add songs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            FilledTonalButton(onClick = onAddSongs) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Songs")
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    song: Song,
    nextSong: Song?,
    remainingCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Now Playing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row {
                nextSong?.let {
                    Text(
                        text = "Next: ${it.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Text(
                    text = "$remainingCount more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun SwipeablePlaylistItem(
    song: Song,
    index: Int,
    isCurrentSong: Boolean,
    isPastSong: Boolean,
    onRemove: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Simplified version without swipe - use long press to show remove option
    var showRemoveConfirm by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        PlaylistItemContent(
            song = song,
            index = index,
            isCurrentSong = isCurrentSong,
            isPastSong = isPastSong,
            onPlay = onPlay,
            modifier = Modifier
                .combinedClickable(
                    onClick = onPlay,
                    onLongClick = { showRemoveConfirm = true }
                )
        )
        
        if (showRemoveConfirm) {
            AlertDialog(
                onDismissRequest = { showRemoveConfirm = false },
                title = { Text("Remove Song") },
                text = { Text("Remove \"${song.title}\" from playlist?") },
                confirmButton = {
                    TextButton(onClick = { 
                        onRemove()
                        showRemoveConfirm = false
                    }) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun PlaylistItemContent(
    song: Song,
    index: Int,
    isCurrentSong: Boolean,
    isPastSong: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isCurrentSong -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            isPastSong -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "bgColor"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index or playing indicator
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrentSong) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPastSong) 
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Album art
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isCurrentSong -> MaterialTheme.colorScheme.primary
                    isPastSong -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isPastSong) 0.4f else 1f
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Duration
        Text(
            text = song.durationFormatted,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Drag handle
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun AddSongsOverlay(
    searchState: PlaylistSearchState,
    onQueryChange: (String) -> Unit,
    onSongAdd: (Song, Boolean) -> Unit,
    onVoiceInput: () -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with search
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, "Close")
                        }
                        
                        Text(
                            text = "Add Songs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Voice input button
                        IconButton(onClick = onVoiceInput) {
                            Icon(
                                Icons.Default.Mic,
                                "Voice search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Search field
                    OutlinedTextField(
                        value = searchState.query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Type song name (fuzzy search enabled)...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchState.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    // Suggestions chips
                    if (searchState.suggestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchState.suggestions) { suggestion ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onQueryChange(suggestion) },
                                    label = { Text(suggestion, maxLines = 1) }
                                )
                            }
                        }
                    }
                    
                    // Results info
                    if (searchState.query.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${searchState.totalResults} results",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchState.isNarrowing) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "↓ narrowing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            
            // Search results
            if (searchState.hasResults) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Exact matches
                    if (searchState.exactMatches.isNotEmpty()) {
                        item {
                            SectionHeader("Best Matches")
                        }
                        items(searchState.exactMatches) { match ->
                            SearchResultItem(
                                match = match,
                                onAdd = { onSongAdd(match.song, false) },
                                onPlayNext = { onSongAdd(match.song, true) }
                            )
                        }
                    }
                    
                    // Fuzzy matches
                    if (searchState.fuzzyMatches.isNotEmpty()) {
                        item {
                            SectionHeader("Similar Matches (fuzzy)")
                        }
                        items(searchState.fuzzyMatches) { match ->
                            SearchResultItem(
                                match = match,
                                onAdd = { onSongAdd(match.song, false) },
                                onPlayNext = { onSongAdd(match.song, true) }
                            )
                        }
                    }
                    
                    // Similar matches
                    if (searchState.similarMatches.isNotEmpty()) {
                        item {
                            SectionHeader("Maybe you meant...")
                        }
                        items(searchState.similarMatches) { match ->
                            SearchResultItem(
                                match = match,
                                onAdd = { onSongAdd(match.song, false) },
                                onPlayNext = { onSongAdd(match.song, true) }
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No songs found for \"${searchState.query}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try a different spelling or use voice search",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                // Empty state - show tips
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Start typing to search",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Fuzzy matching handles typos\n• Results narrow as you type\n• Use mic for voice input",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SearchResultItem(
    match: SongMatch,
    onAdd: () -> Unit,
    onPlayNext: () -> Unit
) {
    var showAddedFeedback by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                onAdd()
                showAddedFeedback = true
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (match.song.albumArtUri != null) {
                AsyncImage(
                    model = match.song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(Icons.Default.MusicNote, null)
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info with highlights
        Column(modifier = Modifier.weight(1f)) {
            HighlightedTextView(match.highlightedTitle)
            HighlightedTextView(
                match.highlightedArtist,
                style = MaterialTheme.typography.bodySmall,
                defaultColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Match type indicator
            Text(
                text = when (match.matchType) {
                    MatchType.EXACT -> "✓ Exact match"
                    MatchType.STRONG -> "✓ Strong match"
                    MatchType.GOOD -> "~ Fuzzy match"
                    MatchType.PARTIAL -> "? Partial match"
                    MatchType.WEAK -> "? Similar"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (match.matchType) {
                    MatchType.EXACT, MatchType.STRONG -> MaterialTheme.colorScheme.primary
                    MatchType.GOOD -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }
        
        // Add buttons
        Row {
            // Play next button
            IconButton(
                onClick = {
                    onPlayNext()
                    showAddedFeedback = true
                }
            ) {
                Icon(
                    Icons.Default.Queue,
                    contentDescription = "Play next",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Add to end button
            IconButton(
                onClick = {
                    onAdd()
                    showAddedFeedback = true
                }
            ) {
                if (showAddedFeedback) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Added",
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // Reset feedback after delay
    LaunchedEffect(showAddedFeedback) {
        if (showAddedFeedback) {
            kotlinx.coroutines.delay(1500)
            showAddedFeedback = false
        }
    }
}

@Composable
private fun HighlightedTextView(
    highlightedText: HighlightedText,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    defaultColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val annotatedString = buildAnnotatedString {
        for (segment in highlightedText.segments) {
            if (segment.isHighlighted) {
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    append(segment.text)
                }
            } else {
                withStyle(SpanStyle(color = defaultColor)) {
                    append(segment.text)
                }
            }
        }
    }
    
    Text(
        text = annotatedString,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun VoiceAddOverlay(
    onVoiceResult: (String) -> Unit,
    onClose: () -> Unit,
    viewModel: MainViewModel
) {
    val extremeVoiceState by viewModel.extremeVoiceState.collectAsState()
    val noiseLevel by viewModel.currentNoiseLevel.collectAsState()
    val noiseLevelDb by viewModel.noiseLevelDb.collectAsState()
    val canSpeak by viewModel.canSpeak.collectAsState()

    // Start voice capture when overlay appears
    LaunchedEffect(Unit) {
        viewModel.startExtremeVoiceCapture()
    }

    // Handle voice result
    LaunchedEffect(extremeVoiceState) {
        when (val state = extremeVoiceState) {
            is com.ultramusic.player.audio.ExtremeCaptureState.Result -> {
                onVoiceResult(state.text)
            }
            else -> { }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dynamic icon with color based on state
                    val iconColor = when (extremeVoiceState) {
                        is com.ultramusic.player.audio.ExtremeCaptureState.Listening -> MaterialTheme.colorScheme.primary
                        is com.ultramusic.player.audio.ExtremeCaptureState.ReadyToListen -> Color(0xFF4CAF50)
                        is com.ultramusic.player.audio.ExtremeCaptureState.Processing -> MaterialTheme.colorScheme.tertiary
                        is com.ultramusic.player.audio.ExtremeCaptureState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = iconColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic status text
                    val statusText = when (extremeVoiceState) {
                        is com.ultramusic.player.audio.ExtremeCaptureState.Idle -> "Initializing..."
                        is com.ultramusic.player.audio.ExtremeCaptureState.Calibrating -> "Calibrating noise level..."
                        is com.ultramusic.player.audio.ExtremeCaptureState.WaitingForQuiet -> "Waiting for quieter moment..."
                        is com.ultramusic.player.audio.ExtremeCaptureState.ReadyToListen -> "Speak now!"
                        is com.ultramusic.player.audio.ExtremeCaptureState.Listening -> "Listening..."
                        is com.ultramusic.player.audio.ExtremeCaptureState.Processing -> "Processing..."
                        is com.ultramusic.player.audio.ExtremeCaptureState.Result -> "Found match!"
                        is com.ultramusic.player.audio.ExtremeCaptureState.Error -> (extremeVoiceState as com.ultramusic.player.audio.ExtremeCaptureState.Error).message
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (extremeVoiceState is com.ultramusic.player.audio.ExtremeCaptureState.ReadyToListen)
                            Color(0xFF4CAF50)
                        else
                            MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Noise level indicator
                    val noiseLevelText = when (noiseLevel) {
                        com.ultramusic.player.audio.NoiseLevel.QUIET -> "Quiet environment"
                        com.ultramusic.player.audio.NoiseLevel.MODERATE -> "Moderate noise"
                        com.ultramusic.player.audio.NoiseLevel.LOUD -> "Loud - speak clearly"
                        com.ultramusic.player.audio.NoiseLevel.VERY_LOUD -> "Very loud - cup mic"
                        com.ultramusic.player.audio.NoiseLevel.EXTREME -> "Extremely loud!"
                    }

                    val noiseLevelColor = when (noiseLevel) {
                        com.ultramusic.player.audio.NoiseLevel.QUIET -> Color(0xFF4CAF50)
                        com.ultramusic.player.audio.NoiseLevel.MODERATE -> Color(0xFF8BC34A)
                        com.ultramusic.player.audio.NoiseLevel.LOUD -> Color(0xFFFF9800)
                        com.ultramusic.player.audio.NoiseLevel.VERY_LOUD -> Color(0xFFFF5722)
                        com.ultramusic.player.audio.NoiseLevel.EXTREME -> Color(0xFFF44336)
                    }

                    Text(
                        text = "$noiseLevelText (${noiseLevelDb.toInt()} dB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = noiseLevelColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tip based on state
                    val tipText = when (extremeVoiceState) {
                        is com.ultramusic.player.audio.ExtremeCaptureState.Error ->
                            (extremeVoiceState as com.ultramusic.player.audio.ExtremeCaptureState.Error).suggestion
                        is com.ultramusic.player.audio.ExtremeCaptureState.WaitingForQuiet ->
                            "Wait for a break in the music"
                        is com.ultramusic.player.audio.ExtremeCaptureState.ReadyToListen ->
                            "Say the song name clearly"
                        else -> "Fuzzy matching will find the closest match"
                    }

                    Text(
                        text = tipText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Retry button (show on error)
                        if (extremeVoiceState is com.ultramusic.player.audio.ExtremeCaptureState.Error) {
                            FilledTonalButton(
                                onClick = { viewModel.startExtremeVoiceCapture() }
                            ) {
                                Text("Retry")
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.cancelExtremeVoiceCapture()
                                onClose()
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes} min"
    }
}
