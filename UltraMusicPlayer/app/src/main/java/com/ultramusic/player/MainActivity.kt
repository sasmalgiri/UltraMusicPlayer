@file:androidx.media3.common.util.UnstableApi

package com.ultramusic.player

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ultramusic.player.ui.MainViewModel
import com.ultramusic.player.ui.components.SwipeBackContainer
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

/**
 * Permission state for storage/audio access
 */
enum class PermissionState {
    UNKNOWN,
    GRANTED,
    DENIED
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Permission launcher for audio/storage access - AUTO-SCAN on grant!
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
            viewModel.loadMusic()  // AUTO-SCAN after permission granted!
        } else {
            viewModel.onPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check and request permission IMMEDIATELY on launch
        checkAndRequestPermission()

        setContent {
            UltraMusicPlayerTheme {
                val permissionState by viewModel.permissionState.collectAsState()
                val isScanning by viewModel.isScanning.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        permissionState == PermissionState.GRANTED && isScanning -> {
                            // Show scanning screen while loading music
                            ScanningScreen()
                        }
                        permissionState == PermissionState.GRANTED -> {
                            // Permission granted, show main app
                            UltraMusicNavigation()
                        }
                        permissionState == PermissionState.DENIED -> {
                            // Permission denied, show request screen
                            PermissionRequestScreen(
                                onRequestPermission = { requestStoragePermission() }
                            )
                        }
                        else -> {
                            // Checking permission state
                            ScanningScreen()
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted - AUTO-SCAN immediately!
                viewModel.onPermissionGranted()
                viewModel.loadMusic()
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        requestPermissionLauncher.launch(permission)
    }
}

/**
 * Screen shown while scanning for music
 */
@Composable
fun ScanningScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Scanning Your Music Library",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Finding all songs on your device including SD card...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Screen to request storage/audio permission
 */
@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Access Your Music",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "UltraMusic needs permission to scan your device for music files. This includes internal storage and SD card.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We only access audio files. Your data stays on your device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UltraMusicNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = hiltViewModel()

    // Recompose when route changes so swipe-back enablement stays correct
    navController.currentBackStackEntryAsState()
    val canSwipeBack = navController.previousBackStackEntry != null
    
    SwipeBackContainer(
        enabled = canSwipeBack,
        onSwipeBack = { navController.popBackStack() }
    ) {
        NavHost(
            navController = navController,
            startDestination = "home"  // Home screen with browse tabs for easy navigation
        ) {
            // Main Easy Player Screen - the primary UI
            composable("easy_player") {
                EasyPlayerScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.navigate("home")
                    },
                    onNavigateToVoiceSearch = {
                        navController.navigate("voice_search")
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
                    },
                    onNavigateToEasyPlayer = {
                        navController.navigate("easy_player")
                    },
                    onNavigateToEnhancements = {
                        navController.navigate("enhancements")
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
}
