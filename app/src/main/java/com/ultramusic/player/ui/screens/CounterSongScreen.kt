package com.ultramusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.AutoMirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ultramusic.player.ai.CounterEngineState
import com.ultramusic.player.ai.CounterRecommendation
import com.ultramusic.player.ai.CounterStrategy
import com.ultramusic.player.ai.Mood
import com.ultramusic.player.ui.MainViewModel

/**
 * Counter Song Screen - Battle Mode UI
 * 
 * Features:
 * 1. Enter opponent's song (type or listen)
 * 2. Select counter strategy
 * 3. Get AI-powered recommendations
 * 4. One-tap to play counter song
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterSongScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onPlaySong: (com.ultramusic.player.data.Song) -> Unit
) {
    val counterState by viewModel.counterEngineState.collectAsState()
    val recommendations by viewModel.counterRecommendations.collectAsState()
    
    var opponentSong by remember { mutableStateOf("") }
    var opponentArtist by remember { mutableStateOf("") }
    var selectedStrategy by remember { mutableStateOf(CounterStrategy.AUTO) }
    var isListening by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF5722)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Battle Mode",
                            fontWeight = FontWeight.Bold
                        )
                    }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== OPPONENT SONG INPUT =====
            item {
                OpponentSongCard(
                    songTitle = opponentSong,
                    artistName = opponentArtist,
                    isListening = isListening,
                    onSongChange = { opponentSong = it },
                    onArtistChange = { opponentArtist = it },
                    onStartListening = {
                        isListening = true
                        viewModel.startListeningToOpponent()
                    },
                    onStopListening = {
                        isListening = false
                        viewModel.stopListeningToOpponent()
                    }
                )
            }
            
            // ===== STRATEGY SELECTION =====
            item {
                StrategySelector(
                    selectedStrategy = selectedStrategy,
                    onStrategySelected = { selectedStrategy = it }
                )
            }
            
            // ===== FIND COUNTER BUTTON =====
            item {
                FilledTonalButton(
                    onClick = {
                        viewModel.findCounterSong(opponentSong, opponentArtist, selectedStrategy)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = opponentSong.isNotBlank() && counterState !is CounterEngineState.Analyzing
                ) {
                    if (counterState is CounterEngineState.Analyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...")
                    } else {
                        Icon(Icons.Default.Psychology, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Find Counter Song", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // ===== STATE INDICATOR =====
            item {
                when (val state = counterState) {
                    is CounterEngineState.Indexing -> {
                        IndexingProgress(state.current, state.total)
                    }
                    is CounterEngineState.Listening -> {
                        ListeningIndicator()
                    }
                    is CounterEngineState.Error -> {
                        ErrorCard(state.message)
                    }
                    else -> {}
                }
            }
            
            // ===== RECOMMENDATIONS =====
            if (recommendations.isNotEmpty()) {
                item {
                    Text(
                        text = "🎯 Counter Recommendations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                items(recommendations) { recommendation ->
                    CounterRecommendationCard(
                        recommendation = recommendation,
                        onPlay = { onPlaySong(recommendation.song) },
                        onAddToPlaylist = { viewModel.addToPlaylistEnd(recommendation.song) }
                    )
                }
            }
            
            // ===== TIPS SECTION =====
            item {
                TipsCard()
            }
        }
    }
}

@Composable
private fun OpponentSongCard(
    songTitle: String,
    artistName: String,
    isListening: Boolean,
    onSongChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Compare,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Opponent's Song",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Song title input
            OutlinedTextField(
                value = songTitle,
                onValueChange = onSongChange,
                label = { Text("Song Title") },
                placeholder = { Text("e.g., Kesariya") },
                leadingIcon = { Icon(Icons.Default.MusicNote, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Artist input
            OutlinedTextField(
                value = artistName,
                onValueChange = onArtistChange,
                label = { Text("Artist (optional)") },
                placeholder = { Text("e.g., Arijit Singh") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Listen button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilledTonalButton(
                    onClick = if (isListening) onStopListening else onStartListening
                ) {
                    Icon(
                        if (isListening) Icons.Default.Hearing else Icons.Default.Mic,
                        null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isListening) "Stop Listening" else "Listen to Opponent")
                }
            }
            
            if (isListening) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🎧 Listening to opponent's song...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StrategySelector(
    selectedStrategy: CounterStrategy,
    onStrategySelected: (CounterStrategy) -> Unit
) {
    Column {
        Text(
            text = "Select Strategy",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val strategies = listOf(
                StrategyInfo(CounterStrategy.AUTO, "🤖 Auto", "AI picks best"),
                StrategyInfo(CounterStrategy.CONTRAST, "🔄 Contrast", "Opposite vibe"),
                StrategyInfo(CounterStrategy.ESCALATE, "⬆️ Escalate", "More intense"),
                StrategyInfo(CounterStrategy.SURPRISE, "🎭 Surprise", "Unexpected"),
                StrategyInfo(CounterStrategy.CROWD_PLEASER, "🎉 Popular", "Safe hit"),
                StrategyInfo(CounterStrategy.SMOOTH_TRANSITION, "🎧 Mix", "DJ friendly")
            )
            
            items(strategies) { info ->
                FilterChip(
                    selected = selectedStrategy == info.strategy,
                    onClick = { onStrategySelected(info.strategy) },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(info.emoji, style = MaterialTheme.typography.bodyMedium)
                            Text(info.subtitle, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            }
        }
    }
}

private data class StrategyInfo(
    val strategy: CounterStrategy,
    val emoji: String,
    val subtitle: String
)

@Composable
private fun CounterRecommendationCard(
    recommendation: CounterRecommendation,
    onPlay: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val scoreColor = when {
        recommendation.score > 70 -> Color(0xFF4CAF50)
        recommendation.score > 50 -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Album art
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (recommendation.song.albumArtUri != null) {
                        AsyncImage(
                            model = recommendation.song.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Song info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = recommendation.song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Strategy badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = when (recommendation.strategy) {
                                    CounterStrategy.CONTRAST -> "🔄 Contrast"
                                    CounterStrategy.ESCALATE -> "⬆️ Escalate"
                                    CounterStrategy.SURPRISE -> "🎭 Surprise"
                                    CounterStrategy.CROWD_PLEASER -> "🎉 Popular"
                                    CounterStrategy.SMOOTH_TRANSITION -> "🎧 Mix"
                                    CounterStrategy.AUTO -> "🤖 Auto"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(scoreColor.copy(alpha = 0.2f))
                            .border(2.dp, scoreColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${recommendation.score.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Reasoning
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "💡 ${recommendation.reasoning}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Features
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FeatureBadge("${recommendation.features.bpm.toInt()} BPM")
                FeatureBadge(recommendation.features.musicalKey)
                FeatureBadge(
                    when (recommendation.features.mood) {
                        Mood.HAPPY -> "😊"
                        Mood.SAD -> "😢"
                        Mood.ANGRY -> "😤"
                        Mood.CALM -> "😌"
                        Mood.NEUTRAL -> "😐"
                    }
                )
                FeatureBadge("${(recommendation.features.energy * 100).toInt()}% Energy")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilledTonalButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play Now")
                }
                FilledTonalButton(onClick = onAddToPlaylist) {
                    Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Queue")
                }
            }
        }
    }
}

@Composable
private fun FeatureBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun IndexingProgress(current: Int, total: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Indexing library: $current / $total songs",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ListeningIndicator() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Animated listening indicator
            Icon(
                Icons.Default.Hearing,
                null,
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Listening to opponent's song...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = "⚠️ $message",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun TipsCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "⚔️ Battle Tips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val tips = listOf(
                "🔄 Contrast: If they play slow sad song, hit them with high energy",
                "⬆️ Escalate: Same genre but MORE - faster, louder, better",
                "🎭 Surprise: Switch genres completely to throw them off",
                "🎉 Popular: When in doubt, play what the crowd knows",
                "🎧 Mix: Use compatible keys for smooth DJ transitions"
            )
            
            tips.forEach { tip ->
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
