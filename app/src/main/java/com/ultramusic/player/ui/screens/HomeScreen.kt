package com.ultramusic.player.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.data.AudioPreset
import com.ultramusic.player.data.Song
import com.ultramusic.player.data.SortOption
import com.ultramusic.player.ui.BrowseTab
import com.ultramusic.player.ui.MainViewModel
import com.ultramusic.player.ui.components.NowPlayingBar
import com.ultramusic.player.ui.components.PresetPanel
import com.ultramusic.player.ui.components.SongListItem
import com.ultramusic.player.ui.components.SpeedPitchControl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToVoiceSearch: () -> Unit,
    onNavigateToPlaylist: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    
    var isSearching by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // NOTE: Permission handling moved to MainActivity for auto-scan on launch

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search songs...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text(
                                text = "UltraMusic",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.filteredSongs.size} songs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Smart Playlist
                    IconButton(onClick = onNavigateToPlaylist) {
                        Icon(Icons.Default.QueueMusic, contentDescription = "Smart Playlist")
                    }
                    
                    // Voice Search - for loud environments
                    IconButton(onClick = onNavigateToVoiceSearch) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                    }
                    
                    // Folder browser
                    IconButton(onClick = onNavigateToFolders) {
                        Icon(Icons.Default.Folder, contentDescription = "Browse Folders")
                    }
                    
                    if (isSearching) {
                        IconButton(onClick = {
                            isSearching = false
                            viewModel.clearSearch()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                    
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }
                        
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = option.name.replace("_", " ").lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            color = if (uiState.sortOption == option)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // NOTE: Permission handling moved to MainActivity
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text(
                                text = "Scanning music library...",
                                modifier = Modifier.padding(top = 16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                uiState.filteredSongs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) 
                                "No songs found for \"${uiState.searchQuery}\""
                            else 
                                "No music found on device",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                else -> {
                    // Handle back press to exit selection mode
                    BackHandler(enabled = uiState.isSelectionMode) {
                        viewModel.exitSelectionMode()
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Browse Tabs
                        BrowseTabRow(
                            selectedTab = uiState.selectedBrowseTab,
                            onTabSelected = { viewModel.setBrowseTab(it) }
                        )

                        // Selection Mode Header
                        if (uiState.isSelectionMode) {
                            SelectionModeHeader(
                                selectedCount = uiState.selectedSongIds.size,
                                totalCount = uiState.filteredSongs.size,
                                onSelectAll = { viewModel.selectAllSongs() },
                                onDeselectAll = { viewModel.deselectAllSongs() },
                                onClose = { viewModel.exitSelectionMode() }
                            )
                        }

                        // Content based on selected tab
                        when (uiState.selectedBrowseTab) {
                            BrowseTab.ALL_SONGS -> {
                                // Song list with selection support
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(
                                        bottom = if (playbackState.currentSong != null || uiState.isSelectionMode) 80.dp else 0.dp
                                    )
                                ) {
                                    items(
                                        items = uiState.filteredSongs,
                                        key = { it.id }
                                    ) { song ->
                                        SelectableSongItem(
                                            song = song,
                                            isPlaying = playbackState.currentSong?.id == song.id,
                                            isSelectionMode = uiState.isSelectionMode,
                                            isSelected = uiState.selectedSongIds.contains(song.id),
                                            onClick = {
                                                if (uiState.isSelectionMode) {
                                                    viewModel.toggleSongSelection(song.id)
                                                } else {
                                                    viewModel.playSong(song)
                                                }
                                            },
                                            onLongClick = {
                                                if (!uiState.isSelectionMode) {
                                                    viewModel.enterSelectionMode(song.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            BrowseTab.ARTISTS -> {
                                ArtistsList(
                                    artists = uiState.artistsList,
                                    onArtistClick = { artist ->
                                        viewModel.playArtist(artist)
                                    },
                                    modifier = Modifier.weight(1f),
                                    bottomPadding = if (playbackState.currentSong != null) 80.dp else 0.dp
                                )
                            }

                            BrowseTab.ALBUMS -> {
                                AlbumsList(
                                    albums = uiState.albumsList,
                                    onAlbumClick = { album ->
                                        viewModel.playAlbum(album)
                                    },
                                    modifier = Modifier.weight(1f),
                                    bottomPadding = if (playbackState.currentSong != null) 80.dp else 0.dp
                                )
                            }

                            BrowseTab.FOLDERS -> {
                                // Redirect to folder browser
                                LaunchedEffect(Unit) {
                                    onNavigateToFolders()
                                    viewModel.setBrowseTab(BrowseTab.ALL_SONGS)
                                }
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Opening folder browser...")
                                }
                            }
                        }

                        // Selection Action Bar
                        if (uiState.isSelectionMode && uiState.selectedSongIds.isNotEmpty()) {
                            SelectionActionBar(
                                selectedCount = uiState.selectedSongIds.size,
                                onAddToPlaylist = { viewModel.addSelectedToPlaylist() },
                                onPlaySelected = { viewModel.playSelectedSongs() }
                            )
                        }
                    }
                }
            }
            
            // Now Playing Bar
            AnimatedVisibility(
                visible = playbackState.currentSong != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                NowPlayingBar(
                    playbackState = playbackState,
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onPreviousClick = { viewModel.playPrevious() },
                    onNextClick = { viewModel.playNext() },
                    onClick = onNavigateToNowPlaying
                )
            }
            
            // Speed/Pitch Panel
            AnimatedVisibility(
                visible = uiState.showSpeedPitchPanel,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SpeedPitchControl(
                    speed = playbackState.speed,
                    pitch = playbackState.pitch,
                    onSpeedChange = { viewModel.setSpeed(it) },
                    onPitchChange = { viewModel.setPitch(it) },
                    onSpeedAdjust = { viewModel.adjustSpeed(it) },
                    onPitchAdjust = { viewModel.adjustPitch(it) },
                    onResetSpeed = { viewModel.resetSpeed() },
                    onResetPitch = { viewModel.resetPitch() },
                    onResetAll = { viewModel.resetAll() }
                )
            }
            
            // Preset Panel
            AnimatedVisibility(
                visible = uiState.showPresetPanel,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PresetPanel(
                    presets = AudioPreset.PRESETS,
                    selectedPreset = uiState.selectedPreset,
                    onPresetSelected = { viewModel.applyPreset(it) },
                    speed = playbackState.speed,
                    pitch = playbackState.pitch,
                    onSpeedChange = { viewModel.setSpeed(it) },
                    onPitchChange = { viewModel.setPitch(it) },
                    onResetAll = { viewModel.resetAll() }
                )
            }
        }
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🎵",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "UltraMusic needs access to your music files to play them",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            androidx.compose.material3.Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

// ==================== BROWSE TABS ====================

@Composable
private fun BrowseTabRow(
    selectedTab: BrowseTab,
    onTabSelected: (BrowseTab) -> Unit
) {
    val tabs = listOf(
        BrowseTab.ALL_SONGS to "All Songs",
        BrowseTab.ARTISTS to "Artists",
        BrowseTab.ALBUMS to "Albums",
        BrowseTab.FOLDERS to "Folders"
    )

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab },
        edgePadding = 16.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        tabs.forEach { (tab, title) ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                BrowseTab.ALL_SONGS -> Icons.Default.MusicNote
                                BrowseTab.ARTISTS -> Icons.Default.Person
                                BrowseTab.ALBUMS -> Icons.Default.Album
                                BrowseTab.FOLDERS -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(title)
                    }
                }
            )
        }
    }
}

// ==================== SELECTION MODE ====================

@Composable
private fun SelectionModeHeader(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Exit selection")
                }
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                FilledTonalButton(
                    onClick = if (selectedCount < totalCount) onSelectAll else onDeselectAll,
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (selectedCount < totalCount) "Select All" else "Deselect All",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableSongItem(
    song: Song,
    isPlaying: Boolean,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection checkbox or playing indicator
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
        }

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            text = formatDuration(song.duration),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Playing indicator
        if (isPlaying && !isSelectionMode) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Now playing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = (durationMs / 1000) / 60
    val seconds = (durationMs / 1000) % 60
    return "%d:%02d".format(minutes, seconds)
}

// ==================== ARTISTS LIST ====================

@Composable
private fun ArtistsList(
    artists: List<Pair<String, Int>>,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    if (artists.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No artists found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            items(artists) { (artist, songCount) ->
                ArtistItem(
                    artistName = artist,
                    songCount = songCount,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}

@Composable
private fun ArtistItem(
    artistName: String,
    songCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artist icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Artist info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$songCount songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Play button
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play all",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ==================== ALBUMS LIST ====================

@Composable
private fun AlbumsList(
    albums: List<Triple<String, String, Int>>,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    if (albums.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No albums found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            items(albums) { (album, artist, songCount) ->
                AlbumItem(
                    albumName = album,
                    artistName = artist,
                    songCount = songCount,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
    }
}

@Composable
private fun AlbumItem(
    albumName: String,
    artistName: String,
    songCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Album info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = albumName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$songCount songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Play button
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play all",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ==================== SELECTION ACTION BAR ====================

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onAddToPlaylist: () -> Unit,
    onPlaySelected: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add to Playlist button
            Button(
                onClick = onAddToPlaylist,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Add to Playlist")
            }

            Spacer(Modifier.width(12.dp))

            // Play Selected button
            Button(
                onClick = onPlaySelected,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Play ($selectedCount)")
            }
        }
    }
}
