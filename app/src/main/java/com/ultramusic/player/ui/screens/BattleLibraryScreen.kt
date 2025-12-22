package com.ultramusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ultramusic.player.data.Song
import com.ultramusic.player.ui.MainViewModel

/**
 * Battle Library Screen
 * 
 * A dedicated music library section customized for sound battles:
 * - Songs organized by energy level (High/Medium/Low)
 * - Quick filters for battle situations
 * - Battle favorites
 * - Recent battle songs
 * - AI-suggested battle songs
 */

// Energy categories for battle
enum class EnergyCategory(val emoji: String, val label: String, val color: Color) {
    HIGH("🔥", "High Energy", Color(0xFFE53935)),
    MEDIUM("⚡", "Medium", Color(0xFFFF9800)),
    LOW("🌊", "Chill/Build", Color(0xFF2196F3)),
    BASS_HEAVY("💥", "Bass Heavy", Color(0xFF9C27B0)),
    CROWD_PLEASER("🎉", "Crowd Pleasers", Color(0xFF4CAF50))
}

// Battle filters
enum class BattleFilter(val emoji: String, val label: String) {
    ALL("📚", "All Songs"),
    FAVORITES("⭐", "Battle Favorites"),
    RECENT("🕐", "Recently Used"),
    HIGH_ENERGY("🔥", "High Energy"),
    BASS_DROPS("💥", "Bass Drops"),
    OPENERS("🎬", "Openers"),
    CLOSERS("🏆", "Closers"),
    COUNTERS("⚔️", "Counter Songs")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BattleLibraryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onNavigateToCounterSong: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    
    var selectedFilter by remember { mutableStateOf(BattleFilter.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    
    // Battle favorites (simulated - in production, store in preferences)
    var battleFavorites by remember { mutableStateOf(setOf<Long>()) }
    
    // Categorize songs by energy
    val categorizedSongs by remember(uiState.songs) {
        derivedStateOf {
            categorizeSongsForBattle(uiState.songs)
        }
    }
    
    // Filtered songs based on selected filter
    val filteredSongs by remember(uiState.songs, selectedFilter, searchQuery, battleFavorites) {
        derivedStateOf {
            filterSongsForBattle(
                songs = uiState.songs,
                filter = selectedFilter,
                searchQuery = searchQuery,
                favorites = battleFavorites
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search battle songs...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF5722)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Battle Library", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            if (showSearch) Icons.Default.FilterList else Icons.Default.Search,
                            "Search"
                        )
                    }
                    IconButton(onClick = onNavigateToCounterSong) {
                        Icon(
                            Icons.Default.Psychology,
                            "AI Counter",
                            tint = Color(0xFF9C27B0)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0D0D1A)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== QUICK STATS =====
            item {
                BattleStatsCard(
                    totalSongs = uiState.songs.size,
                    favorites = battleFavorites.size,
                    highEnergy = categorizedSongs[EnergyCategory.HIGH]?.size ?: 0,
                    bassHeavy = categorizedSongs[EnergyCategory.BASS_HEAVY]?.size ?: 0
                )
            }
            
            // ===== FILTER CHIPS =====
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BattleFilter.entries) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(filter.emoji)
                                    Text(filter.label)
                                }
                            }
                        )
                    }
                }
            }
            
            // ===== ENERGY CATEGORIES (when filter is ALL) =====
            if (selectedFilter == BattleFilter.ALL && searchQuery.isEmpty()) {
                item {
                    Text(
                        "⚡ By Energy Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                items(EnergyCategory.entries) { category ->
                    val songs = categorizedSongs[category] ?: emptyList()
                    if (songs.isNotEmpty()) {
                        EnergyCategorySection(
                            category = category,
                            songs = songs.take(5),
                            totalCount = songs.size,
                            isFavorite = { battleFavorites.contains(it) },
                            onToggleFavorite = { songId ->
                                battleFavorites = if (battleFavorites.contains(songId)) {
                                    battleFavorites - songId
                                } else {
                                    battleFavorites + songId
                                }
                            },
                            onPlaySong = onPlaySong,
                            onAddToQueue = onAddToQueue,
                            onShowAll = { selectedFilter = when(category) {
                                EnergyCategory.HIGH -> BattleFilter.HIGH_ENERGY
                                EnergyCategory.BASS_HEAVY -> BattleFilter.BASS_DROPS
                                else -> BattleFilter.ALL
                            }}
                        )
                    }
                }
            }
            
            // ===== FILTERED SONG LIST =====
            if (selectedFilter != BattleFilter.ALL || searchQuery.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selectedFilter.emoji} ${selectedFilter.label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "${filteredSongs.size} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                items(filteredSongs) { song ->
                    BattleSongCard(
                        song = song,
                        isFavorite = battleFavorites.contains(song.id),
                        isPlaying = playbackState.currentSong?.id == song.id,
                        onPlay = { onPlaySong(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onToggleFavorite = {
                            battleFavorites = if (battleFavorites.contains(song.id)) {
                                battleFavorites - song.id
                            } else {
                                battleFavorites + song.id
                            }
                        }
                    )
                }
            }
            
            // ===== BATTLE TIPS =====
            item {
                BattleTipsCard()
            }
        }
    }
}

@Composable
private fun BattleStatsCard(
    totalSongs: Int,
    favorites: Int,
    highEnergy: Int,
    bassHeavy: Int
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E3F)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("📚", "$totalSongs", "Total")
            StatItem("⭐", "$favorites", "Favorites")
            StatItem("🔥", "$highEnergy", "High Energy")
            StatItem("💥", "$bassHeavy", "Bass Heavy")
        }
    }
}

