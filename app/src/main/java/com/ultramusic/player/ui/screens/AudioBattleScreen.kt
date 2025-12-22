package com.ultramusic.player.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.ultramusic.player.audio.BattlePreset
import com.ultramusic.player.audio.AudioBattleEngine
import com.ultramusic.player.audio.EQBand
import com.ultramusic.player.ui.MainViewModel

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
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
            
            // ===== BASS CONTROL =====
            item {
                BattleControlCard(
                    title = "🔊 BASS POWER",
                    subtitle = "Overpower opponent's low end",
                    value = bassLevel,
                    maxValue = 1000,
                    enabled = isEnabled,
                    color = Color(0xFFE91E63),
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    onValueChange = { viewModel.setBattleBass(it.toInt()) }
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
            
            // ===== PRESETS =====
            item {
                PresetsSection(
                    enabled = isEnabled,
                    onPresetSelected = { viewModel.applyBattlePreset(it) }
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
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Level indicator
        Text(
            text = "${band.levelDb.toInt()}dB",
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) color else Color.Gray
        )
        
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

@Composable
private fun PresetsSection(
    enabled: Boolean,
    onPresetSelected: (BattlePreset) -> Unit
) {
    Column {
        Text(
            text = "Battle Presets",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AudioBattleEngine.DEFAULT_PRESETS) { preset ->
                PresetCard(
                    preset = preset,
                    enabled = enabled,
                    onClick = { onPresetSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: BattlePreset,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mini stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniStat("B", preset.bassLevel / 10, Color(0xFFE91E63))
                MiniStat("L", preset.loudnessGain / 10, Color(0xFFFF5722))
                MiniStat("C", preset.clarityLevel, Color(0xFF2196F3))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        Text(
            text = "$value",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
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
