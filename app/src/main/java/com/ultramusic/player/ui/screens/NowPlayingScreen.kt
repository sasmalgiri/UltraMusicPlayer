package com.ultramusic.player.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.audio.NativeAudioEngine
import com.ultramusic.player.ui.components.*
import com.ultramusic.player.ui.theme.*
import com.ultramusic.player.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: PlayerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Song Info
        SongInfoCard(
            title = uiState.currentSong?.title ?: "No song selected",
            artist = uiState.currentSong?.artist ?: "",
            album = uiState.currentSong?.album ?: ""
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Waveform / Progress
        WaveformProgressBar(
            currentPosition = uiState.currentPositionMs,
            duration = uiState.durationMs,
            isLooping = uiState.isLooping,
            loopStart = uiState.loopRegion?.startMs ?: 0,
            loopEnd = uiState.loopRegion?.endMs ?: 0,
            onSeek = { viewModel.seekTo(it) },
            onLoopRegionChange = { start, end -> viewModel.setLoopRegion(start, end) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(uiState.currentPositionMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = formatTime(uiState.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Playback controls
        PlaybackControls(
            isPlaying = uiState.isPlaying,
            onPlayPause = { viewModel.togglePlayPause() },
            onPrevious = { viewModel.playPrevious() },
            onNext = { viewModel.playNext() },
            onStop = { viewModel.stop() }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // ═══════════════════════════════════════════════════════════════════
        // SPEED CONTROL - Extended Range: 0.05x to 10.0x
        // ═══════════════════════════════════════════════════════════════════
        SpeedControlCard(
            speed = uiState.speed,
            onSpeedChange = { viewModel.setSpeed(it) },
            onReset = { viewModel.resetSpeed() },
            expanded = uiState.showSpeedControl,
            onExpandToggle = { viewModel.toggleSpeedControl() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ═══════════════════════════════════════════════════════════════════
        // PITCH CONTROL - Extended Range: -36 to +36 semitones
        // ═══════════════════════════════════════════════════════════════════
        PitchControlCard(
            semitones = uiState.pitchSemitones,
            cents = uiState.pitchCents,
            onSemitonesChange = { viewModel.setPitchSemitones(it) },
            onCentsChange = { viewModel.setPitchCents(it) },
            onReset = { viewModel.resetPitch() },
            preserveFormants = uiState.preserveFormants,
            onPreserveFormantsChange = { viewModel.setPreserveFormants(it) },
            expanded = uiState.showPitchControl,
            onExpandToggle = { viewModel.togglePitchControl() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Loop Control
        LoopControlCard(
            isLooping = uiState.isLooping,
            loopRegion = uiState.loopRegion,
            onLoopToggle = { viewModel.toggleLoop() },
            onClearLoop = { viewModel.clearLoop() },
            expanded = uiState.showLoopControl,
            onExpandToggle = { viewModel.toggleLoopControl() }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Presets
        QuickPresetsRow(
            onNightcore = { viewModel.applyNightcore() },
            onSlowed = { viewModel.applySlowed() },
            onVaporwave = { viewModel.applyVaporwave() },
            onReset = { viewModel.resetToDefault() }
        )
        
        Spacer(modifier = Modifier.height(80.dp)) // Space for mini player
    }
}

@Composable
fun SongInfoCard(
    title: String,
    artist: String,
    album: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album art placeholder
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (album.isNotEmpty()) {
                Text(
                    text = album,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onStop) {
            Icon(Icons.Default.Stop, "Stop", modifier = Modifier.size(32.dp))
        }
        
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(40.dp))
        }
        
        // Large play/pause button
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp)
            )
        }
        
        IconButton(onClick = onNext) {
            Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(40.dp))
        }
        
        IconButton(onClick = { /* Shuffle */ }) {
            Icon(Icons.Default.Shuffle, "Shuffle", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun SpeedControlCard(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onReset: () -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = SpeedIndicatorColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Speed value display
                    Text(
                        text = String.format("%.2fx", speed),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SpeedIndicatorColor
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }
                }
            }
            
            // Main slider (always visible)
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..2.0f,  // Common range
                steps = 29,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = SpeedIndicatorColor,
                    activeTrackColor = SpeedIndicatorColor
                )
            )
            
            // Extended controls (when expanded)
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Extended range slider (0.05x - 10.0x)
                    Text(
                        text = "Extended Range (0.05x - 10.0x)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Slider(
                        value = speed,
                        onValueChange = onSpeedChange,
                        valueRange = NativeAudioEngine.MIN_SPEED..NativeAudioEngine.MAX_SPEED,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Fine adjustment buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SpeedButton("-0.1", onClick = { onSpeedChange(speed - 0.1f) })
                        SpeedButton("-0.01", onClick = { onSpeedChange(speed - 0.01f) })
                        OutlinedButton(onClick = onReset) {
                            Text("Reset")
                        }
                        SpeedButton("+0.01", onClick = { onSpeedChange(speed + 0.01f) })
                        SpeedButton("+0.1", onClick = { onSpeedChange(speed + 0.1f) })
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Quick speed presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { preset ->
                            FilterChip(
                                selected = speed == preset,
                                onClick = { onSpeedChange(preset) },
                                label = { Text("${preset}x") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text, fontSize = 12.sp)
    }
}

@Composable
fun PitchControlCard(
    semitones: Float,
    cents: Float,
    onSemitonesChange: (Float) -> Unit,
    onCentsChange: (Float) -> Unit,
    onReset: () -> Unit,
    preserveFormants: Boolean,
    onPreserveFormantsChange: (Boolean) -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = PitchIndicatorColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pitch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Total pitch display
                    val totalPitch = semitones + (cents / 100f)
                    val sign = if (totalPitch >= 0) "+" else ""
                    Text(
                        text = "$sign${String.format("%.2f", totalPitch)} st",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PitchIndicatorColor
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }
                }
            }
            
            // Semitones slider (always visible)
            Text(
                text = "Semitones: ${semitones.toInt()}",
                style = MaterialTheme.typography.labelMedium
            )
            Slider(
                value = semitones,
                onValueChange = onSemitonesChange,
                valueRange = -12f..12f,  // Common range
                steps = 23,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = PitchIndicatorColor,
                    activeTrackColor = PitchIndicatorColor
                )
            )
            
            // Extended controls (when expanded)
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Extended range (-36 to +36)
                    Text(
                        text = "Extended Range (-36 to +36 semitones)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Slider(
                        value = semitones,
                        onValueChange = onSemitonesChange,
                        valueRange = NativeAudioEngine.MIN_PITCH..NativeAudioEngine.MAX_PITCH,
                        steps = 71,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Fine tune (cents)
                    Text(
                        text = "Fine Tune: ${cents.toInt()} cents",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = cents,
                        onValueChange = onCentsChange,
                        valueRange = -100f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Formant preservation toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Preserve Formants",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Keep natural voice character",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = preserveFormants,
                            onCheckedChange = onPreserveFormantsChange
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Reset button
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Pitch")
                    }
                }
            }
        }
    }
}

