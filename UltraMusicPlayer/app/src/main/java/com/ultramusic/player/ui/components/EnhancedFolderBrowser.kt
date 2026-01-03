package com.ultramusic.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ultramusic.player.data.Song

/**
 * Folder Browser View Mode
 */
enum class FolderViewMode {
    HIERARCHICAL,  // Tree structure - folders within folders
    LINEAR         // Flat list - all folders at once
}

/**
 * Data class representing a folder in the browser
 */
data class FolderItem(
    val path: String,
    val name: String,
    val songCount: Int,
    val subFolders: List<FolderItem> = emptyList(),
    val isShortcut: Boolean = false,
    val parentPath: String? = null
)

/**
 * Breadcrumb item for navigation
 */
data class BreadcrumbItem(
    val path: String,
    val name: String,
    val isRoot: Boolean = false
)

/**
 * Enhanced Folder Browser (Musicolet-style)
 *
 * Features:
 * - Hierarchical view (tree structure)
 * - Linear view (flat list of all folders)
 * - Breadcrumb navigation
 * - Folder shortcuts (favorites)
 * - One-tap navigation row
 * - Search functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedFolderBrowser(
    folders: List<FolderItem>,
    currentPath: String,
    shortcuts: List<FolderItem>,
    viewMode: FolderViewMode,
    onViewModeChange: (FolderViewMode) -> Unit,
    onFolderClick: (FolderItem) -> Unit,
    onPlayFolder: (FolderItem) -> Unit,
    onToggleShortcut: (FolderItem) -> Unit,
    onNavigateToPath: (String) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    // New parameters for songs
    songs: List<Song> = emptyList(),
    currentPlayingSongId: Long? = null,
    onSongClick: (Song) -> Unit = {},
    onAddToPlaylist: (Song) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Build breadcrumb from current path
    val breadcrumbs = remember(currentPath) {
        buildBreadcrumbs(currentPath)
    }

    // In hierarchical mode, show the "current" folder's children (so clicking a folder actually enters it).
    val baseFoldersForCurrentPath = remember(folders, currentPath, viewMode) {
        if (viewMode != FolderViewMode.HIERARCHICAL) {
            folders
        } else {
            if (currentPath.isBlank()) {
                folders
            } else {
                findFolderByPath(folders, currentPath)?.subFolders ?: emptyList()
            }
        }
    }

    // Filter folders based on search (applies to the currently visible folder list)
    val filteredFolders = remember(baseFoldersForCurrentPath, searchQuery) {
        if (searchQuery.isBlank()) baseFoldersForCurrentPath
        else baseFoldersForCurrentPath.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Bar with View Toggle and Search
        TopNavigationBar(
            viewMode = viewMode,
            onViewModeChange = onViewModeChange,
            isSearchExpanded = isSearchExpanded,
            onSearchToggle = { isSearchExpanded = !isSearchExpanded },
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            accentColor = accentColor
        )

        // Shortcuts Row (Quick Access)
        if (shortcuts.isNotEmpty()) {
            ShortcutsRow(
                shortcuts = shortcuts,
                onShortcutClick = onFolderClick,
                onRemoveShortcut = onToggleShortcut,
                accentColor = accentColor
            )
        }

        // Breadcrumb Navigation
        if (viewMode == FolderViewMode.HIERARCHICAL && breadcrumbs.size > 1) {
            BreadcrumbNavigation(
                breadcrumbs = breadcrumbs,
                onBreadcrumbClick = onNavigateToPath,
                accentColor = accentColor
            )
        }

        Divider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )

        // Folder and Song List
        when (viewMode) {
            FolderViewMode.HIERARCHICAL -> {
                HierarchicalFolderList(
                    folders = filteredFolders,
                    songs = songs,
                    currentPlayingSongId = currentPlayingSongId,
                    onFolderClick = onFolderClick,
                    onPlayFolder = onPlayFolder,
                    onToggleShortcut = onToggleShortcut,
                    onSongClick = onSongClick,
                    onAddToPlaylist = onAddToPlaylist,
                    shortcuts = shortcuts,
                    accentColor = accentColor
                )
            }
            FolderViewMode.LINEAR -> {
                LinearFolderList(
                    folders = filteredFolders,
                    songs = songs,
                    currentPlayingSongId = currentPlayingSongId,
                    onFolderClick = onFolderClick,
                    onPlayFolder = onPlayFolder,
                    onToggleShortcut = onToggleShortcut,
                    onSongClick = onSongClick,
                    onAddToPlaylist = onAddToPlaylist,
                    shortcuts = shortcuts,
                    accentColor = accentColor
                )
            }
        }
    }
}

private fun findFolderByPath(
    folders: List<FolderItem>,
    targetPath: String
): FolderItem? {
    for (folder in folders) {
        if (folder.path == targetPath) return folder
        val matchInChildren = findFolderByPath(folder.subFolders, targetPath)
        if (matchInChildren != null) return matchInChildren
    }
    return null
}

@Composable
private fun TopNavigationBar(
    viewMode: FolderViewMode,
    onViewModeChange: (FolderViewMode) -> Unit,
    isSearchExpanded: Boolean,
    onSearchToggle: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    accentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title
            Text(
                text = "FOLDERS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View Mode Toggle
                ViewModeToggle(
                    viewMode = viewMode,
                    onViewModeChange = onViewModeChange,
                    accentColor = accentColor
                )

                // Search Button
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchExpanded) accentColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Search Field (expandable)
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                placeholder = { Text("Search folders...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = accentColor,
                    cursorColor = accentColor
                )
            )
        }
    }
}

@Composable
private fun ViewModeToggle(
    viewMode: FolderViewMode,
    onViewModeChange: (FolderViewMode) -> Unit,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Hierarchical View
            FilterChip(
                selected = viewMode == FolderViewMode.HIERARCHICAL,
                onClick = { onViewModeChange(FolderViewMode.HIERARCHICAL) },
                label = {
                    Icon(
                        Icons.Default.AccountTree,
                        contentDescription = "Tree View",
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor,
                    selectedLabelColor = Color.White
                ),
                border = null
            )

            // Linear View
            FilterChip(
                selected = viewMode == FolderViewMode.LINEAR,
                onClick = { onViewModeChange(FolderViewMode.LINEAR) },
                label = {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "List View",
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor,
                    selectedLabelColor = Color.White
                ),
                border = null
            )
        }
    }
}

@Composable
private fun ShortcutsRow(
    shortcuts: List<FolderItem>,
    onShortcutClick: (FolderItem) -> Unit,
    onRemoveShortcut: (FolderItem) -> Unit,
    accentColor: Color
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "QUICK ACCESS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(shortcuts) { folder ->
                ShortcutChip(
                    folder = folder,
                    onClick = { onShortcutClick(folder) },
                    onRemove = { onRemoveShortcut(folder) },
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun ShortcutChip(
    folder: FolderItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(20.dp)),
        color = accentColor.copy(alpha = 0.15f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = folder.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Badge(
                containerColor = accentColor.copy(alpha = 0.3f),
                contentColor = accentColor
            ) {
                Text(
                    text = "${folder.songCount}",
                    fontSize = 10.sp
                )
            }
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove shortcut",
                tint = accentColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

@Composable
private fun BreadcrumbNavigation(
    breadcrumbs: List<BreadcrumbItem>,
    onBreadcrumbClick: (String) -> Unit,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        breadcrumbs.forEachIndexed { index, breadcrumb ->
            val isLast = index == breadcrumbs.lastIndex

            Surface(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)),
                color = if (isLast) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                onClick = { if (!isLast) onBreadcrumbClick(breadcrumb.path) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (breadcrumb.isRoot) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Root",
                            tint = if (isLast) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = breadcrumb.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            if (!isLast) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HierarchicalFolderList(
    folders: List<FolderItem>,
    songs: List<Song>,
    currentPlayingSongId: Long?,
    onFolderClick: (FolderItem) -> Unit,
    onPlayFolder: (FolderItem) -> Unit,
    onToggleShortcut: (FolderItem) -> Unit,
    onSongClick: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    shortcuts: List<FolderItem>,
    accentColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Show folders first
        items(folders) { folder ->
            HierarchicalFolderItem(
                folder = folder,
                level = 0,
                isShortcut = shortcuts.any { it.path == folder.path },
                onFolderClick = onFolderClick,
                onPlayFolder = onPlayFolder,
                onToggleShortcut = onToggleShortcut,
                shortcuts = shortcuts,
                accentColor = accentColor
            )
        }

        // Show songs after folders
        if (songs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SONGS (${songs.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            items(songs) { song ->
                SongListItem(
                    song = song,
                    isPlaying = currentPlayingSongId == song.id,
                    onSongClick = onSongClick,
                    onAddToPlaylist = onAddToPlaylist,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun HierarchicalFolderItem(
    folder: FolderItem,
    level: Int,
    isShortcut: Boolean,
    onFolderClick: (FolderItem) -> Unit,
    onPlayFolder: (FolderItem) -> Unit,
    onToggleShortcut: (FolderItem) -> Unit,
    shortcuts: List<FolderItem>,
    accentColor: Color
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        label = "chevron_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isShortcut)
                    accentColor.copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            // Clicking the card opens the folder to show songs
            onClick = { onFolderClick(folder) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expand/Collapse indicator - separate clickable
                if (folder.subFolders.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isExpanded = !isExpanded }
                            .background(
                                if (isExpanded) accentColor.copy(alpha = 0.1f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = if (isExpanded) "Collapse subfolders" else "Expand subfolders",
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotationAngle),
                            tint = if (isExpanded) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(28.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Folder Icon
                Icon(
                    if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Folder Name and Song Count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folder.songCount} songs" +
                                if (folder.subFolders.isNotEmpty()) " • ${folder.subFolders.size} folders" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Shortcut Toggle
                IconButton(
                    onClick = { onToggleShortcut(folder) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isShortcut) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isShortcut) "Remove shortcut" else "Add shortcut",
                        tint = if (isShortcut) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play Button
                IconButton(
                    onClick = { onPlayFolder(folder) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play folder",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Subfolders (when expanded)
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                folder.subFolders.forEach { subFolder ->
                    Spacer(modifier = Modifier.height(4.dp))
                    HierarchicalFolderItem(
                        folder = subFolder,
                        level = level + 1,
                        isShortcut = shortcuts.any { it.path == subFolder.path },
                        onFolderClick = onFolderClick,
                        onPlayFolder = onPlayFolder,
                        onToggleShortcut = onToggleShortcut,
                        shortcuts = shortcuts,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LinearFolderList(
    folders: List<FolderItem>,
    songs: List<Song>,
    currentPlayingSongId: Long?,
    onFolderClick: (FolderItem) -> Unit,
    onPlayFolder: (FolderItem) -> Unit,
    onToggleShortcut: (FolderItem) -> Unit,
    onSongClick: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    shortcuts: List<FolderItem>,
    accentColor: Color
) {
    // Flatten the folder structure for linear view
    val flatFolders = remember(folders) {
        flattenFolders(folders)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Show folders first
        items(flatFolders.sortedBy { it.name.lowercase() }) { folder ->
            LinearFolderItem(
                folder = folder,
                isShortcut = shortcuts.any { it.path == folder.path },
                onFolderClick = onFolderClick,
                onPlayFolder = onPlayFolder,
                onToggleShortcut = onToggleShortcut,
                accentColor = accentColor
            )
        }

        // Show songs after folders
        if (songs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SONGS (${songs.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
            }

            items(songs) { song ->
                SongListItem(
                    song = song,
                    isPlaying = currentPlayingSongId == song.id,
                    onSongClick = onSongClick,
                    onAddToPlaylist = onAddToPlaylist,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun LinearFolderItem(
    folder: FolderItem,
    isShortcut: Boolean,
    onFolderClick: (FolderItem) -> Unit,
    onPlayFolder: (FolderItem) -> Unit,
    onToggleShortcut: (FolderItem) -> Unit,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isShortcut)
                accentColor.copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = { onFolderClick(folder) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folder Icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Folder Name and Path
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${folder.songCount} songs",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Shortcut Toggle
            IconButton(
                onClick = { onToggleShortcut(folder) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    if (isShortcut) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isShortcut) "Remove shortcut" else "Add shortcut",
                    tint = if (isShortcut) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Play Button
            Surface(
                shape = CircleShape,
                color = accentColor,
                modifier = Modifier.size(36.dp),
                onClick = { onPlayFolder(folder) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play folder",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Build breadcrumb items from a path
 */
