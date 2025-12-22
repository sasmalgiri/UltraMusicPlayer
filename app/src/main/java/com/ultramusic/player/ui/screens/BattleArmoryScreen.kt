package com.ultramusic.player.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.core.BattleArmory
import com.ultramusic.player.core.BattleSongRecommendations
import com.ultramusic.player.core.ClipPurpose
import com.ultramusic.player.core.ClipRecommendation
import com.ultramusic.player.core.CounterClip
import com.ultramusic.player.core.RecommendationPriority
import com.ultramusic.player.core.SongTypeRecommendation
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleArmoryScreen(
    armory: BattleArmory,
    onNavigateBack: () -> Unit,
    onPlayClip: (CounterClip) -> Unit,
    onCreateClip: () -> Unit
) {
    val counterClips by armory.counterClips.collectAsState()
    val quickFireClips by armory.quickFireClips.collectAsState()
    val clipsByPurpose by armory.clipsByPurpose.collectAsState()
    val recommendations = armory.getPreparationRecommendations()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("🚀 Quick Fire", "📦 All Clips", "📋 Prepare", "📚 Guide")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "⚔️ BATTLE ARMORY",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(padding)
        ) {
            // Tab Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.size) { index ->
                    TabButton(
                        text = tabs[index],
                        isSelected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
            
            // Content
            when (selectedTab) {
                0 -> QuickFireSection(
                    clips = quickFireClips,
                    onPlayClip = onPlayClip,
                    onRemoveFromQuickFire = { armory.removeFromQuickFire(it.id) }
                )
                1 -> AllClipsSection(
                    clipsByPurpose = clipsByPurpose,
                    onPlayClip = onPlayClip,
                    onAddToQuickFire = { armory.addToQuickFire(it) },
                    onDeleteClip = { armory.deleteClip(it.id) },
                    onCreateClip = onCreateClip
                )
                2 -> PreparationSection(
                    recommendations = recommendations,
                    totalClips = counterClips.size
                )
                3 -> GuideSection()
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2196F3) else Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ==================== QUICK FIRE SECTION ====================

@Composable
private fun QuickFireSection(
    clips: List<CounterClip>,
    onPlayClip: (CounterClip) -> Unit,
    onRemoveFromQuickFire: (CounterClip) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "🚀 QUICK FIRE - Your Top 10 Counters",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "One-tap access during battle. Drag to reorder.",
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (clips.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "🎯",
                    title = "No Quick Fire Clips",
                    message = "Add your best counter clips here for instant access during battle!"
                )
            }
        } else {
            items(clips) { clip ->
                QuickFireClipCard(
                    clip = clip,
                    index = clips.indexOf(clip) + 1,
                    onPlay = { onPlayClip(clip) },
                    onRemove = { onRemoveFromQuickFire(clip) }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun QuickFireClipCard(
    clip: CounterClip,
    index: Int,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFFF5722), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$index",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    clip.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${clip.songTitle} • ${formatDuration(clip.durationMs)}",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        clip.purpose.emoji,
                        fontSize = 12.sp
                    )
                    Text(
                        " ${clip.purpose.displayName}",
                        color = Color(0xFF64B5F6),
                        fontSize = 12.sp
                    )
                    if (clip.useCount > 0) {
                        Text(
                            " • ${(clip.winRate * 100).roundToInt()}% win",
                            color = Color(0xFF81C784),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // Play button
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF4CAF50), CircleShape)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White
                )
            }
            
            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color(0xFFE57373)
                )
            }
        }
    }
}

// ==================== ALL CLIPS SECTION ====================

