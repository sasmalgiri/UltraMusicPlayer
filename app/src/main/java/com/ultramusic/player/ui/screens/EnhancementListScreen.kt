package com.ultramusic.player.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.AutoMirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.ui.MainViewModel

/**
 * Enhancement List Screen
 * 
 * Shows all available audio enhancements in an easy-to-use list format:
 * - Speed control with slider
 * - Pitch control with slider
 * - A-B Loop settings
 * - Audio presets
 * - Quality settings
 * - Formant preservation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancementListScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val playbackState by viewModel.playbackState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Audio Enhancements",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ===== SPEED CONTROL =====
            item {
                EnhancementCard(
                    title = "Speed Control",
                    subtitle = "Current: ${playbackState.speed}x",
                    icon = Icons.Default.Speed,
                    iconColor = Color(0xFF2196F3)
                ) {
                    SpeedControlContent(
                        speed = playbackState.speed,
                        onSpeedChange = { viewModel.setSpeed(it) }
                    )
                }
            }
            
            // ===== PITCH CONTROL =====
            item {
                EnhancementCard(
                    title = "Pitch Control",
                    subtitle = "Current: ${if (playbackState.pitch >= 0) "+" else ""}${playbackState.pitch.toInt()} semitones",
                    icon = Icons.Default.GraphicEq,
                    iconColor = Color(0xFF9C27B0)
                ) {
                    PitchControlContent(
                        pitch = playbackState.pitch,
                        onPitchChange = { viewModel.setPitch(it) }
                    )
                }
            }
            
            // ===== A-B LOOP =====
            item {
                EnhancementCard(
                    title = "A-B Loop",
                    subtitle = if (playbackState.loopStartPosition != null && playbackState.loopEndPosition != null)
                        "Active: ${formatTime(playbackState.loopStartPosition!!)} - ${formatTime(playbackState.loopEndPosition!!)}"
                    else "Set points to loop a section",
                    icon = Icons.Default.Loop,
                    iconColor = Color(0xFF4CAF50)
                ) {
                    ABLoopContent(
                        loopStart = playbackState.loopStartPosition,
                        loopEnd = playbackState.loopEndPosition,
                        currentPosition = playbackState.position,
                        duration = playbackState.duration,
                        onSetA = { viewModel.setLoopStart() },
                        onSetB = { viewModel.setLoopEnd() },
                        onClear = { viewModel.clearLoop() }
                    )
                }
            }
            
            // ===== QUICK PRESETS =====
            item {
                EnhancementCard(
                    title = "Quick Presets",
                    subtitle = "One-tap audio effects",
                    icon = Icons.Default.Tune,
                    iconColor = Color(0xFFFF9800)
                ) {
                    QuickPresetsContent(
                        currentSpeed = playbackState.speed,
                        currentPitch = playbackState.pitch,
                        onApplyPreset = { speed, pitch ->
                            viewModel.setSpeed(speed)
                            viewModel.setPitch(pitch)
                        }
                    )
                }
            }
            
            // ===== QUALITY SETTINGS =====
            item {
                EnhancementCard(
                    title = "Audio Quality",
                    subtitle = "Quality: ${uiState.qualityPercent}%",
                    icon = Icons.Default.HighQuality,
                    iconColor = Color(0xFFE91E63)
                ) {
                    QualitySettingsContent(
                        qualityPercent = uiState.qualityPercent,
                        formantPreservation = uiState.formantPreservation,
                        onFormantToggle = { viewModel.setFormantPreservation(it) }
                    )
                }
            }
            
            // ===== PLAYBACK MODES =====
            item {
                EnhancementCard(
                    title = "Playback Modes",
                    subtitle = "Shuffle & Repeat options",
                    icon = Icons.Default.Repeat,
                    iconColor = Color(0xFF00BCD4)
                ) {
                    PlaybackModesContent(
                        isShuffleEnabled = playbackState.isShuffleEnabled,
                        repeatMode = playbackState.repeatMode,
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onToggleRepeat = { viewModel.toggleRepeat() }
                    )
                }
            }
            
            // ===== INFO SECTION =====
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "💡 Tips",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val tips = listOf(
                            "🎵 Use Nightcore for energetic anime-style music",
                            "🎧 Slowed preset creates chill, reverb-style audio",
                            "🎸 Use 0.75x speed to learn difficult guitar parts",
                            "🎤 Pitch shift helps match songs to your vocal range",
                            "🔁 A-B Loop is perfect for practicing specific sections"
                        )
                        
                        tips.forEach { tip ->
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancementCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content
            content()
        }
    }
}

@Composable
private fun SpeedControlContent(
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    Column {
        // Slider
        Slider(
            value = speed,
            onValueChange = onSpeedChange,
            valueRange = 0.25f..4f,
            steps = 0,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Quick buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { preset ->
                SpeedButton(
                    speed = preset,
                    isSelected = speed == preset,
                    onClick = { onSpeedChange(preset) }
                )
            }
        }
    }
}

@Composable
private fun SpeedButton(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        label = "bg"
    )
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = "${speed}x",
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PitchControlContent(
    pitch: Float,
    onPitchChange: (Float) -> Unit
) {
    Column {
        // Slider
        Slider(
            value = pitch,
            onValueChange = onPitchChange,
            valueRange = -36f..36f,
            steps = 0,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Quick buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(-12f, -6f, -3f, 0f, 3f, 6f, 12f).forEach { preset ->
                PitchButton(
                    pitch = preset,
                    isSelected = pitch == preset,
                    onClick = { onPitchChange(preset) }
                )
            }
        }
    }
}

@Composable
private fun PitchButton(
    pitch: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        label = "bg"
    )
    
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = if (pitch >= 0) "+${pitch.toInt()}" else "${pitch.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ABLoopContent(
    loopStart: Long?,
    loopEnd: Long?,
    currentPosition: Long,
    duration: Long,
    onSetA: () -> Unit,
    onSetB: () -> Unit,
    onClear: () -> Unit
) {
    Column {
        // Visual representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Progress
            if (duration > 0) {
                val progress = currentPosition.toFloat() / duration
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
                
                // Loop region
                if (loopStart != null && loopEnd != null) {
                    val startFraction = loopStart.toFloat() / duration
                    val endFraction = loopEnd.toFloat() / duration
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(endFraction - startFraction)
                            .fillMaxHeight()
                            .padding(start = (startFraction * 300).dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                    )
                }
            }
            
            // Labels
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "A: ${if (loopStart != null) formatTime(loopStart) else "--:--"}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Current: ${formatTime(currentPosition)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "B: ${if (loopEnd != null) formatTime(loopEnd) else "--:--"}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LoopButton(
                text = if (loopStart != null) "A ✓" else "Set A",
                isSet = loopStart != null,
                onClick = onSetA
            )
            LoopButton(
                text = if (loopEnd != null) "B ✓" else "Set B",
                isSet = loopEnd != null,
                onClick = onSetB
            )
            if (loopStart != null || loopEnd != null) {
                LoopButton(
                    text = "Clear",
                    isSet = false,
                    onClick = onClear
                )
            }
        }
    }
}

@Composable
private fun LoopButton(
    text: String,
    isSet: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSet) 
            MaterialTheme.colorScheme.tertiary 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSet) 
                MaterialTheme.colorScheme.onTertiary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun QuickPresetsContent(
    currentSpeed: Float,
    currentPitch: Float,
    onApplyPreset: (Float, Float) -> Unit
) {
    val presets = listOf(
        PresetItem("🌙 Nightcore", 1.25f, 4f),
        PresetItem("🎧 Slowed", 0.85f, -2f),
        PresetItem("🌊 Vaporwave", 0.8f, -3f),
        PresetItem("🐿️ Chipmunk", 1f, 12f),
        PresetItem("👹 Deep", 1f, -12f),
        PresetItem("📚 Study", 0.9f, 0f),
        PresetItem("🎸 Practice", 0.75f, 0f),
        PresetItem("⚡ Fast", 2f, 0f),
        PresetItem("🔄 Reset", 1f, 0f)
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { preset ->
                    val isSelected = currentSpeed == preset.speed && currentPitch == preset.pitch
                    
                    Surface(
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onApplyPreset(preset.speed, preset.pitch) }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${preset.speed}x / ${if (preset.pitch >= 0) "+" else ""}${preset.pitch.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Fill remaining space if needed
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class PresetItem(
    val name: String,
    val speed: Float,
    val pitch: Float
)

@Composable
private fun QualitySettingsContent(
    qualityPercent: Int,
    formantPreservation: Boolean,
    onFormantToggle: (Boolean) -> Unit
) {
    Column {
        // Quality indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Estimated Quality",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$qualityPercent%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    qualityPercent >= 80 -> Color(0xFF4CAF50)
                    qualityPercent >= 50 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Quality bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(qualityPercent / 100f)
                    .fillMaxHeight()
                    .background(
                        when {
                            qualityPercent >= 80 -> Color(0xFF4CAF50)
                            qualityPercent >= 50 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Formant preservation toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Formant Preservation",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Keeps vocals natural at extreme pitches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = formantPreservation,
                onCheckedChange = onFormantToggle
            )
        }
    }
}

@Composable
private fun PlaybackModesContent(
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Shuffle
        ModeButton(
            icon = Icons.Default.Shuffle,
            label = "Shuffle",
            isEnabled = isShuffleEnabled,
            onClick = onToggleShuffle
        )
        
        // Repeat Off
        ModeButton(
            icon = Icons.Default.Repeat,
            label = "Repeat",
            isEnabled = repeatMode > 0,
            sublabel = when (repeatMode) {
                1 -> "All"
                2 -> "One"
                else -> "Off"
            },
            onClick = onToggleRepeat
        )
    }
}

@Composable
private fun ModeButton(
    icon: ImageVector,
    label: String,
    isEnabled: Boolean,
    sublabel: String? = null,
    onClick: () -> Unit
) {
    Surface(
        color = if (isEnabled) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isEnabled) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
                sublabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isEnabled) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
