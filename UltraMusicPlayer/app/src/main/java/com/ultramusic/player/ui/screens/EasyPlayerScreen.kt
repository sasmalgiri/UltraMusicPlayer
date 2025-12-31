package com.ultramusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ultramusic.player.data.ActivePlaylist
import com.ultramusic.player.data.MatchType
import com.ultramusic.player.data.PlaybackState
import com.ultramusic.player.data.Song
import com.ultramusic.player.data.SongMatch
import com.ultramusic.player.audio.AudioEngineType
import com.ultramusic.player.ui.MainViewModel
import kotlin.math.sin
import kotlin.random.Random

/**
 * Easy Player Screen
 * 
 * A unified, simple UI with:
 * 1. Quick search bar at top
 * 2. Active playlist with track completion
 * 3. Now playing with large controls
 * 4. Waveform visualization with A-B loop markers
 * 5. Quick enhancement buttons
 * 6. Quick access to Battle features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EasyPlayerScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBattleLibrary: () -> Unit = {},
    onNavigateToBattleHQ: () -> Unit = {},
    onNavigateToActiveBattle: () -> Unit = {},
    onNavigateToCounterSong: () -> Unit = {},
    onNavigateToBattleAnalyzer: () -> Unit = {},
    onNavigateToBattleArmory: () -> Unit = {}
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val playlist by viewModel.activePlaylist.collectAsState()
    val searchState by viewModel.playlistSearchState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Audio Enhancement States
    val bassLevel by viewModel.battleBassLevel.collectAsState()
    val loudnessGain by viewModel.battleLoudness.collectAsState()
    val clarityLevel by viewModel.battleClarity.collectAsState()
    val spatialLevel by viewModel.battleSpatial.collectAsState()

    // Safe Mode (FULL SEND toggle)
    val safeMode by viewModel.safeMode.collectAsState()

    // Audio Engine selection
    val audioEngine by viewModel.audioEngine.collectAsState()

    // Hardware Protection & Audiophile Mode
    val hardwareProtection by viewModel.hardwareProtection.collectAsState()
    val audiophileMode by viewModel.audiophileMode.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ===== TOP: Search Bar =====
        QuickSearchBar(
            query = searchQuery,
            isActive = isSearchActive,
            onQueryChange = { 
                searchQuery = it
                viewModel.updatePlaylistSearchQuery(it)
            },
            onActiveChange = { isSearchActive = it },
            onVoiceSearch = { viewModel.startVoiceSearch() },
            searchResults = searchState.allMatches,
            onAddSong = { song, playNext ->
                viewModel.addToPlaylistFromSearch(song, playNext)
                searchQuery = ""
                isSearchActive = false
            }
        )
        
        // ===== BATTLE QUICK ACCESS =====
        BattleQuickAccess(
            onBattleLibrary = onNavigateToBattleLibrary,
            onBattleHQ = onNavigateToBattleHQ,
            onActiveBattle = onNavigateToActiveBattle,
            onCounterSong = onNavigateToCounterSong,
            onBattleAnalyzer = onNavigateToBattleAnalyzer,
            onBattleArmory = onNavigateToBattleArmory
        )
        
        // ===== MIDDLE: Playlist with completion =====
        ActivePlaylistSection(
            playlist = playlist,
            currentSong = playbackState.currentSong,
            progress = if (playbackState.duration > 0) 
                playbackState.position.toFloat() / playbackState.duration else 0f,
            onSongClick = { index -> viewModel.playFromPlaylistIndex(index) },
            onRemoveSong = { index -> viewModel.removeFromPlaylist(index) },
            modifier = Modifier.weight(1f)
        )
        
        // ===== BOTTOM: Now Playing + Waveform + Controls =====
        NowPlayingSection(
            playbackState = playbackState,
            onPlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.playlistNext() },
            onPrevious = { viewModel.playlistPrevious() },
            onSeek = { viewModel.seekTo(it) },
            onSetLoopA = { viewModel.setLoopStart() },
            onSetLoopB = { viewModel.setLoopEnd() },
            onClearLoop = { viewModel.clearLoop() },
            onSpeedChange = { viewModel.setSpeed(it) },
            onPitchChange = { viewModel.setPitch(it) },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onToggleRepeat = { viewModel.toggleRepeat() },
            // Audio Enhancement
            bassLevel = bassLevel,
            loudnessGain = loudnessGain,
            clarityLevel = clarityLevel,
            spatialLevel = spatialLevel,
            onBassChange = { viewModel.setBattleBass(it) },
            onLoudnessChange = { viewModel.setBattleLoudness(it) },
            onClarityChange = { viewModel.setBattleClarity(it) },
            onSpatialChange = { viewModel.setBattleSpatial(it) },
            onResetAudioEffects = { viewModel.resetBattleAudioEffects() },
            // Safe Mode & Engine Selection
            safeMode = safeMode,
            audioEngine = audioEngine,
            hardwareProtection = hardwareProtection,
            audiophileMode = audiophileMode,
            onSafeModeChange = { viewModel.setSafeMode(it) },
            onAudioEngineChange = { viewModel.setAudioEngine(it) },
            onHardwareProtectionChange = { viewModel.setHardwareProtection(it) },
            onAudiophileModeChange = { viewModel.setAudiophileMode(it) }
        )
    }
}

// ==================== QUICK SEARCH BAR ====================

@Composable
private fun QuickSearchBar(
    query: String,
    isActive: Boolean,
    onQueryChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onVoiceSearch: () -> Unit,
    searchResults: List<SongMatch>,
    onAddSong: (Song, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Search input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    onActiveChange(it.isNotEmpty())
                },
                placeholder = { 
                    Text(
                        "🔍 Search to add songs...",
                        style = MaterialTheme.typography.bodyMedium
                    ) 
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    Row {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { 
                                onQueryChange("")
                                onActiveChange(false)
                            }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                        IconButton(onClick = onVoiceSearch) {
                            Icon(
                                Icons.Default.Mic, 
                                "Voice search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }
        
        // Search results dropdown
        AnimatedVisibility(
            visible = isActive && searchResults.isNotEmpty(),
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(searchResults.take(5)) { match ->
                    SearchResultRow(
                        match = match,
                        onAdd = { onAddSong(match.song, false) },
                        onPlayNext = { onAddSong(match.song, true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    match: SongMatch,
    onAdd: () -> Unit,
    onPlayNext: () -> Unit
) {
    var added by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { 
                onAdd()
                added = true
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Match indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when (match.matchType) {
                        MatchType.EXACT, MatchType.STRONG -> Color(0xFF4CAF50)
                        MatchType.GOOD -> Color(0xFFFF9800)
                        else -> Color(0xFF9E9E9E)
                    }
                )
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = match.song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = match.song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        
        // Action buttons
        if (added) {
            Icon(
                Icons.Default.Check,
                "Added",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            IconButton(onClick = onPlayNext, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Queue, "Play next", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { onAdd(); added = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
    
    LaunchedEffect(added) {
        if (added) {
            kotlinx.coroutines.delay(1500)
            added = false
        }
    }
}

// ==================== ACTIVE PLAYLIST SECTION ====================

@Composable
private fun ActivePlaylistSection(
    playlist: ActivePlaylist,
    currentSong: Song?,
    progress: Float,
    onSongClick: (Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Queue,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Up Next",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${playlist.size} songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (playlist.isEmpty) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🎵",
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Search above to add songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Playlist items
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                itemsIndexed(playlist.queue) { index, song ->
                    val isCurrentSong = index == playlist.currentIndex
                    val isPastSong = index < playlist.currentIndex
                    
                    PlaylistTrackItem(
                        song = song,
                        index = index,
                        isPlaying = isCurrentSong,
                        isPast = isPastSong,
                        progress = if (isCurrentSong) progress else if (isPastSong) 1f else 0f,
                        onClick = { onSongClick(index) },
                        onRemove = { onRemoveSong(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistTrackItem(
    song: Song,
    index: Int,
    isPlaying: Boolean,
    isPast: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPlaying -> MaterialTheme.colorScheme.primaryContainer
            isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        label = "bg"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 4.dp else 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Index / Playing indicator
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        // Animated playing indicator
                        PlayingIndicator()
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isPast) 
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
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.albumArtUri != null) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, contentDescription = "No album art")
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Song info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isPlaying -> MaterialTheme.colorScheme.primary
                            isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isPast) 0.4f else 0.7f
                        ),
                        maxLines = 1
                    )
                }
                
                // Duration / Progress
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = song.durationFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isPlaying || isPast) {
                        Text(
                            text = if (isPast) "✓" else "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPast) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Remove button (swipe alternative)
                if (!isPlaying && !isPast) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            "Remove",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            // Progress bar for current song
            if (isPlaying) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun PlayingIndicator() {
    val bars = remember { listOf(0.3f, 0.7f, 0.5f, 0.9f) }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(20.dp)
    ) {
        bars.forEachIndexed { index, baseHeight ->
            val animatedHeight by animateFloatAsState(
                targetValue = baseHeight + Random.nextFloat() * 0.3f,
                animationSpec = tween(300 + index * 100),
                label = "bar$index"
            )
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ==================== NOW PLAYING SECTION ====================

@Composable
private fun NowPlayingSection(
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onSetLoopA: () -> Unit,
    onSetLoopB: () -> Unit,
    onClearLoop: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    // Audio Enhancement
    bassLevel: Int = 500,
    loudnessGain: Int = 0,
    clarityLevel: Int = 50,
    spatialLevel: Int = 500,
    onBassChange: (Int) -> Unit = {},
    onLoudnessChange: (Int) -> Unit = {},
    onClarityChange: (Int) -> Unit = {},
    onSpatialChange: (Int) -> Unit = {},
    onResetAudioEffects: () -> Unit = {},
    // Safe Mode & Engine Selection
    safeMode: Boolean = true,
    audioEngine: AudioEngineType = AudioEngineType.SOUNDTOUCH,
    hardwareProtection: Boolean = true,
    audiophileMode: Boolean = false,
    onSafeModeChange: (Boolean) -> Unit = {},
    onAudioEngineChange: (AudioEngineType) -> Unit = {},
    onHardwareProtectionChange: (Boolean) -> Unit = {},
    onAudiophileModeChange: (Boolean) -> Unit = {}
) {
    val currentSong = playbackState.currentSong
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Song info row
            if (currentSong != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album art
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentSong.albumArtUri != null) {
                            AsyncImage(
                                model = currentSong.albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                null,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Title and artist
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    
                    // Speed/Pitch indicator
                    if (playbackState.speed != 1f || playbackState.pitch != 0f) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "${playbackState.speed}x",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Waveform with A-B markers
            WaveformWithABMarkers(
                songPath = playbackState.currentSong?.path,
                progress = if (playbackState.duration > 0) 
                    playbackState.position.toFloat() / playbackState.duration else 0f,
                duration = playbackState.duration,
                position = playbackState.position,
                loopStartPosition = playbackState.loopStartPosition,
                loopEndPosition = playbackState.loopEndPosition,
                onSeek = { progress ->
                    onSeek((progress * playbackState.duration).toLong())
                },
                onSetLoopA = onSetLoopA,
                onSetLoopB = onSetLoopB,
                onClearLoop = onClearLoop
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Main controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Default.Shuffle,
                        "Shuffle",
                        tint = if (playbackState.isShuffleEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Previous
                FilledTonalIconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, "Previous")
                }
                
                // Play/Pause (large)
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Next
                FilledTonalIconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, "Next")
                }
                
                // Repeat
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        if (playbackState.repeatMode == 2) Icons.Default.RepeatOne 
                        else Icons.Default.Repeat,
                        "Repeat",
                        tint = if (playbackState.repeatMode > 0)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Quick enhancements
            QuickEnhancementsRow(
                speed = playbackState.speed,
                pitch = playbackState.pitch,
                hasLoop = playbackState.loopStartPosition != null,
                onSpeedChange = onSpeedChange,
                onPitchChange = onPitchChange,
                onClearLoop = onClearLoop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Enhancement Controls (Bass, Loudness, etc.)
            AudioEnhancementRow(
                bassLevel = bassLevel,
                loudnessGain = loudnessGain,
                clarityLevel = clarityLevel,
                spatialLevel = spatialLevel,
                safeMode = safeMode,
                audioEngine = audioEngine,
                hardwareProtection = hardwareProtection,
                audiophileMode = audiophileMode,
                onBassChange = onBassChange,
                onLoudnessChange = onLoudnessChange,
                onClarityChange = onClarityChange,
                onSpatialChange = onSpatialChange,
                onSafeModeChange = onSafeModeChange,
                onAudioEngineChange = onAudioEngineChange,
                onHardwareProtectionChange = onHardwareProtectionChange,
                onAudiophileModeChange = onAudiophileModeChange,
                onResetAll = onResetAudioEffects
            )
        }
    }
}

// ==================== AUDIO ENHANCEMENT ROW ====================

@Composable
private fun AudioEnhancementRow(
    bassLevel: Int,
    loudnessGain: Int,
    clarityLevel: Int,
    spatialLevel: Int,
    safeMode: Boolean,
    audioEngine: AudioEngineType,
    hardwareProtection: Boolean,
    audiophileMode: Boolean,
    onBassChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onClarityChange: (Int) -> Unit,
    onSpatialChange: (Int) -> Unit,
    onSafeModeChange: (Boolean) -> Unit,
    onAudioEngineChange: (AudioEngineType) -> Unit,
    onHardwareProtectionChange: (Boolean) -> Unit,
    onAudiophileModeChange: (Boolean) -> Unit,
    onResetAll: () -> Unit
) {
    var showAdvanced by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header with expand toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showAdvanced = !showAdvanced }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔊", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Audio Enhancement",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Safe Mode Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = { onSafeModeChange(!safeMode) })
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = if (safeMode) "Safe" else "FULL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (safeMode) Color(0xFF4CAF50) else Color(0xFFFF5722)
                    )
                    Switch(
                        checked = !safeMode, // Inverted: OFF = Safe, ON = FULL SEND
                        onCheckedChange = { onSafeModeChange(!it) },
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF5722),
                            checkedTrackColor = Color(0xFFFF5722).copy(alpha = 0.5f),
                            uncheckedThumbColor = Color(0xFF4CAF50),
                            uncheckedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                        )
                    )
                }

                // Show bass level indicator
                if (bassLevel > 0) {
                    Surface(
                        color = Color(0xFFE91E63).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "🎸 ${bassLevel / 10}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE91E63),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (showAdvanced) "▼" else "▶",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick preset buttons (always visible)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                AudioQuickButton("🔥 Max Bass", bassLevel >= 900) {
                    onBassChange(1000)
                }
            }
            item {
                AudioQuickButton("💥 Loud", loudnessGain >= 800) {
                    onLoudnessChange(1000)
                }
            }
            item {
                AudioQuickButton("⚡ Power", bassLevel >= 800 && loudnessGain >= 800) {
                    onBassChange(1000)
                    onLoudnessChange(1000)
                }
            }
            item {
                AudioQuickButton("🎯 Balanced", bassLevel == 500 && loudnessGain == 0) {
                    onBassChange(500)
                    onLoudnessChange(0)
                    onClarityChange(50)
                    onSpatialChange(500)
                }
            }
            item {
                AudioQuickButton("✨ Clear", clarityLevel >= 80) {
                    onClarityChange(100)
                }
            }
            item {
                AudioQuickButton("🎧 3D", spatialLevel >= 800) {
                    onSpatialChange(1000)
                }
            }
        }

        // Advanced controls (expandable)
        AnimatedVisibility(
            visible = showAdvanced,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Audio Engine Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Engine:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AudioEngineType.entries.forEach { engine ->
                        FilterChip(
                            selected = audioEngine == engine,
                            onClick = { onAudioEngineChange(engine) },
                            label = {
                                Text(
                                    text = when (engine) {
                                        AudioEngineType.SOUNDTOUCH -> "ST"
                                        AudioEngineType.SUPERPOWERED -> "SP"
                                        AudioEngineType.RUBBERBAND -> "RB"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(28.dp),
                            leadingIcon = if (audioEngine == engine) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                    Text(
                        text = audioEngine.quality,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                // Safety & Quality Mode Toggles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hardware Protection Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onHardwareProtectionChange(!hardwareProtection) }
                            .background(
                                color = if (hardwareProtection) Color(0xFF1B5E20).copy(alpha = 0.2f)
                                       else Color(0xFFB71C1C).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (hardwareProtection) "🛡️" else "⚠️",
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HW Protect",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hardwareProtection) Color(0xFF4CAF50) else Color(0xFFFF5722)
                            )
                            Text(
                                text = if (hardwareProtection) "Speaker Safe" else "No Limit!",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hardwareProtection,
                            onCheckedChange = onHardwareProtectionChange,
                            modifier = Modifier.height(20.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF4CAF50),
                                checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f),
                                uncheckedThumbColor = Color(0xFFFF5722),
                                uncheckedTrackColor = Color(0xFFFF5722).copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Audiophile Mode Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAudiophileModeChange(!audiophileMode) }
                            .background(
                                color = if (audiophileMode) Color(0xFF0D47A1).copy(alpha = 0.2f)
                                       else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (audiophileMode) "🎼" else "🎵",
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Audiophile",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (audiophileMode) Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (audiophileMode) "Pure Quality" else "Battle Mode",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = audiophileMode,
                            onCheckedChange = onAudiophileModeChange,
                            modifier = Modifier.height(20.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF2196F3),
                                checkedTrackColor = Color(0xFF2196F3).copy(alpha = 0.5f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }

                // Bass slider
                AudioMiniSlider(
                    label = "🎸 Bass",
                    value = bassLevel,
                    maxValue = 1000,
                    color = Color(0xFFE91E63),
                    onValueChange = onBassChange
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Loudness slider
                AudioMiniSlider(
                    label = "📢 Loudness",
                    value = loudnessGain,
                    maxValue = 1000,
                    color = Color(0xFFFF5722),
                    onValueChange = onLoudnessChange
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Clarity slider
                AudioMiniSlider(
                    label = "✨ Clarity",
                    value = clarityLevel,
                    maxValue = 100,
                    color = Color(0xFF2196F3),
                    onValueChange = onClarityChange
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Spatial slider
                AudioMiniSlider(
                    label = "🎧 Spatial",
                    value = spatialLevel,
                    maxValue = 1000,
                    color = Color(0xFF9C27B0),
                    onValueChange = onSpatialChange
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Reset button
                if (bassLevel != 500 || loudnessGain != 0 || clarityLevel != 50 || spatialLevel != 500) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            onClick = onResetAll,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Reset All",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioMiniSlider(
    label: String,
    value: Int,
    maxValue: Int,
    color: Color,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(80.dp)
        )

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..maxValue.toFloat(),
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )

        Text(
            text = if (maxValue == 100) "$value%" else "${value / 10}%",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun AudioQuickButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text, fontSize = 11.sp) }
    )
}

// ==================== WAVEFORM WITH A-B MARKERS ====================

@Composable
private fun WaveformWithABMarkers(
    songPath: String?,
    progress: Float,
    duration: Long,
    position: Long,
    loopStartPosition: Long?,
    loopEndPosition: Long?,
    onSeek: (Float) -> Unit,
    onSetLoopA: () -> Unit,
    onSetLoopB: () -> Unit,
    onClearLoop: () -> Unit
) {
    val loopColor = MaterialTheme.colorScheme.tertiary
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(position),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // A-B Loop status
            if (loopStartPosition != null || loopEndPosition != null) {
                Text(
                    text = "🔁 Loop: ${formatTime(loopStartPosition ?: 0)} - ${formatTime(loopEndPosition ?: duration)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = loopColor
                )
            }
            
            Text(
                text = formatTime(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Real waveform visualization
        com.ultramusic.player.ui.components.WaveformView(
            audioPath = songPath,
            currentPosition = position,
            duration = duration,
            abLoopStart = loopStartPosition,
            abLoopEnd = loopEndPosition,
            onSeek = { seekPos ->
                if (duration > 0) {
                    onSeek(seekPos.toFloat() / duration)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            waveformColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            progressColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.surface,
            loopColor = loopColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // A-B Loop buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Set A button
            FilledTonalButton(
                onClick = onSetLoopA,
                modifier = Modifier.width(80.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (loopStartPosition != null) "A ✓" else "Set A",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Set B button
            FilledTonalButton(
                onClick = onSetLoopB,
                modifier = Modifier.width(80.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (loopEndPosition != null) "B ✓" else "Set B",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Clear loop button
            if (loopStartPosition != null || loopEndPosition != null) {
                FilledTonalButton(
                    onClick = onClearLoop,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ==================== QUICK ENHANCEMENTS ====================

@Composable
private fun QuickEnhancementsRow(
    speed: Float,
    pitch: Float,
    hasLoop: Boolean,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onClearLoop: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        // Speed presets
        item {
            Text(
                text = "Speed:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp, top = 8.dp)
            )
        }
        
        val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        items(speedPresets) { preset ->
            FilterChip(
                selected = speed == preset,
                onClick = { onSpeedChange(preset) },
                label = { Text("${preset}x") }
            )
        }
        
        // Divider
        item { Spacer(modifier = Modifier.width(8.dp)) }
        
        // Pitch presets
        item {
            Text(
                text = "Pitch:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp, top = 8.dp)
            )
        }
        
        val pitchPresets = listOf(-12f, -6f, 0f, 6f, 12f)
        items(pitchPresets) { preset ->
            FilterChip(
                selected = pitch == preset,
                onClick = { onPitchChange(preset) },
                label = { 
                    Text(
                        if (preset == 0f) "0" 
                        else if (preset > 0) "+${preset.toInt()}" 
                        else "${preset.toInt()}"
                    ) 
                }
            )
        }
        
        // Quick presets
        item { Spacer(modifier = Modifier.width(8.dp)) }
        
        item {
            FilterChip(
                selected = speed == 1.25f && pitch == 4f,
                onClick = { 
                    onSpeedChange(1.25f)
                    onPitchChange(4f)
                },
                label = { Text("🌙 Nightcore") }
            )
        }
        
        item {
            FilterChip(
                selected = speed == 0.85f && pitch == -2f,
                onClick = { 
                    onSpeedChange(0.85f)
                    onPitchChange(-2f)
                },
                label = { Text("🎧 Slowed") }
            )
        }
    }
}

// ==================== UTILITIES ====================

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

// ==================== BATTLE QUICK ACCESS ====================

@Composable
private fun BattleQuickAccess(
    onBattleLibrary: () -> Unit,
    onBattleHQ: () -> Unit,
    onActiveBattle: () -> Unit,
    onCounterSong: () -> Unit,
    onBattleAnalyzer: () -> Unit = {},
    onBattleArmory: () -> Unit = {}
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        item {
            BattleQuickButton(
                emoji = "👂",
                label = "ANALYZE",
                color = Color(0xFF4CAF50),
                onClick = onBattleAnalyzer
            )
        }
        item {
            BattleQuickButton(
                emoji = "🎯",
                label = "ARMORY",
                color = Color(0xFFE91E63),
                onClick = onBattleArmory
            )
        }
        item {
            BattleQuickButton(
                emoji = "📚",
                label = "Library",
                color = Color(0xFF2196F3),
                onClick = onBattleLibrary
            )
        }
        item {
            BattleQuickButton(
                emoji = "🎛️",
                label = "Battle HQ",
                color = Color(0xFFFF5722),
                onClick = onBattleHQ
            )
        }
        item {
            BattleQuickButton(
                emoji = "⚔️",
                label = "Active",
                color = Color(0xFF9C27B0),
                onClick = onActiveBattle
            )
        }
        item {
            BattleQuickButton(
                emoji = "🧠",
                label = "AI Counter",
                color = Color(0xFF607D8B),
                onClick = onCounterSong
            )
        }
    }
}

@Composable
private fun BattleQuickButton(
    emoji: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