private fun buildBreadcrumbs(path: String): List<BreadcrumbItem> {
    if (path.isBlank()) return listOf(BreadcrumbItem("/", "Root", true))

    val parts = path.split("/").filter { it.isNotBlank() }
    val breadcrumbs = mutableListOf(BreadcrumbItem("/", "Root", true))

    var currentPath = ""
    parts.forEach { part ->
        currentPath = if (currentPath.isEmpty()) "/$part" else "$currentPath/$part"
        breadcrumbs.add(BreadcrumbItem(currentPath, part))
    }

    return breadcrumbs
}

/**
 * Flatten a hierarchical folder structure for linear view
 */
private fun flattenFolders(folders: List<FolderItem>): List<FolderItem> {
    val result = mutableListOf<FolderItem>()

    fun flatten(folder: FolderItem) {
        result.add(folder)
        folder.subFolders.forEach { flatten(it) }
    }

    folders.forEach { flatten(it) }
    return result
}

/**
 * Song list item with play and add to playlist options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongListItem(
    song: Song,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    accentColor: Color
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                accentColor.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = { onSongClick(song) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art or music icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album art",
                        modifier = Modifier.size(48.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Playing indicator overlay
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                accentColor.copy(alpha = 0.85f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Now playing",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPlaying) accentColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            Text(
                text = song.durationFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 8.dp)
            )

            // More options button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Play") },
                        onClick = {
                            onSongClick(song)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        onClick = {
                            onAddToPlaylist(song)
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}
