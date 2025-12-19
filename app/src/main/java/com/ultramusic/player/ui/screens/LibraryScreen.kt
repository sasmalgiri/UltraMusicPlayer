package com.ultramusic.player.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ultramusic.player.data.model.Song
import com.ultramusic.player.data.model.StorageSource
import com.ultramusic.player.utils.VoiceSearchState
import com.ultramusic.player.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    onSongClick: (Song) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var selectedSource by remember { mutableStateOf<StorageSource?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Search Bar with Voice Search
        SearchBarWithVoice(
            query = searchQuery,
            onQueryChange = { 
                searchQuery = it
                viewModel.searchSongs(it)
            },
            voiceSearchState = uiState.voiceSearchState,
            onVoiceSearchStart = { viewModel.startVoiceSearch() },
            onVoiceSearchStop = { viewModel.stopVoiceSearch() },
            onFilterClick = { showFilters = !showFilters }
        )
        
        // Filter chips
        AnimatedVisibility(visible = showFilters) {
            FilterChipsRow(
                selectedSource = selectedSource,
                onSourceSelected = { selectedSource = it }
            )
        }
        
        // Scanning indicator
        if (uiState.isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = uiState.scanMessage,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        
        // Song count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayedSongs = if (searchQuery.isNotEmpty()) {
                uiState.searchResults
            } else {
                uiState.songs.filter { song ->
                    selectedSource == null || song.storageSource == selectedSource
                }
            }
            
            Text(
                text = "${displayedSongs.size} songs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { viewModel.scanMusic() }) {
                Icon(Icons.Default.Refresh, "Refresh")
            }
        }
        
        // Songs list
        val displayedSongs = if (searchQuery.isNotEmpty()) {
            uiState.searchResults
        } else {
            uiState.songs.filter { song ->
                selectedSource == null || song.storageSource == selectedSource
            }
        }
        
        if (displayedSongs.isEmpty() && !uiState.isScanning) {
            EmptyLibraryMessage(onRefresh = { viewModel.scanMusic() })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(displayedSongs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = uiState.currentSong?.id == song.id,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarWithVoice(
    query: String,
    onQueryChange: (String) -> Unit,
    voiceSearchState: VoiceSearchState,
    onVoiceSearchStart: () -> Unit,
    onVoiceSearchStop: () -> Unit,
    onFilterClick: () -> Unit
) {
    val isListening = voiceSearchState is VoiceSearchState.Listening ||
                      voiceSearchState is VoiceSearchState.Processing
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search field
        OutlinedTextField(
            value = when (voiceSearchState) {
                is VoiceSearchState.Processing -> voiceSearchState.partialResult
                is VoiceSearchState.Success -> voiceSearchState.result
                else -> query
            },
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search songs, artists, albums...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Voice search button
        FilledIconButton(
            onClick = {
                if (isListening) onVoiceSearchStop() else onVoiceSearchStart()
            },
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isListening) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Voice search"
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Filter button
        IconButton(onClick = onFilterClick) {
            Icon(Icons.Default.FilterList, "Filter")
        }
    }
    
    // Voice search feedback
    AnimatedVisibility(visible = isListening) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (voiceSearchState) {
                        is VoiceSearchState.Listening -> "Listening..."
                        is VoiceSearchState.Processing -> "\"${voiceSearchState.partialResult}\""
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedSource: StorageSource?,
    onSourceSelected: (StorageSource?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedSource == null,
            onClick = { onSourceSelected(null) },
            label = { Text("All") },
            leadingIcon = if (selectedSource == null) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        
        FilterChip(
            selected = selectedSource == StorageSource.INTERNAL,
            onClick = { onSourceSelected(StorageSource.INTERNAL) },
            label = { Text("📱 Internal") },
            leadingIcon = if (selectedSource == StorageSource.INTERNAL) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )
        
        FilterChip(
            selected = selectedSource == StorageSource.SD_CARD,
            onClick = { onSourceSelected(StorageSource.SD_CARD) },
            label = { Text("💾 SD Card") },
            leadingIcon = if (selectedSource == StorageSource.SD_CARD) {
                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
            } else null
        )
    }
}

@Composable
fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art placeholder / Now playing indicator
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Icon(
                    Icons.Default.Equalizer,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                // Storage indicator
                Text(
                    text = song.getStorageIcon(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = song.album,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                
                Text(
                    text = song.getFormattedDuration(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        
        // More options
        IconButton(onClick = { /* Show options menu */ }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun EmptyLibraryMessage(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LibraryMusic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No music found",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        
        Text(
            text = "Make sure you have music files on your device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scan for music")
        }
    }
}
