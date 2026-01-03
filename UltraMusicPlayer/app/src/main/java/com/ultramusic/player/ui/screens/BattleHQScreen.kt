@file:androidx.media3.common.util.UnstableApi

package com.ultramusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.audio.BattleAdvice
import com.ultramusic.player.audio.BattleMode
import com.ultramusic.player.audio.BattlePriority
import com.ultramusic.player.audio.BattleTier
import com.ultramusic.player.audio.EQSuggestion
import com.ultramusic.player.audio.FrequencyBand
import com.ultramusic.player.audio.OpponentAnalysis
import com.ultramusic.player.audio.SongBattleRating
import com.ultramusic.player.audio.VenueProfile
import com.ultramusic.player.audio.VenueSize
import com.ultramusic.player.ui.MainViewModel

/**
 * Battle HQ - Command Center for Sound Battles
 * 
 * Tabs:
 * 1. INTEL - Real-time opponent analysis
 * 2. ARSENAL - Song battle ratings
 * 3. VENUE - Venue profiling
 * 4. CONTROLS - Quick battle controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleHQScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSoundBattle: () -> Unit,
    onNavigateToAISettings: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("🎯 INTEL", "🎵 ARSENAL", "📍 VENUE", "⚡ CONTROLS")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚔️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "BATTLE HQ",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5722)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1A1A2E))
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF16213E),
                contentColor = Color.White
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            
            // Tab Content
            when (selectedTab) {
                0 -> IntelTab(viewModel)
                1 -> ArsenalTab(viewModel)
                2 -> VenueTab(viewModel)
                3 -> ControlsTab(viewModel, onNavigateToSoundBattle, onNavigateToAISettings)
            }
        }
    }
}

// ==================== INTEL TAB ====================

@Composable
private fun IntelTab(viewModel: MainViewModel) {
    val isListening by viewModel.battleIntelListening.collectAsState()
    val opponentAnalysis by viewModel.opponentAnalysis.collectAsState()
    val venueSPL by viewModel.venueSPL.collectAsState()
    val spectrum by viewModel.frequencySpectrum.collectAsState()
    val suggestions by viewModel.counterEQSuggestions.collectAsState()
    val advice by viewModel.battleAdvice.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Listen Button
        item {
            ListenButton(
                isListening = isListening,
                onToggle = { viewModel.toggleBattleIntel() }
            )
        }
        
        // SPL Meter
        item {
            SPLMeterCard(spl = venueSPL)
        }
        
        // Spectrum Analyzer
        item {
            SpectrumAnalyzerCard(spectrum = spectrum)
        }
        
        // Opponent Analysis
        if (isListening) {
            item {
                OpponentAnalysisCard(analysis = opponentAnalysis)
            }
            
            // Counter Suggestions
            if (suggestions.isNotEmpty()) {
                item {
                    CounterSuggestionsCard(
                        suggestions = suggestions,
                        onApply = { viewModel.applyCounterEQ(it) }
                    )
                }
            }
            
            // Battle Advice
            if (advice.isNotEmpty()) {
                item {
                    BattleAdviceCard(advice = advice)
                }
            }
        }
    }
}

@Composable
private fun ListenButton(
    isListening: Boolean,
    onToggle: () -> Unit
) {
    Button(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isListening) Color(0xFFE91E63) else Color(0xFF4CAF50)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            if (isListening) Icons.Default.Hearing else Icons.Default.Radar,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            if (isListening) "🔴 LISTENING TO OPPONENT..." else "🎯 START INTEL GATHERING",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun SPLMeterCard(spl: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "📊 VENUE SPL",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Big SPL number
            Text(
                text = "${spl.toInt()} dB",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    spl < 70 -> Color(0xFF4CAF50)
                    spl < 90 -> Color(0xFFFFEB3B)
                    spl < 100 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )
            
            // Level bar
            LinearProgressIndicator(
                progress = (spl / 120f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    spl < 70 -> Color(0xFF4CAF50)
                    spl < 90 -> Color(0xFFFFEB3B)
                    spl < 100 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = when {
                    spl < 60 -> "Quiet - Good time to attack!"
                    spl < 80 -> "Moderate - Normal levels"
                    spl < 95 -> "Loud - Battle is ON!"
                    else -> "⚠️ VERY LOUD - Protect your ears!"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SpectrumAnalyzerCard(spectrum: List<Float>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🎚️ FREQUENCY SPECTRUM",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Spectrum bars
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val barWidth = size.width / spectrum.size
                val barSpacing = 2.dp.toPx()
                
                spectrum.forEachIndexed { index, value ->
                    val barHeight = value * size.height
                    val color = when (index) {
                        in 0..3 -> Color(0xFFE91E63)    // Sub-bass/bass
                        in 4..8 -> Color(0xFFFF9800)    // Low-mid
                        in 9..16 -> Color(0xFFFFEB3B)   // Mid
                        in 17..24 -> Color(0xFF4CAF50) // High-mid
                        else -> Color(0xFF2196F3)       // High
                    }
                    
                    drawRect(
                        color = color,
                        topLeft = Offset(index * barWidth + barSpacing, size.height - barHeight),
                        size = Size(barWidth - barSpacing * 2, barHeight)
                    )
                }
            }
            
            // Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("20Hz", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE91E63))
                Text("200Hz", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
                Text("1kHz", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFEB3B))
                Text("5kHz", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                Text("20kHz", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3))
            }
        }
    }
}

@Composable
private fun OpponentAnalysisCard(analysis: OpponentAnalysis) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "🎯 OPPONENT ANALYSIS",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Strategy detection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Strategy", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                    Text(
                        analysis.strategy.name.replace("_", " "),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Dominant", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                    Text(
                        analysis.dominantBand.label,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Frequency levels
            FrequencyLevelRow("Sub-Bass", analysis.subBassLevel, Color(0xFFE91E63))
            FrequencyLevelRow("Bass", analysis.bassLevel, Color(0xFFFF5722))
            FrequencyLevelRow("Low-Mid", analysis.lowMidLevel, Color(0xFFFF9800))
            FrequencyLevelRow("Mid", analysis.midLevel, Color(0xFFFFEB3B))
            FrequencyLevelRow("High-Mid", analysis.highMidLevel, Color(0xFF4CAF50))
            FrequencyLevelRow("High", analysis.highLevel, Color(0xFF2196F3))
            
            // Weak bands
            if (analysis.weakBands.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "WEAK SPOTS DETECTED!",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                analysis.weakBands.joinToString { it.label },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrequencyLevelRow(label: String, level: Float, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.width(70.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
        LinearProgressIndicator(
            progress = level,
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
        Text(
            "${(level * 100).toInt()}%",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CounterSuggestionsCard(
    suggestions: List<EQSuggestion>,
    onApply: (EQSuggestion) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "⚡ COUNTER EQ SUGGESTIONS",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFFF9800)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            suggestions.forEach { suggestion ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onApply(suggestion) },
                    color = Color(0xFF0F3460),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${suggestion.frequency} +${suggestion.suggestedBoost}dB",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                suggestion.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        FilledTonalButton(onClick = { onApply(suggestion) }) {
                            Text("APPLY")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BattleAdviceCard(advice: List<BattleAdvice>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "💡 BATTLE ADVICE",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            advice.forEach { item ->
                val bgColor = when (item.priority) {
                    BattlePriority.CRITICAL -> Color(0xFFF44336).copy(alpha = 0.2f)
                    BattlePriority.HIGH -> Color(0xFFFF9800).copy(alpha = 0.2f)
                    BattlePriority.WARNING -> Color(0xFFFFEB3B).copy(alpha = 0.2f)
                    BattlePriority.INFO -> Color(0xFF2196F3).copy(alpha = 0.1f)
                }
                
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.icon, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                item.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                item.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== ARSENAL TAB ====================

@Composable
private fun ArsenalTab(viewModel: MainViewModel) {
    val analyzedSongs by viewModel.analyzedSongRatings.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingSongs.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Analysis button
        item {
            Button(
                onClick = { viewModel.analyzeLibraryForBattle() },
                enabled = !isAnalyzing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing...")
                } else {
                    Icon(Icons.Default.Star, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ANALYZE LIBRARY FOR BATTLE")
                }
            }
        }
        
        // Tier sections
        BattleTier.values().forEach { tier ->
            val songsInTier = analyzedSongs.filter { it.value.battleTier == tier }
            if (songsInTier.isNotEmpty()) {
                item {
                    Text(
                        tier.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(songsInTier.values.first().ratingColor)
                    )
                }
                
                items(songsInTier.entries.take(5).toList()) { (songId, rating) ->
                    SongRatingCard(
                        songId = songId,
                        rating = rating,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun SongRatingCard(
    songId: Long,
    rating: SongBattleRating,
    viewModel: MainViewModel
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rating badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(rating.ratingColor).copy(alpha = 0.2f))
                    .border(2.dp, Color(rating.ratingColor), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${rating.overallRating}",
                    fontWeight = FontWeight.Bold,
                    color = Color(rating.ratingColor)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Song #$songId", // Would be song title
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // Mini stats
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("🔊", rating.bassImpact, Color(0xFFE91E63))
                    MiniStat("⚡", rating.energy, Color(0xFFFF9800))
                    MiniStat("🎯", rating.clarity, Color(0xFF2196F3))
                    MiniStat("👥", rating.crowdAppeal, Color(0xFF4CAF50))
                }
                
                // Strengths
                Text(
                    rating.strengths.take(2).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            // Play button
            IconButton(onClick = { viewModel.playSongById(songId) }) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun MiniStat(icon: String, value: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 10.sp)
        Text(
            "$value",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// ==================== VENUE TAB ====================

@Composable
private fun VenueTab(viewModel: MainViewModel) {
    val isProfiling by viewModel.isProfilingVenue.collectAsState()
    val currentVenue by viewModel.currentVenueProfile.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.quickProfileVenue() },
                    enabled = !isProfiling,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                ) {
                    Icon(Icons.Default.Speed, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("QUICK")
                }
                
                Button(
                    onClick = { viewModel.fullProfileVenue() },
                    enabled = !isProfiling,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                ) {
                    Icon(Icons.Default.GraphicEq, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("FULL SCAN")
                }
            }
        }
        
        if (isProfiling) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Profiling venue acoustics...", color = Color.White)
                    }
                }
            }
        }
        
        // Current profile
        currentVenue?.let { profile ->
            item {
                VenueProfileCard(
                    profile = profile,
                    onApply = { viewModel.applyVenueProfile() }
                )
            }
        }
    }
}

@Composable
private fun VenueProfileCard(
    profile: VenueProfile,
    onApply: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile.sizeEmoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        profile.size.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats
            Row(modifier = Modifier.fillMaxWidth()) {
                StatBox("Reverb", "${profile.reverbTime}s", Modifier.weight(1f))
                StatBox("Noise", "${profile.noiseFloor.toInt()}dB", Modifier.weight(1f))
                StatBox("Bass", "${(profile.bassResponse * 100).toInt()}%", Modifier.weight(1f))
                StatBox("Highs", "${(profile.highFreqResponse * 100).toInt()}%", Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Suggested mode
            Surface(
                color = Color(0xFF0F3460),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡 Recommended:", color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        profile.suggestedBattleMode.name.replace("_", " "),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("APPLY VENUE OPTIMIZATIONS")
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

// ==================== CONTROLS TAB ====================

@Composable
private fun ControlsTab(
    viewModel: MainViewModel,
    onNavigateToSoundBattle: () -> Unit,
    onNavigateToAISettings: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Settings Card
        item {
            val isAIConfigured by viewModel.grokAIService.isConfigured.collectAsState()
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAISettings() },
                colors = CardDefaults.cardColors(
                    containerColor = if (isAIConfigured) Color(0xFF4CAF50).copy(alpha = 0.2f) 
                                    else Color(0xFFFF9800).copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isAIConfigured) "🤖" else "⚙️",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isAIConfigured) "AI BATTLE ASSISTANT ✓" else "SETUP FREE AI",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            if (isAIConfigured) "Grok/Groq powered intelligence active"
                                              else "Tap to configure FREE AI (Groq)",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        null,
                        tint = Color.White
                    )
                }
            }
        }

        // Dominant Mode Toggle (DJ Mode)
        item {
            val isDominantMode by viewModel.isDominantMode.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setDominantMode(!isDominantMode) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isDominantMode) Color(0xFFFF5722).copy(alpha = 0.3f)
                                    else Color(0xFF2196F3).copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isDominantMode) "🔥" else "🎧",
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isDominantMode) "DOMINANT MODE: ON" else "DOMINANT MODE: OFF",
                            fontWeight = FontWeight.Bold,
                            color = if (isDominantMode) Color(0xFFFF5722) else Color.White
                        )
                        Text(
                            if (isDominantMode)
                                "NOTHING STOPS THE MUSIC! Calls, notifications ignored."
                            else
                                "Tap to enable - Music plays through EVERYTHING",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    // Toggle indicator
                    Box(
                        modifier = Modifier
                            .size(48.dp, 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isDominantMode) Color(0xFFFF5722)
                                else Color.Gray.copy(alpha = 0.5f)
                            )
                            .padding(2.dp),
                        contentAlignment = if (isDominantMode) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        // Quick actions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336).copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚡ QUICK ACTIONS",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickActionBtn("🔊", "BASS\nDROP", Color(0xFFE91E63)) {
                            viewModel.emergencyBassBoost()
                        }
                        QuickActionBtn("⚔️", "CUT\nTHRU", Color(0xFF2196F3)) {
                            viewModel.cutThrough()
                        }
                        QuickActionBtn("☢️", "GO\nNUCLEAR", Color(0xFFFF5722)) {
                            viewModel.goNuclear()
                        }
                    }
                }
            }
        }
        
        // Navigate to full controls
        item {
            Button(
                onClick = onNavigateToSoundBattle,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
            ) {
                Icon(Icons.Default.Equalizer, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("OPEN FULL BATTLE CONTROLS", fontWeight = FontWeight.Bold)
            }
        }
        
        // Battle modes
        item {
            Text(
                "🎚️ BATTLE MODES",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val modes = listOf(
                    BattleMode.BASS_WARFARE to "🔊 Bass War",
                    BattleMode.CLARITY_STRIKE to "⚔️ Clarity",
                    BattleMode.FULL_ASSAULT to "💀 Assault",
                    BattleMode.SPL_MONSTER to "📊 SPL",
                    BattleMode.CROWD_REACH to "🎯 Reach"
                )
                
                items(modes) { (mode, label) ->
                    FilledTonalButton(onClick = { viewModel.setBattleMode(mode) }) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionBtn(
    emoji: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.size(90.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 24.sp)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
