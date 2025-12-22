package com.ultramusic.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ultramusic.player.ui.MainViewModel
import com.ultramusic.player.ui.screens.ActiveBattleScreen
import com.ultramusic.player.ui.screens.AISettingsScreen
import com.ultramusic.player.ui.screens.AudioBattleScreen
import com.ultramusic.player.ui.screens.BattleAnalyzerScreen
import com.ultramusic.player.ui.screens.BattleArmoryScreen
import com.ultramusic.player.ui.screens.BattleHQScreen
import com.ultramusic.player.ui.screens.BattleLibraryScreen
import com.ultramusic.player.ui.screens.CounterSongScreen
import com.ultramusic.player.ui.screens.EasyPlayerScreen
import com.ultramusic.player.ui.screens.EnhancementListScreen
import com.ultramusic.player.ui.screens.FolderBrowserScreen
import com.ultramusic.player.ui.screens.HomeScreen
import com.ultramusic.player.ui.screens.NowPlayingScreen
import com.ultramusic.player.ui.screens.SmartPlaylistScreen
import com.ultramusic.player.ui.screens.VoiceSearchScreen
import com.ultramusic.player.ui.theme.UltraMusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            UltraMusicPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UltraMusicNavigation()
                }
            }
        }
    }
}

@Composable
fun UltraMusicNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()
    
    NavHost(
        navController = navController,
        startDestination = "easy_player"  // Changed to Easy Player as default
    ) {
        // Main Easy Player Screen - the primary UI
        composable("easy_player") {
            EasyPlayerScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate("home")
                },
                onNavigateToBattleLibrary = {
                    navController.navigate("battle_library")
                },
                onNavigateToBattleHQ = {
                    navController.navigate("battle_hq")
                },
                onNavigateToActiveBattle = {
                    navController.navigate("active_battle")
                },
                onNavigateToCounterSong = {
                    navController.navigate("counter_song")
                },
                onNavigateToBattleAnalyzer = {
                    navController.navigate("battle_analyzer")
                },
                onNavigateToBattleArmory = {
                    navController.navigate("battle_armory")
                }
            )
        }
        
        // Home screen with all songs
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToNowPlaying = {
                    navController.navigate("now_playing")
                },
                onNavigateToFolders = {
                    navController.navigate("folders")
                },
                onNavigateToVoiceSearch = {
                    navController.navigate("voice_search")
                },
                onNavigateToPlaylist = {
                    navController.navigate("playlist")
                }
            )
        }
        
        composable("now_playing") {
            NowPlayingScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("folders") {
            FolderBrowserScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNowPlaying = {
                    navController.navigate("now_playing")
                }
            )
        }
        
        composable("voice_search") {
            VoiceSearchScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNowPlaying = {
                    navController.navigate("now_playing")
                }
            )
        }
        
        composable("playlist") {
            SmartPlaylistScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNowPlaying = {
                    navController.navigate("now_playing")
                }
            )
        }
        
        composable("enhancements") {
            EnhancementListScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("sound_battle") {
            AudioBattleScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("battle_hq") {
            BattleHQScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSoundBattle = {
                    navController.navigate("sound_battle")
                },
                onNavigateToAISettings = {
                    navController.navigate("ai_settings")
                }
            )
        }
        
        // Active Battle AI Screen - Auto-fighting battles
        composable("active_battle") {
            ActiveBattleScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Counter Song Screen - AI song picker
        composable("counter_song") {
            CounterSongScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPlaySong = { song ->
                    viewModel.playSong(song)
                }
            )
        }
        
        // Battle Library - Dedicated song library for battles
        composable("battle_library") {
            BattleLibraryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPlaySong = { song ->
                    viewModel.playSong(song)
                },
                onAddToQueue = { song ->
                    viewModel.addToPlaylistEnd(song)
                },
                onNavigateToCounterSong = {
                    navController.navigate("counter_song")
                }
            )
        }
        
        // Battle Analyzer - Listen to opponent and get counter suggestions
        composable("battle_analyzer") {
            BattleAnalyzerScreen(
                analyzer = viewModel.localBattleAnalyzer,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPlaySong = { song ->
                    viewModel.playSong(song)
                }
            )
        }
        
        // Battle Armory - Pre-prepared counter clips
        composable("battle_armory") {
            BattleArmoryScreen(
                armory = viewModel.battleArmory,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPlayClip = { clip ->
                    // Play the clip with A-B loop set
                    viewModel.playClip(clip)
                },
                onCreateClip = {
                    // Navigate to now playing to create clip from current song
                    navController.navigate("now_playing")
                }
            )
        }
        
        // AI Settings Screen
        composable("ai_settings") {
            AISettingsScreen(
                grokAIService = viewModel.grokAIService,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
