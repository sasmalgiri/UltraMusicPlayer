package com.ultramusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ultramusic.player.audio.BeatMarker
import com.ultramusic.player.data.AudioPreset
import com.ultramusic.player.data.Song
import com.ultramusic.player.ui.MainViewModel
import com.ultramusic.player.ui.components.BattleControlsPanel
import com.ultramusic.player.ui.components.CompactFolderPanel
import com.ultramusic.player.ui.components.SmartPlaylistPanel
import com.ultramusic.player.ui.components.UnifiedControlsPanel
import com.ultramusic.player.ui.components.WaveformVisualizer
import com.ultramusic.player.ui.theme.UltraGradientEnd
import com.ultramusic.player.ui.theme.UltraGradientStart

/**
 * NowPlayingScreen with NEW layout:
 * - TOP: Queue (left) + Folders (right) - BOTH visible side by side
 * - MIDDLE: Compact Now Playing (album art, song info, waveform, controls)
 * - BOTTOM: All Controls (Speed, Pitch, A-B Loop, Presets) - scrollable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val queue by viewModel.queue.collectAsState()

    // Smart Playlist state
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val playlistSearchState by viewModel.playlistSearchState.collectAsState()
    val isPlaylistSearchMode by viewModel.isPlaylistAddingMode.collectAsState()

    // Waveform and beat detection state
    val waveformData by viewModel.currentWaveform.collectAsState()
    val beatMarkers by viewModel.currentBeatMarkers.collectAsState()
    val estimatedBpm by viewModel.estimatedBpm.collectAsState()
    val isExtractingWaveform by viewModel.isExtractingWaveform.collectAsState()

    // Folder browsing state
    val currentFolderPath by viewModel.currentFolderPath.collectAsState()
    val browseItems by viewModel.browseItems.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()

    // Battle controls state - Core
    val battleEngineEnabled by viewModel.battleEngineEnabled.collectAsState()
    val battleMode by viewModel.battleMode.collectAsState()
    val dangerModeEnabled by viewModel.dangerModeEnabled.collectAsState()
    val ourSPL by viewModel.ourSPL.collectAsState()
    val opponentSPL by viewModel.opponentSPL.collectAsState()
    val currentPeakDb by viewModel.currentPeakDb.collectAsState()
    val isClipping by viewModel.isClipping.collectAsState()
    val profileSlotA by viewModel.profileSlotA.collectAsState()
    val profileSlotB by viewModel.profileSlotB.collectAsState()
    val profileSlotC by viewModel.profileSlotC.collectAsState()
    val activeProfileSlot by viewModel.activeProfileSlot.collectAsState()

    // Battle controls - Main sliders
    val battleBassLevel by viewModel.battleBassLevel.collectAsState()
    val bassFrequency by viewModel.bassFrequency.collectAsState()
    val battleLoudness by viewModel.battleLoudness.collectAsState()
    val battleClarity by viewModel.battleClarity.collectAsState()
    val battleSpatial by viewModel.battleSpatial.collectAsState()
    val battleEQBands by viewModel.battleEQBands.collectAsState()

    // Battle controls - Compressor
    val compressorEnabled by viewModel.compressorEnabled.collectAsState()
    val compressorThreshold by viewModel.compressorThreshold.collectAsState()
    val compressorRatio by viewModel.compressorRatio.collectAsState()
    val compressorAttack by viewModel.compressorAttack.collectAsState()
    val compressorRelease by viewModel.compressorRelease.collectAsState()
    val compressorMakeupGain by viewModel.compressorMakeupGain.collectAsState()

    // Battle controls - Limiter
    val limiterEnabled by viewModel.limiterEnabled.collectAsState()
    val limiterThreshold by viewModel.limiterThreshold.collectAsState()
    val limiterCeiling by viewModel.limiterCeiling.collectAsState()
    val limiterAttack by viewModel.limiterAttack.collectAsState()
    val limiterRelease by viewModel.limiterRelease.collectAsState()

    // Battle controls - Stereo, Exciter, Reverb
    val stereoWidthEnabled by viewModel.stereoWidthEnabled.collectAsState()
    val stereoWidth by viewModel.stereoWidth.collectAsState()
    val exciterEnabled by viewModel.exciterEnabled.collectAsState()
    val exciterDrive by viewModel.exciterDrive.collectAsState()
    val exciterMix by viewModel.exciterMix.collectAsState()
    val reverbEnabled by viewModel.reverbEnabled.collectAsState()
    val reverbPreset by viewModel.reverbPreset.collectAsState()

    // Waveform height state (adjustable 30-200 dp)
    var waveformHeight by remember { mutableIntStateOf(80) }

    val song = playbackState.currentSong

    if (song == null) {
        onNavigateBack()
        return
    }

    // Analyze song for waveform and beats when song changes
    LaunchedEffect(song.id) {
        viewModel.analyzeCurrentSong()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        UltraGradientStart.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Now Playing",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // ==================== TOP SECTION: SMART PLAYLIST + FOLDERS SIDE BY SIDE ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f) // 45% of screen for Playlist+Folders
                ) {
                    // Smart Playlist Panel (left side)
                    SmartPlaylistPanel(
                        playlist = activePlaylist,
                        searchState = playlistSearchState,
                        isSearchMode = isPlaylistSearchMode,
                        onPlaySong = { index -> viewModel.playFromPlaylistIndex(index) },
                        onRemoveSong = { index -> viewModel.removeFromPlaylist(index) },
                        onMoveSong = { from, to -> viewModel.moveInPlaylist(from, to) },
                        onSearchQueryChange = { viewModel.updatePlaylistSearchQuery(it) },
                        onAddFromSearch = { addedSong, playNext ->
                            viewModel.addToPlaylistFromSearch(addedSong, playNext)
                        },
                        onToggleSearchMode = { viewModel.togglePlaylistAddingMode() },
                        onToggleLoop = { viewModel.togglePlaylistLoop() },
                        onShuffleRemaining = { viewModel.shufflePlaylistRemaining() },
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                    )

                    // Vertical divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    // Folders Panel (right side)
                    CompactFolderPanel(
                        currentPath = currentFolderPath,
                        breadcrumbs = breadcrumbs,
                        browseItems = browseItems,
                        currentSongId = song.id,
                        onNavigateToPath = { path -> viewModel.navigateToPath(path) },
                        onNavigateUp = { viewModel.navigateUp() },
                        onPlaySong = { s -> viewModel.playSongFromFolder(s) },
                        onPlayNext = { s -> viewModel.addToPlayNext(s) },
                        onAddToEnd = { s -> viewModel.addToPlaylistEnd(s) },
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                    )
                }

                // Horizontal divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                // ==================== MIDDLE SECTION: COMPACT NOW PLAYING ====================
                CompactNowPlayingSection(
                    song = song,
                    isPlaying = playbackState.isPlaying,
                    isShuffling = playbackState.isShuffling,
                    isLooping = playbackState.isLooping,
                    speed = playbackState.speed,
                    pitch = playbackState.pitch,
                    progress = playbackState.progress,
                    duration = playbackState.duration,
                    positionFormatted = playbackState.positionFormatted,
                    durationFormatted = playbackState.durationFormatted,
                    waveformData = waveformData,
                    beatMarkers = beatMarkers,
                    estimatedBpm = estimatedBpm,
                    isExtractingWaveform = isExtractingWaveform,
                    abLoopStart = playbackState.abLoopStart,
                    abLoopEnd = playbackState.abLoopEnd,
                    waveformHeight = waveformHeight,
                    onWaveformHeightChange = { newHeight -> waveformHeight = newHeight },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onPrevious = { viewModel.playPrevious() },
                    onNext = { viewModel.playNext() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleLoop = { viewModel.toggleLoop() },
                    onSeekToPercent = { viewModel.seekToPercent(it) },
                    onLoopStartChange = { viewModel.setWaveformLoopStart(it) },
                    onLoopEndChange = { viewModel.setWaveformLoopEnd(it) },
                    onClearLoop = { viewModel.clearWaveformLoop() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.25f) // 25% of screen for Now Playing
                )

                // ==================== BOTTOM SECTION: SCROLLABLE CONTROLS + BATTLE ====================
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .weight(0.30f) // 30% of screen for Controls
                        .verticalScroll(scrollState)
                ) {
                    // Unified Controls Panel (Speed, Pitch, A-B Loop, Presets)
                    UnifiedControlsPanel(
                        // Speed & Pitch
                        speed = playbackState.speed,
                        pitch = playbackState.pitch,
                        onSpeedChange = { viewModel.setSpeed(it) },
                        onPitchChange = { viewModel.setPitch(it) },
                        onResetSpeed = { viewModel.resetSpeed() },
                        onResetPitch = { viewModel.resetPitch() },
                        onResetAll = { viewModel.resetAll() },
                        // A-B Loop
                        abLoopStart = playbackState.abLoopStart,
                        abLoopEnd = playbackState.abLoopEnd,
                        currentPosition = playbackState.currentPosition,
                        duration = playbackState.duration,
                        onSetLoopStart = { viewModel.setABLoopStart() },
                        onSetLoopEnd = { viewModel.setABLoopEnd() },
                        onClearLoop = { viewModel.clearABLoop() },
                        onSaveToArmory = {
                            playbackState.currentSong?.let { currentSong ->
                                val start = playbackState.abLoopStart ?: 0
                                val end = playbackState.abLoopEnd ?: currentSong.duration
                                viewModel.saveClipToArmory(currentSong, start, end)
                            }
                        },
                        // Presets
                        presets = AudioPreset.PRESETS,
                        selectedPreset = uiState.selectedPreset,
                        onPresetSelected = { viewModel.applyPreset(it) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Divider before Battle Controls
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Battle Controls Panel - FULL controls like AudioBattleScreen
                    BattleControlsPanel(
                        // Core state
                        isEnabled = battleEngineEnabled,
                        battleMode = battleMode,
                        dangerModeEnabled = dangerModeEnabled,
                        // SPL meters
                        ourSPL = ourSPL,
                        opponentSPL = opponentSPL,
                        currentPeakDb = currentPeakDb,
                        isClipping = isClipping,
                        // Quick profiles
                        profileSlotA = profileSlotA,
                        profileSlotB = profileSlotB,
                        profileSlotC = profileSlotC,
                        activeProfileSlot = activeProfileSlot,
                        // Main controls
                        bassLevel = battleBassLevel,
                        bassFrequency = bassFrequency,
                        loudnessGain = battleLoudness,
                        clarityLevel = battleClarity,
                        spatialLevel = battleSpatial,
                        eqBands = battleEQBands,
                        // Compressor
                        compressorEnabled = compressorEnabled,
                        compressorThreshold = compressorThreshold,
                        compressorRatio = compressorRatio,
                        compressorAttack = compressorAttack,
                        compressorRelease = compressorRelease,
                        compressorMakeupGain = compressorMakeupGain,
                        // Limiter
                        limiterEnabled = limiterEnabled,
                        limiterThreshold = limiterThreshold,
                        limiterCeiling = limiterCeiling,
                        limiterAttack = limiterAttack,
                        limiterRelease = limiterRelease,
                        // Stereo
                        stereoWidthEnabled = stereoWidthEnabled,
                        stereoWidth = stereoWidth,
                        // Exciter
                        exciterEnabled = exciterEnabled,
                        exciterDrive = exciterDrive,
                        exciterMix = exciterMix,
                        // Reverb
                        reverbEnabled = reverbEnabled,
                        reverbPreset = reverbPreset,
                        // Core callbacks
                        onToggleBattleEngine = { viewModel.toggleBattleEngine() },
                        onBattleModeChange = { viewModel.setBattleMode(it) },
                        onDangerModeChange = { viewModel.setDangerModeEnabled(it) },
                        onEmergencyBass = { viewModel.emergencyBassBoost() },
                        onCutThrough = { viewModel.cutThrough() },
                        onGoNuclear = { viewModel.goNuclear() },
                        onSaveProfileSlot = { viewModel.saveToProfileSlot(it) },
                        onLoadProfileSlot = { viewModel.loadFromProfileSlot(it) },
                        onClearProfileSlot = { viewModel.clearProfileSlot(it) },
                        onResetAll = { viewModel.resetAllAudioEffects() },
                        // Main control callbacks
                        onBassChange = { viewModel.setBattleBass(it) },
                        onBassFrequencyChange = { viewModel.setBassFrequency(it) },
                        onLoudnessChange = { viewModel.setBattleLoudness(it) },
                        onClarityChange = { viewModel.setBattleClarity(it) },
                        onSpatialChange = { viewModel.setBattleSpatial(it) },
                        onEQBandChange = { bandIndex, level -> viewModel.setBattleEQBand(bandIndex, level) },
                        // Compressor callbacks
                        onCompressorEnabledChange = { viewModel.setCompressorEnabled(it) },
                        onCompressorThresholdChange = { viewModel.setCompressorThreshold(it) },
                        onCompressorRatioChange = { viewModel.setCompressorRatio(it) },
                        onCompressorAttackChange = { viewModel.setCompressorAttack(it) },
                        onCompressorReleaseChange = { viewModel.setCompressorRelease(it) },
                        onCompressorMakeupGainChange = { viewModel.setCompressorMakeupGain(it) },
                        // Limiter callbacks
                        onLimiterEnabledChange = { viewModel.setLimiterEnabled(it) },
                        onLimiterThresholdChange = { viewModel.setLimiterThreshold(it) },
                        onLimiterCeilingChange = { viewModel.setLimiterCeiling(it) },
                        onLimiterAttackChange = { viewModel.setLimiterAttack(it) },
                        onLimiterReleaseChange = { viewModel.setLimiterRelease(it) },
                        // Stereo callbacks
                        onStereoEnabledChange = { viewModel.setStereoWidthEnabled(it) },
                        onStereoWidthChange = { viewModel.setStereoWidth(it) },
                        // Exciter callbacks
                        onExciterEnabledChange = { viewModel.setExciterEnabled(it) },
                        onExciterDriveChange = { viewModel.setExciterDrive(it) },
                        onExciterMixChange = { viewModel.setExciterMix(it) },
                        // Reverb callbacks
                        onReverbEnabledChange = { viewModel.setReverbEnabled(it) },
                        onReverbPresetChange = { viewModel.setReverbPreset(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Compact Now Playing section with horizontal layout
 */
