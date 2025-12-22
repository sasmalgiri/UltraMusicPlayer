package com.ultramusic.player.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ultramusic.player.core.CounterSuggestion
import com.ultramusic.player.core.CounterStrategy
import com.ultramusic.player.core.FrequencyBands
import com.ultramusic.player.core.FrequencyRange
import com.ultramusic.player.core.LocalBattleAnalyzer
import com.ultramusic.player.core.OpponentAudioProfile
import com.ultramusic.player.data.Song
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleAnalyzerScreen(
    analyzer: LocalBattleAnalyzer,
    onNavigateBack: () -> Unit,
    onPlaySong: (Song) -> Unit
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == 
                PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            analyzer.startListening()
        }
    }
    
    val isListening by analyzer.isListening.collectAsState()
    val opponentProfile by analyzer.opponentProfile.collectAsState()
    val frequencyBands by analyzer.frequencyBands.collectAsState()
    val detectedBPM by analyzer.detectedBPM.collectAsState()
    val bpmConfidence by analyzer.bpmConfidence.collectAsState()
    val overallLoudness by analyzer.overallLoudness.collectAsState()
    val dominantRange by analyzer.dominantFrequencyRange.collectAsState()
    val weakRange by analyzer.weakFrequencyRange.collectAsState()
    val counterSuggestions by analyzer.counterSuggestions.collectAsState()
    val topCounter by analyzer.topCounter.collectAsState()
    val counterStrategy by analyzer.counterStrategy.collectAsState()
    val battleAdvice by analyzer.battleAdvice.collectAsState()
    val attackOpportunity by analyzer.attackOpportunity.collectAsState()
    
    // Indexing status
    val isIndexing by analyzer.isIndexing.collectAsState()
    val indexProgress by analyzer.indexingProgress.collectAsState()
    val indexedCount by analyzer.indexedCount.collectAsState()
    val isLibraryIndexed = analyzer.isLibraryIndexed()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (attackOpportunity) Color(0xFF4A1A1A) else Color(0xFF121212),
        animationSpec = tween(500),
        label = "bg"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "⚔️ BATTLE ANALYZER",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A1A)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Listen Button
            item {
                ListenButton(
                    isListening = isListening,
                    hasMicPermission = hasMicPermission,
                    onToggle = {
                        if (isListening) {
                            analyzer.stopListening()
                        } else {
                            if (hasMicPermission) {
                                analyzer.startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                )
            }
            
            // Library Indexing Status
            item {
                LibraryStatusCard(
                    isIndexed = isLibraryIndexed,
                    isIndexing = isIndexing,
                    indexProgress = indexProgress,
                    indexedCount = indexedCount
                )
            }
            
            // Battle Advice Banner
            item {
                BattleAdviceBanner(
                    advice = battleAdvice,
                    isAttackOpportunity = attackOpportunity
                )
            }
            
            // Opponent Analysis Section
            if (isListening || opponentProfile.isPlaying) {
                item {
                    OpponentAnalysisCard(
                        profile = opponentProfile,
                        bands = frequencyBands,
                        bpm = detectedBPM,
                        bpmConfidence = bpmConfidence,
                        loudness = overallLoudness,
                        dominantRange = dominantRange,
                        weakRange = weakRange
                    )
                }
            }
            
            // Counter Strategy
            if (counterStrategy != null && isListening) {
                item {
                    CounterStrategyCard(strategy = counterStrategy!!)
                }
            }
            
            // Counter Suggestions
            if (counterSuggestions.isNotEmpty()) {
                item {
                    Text(
                        "🎵 COUNTER SUGGESTIONS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                items(counterSuggestions) { suggestion ->
                    CounterSuggestionCard(
                        suggestion = suggestion,
                        isTopPick = suggestion == topCounter,
                        onPlay = { onPlaySong(suggestion.song) }
                    )
                }
            }
            
            // Instructions when not listening
            if (!isListening && counterSuggestions.isEmpty()) {
                item {
                    InstructionsCard()
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ListenButton(
    isListening: Boolean,
    hasMicPermission: Boolean = true,
    onToggle: () -> Unit
) {
    val buttonColor by animateColorAsState(
        targetValue = when {
            isListening -> Color(0xFFE53935)
            !hasMicPermission -> Color(0xFFFF9800)
            else -> Color(0xFF4CAF50)
        },
        label = "button"
    )
    
    val buttonText = when {
        isListening -> "STOP LISTENING"
        !hasMicPermission -> "🎤 TAP TO GRANT MIC PERMISSION"
        else -> "START LISTENING TO OPPONENT"
    }
    
    Button(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = buttonText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BattleAdviceBanner(
    advice: String,
    isAttackOpportunity: Boolean
) {
    val bgColor by animateColorAsState(
        targetValue = if (isAttackOpportunity) Color(0xFFFF5722) else Color(0xFF2196F3),
        label = "banner"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAttackOpportunity) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = advice.ifEmpty { "Ready to analyze opponent..." },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isAttackOpportunity) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun OpponentAnalysisCard(
    profile: OpponentAudioProfile,
    bands: FrequencyBands,
    bpm: Double,
    bpmConfidence: Float,
    loudness: Float,
    dominantRange: FrequencyRange,
    weakRange: FrequencyRange
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "👂 OPPONENT ANALYSIS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9800)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Frequency Bands Visualization
            FrequencyBarsVisualization(bands = bands)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // BPM
                StatBox(
                    icon = Icons.Default.Speed,
                    label = "BPM",
                    value = if (bpm > 0) "${bpm.roundToInt()}" else "---",
                    confidence = bpmConfidence
                )
                
                // Loudness
                StatBox(
                    icon = Icons.Default.GraphicEq,
                    label = "LOUD",
                    value = "${loudness.roundToInt()} dB",
                    confidence = ((loudness + 60) / 60 * 100).coerceIn(0f, 100f)
                )
                
                // Energy
                StatBox(
                    icon = Icons.Default.MusicNote,
                    label = "ENERGY",
                    value = "${(profile.energy * 100).roundToInt()}%",
                    confidence = profile.energy * 100
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Dominant & Weak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("💪 DOMINANT", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        dominantRange.displayName,
                        color = Color(0xFFFF5722),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("🎯 WEAK SPOT", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        weakRange.displayName,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyBarsVisualization(bands: FrequencyBands) {
    val bandList = listOf(
        "SUB" to bands.subBass,
        "BASS" to bands.bass,
        "LOW" to bands.lowMid,
        "MID" to bands.mid,
        "HIGH" to bands.highMid,
        "AIR" to bands.high
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        bandList.forEach { (name, value) ->
            val animatedHeight by animateFloatAsState(
                targetValue = value.coerceIn(0f, 1f),
                animationSpec = tween(100),
                label = name
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height((80 * animatedHeight).dp.coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFF5722),
                                    Color(0xFFFF9800),
                                    Color(0xFF4CAF50)
                                )
                            )
                        )
                )
                Text(
                    name,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    confidence: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(20.dp)
        )
        Text(
            label,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        LinearProgressIndicator(
            progress = (confidence / 100f).coerceIn(0f, 1f),
            modifier = Modifier
                .width(50.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFF333333)
        )
    }
}

@Composable
private fun CounterStrategyCard(strategy: CounterStrategy) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    strategy.icon,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    strategy.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                strategy.description,
                color = Color(0xFFB0BEC5),
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "💡 EQ TIP: ${strategy.eqAdvice}",
                color = Color(0xFF64FFDA),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CounterSuggestionCard(
    suggestion: CounterSuggestion,
    isTopPick: Boolean,
    onPlay: () -> Unit
) {
    val borderColor = if (isTopPick) Color(0xFFFFD700) else Color.Transparent
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isTopPick) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(
            containerColor = if (isTopPick) Color(0xFF2E2E2E) else Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isTopPick) {
                        Text(
                            "⭐ TOP PICK",
                            fontSize = 11.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        suggestion.song.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        suggestion.song.artist,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Score
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${suggestion.score.roundToInt()}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Play button
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2196F3), CircleShape)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White
                    )
                }
            }
            
            // Strategy badge
            Text(
                suggestion.strategy,
                fontSize = 11.sp,
                color = Color(0xFFFF9800),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .background(Color(0xFF3E2723), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            
            // Reasons
            if (suggestion.reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                suggestion.reasons.take(3).forEach { reason ->
                    Text(
                        reason,
                        fontSize = 12.sp,
                        color = Color(0xFF81C784)
                    )
                }
            }
            
            // Profile info
            suggestion.profile.bpm?.let { bpm ->
                Text(
                    "BPM: ${bpm.roundToInt()} | Energy: ${(suggestion.profile.energy * 100).roundToInt()}%",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "🎯 HOW TO USE BATTLE ANALYZER",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val steps = listOf(
                "1️⃣ Tap 'START LISTENING' when opponent plays",
                "2️⃣ Hold phone toward their sound system",
                "3️⃣ App analyzes their BPM, frequencies, weak spots",
                "4️⃣ Get instant counter song suggestions",
                "5️⃣ Tap suggested song to play your counter!"
            )
            
            steps.forEach { step ->
                Text(
                    step,
                    fontSize = 14.sp,
                    color = Color(0xFFB0BEC5),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "💡 TIP: Pre-analyze your battle library for better suggestions!",
                fontSize = 13.sp,
                color = Color(0xFF64FFDA),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun LibraryStatusCard(
    isIndexed: Boolean,
    isIndexing: Boolean,
    indexProgress: Float,
    indexedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isIndexed) Color(0xFF1B5E20) else Color(0xFF4A4A00)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isIndexed) "✅" else if (isIndexing) "⏳" else "⚠️",
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when {
                            isIndexing -> "INDEXING LIBRARY..."
                            isIndexed -> "LIBRARY READY"
                            else -> "LIBRARY NOT INDEXED"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = when {
                            isIndexing -> "Analyzing $indexedCount songs..."
                            isIndexed -> "$indexedCount songs pre-analyzed for INSTANT counters"
                            else -> "Songs will be indexed on next load"
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            
            if (isIndexing) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = indexProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF333333)
                )
            }
        }
    }
}
