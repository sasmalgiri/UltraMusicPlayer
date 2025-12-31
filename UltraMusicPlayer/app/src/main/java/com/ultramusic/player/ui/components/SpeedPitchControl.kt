package com.ultramusic.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ultramusic.player.UltraMusicApp
import com.ultramusic.player.ui.theme.PitchHigh
import com.ultramusic.player.ui.theme.PitchLow
import com.ultramusic.player.ui.theme.SpeedFast
import com.ultramusic.player.ui.theme.SpeedSlow

@Composable
fun SpeedPitchControl(
    speed: Float,
    pitch: Float,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSpeedAdjust: (Float) -> Unit,
    onPitchAdjust: (Float) -> Unit,
    onResetSpeed: () -> Unit,
    onResetPitch: () -> Unit,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
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
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed & Pitch Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedButton(
                    onClick = onResetAll,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Reset All")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ==================== SPEED CONTROL ====================
            SpeedControlSection(
                speed = speed,
                onSpeedChange = onSpeedChange,
                onSpeedAdjust = onSpeedAdjust,
                onReset = onResetSpeed
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // ==================== PITCH CONTROL ====================
            PitchControlSection(
                pitch = pitch,
                onPitchChange = onPitchChange,
                onPitchAdjust = onPitchAdjust,
                onReset = onResetPitch
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Info text
            Text(
                text = "Speed: 0.05x - 10.0x | Pitch: -36 to +36 semitones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SpeedControlSection(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onSpeedAdjust: (Float) -> Unit,
    onReset: () -> Unit
) {
    val speedColor = when {
        speed > 1.0f -> SpeedFast
        speed < 1.0f -> SpeedSlow
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        // Speed label and reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Speed",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.height(32.dp)
            ) {
                Text("Reset", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slider with min/max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "0.05x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = UltraMusicApp.MIN_SPEED..UltraMusicApp.MAX_SPEED,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = speedColor,
                    activeTrackColor = speedColor
                )
            )
            Text(
                text = "10x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // CURRENT VALUE - Tap to edit manually
        EditableSpeedBox(
            speed = speed,
            color = speedColor,
            onSpeedChange = onSpeedChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Fine adjustment buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AdjustButton("-0.1", { onSpeedAdjust(-0.1f) })
            AdjustButton("-0.05", { onSpeedAdjust(-0.05f) })
            AdjustButton("-0.01", { onSpeedAdjust(-0.01f) })
            AdjustButton("+0.01", { onSpeedAdjust(0.01f) })
            AdjustButton("+0.05", { onSpeedAdjust(0.05f) })
            AdjustButton("+0.1", { onSpeedAdjust(0.1f) })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick speed buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { s ->
                QuickSpeedButton(
                    speed = s,
                    isSelected = kotlin.math.abs(speed - s) < 0.01f,
                    onClick = { onSpeedChange(s) }
                )
            }
        }
    }
}

@Composable
private fun PitchControlSection(
    pitch: Float,
    onPitchChange: (Float) -> Unit,
    onPitchAdjust: (Float) -> Unit,
    onReset: () -> Unit
) {
    val pitchColor = when {
        pitch > 0f -> PitchHigh
        pitch < 0f -> PitchLow
        else -> MaterialTheme.colorScheme.primary
    }

    Column {
        // Pitch label and reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pitch",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.height(32.dp)
            ) {
                Text("Reset", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slider with min/max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "-36",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = pitch,
                onValueChange = onPitchChange,
                valueRange = UltraMusicApp.MIN_PITCH_SEMITONES..UltraMusicApp.MAX_PITCH_SEMITONES,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = pitchColor,
                    activeTrackColor = pitchColor
                )
            )
            Text(
                text = "+36",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // CURRENT VALUE - Tap to edit manually
        EditablePitchBox(
            pitch = pitch,
            color = pitchColor,
            onPitchChange = onPitchChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Fine adjustment buttons (semitones and cents)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AdjustButton("-12", { onPitchAdjust(-12f) })
            AdjustButton("-1", { onPitchAdjust(-1f) })
            AdjustButton("-0.1", { onPitchAdjust(-0.1f) })
            AdjustButton("+0.1", { onPitchAdjust(0.1f) })
            AdjustButton("+1", { onPitchAdjust(1f) })
            AdjustButton("+12", { onPitchAdjust(12f) })
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick pitch buttons (octaves and common)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(-24f, -12f, -7f, -5f, 0f, 5f, 7f, 12f, 24f).forEach { p ->
                QuickPitchButton(
                    pitch = p,
                    isSelected = kotlin.math.abs(pitch - p) < 0.1f,
                    onClick = { onPitchChange(p) }
                )
            }
        }
    }
}

@Composable
private fun AdjustButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun QuickSpeedButton(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Text(
            text = "${speed}x",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun QuickPitchButton(
    pitch: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when {
        pitch == 0f -> "0"
        pitch > 0 -> "+${pitch.toInt()}"
        else -> "${pitch.toInt()}"
    }
    
    Button(
        onClick = onClick,
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
