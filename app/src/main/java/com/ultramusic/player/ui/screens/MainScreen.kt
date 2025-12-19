package com.ultramusic.player.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ultramusic.player.viewmodel.PlayerViewModel

/**
 * Navigation destinations
 */
sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Library : Screen("library", "Library", { Icon(Icons.Default.LibraryMusic, "Library") })
    object NowPlaying : Screen("now_playing", "Now Playing", { Icon(Icons.Default.PlayCircle, "Now Playing") })
    object Presets : Screen("presets", "Presets", { Icon(Icons.Default.Tune, "Presets") })
    object Settings : Screen("settings", "Settings", { Icon(Icons.Default.Settings, "Settings") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    
    // Request music scan on first launch
    LaunchedEffect(Unit) {
        viewModel.scanMusic()
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                listOf(Screen.Library, Screen.NowPlaying, Screen.Presets, Screen.Settings).forEach { screen ->
                    NavigationBarItem(
                        icon = screen.icon,
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel = viewModel,
                    onSongClick = { song ->
                        viewModel.playSong(song)
                        navController.navigate(Screen.NowPlaying.route)
                    }
                )
            }
            
            composable(Screen.NowPlaying.route) {
                NowPlayingScreen(viewModel = viewModel)
            }
            
            composable(Screen.Presets.route) {
                PresetsScreen(viewModel = viewModel)
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
