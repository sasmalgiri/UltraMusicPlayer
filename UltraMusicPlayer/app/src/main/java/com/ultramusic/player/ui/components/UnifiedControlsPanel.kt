package com.ultramusic.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.UltraMusicApp
import com.ultramusic.player.data.AudioPreset
import com.ultramusic.player.ui.theme.PitchHigh
import com.ultramusic.player.ui.theme.PitchLow
import com.ultramusic.player.ui.theme.SpeedFast
import com.ultramusic.player.ui.theme.SpeedSlow

/**
 * Unified controls panel that combines Speed, Pitch, A-B Loop, Audio Enhancement, and Presets
 * in one always-visible scrollable section at the bottom of NowPlayingScreen
 */
@Composable
fun UnifiedControlsPanel(
    // Speed & Pitch
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onResetSpeed: () -> Unit,
    onResetPitch: () -> Unit,
    onResetAll: () -> Unit,
    // A-B Loop
    abLoopStart: Long?,
    abLoopEnd: Long?,
    currentPosition: Long,
    duration: Long,
    onSetLoopStart: () -> Unit,
    onSetLoopEnd: () -> Unit,
    onClearLoop: () -> Unit,
    onSaveToArmory: () -> Unit,
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
    // Presets
    presets: List<AudioPreset>,
    selectedPreset: AudioPreset?,
    onPresetSelected: (AudioPreset) -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(isExpanded) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (expanded) 400.dp else 60.dp)
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Controls",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Show current values when collapsed
                    if (!expanded && (speed != 1.0f || pitch != 0f)) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (speed != 1.0f) "${String.format("%.2f", speed)}x" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (speed > 1f) SpeedFast else SpeedSlow
                        )
                        if (speed != 1.0f && pitch != 0f) {
                            Text(" | ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (pitch != 0f) {
                            val sign = if (pitch > 0) "+" else ""
                            Text(
                                text = "${sign}${String.format("%.1f", pitch)}st",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (pitch > 0f) PitchHigh else PitchLow
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Reset all button
                    if (speed != 1.0f || pitch != 0f) {
                        OutlinedButton(
                            onClick = onResetAll,
                            modifier = Modifier.height(28.dp),
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded content
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    // ==================== SPEED CONTROL ====================
                    SpeedSection(
                        speed = speed,
                        onSpeedChange = onSpeedChange,
                        onReset = onResetSpeed
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // ==================== PITCH CONTROL ====================
                    PitchSection(
                        pitch = pitch,
                        onPitchChange = onPitchChange,
                        onReset = onResetPitch
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // ==================== A-B LOOP ====================
                    ABLoopSection(
                        abLoopStart = abLoopStart,
                        abLoopEnd = abLoopEnd,
                        currentPosition = currentPosition,
                        duration = duration,
                        onSetStart = onSetLoopStart,
                        onSetEnd = onSetLoopEnd,
                        onClear = onClearLoop,
                        onSaveToArmory = onSaveToArmory
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // ==================== AUDIO ENHANCEMENT ====================
                    AudioEnhancementSection(
                        bassLevel = bassLevel,
                        loudnessGain = loudnessGain,
                        clarityLevel = clarityLevel,
                        spatialLevel = spatialLevel,
                        onBassChange = onBassChange,
                        onLoudnessChange = onLoudnessChange,
                        onClarityChange = onClarityChange,
                        onSpatialChange = onSpatialChange,
                        onResetAll = onResetAudioEffects
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // ==================== PRESETS ====================
                    PresetsSection(
                        presets = presets,
                        selectedPreset = selectedPreset,
                        onPresetSelected = onPresetSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedSection(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    val speedColor = when {
        speed > 1.0f -> SpeedFast
        speed < 1.0f -> SpeedSlow
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        // Header row with label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = speedColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            if (speed != 1.0f) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.height(24.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text("Reset", fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider with value labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Min value label
            Text(
                text = "0.05x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )

            // Slider with current value box
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Slider(
                    value = speed,
                    onValueChange = onSpeedChange,
                    valueRange = UltraMusicApp.MIN_SPEED..UltraMusicApp.MAX_SPEED,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = speedColor,
                        activeTrackColor = speedColor
                    )
                )
            }

            // Max value label
            Text(
                text = "10x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )
        }

        // CURRENT VALUE - Tap to edit manually
        EditableSpeedBox(
            speed = speed,
            color = speedColor,
            onSpeedChange = onSpeedChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Quick buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { s ->
                QuickButton(
                    text = "${s}x",
                    isSelected = kotlin.math.abs(speed - s) < 0.01f,
                    onClick = { onSpeedChange(s) }
                )
            }
        }
    }
}

@Composable
private fun PitchSection(
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    val pitchColor = when {
        pitch > 0f -> PitchHigh
        pitch < 0f -> PitchLow
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        // Header row with label
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🎵",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pitch",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            if (pitch != 0f) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.height(24.dp),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    Text("Reset", fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider with value labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Min value label
            Text(
                text = "-36",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )

            // Slider
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Slider(
                    value = pitch,
                    onValueChange = onPitchChange,
                    valueRange = UltraMusicApp.MIN_PITCH_SEMITONES..UltraMusicApp.MAX_PITCH_SEMITONES,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = pitchColor,
                        activeTrackColor = pitchColor
                    )
                )
            }

            // Max value label
            Text(
                text = "+36",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )
        }

        // CURRENT VALUE - Tap to edit manually
        EditablePitchBox(
            pitch = pitch,
            color = pitchColor,
            onPitchChange = onPitchChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Quick buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(-12f, -7f, -5f, 0f, 5f, 7f, 12f).forEach { p ->
                val label = when {
                    p == 0f -> "0"
                    p > 0 -> "+${p.toInt()}"
                    else -> "${p.toInt()}"
                }
                QuickButton(
                    text = label,
                    isSelected = kotlin.math.abs(pitch - p) < 0.1f,
                    onClick = { onPitchChange(p) }
                )
            }
        }
    }
}

@Composable
private fun ABLoopSection(
    abLoopStart: Long?,
    abLoopEnd: Long?,
    currentPosition: Long,
    duration: Long,
    onSetStart: () -> Unit,
    onSetEnd: () -> Unit,
    onClear: () -> Unit,
    onSaveToArmory: () -> Unit
) {
    fun formatTime(ms: Long): String {
        val minutes = (ms / 1000) / 60
        val seconds = (ms / 1000) % 60
        return "%d:%02d".format(minutes, seconds)
    }

    Column {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Loop,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (abLoopStart != null && abLoopEnd != null)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "A-B Loop",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            // Loop duration display
            if (abLoopStart != null && abLoopEnd != null) {
                Text(
                    text = "${formatTime(abLoopEnd - abLoopStart)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A time
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    text = abLoopStart?.let { formatTime(it) } ?: "--:--",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Current position
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NOW", color = Color.Gray, fontSize = 9.sp)
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // B time
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("B", color = Color(0xFFE91E63), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    text = abLoopEnd?.let { formatTime(it) } ?: "--:--",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSetStart,
                modifier = Modifier.weight(1f).height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (abLoopStart != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant
                ),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text("Set A", fontSize = 11.sp)
            }

            Button(
                onClick = onSetEnd,
                enabled = abLoopStart != null,
                modifier = Modifier.weight(1f).height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (abLoopEnd != null) Color(0xFFE91E63) else MaterialTheme.colorScheme.surfaceVariant
                ),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text("Set B", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onClear,
                enabled = abLoopStart != null || abLoopEnd != null,
                modifier = Modifier.weight(0.7f).height(32.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text("Clear", fontSize = 11.sp)
            }
        }

        // Save to armory button
        if (abLoopStart != null && abLoopEnd != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onSaveToArmory,
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save to Battle Armory", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PresetsSection(
    presets: List<AudioPreset>,
    selectedPreset: AudioPreset?,
    onPresetSelected: (AudioPreset) -> Unit
) {
    Column {
        Text(
            text = "Quick Presets",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                PresetChip(
                    preset = preset,
                    isSelected = preset == selectedPreset,
                    onClick = { onPresetSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun QuickButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Text(text = text, fontSize = 11.sp)
    }
}

@Composable
private fun PresetChip(
    preset: AudioPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.icon,
                fontSize = 18.sp
            )
            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== AUDIO ENHANCEMENT SECTION ====================

@Composable
private fun AudioEnhancementSection(
    bassLevel: Int,
    loudnessGain: Int,
    clarityLevel: Int,
    spatialLevel: Int,
    onBassChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onClarityChange: (Int) -> Unit,
    onSpatialChange: (Int) -> Unit,
    onResetAll: () -> Unit
) {
    var showAdvanced by remember { mutableStateOf(false) }

    Column {
        // Header with reset button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔊", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Audio Enhancement",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Row {
                if (bassLevel != 500 || loudnessGain != 0 || clarityLevel != 50 || spatialLevel != 500) {
                    OutlinedButton(
                        onClick = onResetAll,
                        modifier = Modifier.height(24.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Text("Reset", fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showAdvanced) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== BASS CONTROL ====================
        AudioSlider(
            label = "🎸 Bass",
            value = bassLevel,
            valueRange = 0..1000,
            onValueChange = onBassChange,
            valueDisplay = { "${it / 10}%" },
            accentColor = Color(0xFFE91E63)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== LOUDNESS CONTROL ====================
        AudioSlider(
            label = "📢 Loudness",
            value = loudnessGain,
            valueRange = 0..1000,
            onValueChange = onLoudnessChange,
            valueDisplay = { "${it} mB" },
            accentColor = Color(0xFFFF5722)
        )

        // Advanced controls (collapsible)
        if (showAdvanced) {
            Spacer(modifier = Modifier.height(8.dp))

            // ==================== CLARITY CONTROL ====================
            AudioSlider(
                label = "✨ Clarity",
                value = clarityLevel,
                valueRange = 0..100,
                onValueChange = onClarityChange,
                valueDisplay = { "${it}%" },
                accentColor = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ==================== SPATIAL CONTROL ====================
            AudioSlider(
                label = "🎧 Spatial",
                value = spatialLevel,
                valueRange = 0..1000,
                onValueChange = onSpatialChange,
                valueDisplay = { "${it / 10}%" },
                accentColor = Color(0xFF9C27B0)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick presets row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AudioPresetButton("🔥 Bass Max", bassLevel == 1000) {
                onBassChange(1000)
            }
            AudioPresetButton("💥 Loud", loudnessGain >= 800) {
                onLoudnessChange(1000)
            }
            AudioPresetButton("🎯 Balanced", bassLevel == 500 && loudnessGain == 0) {
                onBassChange(500)
                onLoudnessChange(0)
                onClarityChange(50)
                onSpatialChange(500)
            }
            AudioPresetButton("🎵 Clear", clarityLevel >= 80) {
                onClarityChange(100)
            }
        }
    }
}

@Composable
private fun AudioSlider(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    valueDisplay: (Int) -> String,
    accentColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            // Editable value box
            EditableIntBox(
                value = value,
                min = valueRange.first,
                max = valueRange.last,
                color = accentColor,
                onValueChange = onValueChange,
                modifier = Modifier.width(70.dp)
            )
        }

        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor
            )
        )
    }
}

@Composable
private fun AudioPresetButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Text(text = text, fontSize = 10.sp)
    }
}