@Composable
private fun AllClipsSection(
    clipsByPurpose: Map<ClipPurpose, List<CounterClip>>,
    onPlayClip: (CounterClip) -> Unit,
    onAddToQuickFire: (CounterClip) -> Unit,
    onDeleteClip: (CounterClip) -> Unit,
    onCreateClip: () -> Unit
) {
    val totalClips = clipsByPurpose.values.flatten().size
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "📦 ALL COUNTER CLIPS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "$totalClips clips organized by purpose",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                Button(
                    onClick = onCreateClip,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Clip")
                }
            }
        }
        
        ClipPurpose.values().forEach { purpose ->
            val clips = clipsByPurpose[purpose].orEmpty()
            
            item {
                PurposeCategoryCard(
                    purpose = purpose,
                    clips = clips,
                    onPlayClip = onPlayClip,
                    onAddToQuickFire = onAddToQuickFire,
                    onDeleteClip = onDeleteClip
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PurposeCategoryCard(
    purpose: ClipPurpose,
    clips: List<CounterClip>,
    onPlayClip: (CounterClip) -> Unit,
    onAddToQuickFire: (CounterClip) -> Unit,
    onDeleteClip: (CounterClip) -> Unit
) {
    var expanded by remember { mutableStateOf(clips.isNotEmpty()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (clips.isEmpty()) Color(0xFF1A1A1A) else Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    purpose.emoji,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        purpose.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        purpose.description,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            if (clips.size >= purpose.minRecommended) Color(0xFF4CAF50)
                            else Color(0xFFFF9800),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${clips.size}/${purpose.minRecommended}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Clips list
            if (expanded && clips.isNotEmpty()) {
                clips.forEach { clip ->
                    ClipListItem(
                        clip = clip,
                        onPlay = { onPlayClip(clip) },
                        onAddToQuickFire = { onAddToQuickFire(clip) },
                        onDelete = { onDeleteClip(clip) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClipListItem(
    clip: CounterClip,
    onPlay: () -> Unit,
    onAddToQuickFire: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                clip.name,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                "${clip.songTitle} • ${formatDuration(clip.durationMs)}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        
        IconButton(onClick = onAddToQuickFire) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Add to Quick Fire",
                tint = Color(0xFFFFD700)
            )
        }
        
        IconButton(onClick = onPlay) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color(0xFF4CAF50)
            )
        }
    }
}

// ==================== PREPARATION SECTION ====================

@Composable
private fun PreparationSection(
    recommendations: List<ClipRecommendation>,
    totalClips: Int
) {
    val checklist = BattleSongRecommendations.getBattleChecklist()
    val totalRecommended = BattleSongRecommendations.getTotalRecommended()
    val readiness = (totalClips.toFloat() / totalRecommended * 100).coerceAtMost(100f)
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "📋 BATTLE PREPARATION",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        // Readiness meter
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Battle Readiness",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${readiness.roundToInt()}%",
                            color = when {
                                readiness >= 80 -> Color(0xFF4CAF50)
                                readiness >= 50 -> Color(0xFFFF9800)
                                else -> Color(0xFFE53935)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = readiness / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            readiness >= 80 -> Color(0xFF4CAF50)
                            readiness >= 50 -> Color(0xFFFF9800)
                            else -> Color(0xFFE53935)
                        },
                        trackColor = Color(0xFF333333)
                    )
                    Text(
                        "$totalClips / $totalRecommended clips prepared",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Missing clips
        if (recommendations.isNotEmpty()) {
            item {
                Text(
                    "⚠️ MISSING CLIPS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            items(recommendations) { rec ->
                MissingClipCard(recommendation = rec)
            }
        }
        
        // Checklist
        item {
            Text(
                "✓ BATTLE CHECKLIST",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        items(checklist) { item ->
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item,
                    color = Color(0xFFB0BEC5),
                    fontSize = 14.sp
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MissingClipCard(recommendation: ClipRecommendation) {
    val priorityColor = when (recommendation.priority) {
        RecommendationPriority.CRITICAL -> Color(0xFFE53935)
        RecommendationPriority.HIGH -> Color(0xFFFF9800)
        RecommendationPriority.MEDIUM -> Color(0xFFFFEB3B)
        RecommendationPriority.LOW -> Color(0xFF4CAF50)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, priorityColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (recommendation.priority == RecommendationPriority.CRITICAL)
                    Icons.Default.Warning else Icons.Default.Add,
                contentDescription = null,
                tint = priorityColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${recommendation.purpose.emoji} ${recommendation.purpose.displayName}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Need ${recommendation.recommendedCount - recommendation.currentCount} more",
                    color = priorityColor,
                    fontSize = 12.sp
                )
                Text(
                    recommendation.suggestion,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ==================== GUIDE SECTION ====================

@Composable
private fun GuideSection() {
    val categories = BattleSongRecommendations.ESSENTIAL_CATEGORIES
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "📚 BATTLE SONG GUIDE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "What songs to prepare for any opponent",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
        
        items(categories) { category ->
            SongCategoryGuideCard(category = category)
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SongCategoryGuideCard(category: SongTypeRecommendation) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.emoji,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        category.category,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        category.description,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF2196F3), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Min: ${category.minCount}",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    "Examples:",
                    color = Color(0xFF64B5F6),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                category.examples.forEach { example ->
                    Text(
                        "• $example",
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Tips:",
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                category.tips.forEach { tip ->
                    Text(
                        "💡 $tip",
                        color = Color(0xFFB0BEC5),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

// ==================== HELPERS ====================

@Composable
private fun EmptyStateCard(
    emoji: String,
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                emoji,
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                message,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs = seconds % 60
    return "${minutes}:${secs.toString().padStart(2, '0')}"
}