@Composable
private fun StatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun EnergyCategorySection(
    category: EnergyCategory,
    songs: List<Song>,
    totalCount: Int,
    isFavorite: (Long) -> Boolean,
    onToggleFavorite: (Long) -> Unit,
    onPlaySong: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onShowAll: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = category.color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.emoji, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        category.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = category.color
                    )
                }
                
                Surface(
                    onClick = onShowAll,
                    color = category.color.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "See all $totalCount →",
                        style = MaterialTheme.typography.labelSmall,
                        color = category.color,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            songs.forEach { song ->
                CompactSongRow(
                    song = song,
                    isFavorite = isFavorite(song.id),
                    onPlay = { onPlaySong(song) },
                    onToggleFavorite = { onToggleFavorite(song.id) }
                )
            }
        }
    }
}

@Composable
private fun CompactSongRow(
    song: Song,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A4A)),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
        }
        
        // Favorite button
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                null,
                tint = if (isFavorite) Color(0xFFE91E63) else Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Play button
        IconButton(onClick = onPlay) {
            Icon(
                Icons.Default.PlayArrow,
                null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun BattleSongCard(
    song: Song,
    isFavorite: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isPlaying) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFF1E1E3F),
        label = "bg"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art with play overlay
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A2A4A)),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                // Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        formatDuration(song.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            // Action buttons
            Row {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        null,
                        tint = if (isFavorite) Color(0xFFE91E63) else Color.Gray
                    )
                }
                IconButton(onClick = onAddToQueue) {
                    Icon(Icons.Default.Queue, null, tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun BattleTipsCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "💡 Battle Library Tips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val tips = listOf(
                "⭐ Star your best battle songs for quick access",
                "🔥 High energy songs work great as openers",
                "💥 Bass-heavy tracks dominate in speaker battles",
                "⚔️ Use AI Counter to find songs that beat opponents",
                "🎬 Have your playlist ready before the battle starts"
            )
            
            tips.forEach { tip ->
                Text(
                    tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

// Helper functions
private fun formatDuration(durationMs: Long): String {
    val minutes = (durationMs / 1000) / 60
    val seconds = (durationMs / 1000) % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun categorizeSongsForBattle(songs: List<Song>): Map<EnergyCategory, List<Song>> {
    // In production, use actual audio analysis
    // For now, categorize based on title/artist keywords
    val result = mutableMapOf<EnergyCategory, MutableList<Song>>()
    
    EnergyCategory.entries.forEach { category ->
        result[category] = mutableListOf()
    }
    
    songs.forEach { song ->
        val title = song.title.lowercase()
        val artist = song.artist.lowercase()
        
        when {
            // Bass heavy detection
            title.contains("bass") || title.contains("drop") || 
            title.contains("boom") || title.contains("thump") -> {
                result[EnergyCategory.BASS_HEAVY]?.add(song)
            }
            // High energy detection
            title.contains("party") || title.contains("dance") ||
            title.contains("hype") || title.contains("fire") ||
            title.contains("remix") || title.contains("edm") -> {
                result[EnergyCategory.HIGH]?.add(song)
            }
            // Crowd pleasers (popular artists)
            artist.contains("arijit") || artist.contains("badshah") ||
            artist.contains("honey singh") || artist.contains("diljit") ||
            title.contains("hit") || title.contains("popular") -> {
                result[EnergyCategory.CROWD_PLEASER]?.add(song)
            }
            // Chill/Low energy
            title.contains("slow") || title.contains("chill") ||
            title.contains("ballad") || title.contains("acoustic") -> {
                result[EnergyCategory.LOW]?.add(song)
            }
            // Default to medium
            else -> {
                result[EnergyCategory.MEDIUM]?.add(song)
            }
        }
    }
    
    return result
}

private fun filterSongsForBattle(
    songs: List<Song>,
    filter: BattleFilter,
    searchQuery: String,
    favorites: Set<Long>
): List<Song> {
    var filtered = songs
    
    // Apply search
    if (searchQuery.isNotBlank()) {
        filtered = filtered.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }
    
    // Apply filter
    filtered = when (filter) {
        BattleFilter.ALL -> filtered
        BattleFilter.FAVORITES -> filtered.filter { favorites.contains(it.id) }
        BattleFilter.RECENT -> filtered.take(20) // Placeholder - use actual history
        BattleFilter.HIGH_ENERGY -> filtered.filter { song ->
            val title = song.title.lowercase()
            title.contains("party") || title.contains("dance") ||
            title.contains("remix") || title.contains("edm") ||
            title.contains("hype") || title.contains("fire")
        }
        BattleFilter.BASS_DROPS -> filtered.filter { song ->
            val title = song.title.lowercase()
            title.contains("bass") || title.contains("drop") ||
            title.contains("boom") || title.contains("sub")
        }
        BattleFilter.OPENERS -> filtered.sortedByDescending { 
            // Sort by energy keywords
            val title = it.title.lowercase()
            when {
                title.contains("intro") -> 100
                title.contains("opening") -> 90
                title.contains("start") -> 80
                else -> 50
            }
        }.take(20)
        BattleFilter.CLOSERS -> filtered.sortedByDescending {
            val title = it.title.lowercase()
            when {
                title.contains("finale") -> 100
                title.contains("ending") -> 90
                title.contains("closer") -> 80
                else -> 50
            }
        }.take(20)
        BattleFilter.COUNTERS -> filtered.shuffled().take(10) // AI suggestion placeholder
    }
    
    return filtered
}
