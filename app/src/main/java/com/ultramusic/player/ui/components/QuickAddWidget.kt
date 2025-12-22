package com.ultramusic.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ultramusic.player.data.AddResult
import com.ultramusic.player.data.MatchType
import com.ultramusic.player.data.Song
import com.ultramusic.player.data.SongMatch
import com.ultramusic.player.ui.MainViewModel
import kotlinx.coroutines.delay

/**
 * Quick Add Widget
 * 
 * A floating widget that can be shown on any screen to quickly add songs
 * to the playlist. Features:
 * - Real-time search as you type
 * - Fuzzy matching for typos
 * - Shows narrowing suggestions
 * - Voice input support
 * - Instant feedback when song is added
 */
@Composable
fun QuickAddWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val searchState by viewModel.playlistSearchState.collectAsState()
    val lastAddResult by viewModel.lastAddResult.collectAsState()
    
    Box(modifier = modifier) {
        // Collapsed state - just the FAB
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            FloatingActionButton(
                onClick = { 
                    isExpanded = true
                    viewModel.startPlaylistAddingMode()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, "Quick add song")
            }
        }
        
        // Expanded state - search interface
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            QuickAddPanel(
                searchState = searchState,
                lastAddResult = lastAddResult,
                onQueryChange = { viewModel.updatePlaylistSearchQuery(it) },
                onSongAdd = { song, playNext ->
                    viewModel.addToPlaylistFromSearch(song, playNext)
                },
                onVoiceInput = { viewModel.startVoiceSearch() },
                onClose = {
                    isExpanded = false
                    viewModel.endPlaylistAddingMode()
                    viewModel.clearLastAddResult()
                }
            )
        }
    }
}

@Composable
private fun QuickAddPanel(
    searchState: com.ultramusic.player.data.PlaylistSearchState,
    lastAddResult: AddResult?,
    onQueryChange: (String) -> Unit,
    onSongAdd: (Song, Boolean) -> Unit,
    onVoiceInput: () -> Unit,
    onClose: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick Add to Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Search field
            OutlinedTextField(
                value = searchState.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Type song name...") },
                trailingIcon = {
                    IconButton(onClick = onVoiceInput) {
                        Icon(
                            Icons.Default.Mic,
                            "Voice input",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            
            // Results info
            if (searchState.query.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${searchState.totalResults} found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchState.isNarrowing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "↓ narrowing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Last add result feedback
            AnimatedVisibility(visible = lastAddResult != null) {
                when (lastAddResult) {
                    is AddResult.Success -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Added: ${lastAddResult.song.title}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    is AddResult.NoMatch -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "No match for: ${lastAddResult.query}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    null -> {}
                }
            }
            
            // Quick results (show top 5)
            if (searchState.hasResults) {
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchState.allMatches.take(5)) { match ->
                        QuickAddResultItem(
                            match = match,
                            onAdd = { onSongAdd(match.song, false) },
                            onPlayNext = { onSongAdd(match.song, true) }
                        )
                    }
                    
                    if (searchState.allMatches.size > 5) {
                        item {
                            Text(
                                text = "+${searchState.allMatches.size - 5} more matches",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddResultItem(
    match: SongMatch,
    onAdd: () -> Unit,
    onPlayNext: () -> Unit
) {
    var showAdded by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { 
                onAdd()
                showAdded = true
            }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            if (match.song.albumArtUri != null) {
                AsyncImage(
                    model = match.song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = match.song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = match.song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Match indicator
                Text(
                    text = when (match.matchType) {
                        MatchType.EXACT, MatchType.STRONG -> "✓"
                        MatchType.GOOD -> "~"
                        else -> "?"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (match.matchType) {
                        MatchType.EXACT, MatchType.STRONG -> MaterialTheme.colorScheme.primary
                        MatchType.GOOD -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
        
        // Add buttons
        if (showAdded) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Added",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            IconButton(
                onClick = { 
                    onPlayNext()
                    showAdded = true
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Queue,
                    "Play next",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
    
    // Reset after delay
    LaunchedEffect(showAdded) {
        if (showAdded) {
            delay(1500)
            showAdded = false
        }
    }
}

/**
 * Mini quick add bar that can be shown at the bottom of any screen
 */
@Composable
fun MiniQuickAddBar(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val searchState by viewModel.playlistSearchState.collectAsState()
    var query by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            OutlinedTextField(
                value = query,
                onValueChange = { 
                    query = it
                    viewModel.updatePlaylistSearchQuery(it)
                    isActive = true
                },
                placeholder = { Text("Quick add song...", style = MaterialTheme.typography.bodySmall) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (query.isNotBlank()) {
                            viewModel.quickAddToPlaylist(query)
                            query = ""
                            isActive = false
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Voice button
            IconButton(
                onClick = { viewModel.startVoiceSearch() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    "Voice add",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