@Composable
fun LoopControlCard(
    isLooping: Boolean,
    loopRegion: com.ultramusic.player.data.model.LoopRegion?,
    onLoopToggle: () -> Unit,
    onClearLoop: () -> Unit,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (isLooping) MaterialTheme.colorScheme.primary else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "A-B Loop",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (loopRegion != null) {
                        Text(
                            text = "${formatTime(loopRegion.startMs)} - ${formatTime(loopRegion.endMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Switch(
                        checked = isLooping,
                        onCheckedChange = { onLoopToggle() }
                    )
                }
            }
            
            AnimatedVisibility(visible = expanded || isLooping) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Tap on the waveform to set loop points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    if (loopRegion != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onClearLoop,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear Loop")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPresetsRow(
    onNightcore: () -> Unit,
    onSlowed: () -> Unit,
    onVaporwave: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PresetButton("Nightcore", "🎵", onClick = onNightcore)
                PresetButton("Slowed", "🎧", onClick = onSlowed)
                PresetButton("Vaporwave", "🌴", onClick = onVaporwave)
                PresetButton("Reset", "↩️", onClick = onReset)
            }
        }
    }
}

@Composable
fun PresetButton(name: String, emoji: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(emoji, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    val hours = ms / (1000 * 60 * 60)
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
