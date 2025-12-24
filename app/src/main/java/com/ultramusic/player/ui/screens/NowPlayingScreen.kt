package com.ultramusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ultramusic.player.data.AudioPreset
import com.ultramusic.player.ui.MainViewModel
import com.ultramusic.player.ui.components.CompactFolderPanel
import com.ultramusic.player.ui.components.QueuePanel
import com.ultramusic.player.ui.components.UnifiedControlsPanel
import com.ultramusic.player.ui.components.WaveformVisualizer
import com.ultramusic.player.ui.theme.UltraGradientEnd
import com.ultramusic.player.ui.theme.UltraGradientStart

/**
 * Split-screen Now Playing Screen with:
 * - Left Panel: Now Playing (compact album art, song info, waveform, controls)
 * - Right Panel: Tabbed Queue + Folders browser
 * - Bottom: Unified controls (Speed, Pitch, A-B Loop, Presets) - always visible, scrollable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val queue by viewModel.queue.collectAsState()

    // Waveform and beat detection state
    val waveformData by viewModel.currentWaveform.collectAsState()
    val beatMarkers by viewModel.currentBeatMarkers.collectAsState()
    val estimatedBpm by viewModel.estimatedBpm.collectAsState()
    val isExtractingWaveform by viewModel.isExtractingWaveform.collectAsState()

    // Folder browsing state
    val currentFolderPath by viewModel.currentFolderPath.collectAsState()
    val browseItems by viewModel.browseItems.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()

    val song = playbackState.currentSong

    if (song == null) {
        onNavigateBack()
        return
    }

    // Analyze song for waveform and beats when song changes
    LaunchedEffect(song.id) {
        viewModel.analyzeCurrentSong()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        UltraGradientStart.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Main content area - Split Screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // ==================== LEFT PANEL: NOW PLAYING ====================
                    Column(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Compact Album Art
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.albumArtUri != null) {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = "Album art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Song Info
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${song.artist} • ${song.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Speed/Pitch indicator
                        if (playbackState.speed != 1.0f || playbackState.pitch != 0f) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                if (playbackState.speed != 1.0f) {
                                    Text(
                                        text = "${String.format("%.2f", playbackState.speed)}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (playbackState.speed != 1.0f && playbackState.pitch != 0f) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                if (playbackState.pitch != 0f) {
                                    val sign = if (playbackState.pitch > 0) "+" else ""
                                    Text(
                                        text = "${sign}${String.format("%.1f", playbackState.pitch)} st",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Waveform Visualizer
                        if (waveformData.isNotEmpty()) {
                            WaveformVisualizer(
                                waveformData = waveformData,
                                currentPosition = playbackState.progress,
                                durationMs = playbackState.duration,
                                loopStartMs = playbackState.abLoopStart,
                                loopEndMs = playbackState.abLoopEnd,
                                isLooping = playbackState.isLooping,
                                onSeek = { viewModel.seekToPercent(it) },
                                onLoopStartChange = { viewModel.setWaveformLoopStart(it) },
                                onLoopEndChange = { viewModel.setWaveformLoopEnd(it) },
                                onClearLoop = { viewModel.clearWaveformLoop() },
                                beatMarkers = beatMarkers,
                                estimatedBpm = estimatedBpm,
                                showBeatMarkers = true,
                                modifier = Modifier.fillMaxWidth(),
                                height = 80.dp
                            )
                        } else {
                            // Progress info while loading
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isExtractingWaveform) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Analyzing...",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${playbackState.positionFormatted} / ${playbackState.durationFormatted}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Main Playback Controls (compact)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle
                            IconButton(
                                onClick = { viewModel.toggleShuffle() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (playbackState.isShuffling)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Previous
                            IconButton(
                                onClick = { viewModel.playPrevious() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Play/Pause
                            FilledIconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying)
                                        Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            // Next
                            IconButton(
                                onClick = { viewModel.playNext() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Repeat
                            IconButton(
                                onClick = { viewModel.toggleLoop() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isLooping)
                                        Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    contentDescription = "Repeat",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (playbackState.isLooping)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Divider between panels
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    // ==================== RIGHT PANEL: QUEUE + FOLDERS ====================
                    RightPanel(
                        queue = queue,
                        currentSongId = song.id,
                        currentFolderPath = currentFolderPath,
                        breadcrumbs = breadcrumbs,
                        browseItems = browseItems,
                        onPlayFromQueue = { index -> viewModel.playFromPlaylistIndex(index) },
                        onRemoveFromQueue = { index -> viewModel.removeFromPlaylist(index) },
                        onNavigateToPath = { path -> viewModel.navigateToPath(path) },
                        onNavigateUp = { viewModel.navigateUp() },
                        onPlaySong = { song -> viewModel.playSongFromFolder(song) },
                        onAddToQueue = { song -> viewModel.addToPlaylistEnd(song) },
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                    )
                }

                // ==================== BOTTOM: UNIFIED CONTROLS ====================
                UnifiedControlsPanel(
                    // Speed & Pitch
                    speed = playbackState.speed,
                    pitch = playbackState.pitch,
                    onSpeedChange = { viewModel.setSpeed(it) },
                    onPitchChange = { viewModel.setPitch(it) },
                    onResetSpeed = { viewModel.resetSpeed() },
                    onResetPitch = { viewModel.resetPitch() },
                    onResetAll = { viewModel.resetAll() },
                    // A-B Loop
                    abLoopStart = playbackState.abLoopStart,
                    abLoopEnd = playbackState.abLoopEnd,
                    currentPosition = playbackState.currentPosition,
                    duration = playbackState.duration,
                    onSetLoopStart = { viewModel.setABLoopStart() },
                    onSetLoopEnd = { viewModel.setABLoopEnd() },
                    onClearLoop = { viewModel.clearABLoop() },
                    onSaveToArmory = {
                        playbackState.currentSong?.let { currentSong ->
                            val start = playbackState.abLoopStart ?: 0
                            val end = playbackState.abLoopEnd ?: currentSong.duration
                            viewModel.saveClipToArmory(currentSong, start, end)
                        }
                    },
                    // Presets
                    presets = AudioPreset.PRESETS,
                    selectedPreset = uiState.selectedPreset,
                    onPresetSelected = { viewModel.applyPreset(it) }
                )
            }
        }
    }
}

/**
 * Right panel with tabbed Queue and Folders browser
 */
@Composable
private fun RightPanel(
    queue: List<com.ultramusic.player.data.Song>,
    currentSongId: Long,
    currentFolderPath: String,
    breadcrumbs: List<Pair<String, String>>,
    browseItems: List<com.ultramusic.player.data.BrowseItem>,
    onPlayFromQueue: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onNavigateToPath: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onPlaySong: (com.ultramusic.player.data.Song) -> Unit,
    onAddToQueue: (com.ultramusic.player.data.Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Queue", "Folders")

    Column(modifier = modifier) {
        // Tab row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.QueueMusic else Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(title, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                )
            }
        }

        // Tab content
        when (selectedTab) {
            0 -> QueuePanel(
                queue = queue,
                currentSongId = currentSongId,
                onPlaySong = onPlayFromQueue,
                onRemoveSong = onRemoveFromQueue,
                modifier = Modifier.fillMaxSize()
            )
            1 -> CompactFolderPanel(
                currentPath = currentFolderPath,
                breadcrumbs = breadcrumbs,
                browseItems = browseItems,
                currentSongId = currentSongId,
                onNavigateToPath = onNavigateToPath,
                onNavigateUp = onNavigateUp,
                onPlaySong = onPlaySong,
                onAddToQueue = onAddToQueue,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
