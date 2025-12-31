package com.ultramusic.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.audio.BattleMode
import com.ultramusic.player.audio.EQBand
import com.ultramusic.player.audio.QuickProfile

/**
 * FULL Battle Controls Panel for NowPlayingScreen
 *
 * Contains ALL Sound Battle controls - same as AudioBattleScreen:
 * - SPL Battle meter
 * - Quick Profiles (A/B/C)
 * - Danger Mode
 * - Quick Actions
 * - Battle Mode selector
 * - Bass Power with frequency
 * - Loudness
 * - Clarity
 * - Spatial
 * - Equalizer
 * - Compressor
 * - Limiter
 * - Stereo Widener
 * - Harmonic Exciter
 * - Reverb
 */
@Composable
fun BattleControlsPanel(
    // Battle engine state
    isEnabled: Boolean,
    battleMode: BattleMode,
    dangerModeEnabled: Boolean,
    // SPL meters
    ourSPL: Float,
    opponentSPL: Float,
    currentPeakDb: Float,
    isClipping: Boolean,
    // Quick profiles
    profileSlotA: QuickProfile?,
    profileSlotB: QuickProfile?,
    profileSlotC: QuickProfile?,
    activeProfileSlot: Char?,
    // Main controls
    bassLevel: Int = 0,
    bassFrequency: Float = 80f,
    loudnessGain: Int = 0,
    clarityLevel: Int = 0,
    spatialLevel: Int = 0,
    eqBands: List<EQBand> = emptyList(),
    // Compressor
    compressorEnabled: Boolean = false,
    compressorThreshold: Float = -12f,
    compressorRatio: Float = 4f,
    compressorAttack: Float = 10f,
    compressorRelease: Float = 100f,
    compressorMakeupGain: Float = 0f,
    // Limiter
    limiterEnabled: Boolean = true,
    limiterThreshold: Float = -1f,
    limiterCeiling: Float = -0.1f,
    limiterAttack: Float = 0.5f,
    limiterRelease: Float = 50f,
    // Stereo Widener
    stereoWidthEnabled: Boolean = false,
    stereoWidth: Int = 100,
    // Exciter
    exciterEnabled: Boolean = false,
    exciterDrive: Int = 50,
    exciterMix: Int = 30,
    // Reverb
    reverbEnabled: Boolean = false,
    reverbPreset: Int = 0,
    // Callbacks - Basic
    onToggleBattleEngine: () -> Unit,
    onBattleModeChange: (BattleMode) -> Unit,
    onDangerModeChange: (Boolean) -> Unit,
    onEmergencyBass: () -> Unit,
    onCutThrough: () -> Unit,
    onGoNuclear: () -> Unit,
    onSaveProfileSlot: (Char) -> Unit,
    onLoadProfileSlot: (Char) -> Unit,
    onClearProfileSlot: (Char) -> Unit,
    onResetAll: () -> Unit = {},
    // Callbacks - Main controls
    onBassChange: (Int) -> Unit = {},
    onBassFrequencyChange: (Float) -> Unit = {},
    onLoudnessChange: (Int) -> Unit = {},
    onClarityChange: (Int) -> Unit = {},
    onSpatialChange: (Int) -> Unit = {},
    onEQBandChange: (Int, Int) -> Unit = { _, _ -> },
    // Callbacks - Compressor
    onCompressorEnabledChange: (Boolean) -> Unit = {},
    onCompressorThresholdChange: (Float) -> Unit = {},
    onCompressorRatioChange: (Float) -> Unit = {},
    onCompressorAttackChange: (Float) -> Unit = {},
    onCompressorReleaseChange: (Float) -> Unit = {},
    onCompressorMakeupGainChange: (Float) -> Unit = {},
    // Callbacks - Limiter
    onLimiterEnabledChange: (Boolean) -> Unit = {},
    onLimiterThresholdChange: (Float) -> Unit = {},
    onLimiterCeilingChange: (Float) -> Unit = {},
    onLimiterAttackChange: (Float) -> Unit = {},
    onLimiterReleaseChange: (Float) -> Unit = {},
    // Callbacks - Stereo
    onStereoEnabledChange: (Boolean) -> Unit = {},
    onStereoWidthChange: (Int) -> Unit = {},
    // Callbacks - Exciter
    onExciterEnabledChange: (Boolean) -> Unit = {},
    onExciterDriveChange: (Int) -> Unit = {},
    onExciterMixChange: (Int) -> Unit = {},
    // Callbacks - Reverb
    onReverbEnabledChange: (Boolean) -> Unit = {},
    onReverbPresetChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with master toggle and reset
        BattleHeader(
            isEnabled = isEnabled,
            onToggle = onToggleBattleEngine,
            onReset = onResetAll
        )

        // SPL Battle Meter
        SPLMeterCard(
            enabled = isEnabled,
            ourSPL = ourSPL,
            opponentSPL = opponentSPL,
            currentPeakDb = currentPeakDb,
            isClipping = isClipping
        )

        // Quick Profiles (A/B/C)
        QuickProfilesCard(
            enabled = isEnabled,
            profileSlotA = profileSlotA,
            profileSlotB = profileSlotB,
            profileSlotC = profileSlotC,
            activeSlot = activeProfileSlot,
            onSaveSlot = onSaveProfileSlot,
            onLoadSlot = onLoadProfileSlot,
            onClearSlot = onClearProfileSlot
        )

        // Danger Mode
        DangerModeCard(
            enabled = isEnabled,
            dangerModeEnabled = dangerModeEnabled,
            onDangerModeChange = onDangerModeChange
        )

        // Quick Actions
        QuickActionsCard(
            enabled = isEnabled,
            onEmergencyBass = onEmergencyBass,
            onCutThrough = onCutThrough,
            onGoNuclear = onGoNuclear
        )

        // Battle Mode Selector
        BattleModeSelector(
            currentMode = battleMode,
            enabled = isEnabled,
            onModeSelected = onBattleModeChange
        )

        // Bass Control with Frequency
        BassControlCard(
            bassLevel = bassLevel,
            bassFrequency = bassFrequency,
            enabled = isEnabled,
            onBassChange = { onBassChange(it.toInt()) },
            onFrequencyChange = onBassFrequencyChange
        )

        // Loudness Control
        BattleSliderCard(
            title = "LOUDNESS",
            subtitle = "Maximum SPL (+${loudnessGain/100f}dB)",
            value = loudnessGain,
            maxValue = 1000,
            enabled = isEnabled,
            color = Color(0xFFFF5722),
            icon = Icons.Default.Campaign,
            onValueChange = { onLoudnessChange(it.toInt()) },
            warningThreshold = 800
        )

        // Clarity Control
        BattleSliderCard(
            title = "CLARITY",
            subtitle = "Cut through opponent's sound",
            value = clarityLevel,
            maxValue = 100,
            enabled = isEnabled,
            color = Color(0xFF2196F3),
            icon = Icons.Default.GraphicEq,
            onValueChange = { onClarityChange(it.toInt()) }
        )

        // Spatial Control
        BattleSliderCard(
            title = "SPATIAL",
            subtitle = "Fill the venue with your sound",
            value = spatialLevel,
            maxValue = 1000,
            enabled = isEnabled,
            color = Color(0xFF9C27B0),
            icon = Icons.Default.SpatialAudio,
            onValueChange = { onSpatialChange(it.toInt()) }
        )

        // Equalizer
        if (eqBands.isNotEmpty()) {
            EqualizerCard(
                bands = eqBands,
                enabled = isEnabled,
                onBandChange = onEQBandChange
            )
        }

        // Compressor
        CompressorCard(
            enabled = isEnabled,
            compressorEnabled = compressorEnabled,
            threshold = compressorThreshold,
            ratio = compressorRatio,
            attack = compressorAttack,
            release = compressorRelease,
            makeupGain = compressorMakeupGain,
            onEnabledChange = onCompressorEnabledChange,
            onThresholdChange = onCompressorThresholdChange,
            onRatioChange = onCompressorRatioChange,
            onAttackChange = onCompressorAttackChange,
            onReleaseChange = onCompressorReleaseChange,
            onMakeupGainChange = onCompressorMakeupGainChange
        )

        // Limiter
        LimiterCard(
            enabled = isEnabled,
            limiterEnabled = limiterEnabled,
            threshold = limiterThreshold,
            ceiling = limiterCeiling,
            attack = limiterAttack,
            release = limiterRelease,
            onEnabledChange = onLimiterEnabledChange,
            onThresholdChange = onLimiterThresholdChange,
            onCeilingChange = onLimiterCeilingChange,
            onAttackChange = onLimiterAttackChange,
            onReleaseChange = onLimiterReleaseChange
        )

        // Stereo Widener
        StereoWidenerCard(
            enabled = isEnabled,
            stereoEnabled = stereoWidthEnabled,
            width = stereoWidth,
            onEnabledChange = onStereoEnabledChange,
            onWidthChange = onStereoWidthChange
        )

        // Harmonic Exciter
        ExciterCard(
            enabled = isEnabled,
            exciterEnabled = exciterEnabled,
            drive = exciterDrive,
            mix = exciterMix,
            onEnabledChange = onExciterEnabledChange,
            onDriveChange = onExciterDriveChange,
            onMixChange = onExciterMixChange
        )

        // Reverb
        ReverbCard(
            enabled = isEnabled,
            reverbEnabled = reverbEnabled,
            preset = reverbPreset,
            onEnabledChange = onReverbEnabledChange,
            onPresetChange = onReverbPresetChange
        )

        // Warning Card
        WarningCard()

        // Bottom padding
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==================== HEADER ====================

@Composable
private fun BattleHeader(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                Color(0xFFFF5722).copy(alpha = 0.2f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Campaign,
                    contentDescription = null,
                    tint = if (isEnabled) Color(0xFFFF5722) else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SOUND BATTLE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isEnabled) Color(0xFFFF5722) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEnabled) "Battle Mode Active" else "Tap to enable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, "Reset All", tint = Color.Gray)
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFF5722))
                )
            }
        }
    }
}

