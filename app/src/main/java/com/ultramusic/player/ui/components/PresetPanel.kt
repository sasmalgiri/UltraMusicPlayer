package com.ultramusic.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.UltraMusicApp
import com.ultramusic.player.data.AudioPreset

/**
 * AUDIO CONTROL PANEL
 *
 * Full control for speed and pitch with:
 * - Speed slider (0.05x - 10.0x)
 * - Pitch slider (-36 to +36 semitones)
 * - Fine adjustment buttons
 * - Quick presets for instant apply
 */
@Composable
fun PresetPanel(
    presets: List<AudioPreset>,
    selectedPreset: AudioPreset?,
    onPresetSelected: (AudioPreset) -> Unit,
    modifier: Modifier = Modifier,
    // New parameters for slider control
    speed: Float = 1.0f,
    pitch: Float = 0f,
    onSpeedChange: (Float) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onResetAll: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Reset button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onResetAll,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== SPEED CONTROL ====================
            SpeedSliderControl(
                speed = speed,
                onSpeedChange = onSpeedChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== PITCH CONTROL ====================
            PitchSliderControl(
                pitch = pitch,
                onPitchChange = onPitchChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ==================== QUICK PRESETS ====================
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { preset ->
                    CompactPresetCard(
                        preset = preset,
                        isSelected = selectedPreset == preset,
                        onClick = { onPresetSelected(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedSliderControl(
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speedColor = when {
        speed > 1.0f -> Color(0xFFFF5722)  // Fast - Orange
        speed < 1.0f -> Color(0xFF2196F3)  // Slow - Blue
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        // Label and value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speed",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = String.format("%.2fx", speed),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = speedColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider
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

        // Quick speed buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
                QuickValueButton(
                    label = "${s}x",
                    isSelected = kotlin.math.abs(speed - s) < 0.01f,
                    color = if (s > 1f) Color(0xFFFF5722) else if (s < 1f) Color(0xFF2196F3) else MaterialTheme.colorScheme.primary,
                    onClick = { onSpeedChange(s) }
                )
            }
        }
    }
}

@Composable
private fun PitchSliderControl(
    pitch: Float,
    onPitchChange: (Float) -> Unit
) {
    val pitchColor = when {
        pitch > 0f -> Color(0xFFE91E63)  // High - Pink
        pitch < 0f -> Color(0xFF9C27B0)  // Low - Purple
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        // Label and value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pitch",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            val sign = if (pitch > 0) "+" else ""
            Text(
                text = "${sign}${String.format("%.1f", pitch)} st",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = pitchColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider
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

        // Quick pitch buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(-12f, -5f, 0f, 5f, 12f).forEach { p ->
                val label = when {
                    p == 0f -> "0"
                    p > 0 -> "+${p.toInt()}"
                    else -> "${p.toInt()}"
                }
                QuickValueButton(
                    label = label,
                    isSelected = kotlin.math.abs(pitch - p) < 0.1f,
                    color = if (p > 0) Color(0xFFE91E63) else if (p < 0) Color(0xFF9C27B0) else MaterialTheme.colorScheme.primary,
                    onClick = { onPitchChange(p) }
                )
            }
        }
    }
}

@Composable
private fun QuickValueButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun CompactPresetCard(
    preset: AudioPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(72.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Text(
                text = preset.icon,
                fontSize = 18.sp
            )

            // Name
            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Speed/Pitch values
            Text(
                text = "${preset.speed}x ${if (preset.pitch >= 0) "+" else ""}${preset.pitch.toInt()}st",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