@Composable
private fun CompactNowPlayingSection(
    song: Song,
    isPlaying: Boolean,
    isShuffling: Boolean,
    isLooping: Boolean,
    speed: Float,
    pitch: Float,
    progress: Float,
    duration: Long,
    positionFormatted: String,
    durationFormatted: String,
    waveformData: List<Float>,
    beatMarkers: List<BeatMarker>,
    estimatedBpm: Float?,
    isExtractingWaveform: Boolean,
    abLoopStart: Long?,
    abLoopEnd: Long?,
    waveformHeight: Int,
    onWaveformHeightChange: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleLoop: () -> Unit,
    onSeekToPercent: (Float) -> Unit,
    onLoopStartChange: (Long) -> Unit,
    onLoopEndChange: (Long) -> Unit,
    onClearLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top row: Album art + Song info + Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art (small)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Album art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${song.artist} - ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Speed/Pitch indicator
                if (speed != 1.0f || pitch != 0f) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        if (speed != 1.0f) {
                            Text(
                                text = "${String.format("%.2f", speed)}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (speed != 1.0f && pitch != 0f) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (pitch != 0f) {
                            val sign = if (pitch > 0) "+" else ""
                            Text(
                                text = "${sign}${String.format("%.1f", pitch)}st",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Playback controls (compact)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(16.dp),
                        tint = if (isShuffling)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play/Pause
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying)
                            Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Repeat
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isLooping)
                            Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        modifier = Modifier.size(16.dp),
                        tint = if (isLooping)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Waveform height control + waveform
        if (waveformData.isNotEmpty()) {
            // Height adjustment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wave",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { onWaveformHeightChange((waveformHeight - 20).coerceAtLeast(30)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "${waveformHeight}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(
                    onClick = { onWaveformHeightChange((waveformHeight + 20).coerceAtMost(200)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            WaveformVisualizer(
                waveformData = waveformData,
                currentPosition = progress,
                durationMs = duration,
                loopStartMs = abLoopStart,
                loopEndMs = abLoopEnd,
                isLooping = isLooping,
                onSeek = onSeekToPercent,
                onLoopStartChange = onLoopStartChange,
                onLoopEndChange = onLoopEndChange,
                onClearLoop = onClearLoop,
                beatMarkers = beatMarkers,
                estimatedBpm = estimatedBpm ?: 0f,
                showBeatMarkers = true,
                modifier = Modifier.fillMaxWidth(),
                height = waveformHeight.dp
            )
        } else {
            // Progress info while loading
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isExtractingWaveform) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Analyzing...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$positionFormatted / $durationFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