// ==================== SPL METER ====================

@Composable
private fun SPLMeterCard(
    enabled: Boolean,
    ourSPL: Float,
    opponentSPL: Float,
    currentPeakDb: Float,
    isClipping: Boolean
) {
    val splDifference = ourSPL - opponentSPL
    val isWinning = splDifference > 0
    val winningColor = Color(0xFF4CAF50)
    val losingColor = Color(0xFFF44336)
    val currentColor by animateColorAsState(
        targetValue = if (isWinning) winningColor else losingColor,
        label = "spl_color"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) currentColor.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Speed, null, tint = if (enabled) currentColor else Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SPL BATTLE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Surface(
                    color = if (isClipping) Color.Red else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "${String.format("%.1f", currentPeakDb)}dB",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isClipping) Color.White else Color.Gray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("YOU", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${ourSPL.toInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (enabled && isWinning) winningColor else Color.Gray)
                    Text("dB SPL", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("VS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    if (enabled) {
                        Text(if (isWinning) "+${splDifference.toInt()}" else "${splDifference.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = currentColor)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("OPPONENT", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${opponentSPL.toInt()}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (enabled && !isWinning) losingColor else Color.Gray)
                    Text("dB SPL", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Visual bars
            Row(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Gray.copy(alpha = 0.2f))) {
                    val ourProgress by animateFloatAsState((ourSPL / 150f).coerceIn(0f, 1f), label = "our")
                    Box(modifier = Modifier.fillMaxWidth(ourProgress).fillMaxHeight().background(Brush.horizontalGradient(listOf(winningColor.copy(alpha = 0.3f), winningColor))).align(Alignment.CenterEnd))
                }
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.White))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Gray.copy(alpha = 0.2f))) {
                    val opponentProgress by animateFloatAsState((opponentSPL / 150f).coerceIn(0f, 1f), label = "opp")
                    Box(modifier = Modifier.fillMaxWidth(opponentProgress).fillMaxHeight().background(Brush.horizontalGradient(listOf(losingColor, losingColor.copy(alpha = 0.3f)))).align(Alignment.CenterStart))
                }
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    when {
                        splDifference > 10 -> "DOMINATING!"
                        splDifference > 5 -> "Winning! Push harder!"
                        splDifference > 0 -> "Slight edge"
                        splDifference > -5 -> "Neck to neck - give it all!"
                        else -> "Getting crushed - GO NUCLEAR!"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = currentColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==================== QUICK PROFILES ====================

@Composable
private fun QuickProfilesCard(
    enabled: Boolean,
    profileSlotA: QuickProfile?,
    profileSlotB: QuickProfile?,
    profileSlotC: QuickProfile?,
    activeSlot: Char?,
    onSaveSlot: (Char) -> Unit,
    onLoadSlot: (Char) -> Unit,
    onClearSlot: (Char) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, null, tint = Color(0xFF9C27B0), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("QUICK PROFILES", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Text("Save & instant recall your battle settings", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileSlot('A', profileSlotA, activeSlot == 'A', enabled, Color(0xFFE91E63), onSaveSlot, onLoadSlot, onClearSlot)
                ProfileSlot('B', profileSlotB, activeSlot == 'B', enabled, Color(0xFF2196F3), onSaveSlot, onLoadSlot, onClearSlot)
                ProfileSlot('C', profileSlotC, activeSlot == 'C', enabled, Color(0xFFFF9800), onSaveSlot, onLoadSlot, onClearSlot)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tap to load | Long-press to save | Double-tap to clear", style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center, fontSize = 9.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProfileSlot(slot: Char, profile: QuickProfile?, isActive: Boolean, enabled: Boolean, color: Color, onSave: (Char) -> Unit, onLoad: (Char) -> Unit, onClear: (Char) -> Unit) {
    var lastClickTime by remember { mutableStateOf(0L) }
    Surface(
        modifier = Modifier.size(70.dp).clickable(enabled = enabled && profile != null) {
            val now = System.currentTimeMillis()
            if (now - lastClickTime < 300) onClear(slot) else onLoad(slot)
            lastClickTime = now
        },
        color = when { isActive -> color; profile != null -> color.copy(alpha = 0.3f); else -> Color.Gray.copy(alpha = 0.1f) },
        shape = RoundedCornerShape(12.dp),
        border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, if (profile != null) color.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(slot.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = when { isActive -> Color.White; profile != null -> color; else -> Color.Gray })
                Text(if (profile != null) "SAVED" else "EMPTY", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = when { isActive -> Color.White.copy(alpha = 0.8f); profile != null -> color.copy(alpha = 0.8f); else -> Color.Gray.copy(alpha = 0.5f) })
            }
        }
    }
}

// ==================== DANGER MODE ====================

@Composable
private fun DangerModeCard(enabled: Boolean, dangerModeEnabled: Boolean, onDangerModeChange: (Boolean) -> Unit) {
    val borderColor by animateColorAsState(if (dangerModeEnabled && enabled) Color.Red else Color.Transparent, label = "danger")
    Card(
        modifier = Modifier.border(if (dangerModeEnabled && enabled) 2.dp else 0.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = if (dangerModeEnabled && enabled) Color.Red.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, null, tint = if (dangerModeEnabled && enabled) Color.Red else Color.Gray, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("DANGER MODE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (dangerModeEnabled && enabled) Color.Red else MaterialTheme.colorScheme.onSurface)
                    Text("Bypass limiter - MAXIMUM OUTPUT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
            Switch(checked = dangerModeEnabled, onCheckedChange = onDangerModeChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = Color.Red, checkedThumbColor = Color.White))
        }
    }
}

// ==================== QUICK ACTIONS ====================

@Composable
private fun QuickActionsCard(enabled: Boolean, onEmergencyBass: () -> Unit, onCutThrough: () -> Unit, onGoNuclear: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (enabled) Color(0xFFFF5722).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("QUICK ACTIONS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionButton("BASS\nDROP", Color(0xFFE91E63), enabled, onEmergencyBass)
                ActionButton("CUT\nTHROUGH", Color(0xFF2196F3), enabled, onCutThrough)
                ActionButton("GO\nNUCLEAR", Color(0xFFFF5722), enabled, onGoNuclear)
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, colors = ButtonDefaults.buttonColors(containerColor = color, disabledContainerColor = color.copy(alpha = 0.3f)), modifier = Modifier.size(width = 90.dp, height = 60.dp), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(4.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 14.sp)
    }
}

// ==================== BATTLE MODE SELECTOR ====================

@Composable
private fun BattleModeSelector(currentMode: BattleMode, enabled: Boolean, onModeSelected: (BattleMode) -> Unit) {
    Column {
        Text("Battle Mode", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val modes = listOf(BattleMode.OFF to "Off", BattleMode.BASS_WARFARE to "Bass War", BattleMode.CLARITY_STRIKE to "Clarity", BattleMode.FULL_ASSAULT to "Full Assault", BattleMode.SPL_MONSTER to "SPL Monster", BattleMode.CROWD_REACH to "Crowd Reach")
            items(modes) { (mode, label) ->
                FilterChip(selected = currentMode == mode, onClick = { onModeSelected(mode) }, enabled = enabled || mode == BattleMode.OFF, label = { Text(label, fontSize = 11.sp, fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal) })
            }
        }
    }
}

// ==================== BASS CONTROL ====================

@Composable
private fun BassControlCard(bassLevel: Int, bassFrequency: Float, enabled: Boolean, onBassChange: (Float) -> Unit, onFrequencyChange: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = Color(0xFFE91E63)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, null, tint = if (enabled) color else color.copy(alpha = 0.3f), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("BASS POWER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Boost: ${bassLevel/10}% @ ${bassFrequency.toInt()}Hz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = if (enabled) color.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text("${bassLevel/10}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (enabled) color else Color.Gray, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Bass Boost", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Slider(value = bassLevel.toFloat(), onValueChange = onBassChange, valueRange = 0f..1000f, enabled = enabled, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color))
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Center Frequency: ${bassFrequency.toInt()}Hz", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Slider(value = bassFrequency, onValueChange = onFrequencyChange, valueRange = 20f..200f, enabled = enabled, colors = SliderDefaults.colors(thumbColor = color.copy(alpha = 0.7f), activeTrackColor = color.copy(alpha = 0.7f)))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("20Hz (Sub)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("80Hz", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("200Hz", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ==================== BATTLE SLIDER CARD ====================

@Composable
private fun BattleSliderCard(title: String, subtitle: String, value: Int, maxValue: Int, enabled: Boolean, color: Color, icon: ImageVector, onValueChange: (Float) -> Unit, warningThreshold: Int = maxValue + 1) {
    val isWarning = value >= warningThreshold
    Card(colors = CardDefaults.cardColors(containerColor = if (isWarning && enabled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = if (enabled) color else color.copy(alpha = 0.3f), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Tap to edit value manually
                if (enabled) {
                    EditableIntBox(
                        value = value,
                        min = 0,
                        max = maxValue,
                        color = color,
                        onValueChange = { newValue -> onValueChange(newValue.toFloat()) }
                    )
                } else {
                    Surface(color = Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text("${(value * 100f / maxValue).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Slider(value = value.toFloat(), onValueChange = onValueChange, valueRange = 0f..maxValue.toFloat(), enabled = enabled, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = color.copy(alpha = 0.2f)))
            if (isWarning && enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("High level may cause distortion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ==================== EQUALIZER ====================

@Composable
private fun EqualizerCard(bands: List<EQBand>, enabled: Boolean, onBandChange: (Int, Int) -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Equalizer, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EQUALIZER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                bands.forEach { band ->
                    EQBandSlider(band, enabled) { onBandChange(band.index, it) }
                }
            }
        }
    }
}

@Composable
private fun EQBandSlider(band: EQBand, enabled: Boolean, onValueChange: (Int) -> Unit) {
    val color = when (band.index) { 0 -> Color(0xFFE91E63); 1 -> Color(0xFFFF5722); 2 -> Color(0xFFFFEB3B); 3 -> Color(0xFF4CAF50); 4 -> Color(0xFF2196F3); else -> Color.Gray }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${band.levelDb.toInt()}dB", style = MaterialTheme.typography.labelSmall, color = if (enabled) color else Color.Gray)
        Box(modifier = Modifier.weight(1f).width(40.dp)) {
            Box(modifier = Modifier.fillMaxHeight().width(8.dp).align(Alignment.Center).clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.2f)))
            val progress = (band.currentLevel - band.minLevel).toFloat() / (band.maxLevel - band.minLevel)
            Box(modifier = Modifier.fillMaxHeight(progress).width(8.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(4.dp)).background(if (enabled) color else Color.Gray))
        }
        Text(band.frequencyLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==================== COMPRESSOR ====================

@Composable
private fun CompressorCard(enabled: Boolean, compressorEnabled: Boolean, threshold: Float, ratio: Float, attack: Float, release: Float, makeupGain: Float, onEnabledChange: (Boolean) -> Unit, onThresholdChange: (Float) -> Unit, onRatioChange: (Float) -> Unit, onAttackChange: (Float) -> Unit, onReleaseChange: (Float) -> Unit, onMakeupGainChange: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = Color(0xFF4CAF50)
    Card(colors = CardDefaults.cardColors(containerColor = if (compressorEnabled && enabled) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Compress, null, tint = if (enabled && compressorEnabled) color else Color.Gray, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("COMPRESSOR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ratio ${ratio.toInt()}:1 @ ${threshold.toInt()}dB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = compressorEnabled, onCheckedChange = onEnabledChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = color))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    ParamSlider("Threshold", threshold, -60f..0f, "dB", enabled && compressorEnabled, color, onThresholdChange)
                    ParamSlider("Ratio", ratio, 1f..20f, ":1", enabled && compressorEnabled, color, onRatioChange)
                    ParamSlider("Attack", attack, 0.1f..200f, "ms", enabled && compressorEnabled, color, onAttackChange)
                    ParamSlider("Release", release, 10f..1000f, "ms", enabled && compressorEnabled, color, onReleaseChange)
                    ParamSlider("Makeup Gain", makeupGain, 0f..24f, "dB", enabled && compressorEnabled, color, onMakeupGainChange)
                }
            }
        }
    }
}

// ==================== LIMITER ====================

@Composable
private fun LimiterCard(enabled: Boolean, limiterEnabled: Boolean, threshold: Float, ceiling: Float, attack: Float, release: Float, onEnabledChange: (Boolean) -> Unit, onThresholdChange: (Float) -> Unit, onCeilingChange: (Float) -> Unit, onAttackChange: (Float) -> Unit, onReleaseChange: (Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = Color(0xFFF44336)
    Card(colors = CardDefaults.cardColors(containerColor = if (limiterEnabled && enabled) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = if (enabled && limiterEnabled) color else Color.Gray, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("LIMITER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Threshold: ${threshold}dB, Ceiling: ${ceiling}dB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = limiterEnabled, onCheckedChange = onEnabledChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = color))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    ParamSlider("Threshold", threshold, -12f..0f, "dB", enabled && limiterEnabled, color, onThresholdChange)
                    ParamSlider("Ceiling", ceiling, -3f..0f, "dB", enabled && limiterEnabled, color, onCeilingChange)
                    ParamSlider("Attack", attack, 0.1f..10f, "ms", enabled && limiterEnabled, color, onAttackChange)
                    ParamSlider("Release", release, 10f..500f, "ms", enabled && limiterEnabled, color, onReleaseChange)
                }
            }
        }
    }
}

// ==================== STEREO WIDENER ====================

@Composable
private fun StereoWidenerCard(enabled: Boolean, stereoEnabled: Boolean, width: Int, onEnabledChange: (Boolean) -> Unit, onWidthChange: (Int) -> Unit) {
    val color = Color(0xFF9C27B0)
    val widthLabel = when { width < 80 -> "Mono"; width < 120 -> "Normal"; width < 160 -> "Wide"; else -> "Ultra Wide" }
    Card(colors = CardDefaults.cardColors(containerColor = if (stereoEnabled && enabled) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SpatialAudio, null, tint = if (enabled && stereoEnabled) color else Color.Gray, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("STEREO WIDENER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$widthLabel ($width%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = stereoEnabled, onCheckedChange = onEnabledChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = color))
            }
            if (stereoEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Stereo Width", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Slider(value = width.toFloat(), onValueChange = { onWidthChange(it.toInt()) }, valueRange = 0f..200f, enabled = enabled, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color))
            }
        }
    }
}

// ==================== EXCITER ====================

@Composable
private fun ExciterCard(enabled: Boolean, exciterEnabled: Boolean, drive: Int, mix: Int, onEnabledChange: (Boolean) -> Unit, onDriveChange: (Int) -> Unit, onMixChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val color = Color(0xFFFF9800)
    Card(colors = CardDefaults.cardColors(containerColor = if (exciterEnabled && enabled) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, null, tint = if (enabled && exciterEnabled) color else Color.Gray, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("HARMONIC EXCITER", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Adds presence & air to highs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = exciterEnabled, onCheckedChange = onEnabledChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = color))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
            }
            AnimatedVisibility(visible = expanded && exciterEnabled, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Drive: $drive%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Slider(value = drive.toFloat(), onValueChange = { onDriveChange(it.toInt()) }, valueRange = 0f..100f, enabled = enabled, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Mix (Dry/Wet): $mix%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Slider(value = mix.toFloat(), onValueChange = { onMixChange(it.toInt()) }, valueRange = 0f..100f, enabled = enabled, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color))
                }
            }
        }
    }
}

// ==================== REVERB ====================

@Composable
private fun ReverbCard(enabled: Boolean, reverbEnabled: Boolean, preset: Int, onEnabledChange: (Boolean) -> Unit, onPresetChange: (Int) -> Unit) {
    val color = Color(0xFF00BCD4)
    val presetNames = listOf("None", "Small Room", "Medium Room", "Large Room", "Medium Hall", "Large Hall", "Plate")
    Card(colors = CardDefaults.cardColors(containerColor = if (reverbEnabled && enabled) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Waves, null, tint = if (enabled && reverbEnabled) color else Color.Gray, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("REVERB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(presetNames.getOrElse(preset) { "None" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = reverbEnabled, onCheckedChange = onEnabledChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = color))
            }
            if (reverbEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presetNames.size) { index ->
                        FilterChip(selected = preset == index, onClick = { onPresetChange(index) }, enabled = enabled, label = { Text(presetNames[index], fontSize = 12.sp) })
                    }
                }
            }
        }
    }
}

// ==================== PARAMETER SLIDER ====================

@Composable
private fun ParamSlider(label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, unit: String, enabled: Boolean, color: Color, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray)
            // Tap to edit value manually
            if (enabled) {
                EditableFloatBox(
                    value = value,
                    min = valueRange.start,
                    max = valueRange.endInclusive,
                    color = color,
                    onValueChange = onValueChange,
                    decimals = 1,
                    suffix = unit
                )
            } else {
                Surface(color = Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                    Text("${String.format("%.1f", value)}$unit", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, enabled = enabled, colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = color.copy(alpha = 0.2f)))
    }
}

// ==================== WARNING CARD ====================

@Composable
private fun WarningCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Speaker Protection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("High levels can damage speakers and hearing. Use responsibly in competition.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
