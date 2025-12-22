package com.ultramusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.audio.ActiveBattleMode
import com.ultramusic.player.audio.ActiveBattleState
import com.ultramusic.player.audio.AttackOpportunity
import com.ultramusic.player.audio.BattleLogEntry
import com.ultramusic.player.audio.ActiveBattleSystem
import com.ultramusic.player.audio.BattleScript
import com.ultramusic.player.audio.CrowdMood
import com.ultramusic.player.audio.CrowdTrend
import com.ultramusic.player.audio.DropRecommendation
import com.ultramusic.player.audio.DropTiming
import com.ultramusic.player.audio.Urgency
import com.ultramusic.player.audio.WarfareTactic
import com.ultramusic.player.data.Song
import com.ultramusic.player.ui.MainViewModel
import kotlinx.coroutines.delay

/**
 * ActiveBattleScreen - PRODUCTION VERSION
 * Fully connected to ViewModel and real battle systems
 */
@Composable
fun ActiveBattleScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    // Collect all state from ViewModel (connected to real systems)
    val battleState by viewModel.activeBattleState.collectAsState()
    val battleMode by viewModel.activeBattleMode.collectAsState()
    val momentum by viewModel.battleMomentum.collectAsState()
    val opponentSPL by viewModel.opponentSPL.collectAsState()
    val ourSPL by viewModel.ourSPL.collectAsState()
    val attackOpportunity by viewModel.attackOpportunity.collectAsState()
    val autoCounterEnabled by viewModel.autoCounterEnabled.collectAsState()
    val autoVolumeEnabled by viewModel.autoVolumeEnabled.collectAsState()
    val autoQueueEnabled by viewModel.autoQueueEnabled.collectAsState()
    val crowdEnergy by viewModel.crowdEnergy.collectAsState()
    val crowdTrend by viewModel.crowdTrend.collectAsState()
    val crowdMood by viewModel.crowdMood.collectAsState()
    val dropRecommendation by viewModel.dropRecommendation.collectAsState()
    val nextSongSuggestion by viewModel.nextSongSuggestion.collectAsState()
    val battleLog by viewModel.battleLog.collectAsState()
    val activeTactic by viewModel.activeTactic.collectAsState()
    
    // Call the detailed implementation with real data
    ActiveBattleScreenContent(
        battleState = battleState,
        battleMode = battleMode,
        momentum = momentum,
        opponentSPL = opponentSPL,
        ourSPL = ourSPL,
        attackOpportunity = attackOpportunity,
        autoCounterEnabled = autoCounterEnabled,
        autoVolumeEnabled = autoVolumeEnabled,
        autoQueueEnabled = autoQueueEnabled,
        crowdEnergy = crowdEnergy,
        crowdTrend = crowdTrend,
        crowdMood = crowdMood,
        dropRecommendation = dropRecommendation,
        nextSongSuggestion = nextSongSuggestion,
        battleLog = battleLog,
        activeTactic = activeTactic,
        onStartBattle = { viewModel.startActiveBattle(battleMode) },
        onPauseBattle = { viewModel.pauseActiveBattle() },
        onEndBattle = { viewModel.endActiveBattle() },
        onSetMode = { mode -> viewModel.setActiveBattleMode(mode) },
        onToggleAutoCounter = { enabled -> viewModel.toggleAutoCounter(enabled) },
        onToggleAutoVolume = { enabled -> viewModel.toggleAutoVolume(enabled) },
        onToggleAutoQueue = { enabled -> viewModel.toggleAutoQueue(enabled) },
        onExecuteScript = { script -> viewModel.executeBattleScript(script) },
        onExecuteTactic = { tactic -> viewModel.executeTactic(tactic) },
        onPlayNextSuggestion = { viewModel.playNextSuggestion() },
        onNavigateBack = onNavigateBack
    )
}

