package com.ultramusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ultramusic.player.audio.BeatMarker
import com.ultramusic.player.data.AudioPreset
import com.ultramusic.player.ui.MainViewModel
import com.ultramusic.player.ui.components.PresetPanel
import com.ultramusic.player.ui.components.SpeedPitchControl
import com.ultramusic.player.ui.components.WaveformVisualizer
import com.ultramusic.player.ui.theme.UltraGradientEnd
import com.ultramusic.player.ui.theme.UltraGradientStart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    // Waveform and beat detection state
    val waveformData by viewModel.currentWaveform.collectAsState()
    val beatMarkers by viewModel.currentBeatMarkers.collectAsState()
    val estimatedBpm by viewModel.estimatedBpm.collectAsState()
    val isExtractingWaveform by viewModel.isExtractingWaveform.collectAsState()

    // Show waveform panel state
    var showWaveformPanel by remember { mutableStateOf(true) }

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
                        Column {
                            Text(
                                text = "Now Playing",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
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
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Album Art
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
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
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Song Info
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Speed/Pitch indicator
                if (playbackState.speed != 1.0f || playbackState.pitch != 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (playbackState.speed != 1.0f) {
                            Text(
                                text = "${String.format("%.2f", playbackState.speed)}x",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (playbackState.speed != 1.0f && playbackState.pitch != 0f) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (playbackState.pitch != 0f) {
                            val sign = if (playbackState.pitch > 0) "+" else ""
                            Text(
                                text = "${sign}${String.format("%.1f", playbackState.pitch)} st",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Waveform Visualizer with A-B Loop and Beat Markers
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
                        height = 120.dp
                    )
                } else {
                    // Fallback to simple slider while loading
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isExtractingWaveform) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyzing waveform...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Slider(
                            value = playbackState.progress,
                            onValueChange = { viewModel.seekToPercent(it) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = playbackState.positionFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = playbackState.durationFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Main Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = { viewModel.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffling) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Previous
                    IconButton(
                        onClick = { viewModel.playPrevious() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    // Play/Pause
                    FilledIconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) 
                                Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    // Next
                    IconButton(
                        onClick = { viewModel.playNext() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    // Repeat
                    IconButton(onClick = { viewModel.toggleLoop() }) {
                        Icon(
                            imageVector = if (playbackState.isLooping) 
                                Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (playbackState.isLooping) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Speed/Pitch/Preset Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Speed/Pitch button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleSpeedPitchPanel() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (uiState.showSpeedPitchPanel)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed & Pitch",
                                tint = if (uiState.showSpeedPitchPanel)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Speed/Pitch",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Presets button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = { viewModel.togglePresetPanel() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (uiState.showPresetPanel)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Presets",
                                tint = if (uiState.showPresetPanel)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Presets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // A-B Loop button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleABLoopPanel() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (uiState.showABLoopPanel || 
                                        (playbackState.abLoopStart != null && playbackState.abLoopEnd != null))
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = "A-B Loop",
                                tint = if (uiState.showABLoopPanel ||
                                    (playbackState.abLoopStart != null && playbackState.abLoopEnd != null))
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "A-B Loop",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
            }
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
        
        // A-B Loop Panel
        AnimatedVisibility(
            visible = uiState.showABLoopPanel,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val detectedClips by viewModel.detectedClips.collectAsState()
            val isDetecting by viewModel.isDetectingClips.collectAsState()
            
            ABLoopPanel(
                abLoopStart = playbackState.abLoopStart,
                abLoopEnd = playbackState.abLoopEnd,
                currentPosition = playbackState.currentPosition,
                duration = playbackState.duration,
                detectedClips = detectedClips,
                isDetecting = isDetecting,
                onSetStart = { viewModel.setABLoopStart() },
                onSetEnd = { viewModel.setABLoopEnd() },
                onClear = { viewModel.clearABLoop() },
                onAutoDetect = { viewModel.autoDetectClips() },
                onSelectClip = { clip -> viewModel.setABFromDetectedClip(clip) },
                onSaveClip = { clip -> viewModel.saveDetectedClipToArmory(clip) },
                onPreviewClip = { clip -> viewModel.setABFromDetectedClip(clip) },
                onSaveAllClips = { viewModel.autoDetectAndSaveAllClips() },
                onClose = { viewModel.toggleABLoopPanel() },
                onSaveToArmory = {
                    // Save current A-B loop as counter clip
                    playbackState.currentSong?.let { song ->
                        val start = playbackState.abLoopStart ?: 0
                        val end = playbackState.abLoopEnd ?: song.duration
                        viewModel.saveClipToArmory(song, start, end)
                    }
                },
                onSetManualTime = { startMs, endMs ->
                    viewModel.setManualLoopPoints(startMs, endMs)
                }
            )
        }
    }
}

@Composable
private fun ABLoopPanel(
    abLoopStart: Long?,
    abLoopEnd: Long?,
    currentPosition: Long,
    duration: Long,
    detectedClips: List<com.ultramusic.player.core.DetectedClip> = emptyList(),
    isDetecting: Boolean = false,
    onSetStart: () -> Unit,
    onSetEnd: () -> Unit,
    onClear: () -> Unit,
    onAutoDetect: () -> Unit = {},
    onSelectClip: (com.ultramusic.player.core.DetectedClip) -> Unit = {},
    onSaveClip: (com.ultramusic.player.core.DetectedClip) -> Unit = {},
    onSaveToArmory: () -> Unit = {},
    onPreviewClip: (com.ultramusic.player.core.DetectedClip) -> Unit = {},
    onSaveAllClips: () -> Unit = {},
    onClose: () -> Unit = {},
    onSetManualTime: (startMs: Long, endMs: Long) -> Unit = { _, _ -> }
) {
    // Track selected clips for batch operations
    var selectedClipIds by remember { mutableStateOf(setOf<String>()) }
    var showEditDialog by remember { mutableStateOf<com.ultramusic.player.core.DetectedClip?>(null) }

    // Time input dialog state
    var showTimeInputForA by remember { mutableStateOf(false) }
    var showTimeInputForB by remember { mutableStateOf(false) }
    
    fun formatTime(ms: Long): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return "%d:%02d".format(minutes, seconds)
    }
    
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with close button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 A-B Loop & Clip Detection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // AUTO-DETECT SECTION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "🤖 AUTO MODE",
                            color = Color(0xFF9C27B0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Auto-detect button
                            androidx.compose.material3.Button(
                                onClick = onAutoDetect,
                                enabled = !isDetecting,
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF9C27B0)
                                )
                            ) {
                                if (isDetecting) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyzing...", fontSize = 12.sp)
                                } else {
                                    Text("🔍 AUTO DETECT", fontSize = 12.sp)
                                }
                            }
                            
                            // Save all button (when clips detected)
                            if (detectedClips.isNotEmpty()) {
                                androidx.compose.material3.Button(
                                    onClick = onSaveAllClips,
                                    modifier = Modifier.weight(1f),
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50)
                                    )
                                ) {
                                    Text("💾 SAVE ALL (${detectedClips.size})", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // DETECTED CLIPS LIST
            if (detectedClips.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Found ${detectedClips.size} clips:",
                            color = Color(0xFF4CAF50),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Select all / Deselect all
                        Row {
                            androidx.compose.material3.TextButton(
                                onClick = { 
                                    selectedClipIds = detectedClips.map { "${it.songId}_${it.startMs}" }.toSet() 
                                }
                            ) {
                                Text("Select All", fontSize = 11.sp)
                            }
                            androidx.compose.material3.TextButton(
                                onClick = { selectedClipIds = emptySet() }
                            ) {
                                Text("Clear", fontSize = 11.sp)
                            }
                        }
                    }
                }
                
                items(detectedClips) { clip ->
                    val clipId = "${clip.songId}_${clip.startMs}"
                    val isSelected = clipId in selectedClipIds
                    
                    EnhancedClipItem(
                        clip = clip,
                        isSelected = isSelected,
                        onToggleSelect = {
                            selectedClipIds = if (isSelected) {
                                selectedClipIds - clipId
                            } else {
                                selectedClipIds + clipId
                            }
                        },
                        onPreview = { onSelectClip(clip) }, // Preview = set A-B and seek
                        onSave = { onSaveClip(clip) },
                        onEdit = { showEditDialog = clip }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                
                // Batch actions for selected
                if (selectedClipIds.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    detectedClips
                                        .filter { "${it.songId}_${it.startMs}" in selectedClipIds }
                                        .forEach { onSaveClip(it) }
                                    selectedClipIds = emptySet()
                                },
                                modifier = Modifier.weight(1f),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE91E63)
                                )
                            ) {
                                Text("Save Selected (${selectedClipIds.size})", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            
            // MANUAL SECTION
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "✋ MANUAL MODE",
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Time display row - tappable to edit manually
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // A time display with edit
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("A", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                                        .clickable { showTimeInputForA = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = abLoopStart?.let { formatTime(it) } ?: "--:--",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit A time",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Current position indicator
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("NOW", color = Color.Gray, fontSize = 10.sp)
                                Text(
                                    text = formatTime(currentPosition),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }

                            // B time display with edit
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("B", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFF333333), RoundedCornerShape(8.dp))
                                        .clickable { showTimeInputForB = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = abLoopEnd?.let { formatTime(it) } ?: "--:--",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit B time",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick set buttons - set at current playback position
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Set A at current position
                            androidx.compose.material3.Button(
                                onClick = onSetStart,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (abLoopStart != null)
                                        Color(0xFF4CAF50) else Color(0xFF555555)
                                ),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Text("Set A Here", fontSize = 12.sp)
                            }

                            // Set B at current position
                            androidx.compose.material3.Button(
                                onClick = onSetEnd,
                                enabled = abLoopStart != null,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (abLoopEnd != null)
                                        Color(0xFFE91E63) else Color(0xFF555555)
                                ),
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            ) {
                                Text("Set B Here", fontSize = 12.sp)
                            }
                        }

                        // Loop info
                        if (abLoopStart != null && abLoopEnd != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Loop: ${formatTime(abLoopEnd - abLoopStart)} duration",
                                color = Color(0xFF81C784),
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
            
            // ACTION BUTTONS
            item {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Save to Armory button (when both A and B are set)
                if (abLoopStart != null && abLoopEnd != null) {
                    androidx.compose.material3.Button(
                        onClick = onSaveToArmory,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE91E63)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🎯 SAVE TO BATTLE ARMORY")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Clear button
                androidx.compose.material3.OutlinedButton(
                    onClick = onClear,
                    enabled = abLoopStart != null || abLoopEnd != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Loop")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
    
    // Edit Dialog
    showEditDialog?.let { clip ->
        EditClipDialog(
            clip = clip,
            onDismiss = { showEditDialog = null },
            onSave = { editedClip ->
                onSaveClip(editedClip)
                showEditDialog = null
            }
        )
    }

    // Time Input Dialog for A
    if (showTimeInputForA) {
        TimeInputDialog(
            title = "Set Point A",
            initialTimeMs = abLoopStart ?: 0L,
            maxTimeMs = duration,
            onDismiss = { showTimeInputForA = false },
            onConfirm = { newTimeMs ->
                // Set A, keep B if it's still valid (after A), otherwise use duration as placeholder
                val validEnd = if (abLoopEnd != null && abLoopEnd > newTimeMs) abLoopEnd else duration
                onSetManualTime(newTimeMs, validEnd)
                showTimeInputForA = false
            }
        )
    }

    // Time Input Dialog for B
    if (showTimeInputForB) {
        TimeInputDialog(
            title = "Set Point B",
            initialTimeMs = abLoopEnd ?: (abLoopStart?.plus(1000) ?: 0L),
            maxTimeMs = duration,
            minTimeMs = abLoopStart?.plus(1) ?: 0L,
            onDismiss = { showTimeInputForB = false },
            onConfirm = { newTimeMs ->
                val startMs = abLoopStart ?: 0L
                if (newTimeMs > startMs) {
                    onSetManualTime(startMs, newTimeMs)
                }
                showTimeInputForB = false
            }
        )
    }
}

@Composable
private fun EnhancedClipItem(
    clip: com.ultramusic.player.core.DetectedClip,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onPreview: () -> Unit,
    onSave: () -> Unit,
    onEdit: () -> Unit
) {
    fun formatTime(ms: Long): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return "%d:%02d".format(minutes, seconds)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color(0xFF3D5A80) else Color(0xFF2A2A2A),
                RoundedCornerShape(8.dp)
            )
            .clickable { onToggleSelect() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4CAF50),
                uncheckedColor = Color.Gray
            ),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Emoji
        Text(clip.purpose.emoji, fontSize = 18.sp)
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = clip.suggestedName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatTime(clip.startMs)} - ${formatTime(clip.endMs)} (${formatTime(clip.durationMs)})",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        
        // Preview button (short play)
        IconButton(
            onClick = onPreview,
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF2196F3), CircleShape)
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Preview",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Edit button
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Tune,
                contentDescription = "Edit",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Save button
        IconButton(
            onClick = onSave,
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFFE91E63), CircleShape)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Save",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EditClipDialog(
    clip: com.ultramusic.player.core.DetectedClip,
    onDismiss: () -> Unit,
    onSave: (com.ultramusic.player.core.DetectedClip) -> Unit
) {
    var editedName by remember { mutableStateOf(clip.suggestedName) }
    var editedStart by remember { mutableStateOf(clip.startMs.toString()) }
    var editedEnd by remember { mutableStateOf(clip.endMs.toString()) }
    var selectedPurpose by remember { mutableStateOf(clip.purpose) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Clip", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Name
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Clip Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Start/End times
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editedStart,
                        onValueChange = { editedStart = it.filter { c -> c.isDigit() } },
                        label = { Text("Start (ms)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editedEnd,
                        onValueChange = { editedEnd = it.filter { c -> c.isDigit() } },
                        label = { Text("End (ms)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // Purpose dropdown
                Text("Purpose:", fontSize = 12.sp, color = Color.Gray)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(com.ultramusic.player.core.ClipPurpose.values().toList()) { purpose ->
                        FilterChip(
                            selected = purpose == selectedPurpose,
                            onClick = { selectedPurpose = purpose },
                            label = { Text("${purpose.emoji} ${purpose.displayName}", fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    val newStart = editedStart.toLongOrNull() ?: clip.startMs
                    val newEnd = editedEnd.toLongOrNull() ?: clip.endMs
                    onSave(clip.copy(
                        suggestedName = editedName,
                        startMs = newStart,
                        endMs = newEnd,
                        purpose = selectedPurpose
                    ))
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for manual time input (MM:SS format)
 */
@Composable
private fun TimeInputDialog(
    title: String,
    initialTimeMs: Long,
    maxTimeMs: Long,
    minTimeMs: Long = 0L,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    // Parse initial time
    val initialMinutes = ((initialTimeMs / 1000) / 60).toInt()
    val initialSeconds = ((initialTimeMs / 1000) % 60).toInt()

    var minutes by remember { mutableStateOf(initialMinutes.toString()) }
    var seconds by remember { mutableStateOf(initialSeconds.toString().padStart(2, '0')) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun formatTime(ms: Long): String {
        val m = (ms / 1000) / 60
        val s = (ms / 1000) % 60
        return "%d:%02d".format(m, s)
    }

    fun validateAndGetMs(): Long? {
        val mins = minutes.toIntOrNull() ?: 0
        val secs = seconds.toIntOrNull() ?: 0

        if (secs >= 60) {
            errorMessage = "Seconds must be 0-59"
            return null
        }

        val totalMs = ((mins * 60L) + secs) * 1000L

        if (totalMs < minTimeMs) {
            errorMessage = "Must be after ${formatTime(minTimeMs)}"
            return null
        }

        if (totalMs > maxTimeMs) {
            errorMessage = "Must be before ${formatTime(maxTimeMs)}"
            return null
        }

        errorMessage = null
        return totalMs
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Enter time in MM:SS format",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minutes field
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 3) {
                                minutes = newValue
                                errorMessage = null
                            }
                        },
                        label = { Text("Min") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text(
                        ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Seconds field
                    OutlinedTextField(
                        value = seconds,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 2) {
                                seconds = newValue
                                errorMessage = null
                            }
                        },
                        label = { Text("Sec") },
                        modifier = Modifier.width(80.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Error message
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFE91E63),
                        fontSize = 12.sp
                    )
                }

                // Duration info
                Text(
                    "Song duration: ${formatTime(maxTimeMs)}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    validateAndGetMs()?.let { timeMs ->
                        onConfirm(timeMs)
                    }
                }
            ) {
                Text("Set")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
