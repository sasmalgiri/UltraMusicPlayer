package com.ultramusic.player.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.audio.BattleMode
import com.ultramusic.player.audio.EQBand
import com.ultramusic.player.ui.MainViewModel
import com.ultramusic.player.ui.components.FloatValueEditDialog
import com.ultramusic.player.ui.components.IntValueEditDialog

/**
 * Audio Battle Screen
 * 
 * Controls for SOUND SYSTEM COMPETITIONS:
 * - Bass boost to overpower opponent
 * - Loudness to maximize SPL
 * - Clarity to cut through
 * - Spatial to fill venue
 * - EQ for fine-tuning
 * - Quick action buttons for battle moments
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioBattleScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val isEnabled by viewModel.battleEngineEnabled.collectAsState()
    val battleMode by viewModel.battleMode.collectAsState()
    val bassLevel by viewModel.battleBassLevel.collectAsState()
    val loudnessGain by viewModel.battleLoudness.collectAsState()
    val clarityLevel by viewModel.battleClarity.collectAsState()
    val spatialLevel by viewModel.battleSpatial.collectAsState()
    val eqBands by viewModel.battleEQBands.collectAsState()

    // Advanced controls state
    val compressorEnabled by viewModel.compressorEnabled.collectAsState()
    val compressorThreshold by viewModel.compressorThreshold.collectAsState()
    val compressorRatio by viewModel.compressorRatio.collectAsState()
    val compressorAttack by viewModel.compressorAttack.collectAsState()
    val compressorRelease by viewModel.compressorRelease.collectAsState()
    val compressorMakeupGain by viewModel.compressorMakeupGain.collectAsState()

    val limiterEnabled by viewModel.limiterEnabled.collectAsState()
    val limiterThreshold by viewModel.limiterThreshold.collectAsState()
    val limiterCeiling by viewModel.limiterCeiling.collectAsState()
    val limiterAttack by viewModel.limiterAttack.collectAsState()
    val limiterRelease by viewModel.limiterRelease.collectAsState()

    val bassFrequency by viewModel.bassFrequency.collectAsState()

    val stereoWidthEnabled by viewModel.stereoWidthEnabled.collectAsState()
    val stereoWidth by viewModel.stereoWidth.collectAsState()

    val exciterEnabled by viewModel.exciterEnabled.collectAsState()
    val exciterDrive by viewModel.exciterDrive.collectAsState()
    val exciterMix by viewModel.exciterMix.collectAsState()

    val reverbEnabled by viewModel.reverbEnabled.collectAsState()
    val reverbPreset by viewModel.reverbPreset.collectAsState()

    // Danger mode
    val dangerModeEnabled by viewModel.dangerModeEnabled.collectAsState()

    // Peak dB monitoring
    val currentPeakDb by viewModel.currentPeakDb.collectAsState()
    val isClipping by viewModel.isClipping.collectAsState()

    // Quick profile slots
    val profileSlotA by viewModel.profileSlotA.collectAsState()
    val profileSlotB by viewModel.profileSlotB.collectAsState()
    val profileSlotC by viewModel.profileSlotC.collectAsState()
    val activeProfileSlot by viewModel.activeProfileSlot.collectAsState()

    // SPL (from active battle system)
    val ourSPL by viewModel.ourSPL.collectAsState()
    val opponentSPL by viewModel.opponentSPL.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFFF5722)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Sound Battle",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Reset button
                    IconButton(onClick = { viewModel.resetAllAudioEffects() }) {
                        Icon(Icons.Default.Refresh, "Reset All", tint = Color.Gray)
                    }
                    // Master enable switch
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleBattleEngine() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isEnabled)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== SPL METER - YOUR vs OPPONENT =====
            item {
                SPLMeterCard(
                    enabled = isEnabled,
                    ourSPL = ourSPL,
                    opponentSPL = opponentSPL,
                    currentPeakDb = currentPeakDb,
                    isClipping = isClipping
                )
            }

            // ===== QUICK PROFILE SLOTS (A/B/C) =====
            item {
                QuickProfileSlotsCard(
                    enabled = isEnabled,
                    profileSlotA = profileSlotA,
                    profileSlotB = profileSlotB,
                    profileSlotC = profileSlotC,
                    activeSlot = activeProfileSlot,
                    onSaveSlot = { viewModel.saveToProfileSlot(it) },
                    onLoadSlot = { viewModel.loadFromProfileSlot(it) },
                    onClearSlot = { viewModel.clearProfileSlot(it) }
                )
            }

            // ===== BATTLE OUTPUT (2 MODE) =====
            item {
                com.ultramusic.player.ui.components.BattleOutputModeSelectorCard(
                    enabled = isEnabled,
                    dangerModeEnabled = dangerModeEnabled,
                    limiterEnabled = limiterEnabled,
                    onSelectPleasant = {
                        viewModel.setDangerModeEnabled(false)
                        viewModel.setLimiterEnabled(true)
                    },
                    onSelectUnsafe = {
                        viewModel.setDangerModeEnabled(true)
                        viewModel.setLimiterEnabled(false)
                    }
                )
            }

            // ===== DANGER MODE =====
            item {
                DangerModeCard(
                    enabled = isEnabled,
                    dangerModeEnabled = dangerModeEnabled,
                    onDangerModeChange = { viewModel.setDangerModeEnabled(it) }
                )
            }

            // ===== QUICK ACTION BUTTONS =====
            item {
                QuickActionButtons(
                    enabled = isEnabled,
                    onEmergencyBass = { viewModel.emergencyBassBoost() },
                    onCutThrough = { viewModel.cutThrough() },
                    onGoNuclear = { viewModel.goNuclear() }
                )
            }

            // ===== BATTLE MODE SELECTOR =====
            item {
                BattleModeSelector(
                    currentMode = battleMode,
                    enabled = isEnabled,
                    onModeSelected = { viewModel.setBattleMode(it) }
                )
            }

            // ===== BASS CONTROL WITH FREQUENCY =====
            item {
                BassControlCard(
                    bassLevel = bassLevel,
                    bassFrequency = bassFrequency,
                    enabled = isEnabled,
                    onBassChange = { viewModel.setBattleBass(it.toInt()) },
                    onFrequencyChange = { viewModel.setBassFrequency(it) }
                )
            }

            // ===== LOUDNESS CONTROL =====
            item {
                BattleControlCard(
                    title = "📢 LOUDNESS",
                    subtitle = "Maximum SPL (+${loudnessGain/100f}dB)",
                    value = loudnessGain,
                    maxValue = 1000,
                    enabled = isEnabled,
                    color = Color(0xFFFF5722),
                    icon = Icons.Default.Campaign,
                    onValueChange = { viewModel.setBattleLoudness(it.toInt()) },
                    warningThreshold = 800
                )
            }

            // ===== CLARITY CONTROL =====
            item {
                BattleControlCard(
                    title = "🎯 CLARITY",
                    subtitle = "Cut through opponent's sound",
                    value = clarityLevel,
                    maxValue = 100,
                    enabled = isEnabled,
                    color = Color(0xFF2196F3),
                    icon = Icons.Default.GraphicEq,
                    onValueChange = { viewModel.setBattleClarity(it.toInt()) }
                )
            }

            // ===== SPATIAL CONTROL =====
            item {
                BattleControlCard(
                    title = "🌊 SPATIAL",
                    subtitle = "Fill the venue with your sound",
                    value = spatialLevel,
                    maxValue = 1000,
                    enabled = isEnabled,
                    color = Color(0xFF9C27B0),
                    icon = Icons.Default.SpatialAudio,
                    onValueChange = { viewModel.setBattleSpatial(it.toInt()) }
                )
            }

            // ===== EQUALIZER =====
            item {
                EqualizerCard(
                    bands = eqBands,
                    enabled = isEnabled,
                    onBandChange = { index, level -> viewModel.setBattleEQBand(index, level) }
                )
            }

            // ===== COMPRESSOR CONTROLS =====
            item {
                CompressorCard(
                    enabled = isEnabled,
                    compressorEnabled = compressorEnabled,
                    threshold = compressorThreshold,
                    ratio = compressorRatio,
                    attack = compressorAttack,
                    release = compressorRelease,
                    makeupGain = compressorMakeupGain,
                    onEnabledChange = { viewModel.setCompressorEnabled(it) },
                    onThresholdChange = { viewModel.setCompressorThreshold(it) },
                    onRatioChange = { viewModel.setCompressorRatio(it) },
                    onAttackChange = { viewModel.setCompressorAttack(it) },
                    onReleaseChange = { viewModel.setCompressorRelease(it) },
                    onMakeupGainChange = { viewModel.setCompressorMakeupGain(it) }
                )
            }

            // ===== LIMITER CONTROLS =====
            item {
                LimiterCard(
                    enabled = isEnabled,
                    limiterEnabled = limiterEnabled,
                    threshold = limiterThreshold,
                    ceiling = limiterCeiling,
                    attack = limiterAttack,
                    release = limiterRelease,
                    onEnabledChange = { viewModel.setLimiterEnabled(it) },
                    onThresholdChange = { viewModel.setLimiterThreshold(it) },
                    onCeilingChange = { viewModel.setLimiterCeiling(it) },
                    onAttackChange = { viewModel.setLimiterAttack(it) },
                    onReleaseChange = { viewModel.setLimiterRelease(it) }
                )
            }

            // ===== STEREO WIDENER =====
            item {
                StereoWidenerCard(
                    enabled = isEnabled,
                    stereoEnabled = stereoWidthEnabled,
                    width = stereoWidth,
                    onEnabledChange = { viewModel.setStereoWidthEnabled(it) },
                    onWidthChange = { viewModel.setStereoWidth(it) }
                )
            }

            // ===== HARMONIC EXCITER =====
            item {
                ExciterCard(
                    enabled = isEnabled,
                    exciterEnabled = exciterEnabled,
                    drive = exciterDrive,
                    mix = exciterMix,
                    onEnabledChange = { viewModel.setExciterEnabled(it) },
                    onDriveChange = { viewModel.setExciterDrive(it) },
                    onMixChange = { viewModel.setExciterMix(it) }
                )
            }

            // ===== REVERB =====
            item {
                ReverbCard(
                    enabled = isEnabled,
                    reverbEnabled = reverbEnabled,
                    preset = reverbPreset,
                    onEnabledChange = { viewModel.setReverbEnabled(it) },
                    onPresetChange = { viewModel.setReverbPreset(it) }
                )
            }

            // ===== WARNING =====
            item {
                WarningCard()
            }
        }
    }
}

@Composable
private fun QuickActionButtons(
    enabled: Boolean,
    onEmergencyBass: () -> Unit,
    onCutThrough: () -> Unit,
    onGoNuclear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚡ QUICK ACTIONS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionButton(
                    text = "🔊 BASS\nDROP",
                    color = Color(0xFFE91E63),
                    enabled = enabled,
                    onClick = onEmergencyBass
                )
                
                QuickActionButton(
                    text = "⚔️ CUT\nTHROUGH",
                    color = Color(0xFF2196F3),
                    enabled = enabled,
                    onClick = onCutThrough
                )
                
                QuickActionButton(
                    text = "☢️ GO\nNUCLEAR",
                    color = Color(0xFFFF5722),
                    enabled = enabled,
                    onClick = onGoNuclear
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = color.copy(alpha = 0.3f)
        ),
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BattleModeSelector(
    currentMode: BattleMode,
    enabled: Boolean,
    onModeSelected: (BattleMode) -> Unit
) {
    Column {
        Text(
            text = "Battle Mode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modes = listOf(
                BattleMode.OFF to "⭕ Off",
                BattleMode.BASS_WARFARE to "🔊 Bass War",
                BattleMode.CLARITY_STRIKE to "⚔️ Clarity",
                BattleMode.FULL_ASSAULT to "💀 Full Assault",
                BattleMode.SPL_MONSTER to "📊 SPL Monster",
                BattleMode.CROWD_REACH to "🎯 Crowd Reach"
            )
            
            items(modes) { (mode, label) ->
                FilterChip(
                    selected = currentMode == mode,
                    onClick = { onModeSelected(mode) },
                    enabled = enabled || mode == BattleMode.OFF,
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun BattleControlCard(
    title: String,
    subtitle: String,
    value: Int,
    maxValue: Int,
    enabled: Boolean,
    color: Color,
    icon: ImageVector,
    onValueChange: (Float) -> Unit,
    warningThreshold: Int = maxValue + 1
) {
    val isWarning = value >= warningThreshold
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        IntValueEditDialog(
            title = "Edit $title",
            initialValue = value,
            valueRange = 0..maxValue,
            onDismiss = { showEditDialog = false },
            onConfirm = {
                showEditDialog = false
                onValueChange(it.toFloat())
            }
        )
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning && enabled)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) color else color.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Value display
                Surface(
                    color = if (enabled) color.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${(value * 100f / maxValue).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) color else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                if (enabled) {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit $title", tint = color)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Slider
            Slider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = 0f..maxValue.toFloat(),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = color.copy(alpha = 0.2f)
                )
            )
            
            // Warning
            if (isWarning && enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "High level may cause distortion",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerCard(
    bands: List<EQBand>,
    enabled: Boolean,
    onBandChange: (Int, Int) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EQUALIZER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // EQ Bands
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bands.forEach { band ->
                    EQBandSlider(
                        band = band,
                        enabled = enabled,
                        onValueChange = { onBandChange(band.index, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EQBandSlider(
    band: EQBand,
    enabled: Boolean,
    onValueChange: (Int) -> Unit
) {
    val color = when (band.index) {
        0 -> Color(0xFFE91E63)  // Sub-bass - pink
        1 -> Color(0xFFFF5722)  // Bass - orange
        2 -> Color(0xFFFFEB3B)  // Low-mid - yellow
        3 -> Color(0xFF4CAF50)  // Mid - green
        4 -> Color(0xFF2196F3)  // High - blue
        else -> Color.Gray
    }

    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        IntValueEditDialog(
            title = "Edit ${band.frequencyLabel}",
            initialValue = band.currentLevel,
            valueRange = band.minLevel..band.maxLevel,
            onDismiss = { showEditDialog = false },
            onConfirm = {
                showEditDialog = false
                onValueChange(it)
            }
        )
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${band.levelDb.toInt()}dB",
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) color else Color.Gray
            )
            if (enabled) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit ${band.frequencyLabel}", tint = color)
                }
            }
        }
        
        // Vertical slider
        Box(
            modifier = Modifier
                .weight(1f)
                .width(40.dp)
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(8.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.2f))
            )
            
            // Active track
            val progress = (band.currentLevel - band.minLevel).toFloat() / 
                          (band.maxLevel - band.minLevel)
            Box(
                modifier = Modifier
                    .fillMaxHeight(progress)
                    .width(8.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (enabled) color else Color.Gray)
            )
            
            // Slider (invisible, for interaction)
            androidx.compose.material3.Slider(
                value = band.currentLevel.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                enabled = enabled,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
                    .align(Alignment.Center),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
        
        // Frequency label
        Text(
            text = band.frequencyLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== BASS CONTROL WITH FREQUENCY ====================

@Composable
private fun BassControlCard(
    bassLevel: Int,
    bassFrequency: Float,
    enabled: Boolean,
    onBassChange: (Float) -> Unit,
    onFrequencyChange: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val color = Color(0xFFE91E63)
    var showBassEditDialog by remember { mutableStateOf(false) }
    var showFreqEditDialog by remember { mutableStateOf(false) }

    if (showBassEditDialog) {
        IntValueEditDialog(
            title = "Edit Bass Boost",
            initialValue = bassLevel / 10,
            valueRange = 0..100,
            suffix = "%",
            onDismiss = { showBassEditDialog = false },
            onConfirm = {
                showBassEditDialog = false
                onBassChange((it * 10).toFloat())
            }
        )
    }

    if (showFreqEditDialog) {
        FloatValueEditDialog(
            title = "Edit Bass Frequency",
            initialValue = bassFrequency,
            valueRange = 20f..200f,
            decimals = 0,
            suffix = "Hz",
            onDismiss = { showFreqEditDialog = false },
            onConfirm = {
                showFreqEditDialog = false
                onFrequencyChange(it)
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = if (enabled) color else color.copy(alpha = 0.3f),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔊 BASS POWER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Boost: ${bassLevel/10}% @ ${bassFrequency.toInt()}Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = if (enabled) color.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${bassLevel/10}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) color else Color.Gray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                if (enabled) {
                    IconButton(onClick = { showBassEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Bass Boost", tint = color)
                    }
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main bass slider
            Text("Bass Boost", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Slider(
                value = bassLevel.toFloat(),
                onValueChange = onBassChange,
                valueRange = 0f..1000f,
                enabled = enabled,
                colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Frequency selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Center Frequency: ${bassFrequency.toInt()}Hz",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        if (enabled) {
                            IconButton(onClick = { showFreqEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Bass Frequency", tint = color.copy(alpha = 0.7f))
                            }
                        }
                    }
                    Slider(
                        value = bassFrequency,
                        onValueChange = onFrequencyChange,
                        valueRange = 20f..200f,
                        enabled = enabled,
                        colors = SliderDefaults.colors(thumbColor = color.copy(alpha = 0.7f), activeTrackColor = color.copy(alpha = 0.7f))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("20Hz (Sub)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("80Hz", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("200Hz", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ==================== COMPRESSOR CARD ====================

@Composable
private fun CompressorCard(
    enabled: Boolean,
    compressorEnabled: Boolean,
    threshold: Float,
    ratio: Float,
    attack: Float,
    release: Float,
    makeupGain: Float,
    onEnabledChange: (Boolean) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onRatioChange: (Float) -> Unit,
    onAttackChange: (Float) -> Unit,
    onReleaseChange: (Float) -> Unit,
    onMakeupGainChange: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val color = Color(0xFF4CAF50)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (compressorEnabled && enabled)
                color.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Compress,
                    contentDescription = null,
                    tint = if (enabled && compressorEnabled) color else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎚️ COMPRESSOR",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ratio ${ratio.toInt()}:1 @ ${threshold.toInt()}dB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = compressorEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = color)
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Threshold
                    ParameterSlider(
                        label = "Threshold",
                        value = threshold,
                        valueRange = -60f..0f,
                        unit = "dB",
                        enabled = enabled && compressorEnabled,
                        color = color,
                        onValueChange = onThresholdChange,
                        explanation = "Signal level where compression starts. Lower = more compression"
                    )

                    // Ratio
                    ParameterSlider(
                        label = "Ratio",
                        value = ratio,
                        valueRange = 1f..20f,
                        unit = ":1",
                        enabled = enabled && compressorEnabled,
                        color = color,
                        onValueChange = onRatioChange,
                        explanation = "How much to reduce signal above threshold. Higher = more squash"
                    )

                    // Attack
                    ParameterSlider(
                        label = "Attack",
                        value = attack,
                        valueRange = 0.1f..200f,
                        unit = "ms",
                        enabled = enabled && compressorEnabled,
                        color = color,
                        onValueChange = onAttackChange,
                        explanation = "How fast compression kicks in. Fast = punchy, Slow = natural"
                    )

                    // Release
                    ParameterSlider(
                        label = "Release",
                        value = release,
                        valueRange = 10f..1000f,
                        unit = "ms",
                        enabled = enabled && compressorEnabled,
                        color = color,
                        onValueChange = onReleaseChange,
                        explanation = "How fast compression lets go. Short = pumping, Long = smooth"
                    )

                    // Makeup Gain
                    ParameterSlider(
                        label = "Makeup Gain",
                        value = makeupGain,
                        valueRange = 0f..24f,
                        unit = "dB",
                        enabled = enabled && compressorEnabled,
                        color = color,
                        onValueChange = onMakeupGainChange,
                        explanation = "Boost after compression to restore loudness. MORE POWER!"
                    )
                }
            }
        }
    }
}

// ==================== LIMITER CARD ====================

@Composable
private fun LimiterCard(
    enabled: Boolean,
    limiterEnabled: Boolean,
    threshold: Float,
    ceiling: Float,
    attack: Float,
    release: Float,
    onEnabledChange: (Boolean) -> Unit,
    onThresholdChange: (Float) -> Unit,
    onCeilingChange: (Float) -> Unit,
    onAttackChange: (Float) -> Unit,
    onReleaseChange: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val color = Color(0xFFF44336)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (limiterEnabled && enabled)
                color.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (enabled && limiterEnabled) color else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🛡️ LIMITER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Threshold: ${threshold}dB, Ceiling: ${ceiling}dB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = limiterEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = color)
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Threshold
                    ParameterSlider(
                        label = "Threshold",
                        value = threshold,
                        valueRange = -12f..0f,
                        unit = "dB",
                        enabled = enabled && limiterEnabled,
                        color = color,
                        onValueChange = onThresholdChange,
                        explanation = "Level where limiting starts. Lower = more limiting, louder output"
                    )

                    // Ceiling
                    ParameterSlider(
                        label = "Ceiling",
                        value = ceiling,
                        valueRange = -3f..0f,
                        unit = "dB",
                        enabled = enabled && limiterEnabled,
                        color = color,
                        onValueChange = onCeilingChange,
                        explanation = "Maximum output level. -0.1dB prevents digital clipping"
                    )

                    // Attack
                    ParameterSlider(
                        label = "Attack",
                        value = attack,
                        valueRange = 0.1f..10f,
                        unit = "ms",
                        enabled = enabled && limiterEnabled,
                        color = color,
                        onValueChange = onAttackChange,
                        explanation = "How fast limiter reacts. Ultra-fast for transient control"
                    )

                    // Release
                    ParameterSlider(
                        label = "Release",
                        value = release,
                        valueRange = 10f..500f,
                        unit = "ms",
                        enabled = enabled && limiterEnabled,
                        color = color,
                        onValueChange = onReleaseChange,
                        explanation = "Recovery time. Fast = aggressive, Slow = transparent"
                    )
                }
            }
        }
    }
}

// ==================== STEREO WIDENER CARD ====================

@Composable
private fun StereoWidenerCard(
    enabled: Boolean,
    stereoEnabled: Boolean,
    width: Int,
    onEnabledChange: (Boolean) -> Unit,
    onWidthChange: (Int) -> Unit
) {
    val color = Color(0xFF9C27B0)
    var showWidthEditDialog by remember { mutableStateOf(false) }
    val widthLabel = when {
        width < 80 -> "Mono"
        width < 120 -> "Normal"
        width < 160 -> "Wide"
        else -> "Ultra Wide"
    }

    if (showWidthEditDialog) {
        IntValueEditDialog(
            title = "Edit Stereo Width",
            initialValue = width,
            valueRange = 0..200,
            suffix = "%",
            onDismiss = { showWidthEditDialog = false },
            onConfirm = {
                showWidthEditDialog = false
                onWidthChange(it)
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (stereoEnabled && enabled)
                color.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SpatialAudio,
                    contentDescription = null,
                    tint = if (enabled && stereoEnabled) color else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔈 STEREO WIDENER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$widthLabel ($width%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = stereoEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = color)
                )
            }

            if (stereoEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stereo Width", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    if (enabled) {
                        IconButton(onClick = { showWidthEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Stereo Width", tint = color)
                        }
                    }
                }
                Slider(
                    value = width.toFloat(),
                    onValueChange = { onWidthChange(it.toInt()) },
                    valueRange = 0f..200f,
                    enabled = enabled,
                    colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mono", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("Normal", style = MaterialTheme.typography.labelSmall, color = color)
                    Text("Ultra Wide", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}

// ==================== HARMONIC EXCITER CARD ====================

@Composable
private fun ExciterCard(
    enabled: Boolean,
    exciterEnabled: Boolean,
    drive: Int,
    mix: Int,
    onEnabledChange: (Boolean) -> Unit,
    onDriveChange: (Int) -> Unit,
    onMixChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val color = Color(0xFFFF9800)
    var showDriveEditDialog by remember { mutableStateOf(false) }
    var showMixEditDialog by remember { mutableStateOf(false) }

    if (showDriveEditDialog) {
        IntValueEditDialog(
            title = "Edit Exciter Drive",
            initialValue = drive,
            valueRange = 0..100,
            suffix = "%",
            onDismiss = { showDriveEditDialog = false },
            onConfirm = {
                showDriveEditDialog = false
                onDriveChange(it)
            }
        )
    }

    if (showMixEditDialog) {
        IntValueEditDialog(
            title = "Edit Exciter Mix",
            initialValue = mix,
            valueRange = 0..100,
            suffix = "%",
            onDismiss = { showMixEditDialog = false },
            onConfirm = {
                showMixEditDialog = false
                onMixChange(it)
            }
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (exciterEnabled && enabled)
                color.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (enabled && exciterEnabled) color else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "✨ HARMONIC EXCITER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Adds presence & air to highs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = exciterEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = color)
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(
                visible = expanded && exciterEnabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Drive
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Drive: $drive%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        if (enabled) {
                            IconButton(onClick = { showDriveEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Drive", tint = color)
                            }
                        }
                    }
                    Slider(
                        value = drive.toFloat(),
                        onValueChange = { onDriveChange(it.toInt()) },
                        valueRange = 0f..100f,
                        enabled = enabled,
                        colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mix (Dry/Wet): $mix%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        if (enabled) {
                            IconButton(onClick = { showMixEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Mix", tint = color)
                            }
                        }
                    }
                    Slider(
                        value = mix.toFloat(),
                        onValueChange = { onMixChange(it.toInt()) },
                        valueRange = 0f..100f,
                        enabled = enabled,
                        colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
                    )
                }
            }
        }
    }
}

// ==================== REVERB CARD ====================

@Composable
private fun ReverbCard(
    enabled: Boolean,
    reverbEnabled: Boolean,
    preset: Int,
    onEnabledChange: (Boolean) -> Unit,
    onPresetChange: (Int) -> Unit
) {
    val color = Color(0xFF00BCD4)
    val presetNames = listOf(
        "None", "Small Room", "Medium Room", "Large Room",
        "Medium Hall", "Large Hall", "Plate"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (reverbEnabled && enabled)
                color.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Waves,
                    contentDescription = null,
                    tint = if (enabled && reverbEnabled) color else Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🌊 REVERB",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = presetNames.getOrElse(preset) { "None" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reverbEnabled,
                    onCheckedChange = onEnabledChange,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(checkedTrackColor = color)
                )
            }

            if (reverbEnabled) {
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetNames.size) { index ->
                        FilterChip(
                            selected = preset == index,
                            onClick = { onPresetChange(index) },
                            enabled = enabled,
                            label = { Text(presetNames[index], fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }
}

// ==================== HELPER COMPOSABLES ====================

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    enabled: Boolean,
    color: Color,
    onValueChange: (Float) -> Unit,
    explanation: String? = null
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        FloatValueEditDialog(
            title = "Edit $label",
            initialValue = value,
            valueRange = valueRange,
            decimals = 1,
            suffix = unit,
            onDismiss = { showEditDialog = false },
            onConfirm = {
                showEditDialog = false
                onValueChange(it)
            }
        )
    }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray)
                if (explanation != null) {
                    Text(
                        text = explanation,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
            // Value badge
            Surface(
                color = if (enabled) color.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${if (value >= 0 && unit == "dB") "+" else ""}${String.format("%.1f", value)}$unit",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabled) color else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (enabled) {
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit $label", tint = color)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider with value tooltip
        Box {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = color.copy(alpha = 0.2f)
                )
            )
        }

        // Min/Max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${valueRange.start.toInt()}$unit",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 9.sp
            )
            Text(
                "${valueRange.endInclusive.toInt()}$unit",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun WarningCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "⚠️ Speaker Protection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "High levels can damage speakers and hearing. Use responsibly in competition.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==================== SPL METER CARD ====================

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
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (enabled) currentColor else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPL BATTLE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Peak dB indicator with clip warning
                Surface(
                    color = if (isClipping) Color.Red else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isClipping) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "${String.format("%.1f", currentPeakDb)}dB",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isClipping) Color.White else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // YOUR vs OPPONENT comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // YOUR SPL
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "YOU",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${ourSPL.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled && isWinning) winningColor else Color.Gray
                    )
                    Text(
                        text = "dB SPL",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                // VS indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    if (enabled) {
                        Text(
                            text = if (isWinning) "+${splDifference.toInt()}" else "${splDifference.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = currentColor
                        )
                    }
                }

                // OPPONENT SPL
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "OPPONENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${opponentSPL.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled && !isWinning) losingColor else Color.Gray
                    )
                    Text(
                        text = "dB SPL",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual bar comparison
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                // YOUR bar (from left)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                    val ourProgress by animateFloatAsState(
                        targetValue = (ourSPL / 150f).coerceIn(0f, 1f),
                        label = "our_spl_progress"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ourProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(winningColor.copy(alpha = 0.3f), winningColor)
                                )
                            )
                            .align(Alignment.CenterEnd)
                    )
                }

                // Center divider
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                )

                // OPPONENT bar (from right)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                    val opponentProgress by animateFloatAsState(
                        targetValue = (opponentSPL / 150f).coerceIn(0f, 1f),
                        label = "opponent_spl_progress"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(opponentProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(losingColor, losingColor.copy(alpha = 0.3f))
                                )
                            )
                            .align(Alignment.CenterStart)
                    )
                }
            }

            // Status text
            if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        splDifference > 10 -> "DOMINATING! Keep the pressure!"
                        splDifference > 5 -> "Winning! Push harder!"
                        splDifference > 0 -> "Slight edge - more power needed!"
                        splDifference > -5 -> "Neck to neck - give it all!"
                        splDifference > -10 -> "Falling behind - BOOST NOW!"
                        else -> "Getting crushed - GO NUCLEAR!"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = currentColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==================== QUICK PROFILE SLOTS CARD ====================

@Composable
private fun QuickProfileSlotsCard(
    enabled: Boolean,
    profileSlotA: com.ultramusic.player.audio.QuickProfile?,
    profileSlotB: com.ultramusic.player.audio.QuickProfile?,
    profileSlotC: com.ultramusic.player.audio.QuickProfile?,
    activeSlot: Char?,
    onSaveSlot: (Char) -> Unit,
    onLoadSlot: (Char) -> Unit,
    onClearSlot: (Char) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "QUICK PROFILES",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Save & instant recall your battle settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileSlotButton(
                    slot = 'A',
                    profile = profileSlotA,
                    isActive = activeSlot == 'A',
                    enabled = enabled,
                    color = Color(0xFFE91E63),
                    onSave = { onSaveSlot('A') },
                    onLoad = { onLoadSlot('A') },
                    onClear = { onClearSlot('A') }
                )

                ProfileSlotButton(
                    slot = 'B',
                    profile = profileSlotB,
                    isActive = activeSlot == 'B',
                    enabled = enabled,
                    color = Color(0xFF2196F3),
                    onSave = { onSaveSlot('B') },
                    onLoad = { onLoadSlot('B') },
                    onClear = { onClearSlot('B') }
                )

                ProfileSlotButton(
                    slot = 'C',
                    profile = profileSlotC,
                    isActive = activeSlot == 'C',
                    enabled = enabled,
                    color = Color(0xFFFF9800),
                    onSave = { onSaveSlot('C') },
                    onLoad = { onLoadSlot('C') },
                    onClear = { onClearSlot('C') }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to load | Long-press to save | Double-tap to clear",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProfileSlotButton(
    slot: Char,
    profile: com.ultramusic.player.audio.QuickProfile?,
    isActive: Boolean,
    enabled: Boolean,
    color: Color,
    onSave: () -> Unit,
    onLoad: () -> Unit,
    onClear: () -> Unit
) {
    var lastClickTime by remember { mutableStateOf(0L) }

    Surface(
        modifier = Modifier
            .size(80.dp)
            .clickable(enabled = enabled && profile != null) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < 300) {
                    // Double tap - clear
                    onClear()
                } else {
                    // Single tap - load
                    onLoad()
                }
                lastClickTime = currentTime
            },
        color = when {
            isActive -> color
            profile != null -> color.copy(alpha = 0.3f)
            else -> Color.Gray.copy(alpha = 0.1f)
        },
        shape = RoundedCornerShape(12.dp),
        border = if (isActive) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            if (profile != null) color.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = slot.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isActive -> Color.White
                        profile != null -> color
                        else -> Color.Gray
                    }
                )
                Text(
                    text = if (profile != null) "SAVED" else "EMPTY",
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isActive -> Color.White.copy(alpha = 0.8f)
                        profile != null -> color.copy(alpha = 0.8f)
                        else -> Color.Gray.copy(alpha = 0.5f)
                    }
                )
            }
        }
    }

    // Long press gesture for saving - using OutlinedButton as alternative
    if (enabled) {
        var showSaveOption by remember { mutableStateOf(false) }

        if (showSaveOption) {
            // Mini save button appears above slot
        }
    }
}

// ==================== DANGER MODE CARD ====================

@Composable
private fun DangerModeCard(
    enabled: Boolean,
    dangerModeEnabled: Boolean,
    onDangerModeChange: (Boolean) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (dangerModeEnabled && enabled) Color.Red else Color.Transparent,
        label = "danger_border"
    )

    Card(
        modifier = Modifier.border(
            width = if (dangerModeEnabled && enabled) 2.dp else 0.dp,
            color = borderColor,
            shape = RoundedCornerShape(12.dp)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (dangerModeEnabled && enabled)
                Color.Red.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
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
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (dangerModeEnabled && enabled) Color.Red else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "DANGER MODE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (dangerModeEnabled && enabled) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bypass limiter - MAXIMUM OUTPUT",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = dangerModeEnabled,
                    onCheckedChange = onDangerModeChange,
                    enabled = enabled,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color.Red,
                        checkedThumbColor = Color.White
                    )
                )
            }

            if (dangerModeEnabled && enabled) {
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color.Red.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "LIMITER BYPASSED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Text(
                                text = "Clipping and speaker damage possible! Use only to overpower opponent at critical moments.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