/**
 * ACTIVE BATTLE SCREEN
 * 
 * The main battle command center where the AI fights for you!
 * 
 * Sections:
 * 1. Battle Status - Current state, momentum, SPL comparison
 * 2. Attack Opportunity - Real-time alerts for perfect moments
 * 3. Auto Features - Toggle auto-counter, auto-volume, etc.
 * 4. Quick Actions - Battle scripts, immediate attacks
 * 5. Crowd Analysis - Real-time crowd energy
 * 6. Frequency Warfare - Active tactics
 * 7. Next Song - AI song suggestion
 * 8. Battle Log - What's happening
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveBattleScreenContent(
    battleState: ActiveBattleState,
    battleMode: ActiveBattleMode,
    momentum: Int,
    opponentSPL: Float,
    ourSPL: Float,
    attackOpportunity: AttackOpportunity?,
    autoCounterEnabled: Boolean,
    autoVolumeEnabled: Boolean,
    autoQueueEnabled: Boolean,
    crowdEnergy: Int,
    crowdTrend: CrowdTrend,
    crowdMood: CrowdMood,
    dropRecommendation: DropRecommendation?,
    nextSongSuggestion: Song?,
    battleLog: List<BattleLogEntry>,
    activeTactic: WarfareTactic?,
    onStartBattle: () -> Unit,
    onPauseBattle: () -> Unit,
    onEndBattle: () -> Unit,
    onSetMode: (ActiveBattleMode) -> Unit,
    onToggleAutoCounter: (Boolean) -> Unit,
    onToggleAutoVolume: (Boolean) -> Unit,
    onToggleAutoQueue: (Boolean) -> Unit,
    onExecuteScript: (BattleScript) -> Unit,
    onExecuteTactic: (WarfareTactic) -> Unit,
    onPlayNextSuggestion: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val isActive = battleState == ActiveBattleState.ACTIVE
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Recording indicator
                        if (isActive) {
                            Icon(
                                Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            "⚔️ ACTIVE BATTLE",
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color(0xFFFF5722) else Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D1A)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D0D1A))
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. BATTLE CONTROL
            item {
                BattleControlPanel(
                    state = battleState,
                    mode = battleMode,
                    onStart = onStartBattle,
                    onPause = onPauseBattle,
                    onEnd = onEndBattle,
                    onSetMode = onSetMode
                )
            }
            
            // 2. MOMENTUM & SPL
            item {
                MomentumPanel(
                    momentum = momentum,
                    opponentSPL = opponentSPL,
                    ourSPL = ourSPL
                )
            }
            
            // 3. ATTACK OPPORTUNITY (Pulsing alert!)
            if (attackOpportunity != null && isActive) {
                item {
                    AttackOpportunityAlert(
                        opportunity = attackOpportunity
                    )
                }
            }
            
            // 4. AUTO FEATURES
            item {
                AutoFeaturesPanel(
                    autoCounterEnabled = autoCounterEnabled,
                    autoVolumeEnabled = autoVolumeEnabled,
                    autoQueueEnabled = autoQueueEnabled,
                    onToggleAutoCounter = onToggleAutoCounter,
                    onToggleAutoVolume = onToggleAutoVolume,
                    onToggleAutoQueue = onToggleAutoQueue,
                    enabled = isActive
                )
            }
            
            // 5. CROWD ANALYSIS
            item {
                CrowdPanel(
                    energy = crowdEnergy,
                    trend = crowdTrend,
                    mood = crowdMood,
                    dropRecommendation = dropRecommendation
                )
            }
            
            // 6. QUICK ACTIONS (Battle Scripts)
            item {
                QuickActionsPanel(
                    onExecuteScript = onExecuteScript,
                    enabled = isActive
                )
            }
            
            // 7. FREQUENCY WARFARE
            item {
                FrequencyWarfarePanel(
                    activeTactic = activeTactic,
                    onExecuteTactic = onExecuteTactic,
                    enabled = isActive
                )
            }
            
            // 8. NEXT SONG SUGGESTION
            if (nextSongSuggestion != null) {
                item {
                    NextSongCard(
                        song = nextSongSuggestion,
                        onPlay = onPlayNextSuggestion
                    )
                }
            }
            
            // 9. BATTLE LOG
            item {
                BattleLogPanel(log = battleLog)
            }
        }
    }
}

@Composable
private fun BattleControlPanel(
    state: ActiveBattleState,
    mode: ActiveBattleMode,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onEnd: () -> Unit,
    onSetMode: (ActiveBattleMode) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when (state) {
                    ActiveBattleState.IDLE, ActiveBattleState.ENDED -> {
                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START BATTLE", fontWeight = FontWeight.Bold)
                        }
                    }
                    ActiveBattleState.ACTIVE -> {
                        Button(
                            onClick = onPause,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Pause, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PAUSE")
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Button(
                            onClick = onEnd,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Stop, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("END")
                        }
                    }
                    ActiveBattleState.PAUSED -> {
                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESUME")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Battle mode selector
            Text(
                "Battle Mode",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val modes = listOf(
                    ActiveBattleMode.BALANCED to "⚖️ Balanced",
                    ActiveBattleMode.AGGRESSIVE to "🔥 Aggressive",
                    ActiveBattleMode.DEFENSIVE to "🛡️ Defensive",
                    ActiveBattleMode.STEALTH to "🥷 Stealth",
                    ActiveBattleMode.COUNTER_ONLY to "🎯 Counter"
                )
                
                items(modes) { (battleMode, label) ->
                    FilterChip(
                        selected = mode == battleMode,
                        onClick = { onSetMode(battleMode) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MomentumPanel(
    momentum: Int,
    opponentSPL: Float,
    ourSPL: Float
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Momentum bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("OPPONENT", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                Text("MOMENTUM", color = Color.White.copy(alpha = 0.7f))
                Text("YOU", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Visual momentum bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A3E))
            ) {
                // Our side (from right)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(momentum / 100f)
                        .align(Alignment.CenterEnd)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50))
                            )
                        )
                )
                
                // Center indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .background(Color.White)
                )
                
                // Percentage
                Text(
                    "${momentum}%",
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // SPL comparison
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Opponent SPL", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                    Text(
                        "${opponentSPL.toInt()} dB",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val diff = ourSPL - opponentSPL
                    val diffColor = if (diff > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    Text(
                        if (diff > 0) "+${diff.toInt()} dB" else "${diff.toInt()} dB",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = diffColor
                    )
                    Text(
                        if (diff > 0) "LOUDER" else "QUIETER",
                        style = MaterialTheme.typography.labelSmall,
                        color = diffColor
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your SPL", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                    Text(
                        "${ourSPL.toInt()} dB",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttackOpportunityAlert(opportunity: AttackOpportunity) {
    var visible by remember { mutableStateOf(true) }
    
    // Pulsing animation
    LaunchedEffect(opportunity) {
        while (true) {
            visible = !visible
            delay(300)
        }
    }
    
    val bgColor = when (opportunity.urgency) {
        Urgency.CRITICAL -> Color(0xFFF44336)
        Urgency.HIGH -> Color(0xFFFF9800)
        Urgency.MEDIUM -> Color(0xFFFFEB3B)
        Urgency.LOW -> Color(0xFF2196F3)
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚡", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        opportunity.message,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        opportunity.suggestedAction,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
                
                Text(
                    "NOW!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AutoFeaturesPanel(
    autoCounterEnabled: Boolean,
    autoVolumeEnabled: Boolean,
    autoQueueEnabled: Boolean,
    onToggleAutoCounter: (Boolean) -> Unit,
    onToggleAutoVolume: (Boolean) -> Unit,
    onToggleAutoQueue: (Boolean) -> Unit,
    enabled: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🤖 AUTO FEATURES",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AutoFeatureRow(
                emoji = "🎯",
                title = "Auto-Counter EQ",
                description = "Exploit opponent's weak frequencies",
                enabled = autoCounterEnabled && enabled,
                onToggle = { onToggleAutoCounter(it) }
            )
            
            AutoFeatureRow(
                emoji = "📢",
                title = "Auto-Volume Match",
                description = "Always stay louder than opponent",
                enabled = autoVolumeEnabled && enabled,
                onToggle = { onToggleAutoVolume(it) }
            )
            
            AutoFeatureRow(
                emoji = "🎵",
                title = "Auto-Queue",
                description = "AI picks counter songs",
                enabled = autoQueueEnabled && enabled,
                onToggle = { onToggleAutoQueue(it) }
            )
        }
    }
}

@Composable
private fun AutoFeatureRow(
    emoji: String,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun CrowdPanel(
    energy: Int,
    trend: CrowdTrend,
    mood: CrowdMood,
    dropRecommendation: DropRecommendation?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "👥 CROWD ANALYSIS",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Energy
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$energy%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            energy >= 70 -> Color(0xFF4CAF50)
                            energy >= 40 -> Color(0xFFFFEB3B)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Text("Energy", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                }
                
                // Trend
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(trend.emoji, fontSize = 32.sp)
                    Text(trend.description, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                }
                
                // Mood
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(mood.emoji, fontSize = 32.sp)
                    Text(mood.description, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                }
            }
            
            // Drop recommendation
            dropRecommendation?.let { rec ->
                Spacer(modifier = Modifier.height(12.dp))
                
                val bgColor = when (rec.timing) {
                    DropTiming.PERFECT -> Color(0xFF4CAF50)
                    DropTiming.GOOD -> Color(0xFF8BC34A)
                    DropTiming.READY -> Color(0xFFFFEB3B)
                    DropTiming.BUILD_MORE -> Color(0xFFFF9800)
                    DropTiming.WAIT -> Color(0xFF9E9E9E)
                }
                
                Surface(
                    color = bgColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(rec.message, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "${rec.confidence}%",
                            color = bgColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsPanel(
    onExecuteScript: (BattleScript) -> Unit,
    enabled: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "⚡ BATTLE SCRIPTS",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ActiveBattleSystem.SCRIPTS) { script ->
                    Button(
                        onClick = { onExecuteScript(script) },
                        enabled = enabled,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(80.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(script.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(script.description, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencyWarfarePanel(
    activeTactic: WarfareTactic?,
    onExecuteTactic: (WarfareTactic) -> Unit,
    enabled: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⚔️ FREQUENCY WARFARE",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                if (activeTactic != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            activeTactic.emoji + " ACTIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Tactic buttons in grid
            val tactics = WarfareTactic.values().filter { it != WarfareTactic.ADAPTIVE }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tactics.take(3).forEach { tactic ->
                        TacticButton(
                            tactic = tactic,
                            isActive = activeTactic == tactic,
                            enabled = enabled,
                            onClick = { onExecuteTactic(tactic) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tactics.drop(3).forEach { tactic ->
                        TacticButton(
                            tactic = tactic,
                            isActive = activeTactic == tactic,
                            enabled = enabled,
                            onClick = { onExecuteTactic(tactic) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticButton(
    tactic: WarfareTactic,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) Color(0xFF4CAF50) else Color(0xFF2A2A3E)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(56.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(tactic.emoji, fontSize = 16.sp)
            Text(
                tactic.name.replace("_", " "),
                fontSize = 8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NextSongCard(
    song: Song,
    onPlay: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3460))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "🎵 AI SUGGESTS NEXT:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    song.title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.SkipNext, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("PLAY")
            }
        }
    }
}

@Composable
private fun BattleLogPanel(log: List<BattleLogEntry>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .height(200.dp)
        ) {
            Text(
                "📜 BATTLE LOG",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn {
                items(log.reversed().take(10)) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            entry.title,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            entry.message,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
