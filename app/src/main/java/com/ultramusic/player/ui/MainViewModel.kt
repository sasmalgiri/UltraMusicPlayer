package com.ultramusic.player.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultramusic.player.ai.CounterEngineState
import com.ultramusic.player.ai.CounterRecommendation
import com.ultramusic.player.ai.CounterSongEngine
import com.ultramusic.player.ai.CounterStrategy
import com.ultramusic.player.audio.AudioBattleEngine
import com.ultramusic.player.audio.ActiveBattleSystem
import com.ultramusic.player.audio.ActiveBattleState
import com.ultramusic.player.audio.ActiveBattleMode
import com.ultramusic.player.audio.AttackOpportunity
import com.ultramusic.player.audio.BattleLogEntry
import com.ultramusic.player.audio.BattleScript
import com.ultramusic.player.audio.BattleAdvice
import com.ultramusic.player.audio.BattleIntelligence
import com.ultramusic.player.audio.BattleMode
import com.ultramusic.player.audio.BattlePreset
import com.ultramusic.player.audio.CrowdAnalyzer
import com.ultramusic.player.audio.CrowdTrend
import com.ultramusic.player.audio.CrowdMood
import com.ultramusic.player.audio.DropRecommendation
import com.ultramusic.player.audio.FrequencyWarfare
import com.ultramusic.player.audio.WarfareTactic
import com.ultramusic.player.audio.FrequencyAnalysis
import com.ultramusic.player.audio.EQBand
import com.ultramusic.player.audio.EQSuggestion
import com.ultramusic.player.audio.OpponentAnalysis
import com.ultramusic.player.audio.SongBattleAnalyzer
import com.ultramusic.player.audio.SongBattleRating
import com.ultramusic.player.audio.VenueProfile
import com.ultramusic.player.audio.VenueProfiler
import com.ultramusic.player.audio.AudioQualityManager
import com.ultramusic.player.audio.ExtremeNoiseVoiceCapture
import com.ultramusic.player.audio.ExtremeCaptureState
import com.ultramusic.player.audio.MusicController
import com.ultramusic.player.audio.NoiseLevel
import com.ultramusic.player.audio.VoiceSearchManager
import com.ultramusic.player.audio.VoiceSearchState
import com.ultramusic.player.data.ActivePlaylist
import com.ultramusic.player.data.AddResult
import com.ultramusic.player.data.AudioPreset
import com.ultramusic.player.data.BrowseItem
import com.ultramusic.player.data.FolderRepository
import com.ultramusic.player.data.MusicRepository
import com.ultramusic.player.data.PlaybackState
import com.ultramusic.player.data.PlaylistSearchState
import com.ultramusic.player.data.SmartPlaylistManager
import com.ultramusic.player.data.SmartSearchEngine
import com.ultramusic.player.data.Song
import com.ultramusic.player.data.SongMetadataManager
import com.ultramusic.player.data.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the main screen
 */
data class MainUiState(
    val isLoading: Boolean = true,
    val songs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.TITLE,
    val selectedPreset: AudioPreset? = null,
    val showSpeedPitchPanel: Boolean = false,
    val showPresetPanel: Boolean = false,
    val showABLoopPanel: Boolean = false,
    val errorMessage: String? = null,
    val qualityWarning: String? = null,
    val qualityPercent: Int = 100,
    val formantPreservation: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val folderRepository: FolderRepository,
    private val musicController: MusicController,
    private val voiceSearchManager: VoiceSearchManager,
    private val extremeVoiceCapture: ExtremeNoiseVoiceCapture,
    private val audioQualityManager: AudioQualityManager,
    private val smartSearchEngine: SmartSearchEngine,
    private val songMetadataManager: SongMetadataManager,
    private val smartPlaylistManager: SmartPlaylistManager,
    private val counterSongEngine: CounterSongEngine,
    private val audioBattleEngine: AudioBattleEngine,
    private val battleIntelligence: BattleIntelligence,
    private val songBattleAnalyzer: SongBattleAnalyzer,
    private val venueProfiler: VenueProfiler,
    private val activeBattleSystem: ActiveBattleSystem,
    private val crowdAnalyzer: CrowdAnalyzer,
    private val frequencyWarfare: FrequencyWarfare,
    val localBattleAnalyzer: com.ultramusic.player.core.LocalBattleAnalyzer,
    val battleArmory: com.ultramusic.player.core.BattleArmory,
    val autoClipDetector: com.ultramusic.player.core.AutoClipDetector,
    val grokAIService: com.ultramusic.player.ai.GrokAIService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    val playbackState: StateFlow<PlaybackState> = musicController.playbackState
    val queue: StateFlow<List<Song>> = musicController.queue
    
    // ==================== AUTO CLIP DETECTION ====================
    private val _detectedClips = MutableStateFlow<List<com.ultramusic.player.core.DetectedClip>>(emptyList())
    val detectedClips: StateFlow<List<com.ultramusic.player.core.DetectedClip>> = _detectedClips.asStateFlow()
    
    private val _isDetectingClips = MutableStateFlow(false)
    val isDetectingClips: StateFlow<Boolean> = _isDetectingClips.asStateFlow()
    
    // ==================== FOLDER BROWSING ====================
    
    private val _currentFolderPath = MutableStateFlow("")
    val currentFolderPath: StateFlow<String> = _currentFolderPath.asStateFlow()
    
    private val _browseItems = MutableStateFlow<List<BrowseItem>>(emptyList())
    val browseItems: StateFlow<List<BrowseItem>> = _browseItems.asStateFlow()
    
    private val _breadcrumbs = MutableStateFlow<List<Pair<String, String>>>(listOf("Home" to ""))
    val breadcrumbs: StateFlow<List<Pair<String, String>>> = _breadcrumbs.asStateFlow()
    
    // ==================== VOICE SEARCH ====================
    
    val voiceSearchState: StateFlow<VoiceSearchState> = voiceSearchManager.state
    val noiseLevel: StateFlow<Float> = voiceSearchManager.noiseLevel
    
    // Extreme noise voice capture states
    val extremeVoiceState: StateFlow<ExtremeCaptureState> = extremeVoiceCapture.state
    val currentNoiseLevel: StateFlow<NoiseLevel> = extremeVoiceCapture.noiseLevel
    val noiseLevelDb: StateFlow<Float> = extremeVoiceCapture.noiseLevelDb
    val canSpeak: StateFlow<Boolean> = extremeVoiceCapture.canSpeak
    
    private val _voiceSearchResults = MutableStateFlow<List<Song>>(emptyList())
    val voiceSearchResults: StateFlow<List<Song>> = _voiceSearchResults.asStateFlow()
    
    private val _voiceCapabilities = MutableStateFlow("Checking capabilities...")
    val voiceCapabilities: StateFlow<String> = _voiceCapabilities.asStateFlow()
    
    // ==================== SMART SEARCH ====================
    
    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()
    
    // ==================== SMART PLAYLIST ====================
    
    val activePlaylist: StateFlow<ActivePlaylist> = smartPlaylistManager.activePlaylist
    val playlistSearchState: StateFlow<PlaylistSearchState> = smartPlaylistManager.searchState
    val isPlaylistAddingMode: StateFlow<Boolean> = smartPlaylistManager.isAddingMode
    
    private val _lastAddResult = MutableStateFlow<AddResult?>(null)
    val lastAddResult: StateFlow<AddResult?> = _lastAddResult.asStateFlow()
    
    // ==================== COUNTER SONG ENGINE ====================
    
    val counterEngineState: StateFlow<CounterEngineState> = counterSongEngine.state
    val counterRecommendations: StateFlow<List<CounterRecommendation>> = counterSongEngine.recommendations
    
    // ==================== AUDIO BATTLE ENGINE ====================
    
    val battleEngineEnabled: StateFlow<Boolean> = audioBattleEngine.isEnabled
    val battleMode: StateFlow<BattleMode> = audioBattleEngine.battleMode
    val battleBassLevel: StateFlow<Int> = audioBattleEngine.bassLevel
    val battleLoudness: StateFlow<Int> = audioBattleEngine.loudnessGain
    val battleClarity: StateFlow<Int> = audioBattleEngine.clarityLevel
    val battleSpatial: StateFlow<Int> = audioBattleEngine.spatialLevel
    val battleEQBands: StateFlow<List<EQBand>> = audioBattleEngine.eqBands
    val battlePreset: StateFlow<BattlePreset?> = audioBattleEngine.currentPreset
    
    // ==================== BATTLE INTELLIGENCE ====================
    
    val battleIntelListening: StateFlow<Boolean> = battleIntelligence.isListening
    val opponentAnalysis: StateFlow<OpponentAnalysis> = battleIntelligence.opponentAnalysis
    val venueSPL: StateFlow<Float> = battleIntelligence.venueSPL
    val frequencySpectrum: StateFlow<List<Float>> = battleIntelligence.frequencySpectrum
    val counterEQSuggestions: StateFlow<List<EQSuggestion>> = battleIntelligence.counterEQSuggestion
    val battleAdvice: StateFlow<List<BattleAdvice>> = battleIntelligence.battleAdvice
    
    // ==================== SONG BATTLE ANALYZER ====================
    
    val analyzedSongRatings: StateFlow<Map<Long, SongBattleRating>> = songBattleAnalyzer.analyzedSongs
    val isAnalyzingSongs: StateFlow<Boolean> = songBattleAnalyzer.isAnalyzing
    
    // ==================== VENUE PROFILER ====================
    
    val isProfilingVenue: StateFlow<Boolean> = venueProfiler.isProfiling
    val currentVenueProfile: StateFlow<VenueProfile?> = venueProfiler.currentVenue
    
    // ==================== ACTIVE BATTLE SYSTEM ====================
    
    val activeBattleState: StateFlow<ActiveBattleState> = activeBattleSystem.battleState
    val activeBattleMode: StateFlow<ActiveBattleMode> = activeBattleSystem.battleMode
    val battleMomentum: StateFlow<Int> = activeBattleSystem.momentum
    val opponentSPL: StateFlow<Float> = activeBattleSystem.opponentSPL
    val ourSPL: StateFlow<Float> = activeBattleSystem.ourSPL
    val attackOpportunity: StateFlow<AttackOpportunity?> = activeBattleSystem.attackOpportunity
    val battleLog: StateFlow<List<BattleLogEntry>> = activeBattleSystem.battleLog
    val nextSongSuggestion: StateFlow<Song?> = activeBattleSystem.nextSongSuggestion
    val autoCounterEnabled: StateFlow<Boolean> = activeBattleSystem.autoCounterEnabled
    val autoVolumeEnabled: StateFlow<Boolean> = activeBattleSystem.autoVolumeEnabled
    val autoQueueEnabled: StateFlow<Boolean> = activeBattleSystem.autoQueueEnabled
    
    // ==================== CROWD ANALYZER ====================
    
    val isCrowdAnalyzing: StateFlow<Boolean> = crowdAnalyzer.isAnalyzing
    val crowdEnergy: StateFlow<Int> = crowdAnalyzer.crowdEnergy
    val crowdTrend: StateFlow<CrowdTrend> = crowdAnalyzer.crowdTrend
    val crowdMood: StateFlow<CrowdMood> = crowdAnalyzer.crowdMood
    val dropRecommendation: StateFlow<DropRecommendation?> = crowdAnalyzer.dropRecommendation
    
    // ==================== FREQUENCY WARFARE ====================
    
    val activeTactic: StateFlow<WarfareTactic?> = frequencyWarfare.activeTactic
    val isWarfareActive: StateFlow<Boolean> = frequencyWarfare.isActive
    
    init {
        loadMusic()
        observeVoiceSearch()
        observeExtremeVoiceCapture()
        checkVoiceCapabilities()
        observeQuality()
        observePlaylistChanges()
    }
    
    // ==================== MUSIC LOADING ====================
    
    fun loadMusic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // Load with folder structure
                folderRepository.scanMusicWithFolders().collectLatest { (songs, _) ->
                    val sorted = musicRepository.sortSongs(songs, _uiState.value.sortOption)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        songs = sorted,
                        filteredSongs = applySearch(sorted, _uiState.value.searchQuery)
                    )
                    
                    // Load root folders for browser
                    updateFolderContents("")
                    
                    // Initialize playlist manager with all songs
                    smartPlaylistManager.initialize(songs)
                    
                    // Initialize counter song engine with library
                    initializeCounterEngine(songs)
                    
                    // Index songs for battle (INSTANT counter picks!)
                    indexSongsForBattle(songs)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load music: ${e.message}"
                )
            }
        }
    }
    
    private fun observePlaylistChanges() {
        viewModelScope.launch {
            // When playlist moves to next song, update the music controller
            smartPlaylistManager.activePlaylist.collectLatest { playlist ->
                playlist.currentSong?.let { song ->
                    // Sync with music controller if needed
                    if (playbackState.value.currentSong?.id != song.id) {
                        // Play the current song from playlist
                        musicController.playSong(song, playlist.queue)
                    }
                }
            }
        }
    }
    
    // ==================== BATTLE SONG DATABASE ====================
    
    /**
     * Index songs for instant battle counter picks
     * This runs ONCE when library loads, then results are persisted
     */
    private fun indexSongsForBattle(songs: List<Song>) {
        viewModelScope.launch {
            localBattleAnalyzer.indexLibrary(songs)
        }
    }
    
    // ==================== COUNTER SONG ENGINE ====================
    
    private fun initializeCounterEngine(songs: List<Song>) {
        viewModelScope.launch {
            counterSongEngine.indexLibrary(songs)
        }
    }
    
    /**
     * Find counter song by opponent's song name
     */
    fun findCounterSong(
        opponentSong: String,
        opponentArtist: String?,
        strategy: CounterStrategy = CounterStrategy.AUTO
    ) {
        counterSongEngine.findCounterByName(opponentSong, opponentArtist, strategy)
    }
    
    /**
     * Start listening to opponent's song to identify and counter
     */
    fun startListeningToOpponent() {
        counterSongEngine.findCounterByListening(CounterStrategy.AUTO)
    }
    
    /**
     * Stop listening mode
     */
    fun stopListeningToOpponent() {
        counterSongEngine.stopRealtimeMode()
    }
    
    /**
     * Start real-time counter mode
     */
    fun startRealtimeCounterMode(onUpdate: (List<CounterRecommendation>) -> Unit) {
        counterSongEngine.startRealtimeCounterMode(CounterStrategy.AUTO, onUpdate)
    }
    
    // ==================== AUDIO BATTLE ENGINE ====================
    
    /**
     * Initialize battle engine with current audio session
     */
    fun initializeBattleEngine() {
        val sessionId = musicController.getAudioSessionId()
        if (sessionId != 0) {
            audioBattleEngine.initialize(sessionId)
        }
    }
    
    /**
     * Toggle battle engine on/off
     */
    fun toggleBattleEngine() {
        if (battleEngineEnabled.value) {
            audioBattleEngine.setBattleMode(BattleMode.OFF)
        } else {
            initializeBattleEngine()
            audioBattleEngine.enable()
        }
    }
    
    /**
     * Set battle mode preset
     */
    fun setBattleMode(mode: BattleMode) {
        audioBattleEngine.setBattleMode(mode)
    }
    
    /**
     * Set bass boost level (0-1000)
     */
    fun setBattleBass(level: Int) {
        audioBattleEngine.setBassBoost(level)
    }
    
    /**
     * Set loudness gain (0-1000 mB)
     */
    fun setBattleLoudness(gain: Int) {
        audioBattleEngine.setLoudness(gain)
    }
    
    /**
     * Set clarity level (0-100)
     */
    fun setBattleClarity(level: Int) {
        audioBattleEngine.setClarity(level)
    }
    
    /**
     * Set spatial/virtualizer level (0-1000)
     */
    fun setBattleSpatial(level: Int) {
        audioBattleEngine.setVirtualizer(level)
    }
    
    /**
     * Set individual EQ band
     */
    fun setBattleEQBand(bandIndex: Int, level: Int) {
        audioBattleEngine.setEQBand(bandIndex, level)
    }
    
    /**
     * Apply battle preset
     */
    fun applyBattlePreset(preset: BattlePreset) {
        audioBattleEngine.applyPreset(preset)
    }
    
    // Quick action buttons for battle
    
    /**
     * Emergency bass boost - instant maximum bass
     */
    fun emergencyBassBoost() {
        audioBattleEngine.emergencyBassBoost()
    }
    
    /**
     * Cut through opponent's sound
     */
    fun cutThrough() {
        audioBattleEngine.cutThrough()
    }
    
    /**
     * Maximum everything - nuclear option
     */
    fun goNuclear() {
        audioBattleEngine.goNuclear()
    }
    
    // ==================== BATTLE INTELLIGENCE ====================
    
    /**
     * Toggle battle intelligence listening
     */
    fun toggleBattleIntel() {
        if (battleIntelListening.value) {
            battleIntelligence.stopListening()
        } else {
            battleIntelligence.startListening()
        }
    }
    
    /**
     * Apply counter EQ suggestion
     */
    fun applyCounterEQ(suggestion: EQSuggestion) {
        audioBattleEngine.setEQBand(suggestion.band, suggestion.suggestedBoost * 100)
    }
    
    /**
     * Enable auto-counter mode
     */
    fun enableAutoCounter() {
        battleIntelligence.enableAutoCounter(audioBattleEngine)
    }
    
    // ==================== SONG BATTLE ANALYZER ====================
    
    /**
     * Analyze entire library for battle ratings
     */
    fun analyzeLibraryForBattle() {
        viewModelScope.launch {
            songBattleAnalyzer.analyzeLibrary(_uiState.value.songs)
        }
    }
    
    /**
     * Get battle rating for a song
     */
    fun getSongBattleRating(songId: Long): SongBattleRating? {
        return analyzedSongRatings.value[songId]
    }
    
    /**
     * Play song by ID
     */
    fun playSongById(songId: Long) {
        val song = _uiState.value.songs.find { it.id == songId }
        song?.let { playSong(it) }
    }
    
    // ==================== VENUE PROFILER ====================
    
    /**
     * Quick venue profile using ambient sound
     */
    fun quickProfileVenue() {
        viewModelScope.launch {
            venueProfiler.quickProfile()
        }
    }
    
    /**
     * Full venue profile with test tones
     */
    fun fullProfileVenue() {
        viewModelScope.launch {
            venueProfiler.fullProfile { freq, duration ->
                // Play test tone - would need audio generation
            }
        }
    }
    
    /**
     * Apply current venue profile to battle engine
     */
    fun applyVenueProfile() {
        venueProfiler.applyToEngine(audioBattleEngine)
    }
    
    // ==================== ACTIVE BATTLE SYSTEM ====================
    
    /**
     * Initialize active battle system with all dependencies
     */
    fun initializeActiveBattle() {
        activeBattleSystem.initialize(
            engine = audioBattleEngine,
            controller = musicController,
            songs = _uiState.value.songs,
            ratings = analyzedSongRatings.value
        )
        frequencyWarfare.initialize(audioBattleEngine)
    }
    
    /**
     * Start active battle AI
     */
    fun startActiveBattle(mode: ActiveBattleMode = ActiveBattleMode.BALANCED) {
        initializeActiveBattle()
        activeBattleSystem.startBattle(mode)
        crowdAnalyzer.startAnalyzing()
    }
    
    /**
     * Pause active battle
     */
    fun pauseActiveBattle() {
        activeBattleSystem.pauseBattle()
    }
    
    /**
     * Resume active battle
     */
    fun resumeActiveBattle() {
        activeBattleSystem.resumeBattle()
    }
    
    /**
     * End active battle
     */
    fun endActiveBattle() {
        activeBattleSystem.endBattle()
        crowdAnalyzer.stopAnalyzing()
        frequencyWarfare.stopWarfare()
    }
    
    /**
     * Set active battle mode
     */
    fun setActiveBattleMode(mode: ActiveBattleMode) {
        activeBattleSystem.setBattleMode(mode)
    }
    
    /**
     * Toggle auto-counter EQ
     */
    fun toggleAutoCounter(enabled: Boolean) {
        activeBattleSystem.toggleAutoCounter(enabled)
    }
    
    /**
     * Toggle auto-volume matching
     */
    fun toggleAutoVolume(enabled: Boolean) {
        activeBattleSystem.toggleAutoVolume(enabled)
    }
    
    /**
     * Toggle auto-queue AI
     */
    fun toggleAutoQueue(enabled: Boolean) {
        activeBattleSystem.toggleAutoQueue(enabled)
    }
    
    /**
     * Execute battle script
     */
    fun executeBattleScript(script: BattleScript) {
        activeBattleSystem.executeBattleScript(script)
    }
    
    /**
     * Play AI-suggested next song
     */
    fun playNextSuggestion() {
        nextSongSuggestion.value?.let { song ->
            playSong(song)
        }
    }
    
    // ==================== CROWD ANALYZER ====================
    
    /**
     * Start crowd analysis
     */
    fun startCrowdAnalysis() {
        crowdAnalyzer.startAnalyzing()
    }
    
    /**
     * Stop crowd analysis
     */
    fun stopCrowdAnalysis() {
        crowdAnalyzer.stopAnalyzing()
    }
    
    // ==================== FREQUENCY WARFARE ====================
    
    /**
     * Execute frequency warfare tactic
     */
    fun executeTactic(tactic: WarfareTactic) {
        val analysis = FrequencyAnalysis() // Get current analysis
        when (tactic) {
            WarfareTactic.MASKING -> frequencyWarfare.executeMasking(analysis)
            WarfareTactic.AVOIDANCE -> frequencyWarfare.executeAvoidance(analysis)
            WarfareTactic.FLANKING -> frequencyWarfare.executeFlanking(analysis)
            WarfareTactic.SATURATION -> frequencyWarfare.executeSaturation()
            WarfareTactic.SURGICAL_STRIKE -> frequencyWarfare.executeSurgicalStrike(analysis.dominantBand)
            WarfareTactic.FREQUENCY_LOCK -> frequencyWarfare.executeFrequencyLock(analysis)
            WarfareTactic.ADAPTIVE -> frequencyWarfare.executeMasking(analysis) // Adaptive uses masking as base
        }
    }
    
    /**
     * Stop all warfare tactics
     */
    fun stopWarfare() {
        frequencyWarfare.stopWarfare()
    }
    
    // ==================== FOLDER BROWSING ====================
    
    fun openFolder(path: String) {
        _currentFolderPath.value = path
        updateFolderContents(path)
        _breadcrumbs.value = folderRepository.getBreadcrumbs(path)
    }
    
    fun navigateUp() {
        val currentPath = _currentFolderPath.value
        if (currentPath.isNotEmpty()) {
            val parentPath = currentPath.substringBeforeLast("/", "")
            openFolder(parentPath)
        }
    }
    
    fun navigateToPath(path: String) {
        openFolder(path)
    }
    
    private fun updateFolderContents(path: String) {
        _browseItems.value = if (path.isEmpty()) {
            // Root level - show top folders
            folderRepository.getRootFolders().map { BrowseItem.Folder(it) }
        } else {
            folderRepository.getFolderContents(path)
        }
    }
    
    fun playSongFromFolder(song: Song) {
        // Get all songs in current folder and subfolders for queue
        val songsInFolder = folderRepository.getAllSongsInFolder(_currentFolderPath.value)
        musicController.playSong(song, songsInFolder.ifEmpty { listOf(song) })
    }
    
    fun playAllInFolder() {
        val songsInFolder = folderRepository.getAllSongsInFolder(_currentFolderPath.value)
        if (songsInFolder.isNotEmpty()) {
            musicController.playSong(songsInFolder.first(), songsInFolder)
        }
    }
    
    // ==================== VOICE SEARCH ====================
    
    private fun observeVoiceSearch() {
        viewModelScope.launch {
            voiceSearchManager.state.collectLatest { state ->
                when (state) {
                    is VoiceSearchState.Result -> {
                        // Search songs based on recognized text using smart search
                        val results = smartSearchEngine.searchSongs(
                            folderRepository.getAllSongs(), 
                            state.text
                        ).map { it.song }
                        _voiceSearchResults.value = results
                    }
                    else -> { }
                }
            }
        }
    }
    
    private fun observeExtremeVoiceCapture() {
        viewModelScope.launch {
            extremeVoiceCapture.state.collectLatest { state ->
                when (state) {
                    is ExtremeCaptureState.Result -> {
                        // Search with smart search including Bengali support
                        val results = smartSearchEngine.searchSongs(
                            folderRepository.getAllSongs(),
                            state.text
                        ).map { it.song }
                        _voiceSearchResults.value = results
                    }
                    else -> { }
                }
            }
        }
    }
    
    private fun observeQuality() {
        viewModelScope.launch {
            audioQualityManager.qualityState.collectLatest { qualityState ->
                _uiState.value = _uiState.value.copy(
                    qualityWarning = qualityState.qualityWarning,
                    qualityPercent = qualityState.estimatedQualityPercent,
                    formantPreservation = qualityState.formantPreservation
                )
            }
        }
    }
    
    private fun checkVoiceCapabilities() {
        _voiceCapabilities.value = voiceSearchManager.getCapabilitiesInfo()
    }
    
    fun startVoiceSearch() {
        _voiceSearchResults.value = emptyList()
        voiceSearchManager.startListening()
    }
    
    fun stopVoiceSearch() {
        voiceSearchManager.stopListening()
    }
    
    fun cancelVoiceSearch() {
        voiceSearchManager.cancel()
        _voiceSearchResults.value = emptyList()
    }
    
    fun resetVoiceSearch() {
        voiceSearchManager.resetState()
        _voiceSearchResults.value = emptyList()
    }
    
    // ==================== EXTREME NOISE VOICE CAPTURE ====================
    
    /**
     * Start extreme noise voice capture with Bengali + English support
     * This is for VERY LOUD environments like music competitions
     */
    fun startExtremeVoiceCapture() {
        _voiceSearchResults.value = emptyList()
        // Support Bengali, English, and Hindi
        extremeVoiceCapture.startCapture(listOf("bn-IN", "en-IN", "hi-IN"))
    }
    
    fun cancelExtremeVoiceCapture() {
        extremeVoiceCapture.cancel()
        _voiceSearchResults.value = emptyList()
    }
    
    fun resetExtremeVoiceCapture() {
        extremeVoiceCapture.reset()
        _voiceSearchResults.value = emptyList()
    }
    
    fun getVoiceTips(): List<String> {
        return extremeVoiceCapture.getTipsForNoiseLevel()
    }
    
    // ==================== AUDIO QUALITY ====================
    
    fun setQualityMode(mode: com.ultramusic.player.audio.QualityMode) {
        audioQualityManager.setQualityMode(mode)
    }
    
    fun setFormantPreservation(enabled: Boolean) {
        audioQualityManager.setFormantPreservation(enabled)
    }
    
    fun getQualityRecommendations(): List<String> {
        val state = playbackState.value
        return audioQualityManager.getRecommendedSettings(state.speed, state.pitch)
    }
    
    // ==================== SONG METADATA ====================
    
    fun normalizeSongTitles() {
        viewModelScope.launch {
            songMetadataManager.normalizeAll(_uiState.value.songs)
        }
    }
    
    // ==================== SMART PLAYLIST ====================
    
    /**
     * Start adding mode - shows the search overlay
     */
    fun startPlaylistAddingMode() {
        smartPlaylistManager.startAddingMode()
    }
    
    /**
     * End adding mode - closes the search overlay
     */
    fun endPlaylistAddingMode() {
        smartPlaylistManager.endAddingMode()
    }
    
    /**
     * Update search query with real-time narrowing results
     */
    fun updatePlaylistSearchQuery(query: String) {
        smartPlaylistManager.updateSearchQuery(query)
    }
    
    /**
     * Add song from search result to playlist
     * @param song The song to add
     * @param playNext If true, adds to play next; if false, adds to end
     */
    fun addToPlaylistFromSearch(song: Song, playNext: Boolean = false) {
        smartPlaylistManager.addFromSearch(song, playNext)
    }
    
    /**
     * Add song from voice input
     * Uses fuzzy matching to find best match
     */
    fun addToPlaylistFromVoice(recognizedText: String) {
        val result = smartPlaylistManager.addFromVoice(recognizedText)
        _lastAddResult.value = result
    }
    
    /**
     * Quick add by typing partial name - adds best match instantly
     */
    fun quickAddToPlaylist(partialName: String) {
        val result = smartPlaylistManager.quickAdd(partialName)
        _lastAddResult.value = result
    }
    
    /**
     * Add song to play next (after current song)
     */
    fun addToPlayNext(song: Song) {
        smartPlaylistManager.addToPlayNext(song)
    }
    
    /**
     * Add song to end of playlist
     */
    fun addToPlaylistEnd(song: Song) {
        smartPlaylistManager.addSong(song)
    }
    
    /**
     * Add multiple songs at once
     */
    fun addMultipleToPlaylist(songs: List<Song>) {
        smartPlaylistManager.addSongs(songs)
    }
    
    /**
     * Remove song from playlist by index
     */
    fun removeFromPlaylist(index: Int) {
        smartPlaylistManager.removeSong(index)
    }
    
    /**
     * Remove song from playlist by ID
     */
    fun removeFromPlaylistById(songId: Long) {
        smartPlaylistManager.removeSongById(songId)
    }
    
    /**
     * Move song within playlist (drag and drop)
     */
    fun moveInPlaylist(fromIndex: Int, toIndex: Int) {
        smartPlaylistManager.moveSong(fromIndex, toIndex)
    }
    
    /**
     * Play from specific index in playlist
     */
    fun playFromPlaylistIndex(index: Int) {
        smartPlaylistManager.setCurrentIndex(index)
        smartPlaylistManager.activePlaylist.value.queue.getOrNull(index)?.let { song ->
            musicController.playSong(song, smartPlaylistManager.activePlaylist.value.queue)
        }
    }
    
    /**
     * Clear entire playlist
     */
    fun clearPlaylist() {
        smartPlaylistManager.clearPlaylist()
    }
    
    /**
     * Shuffle remaining songs in playlist
     */
    fun shufflePlaylistRemaining() {
        smartPlaylistManager.shuffleRemaining()
    }
    
    /**
     * Toggle loop mode for playlist
     */
    fun togglePlaylistLoop() {
        smartPlaylistManager.toggleLoop()
    }
    
    /**
     * Move to next song in playlist
     */
    fun playlistNext(): Song? {
        val nextSong = smartPlaylistManager.moveToNext()
        nextSong?.let { song ->
            musicController.playSong(song, smartPlaylistManager.activePlaylist.value.queue)
        }
        return nextSong
    }
    
    /**
     * Move to previous song in playlist
     */
    fun playlistPrevious(): Song? {
        val prevSong = smartPlaylistManager.moveToPrevious()
        prevSong?.let { song ->
            musicController.playSong(song, smartPlaylistManager.activePlaylist.value.queue)
        }
        return prevSong
    }
    
    /**
     * Get upcoming songs in playlist
     */
    fun getPlaylistUpcoming(count: Int = 5): List<Song> {
        return smartPlaylistManager.getUpcoming(count)
    }
    
    /**
     * Get remaining song count
     */
    fun getPlaylistRemainingCount(): Int {
        return smartPlaylistManager.getRemainingCount()
    }
    
    /**
     * Get total playlist duration
     */
    fun getPlaylistTotalDuration(): Long {
        return smartPlaylistManager.getTotalDuration()
    }
    
    /**
     * Get remaining playlist duration
     */
    fun getPlaylistRemainingDuration(): Long {
        return smartPlaylistManager.getRemainingDuration()
    }
    
    /**
     * Clear the last add result notification
     */
    fun clearLastAddResult() {
        _lastAddResult.value = null
    }
    
    // ==================== SEARCH & SORT ====================
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        // Use smart search for results
        val results = if (query.isBlank()) {
            _uiState.value.songs
        } else {
            smartSearchEngine.searchSongs(_uiState.value.songs, query)
                .map { it.song }
        }
        
        _uiState.value = _uiState.value.copy(filteredSongs = results)
        
        // Update suggestions
        _searchSuggestions.value = smartSearchEngine.getSuggestions(
            _uiState.value.songs, 
            query
        )
    }
    
    fun clearSearch() {
        updateSearchQuery("")
        _searchSuggestions.value = emptyList()
    }
    
    private fun applySearch(songs: List<Song>, query: String): List<Song> {
        return if (query.isBlank()) songs
        else smartSearchEngine.searchSongs(songs, query).map { it.song }
    }
    
    fun setSortOption(option: SortOption) {
        val sorted = musicRepository.sortSongs(_uiState.value.songs, option)
        _uiState.value = _uiState.value.copy(
            sortOption = option,
            songs = sorted,
            filteredSongs = applySearch(sorted, _uiState.value.searchQuery)
        )
    }
    
    // ==================== PLAYBACK CONTROLS ====================
    
    fun playSong(song: Song) {
        musicController.playSong(song, _uiState.value.filteredSongs)
    }
    
    /**
     * Play a counter clip with automatic A-B loop
     */
    fun playClip(clip: com.ultramusic.player.core.CounterClip) {
        // Find the song in library
        val song = _uiState.value.songs.find { it.id == clip.songId }
        if (song != null) {
            // Play the song
            musicController.playSong(song, listOf(song))
            
            // Seek to clip start
            viewModelScope.launch {
                kotlinx.coroutines.delay(100) // Wait for playback to start
                musicController.seekTo(clip.startMs)
                
                // Set A-B loop points
                musicController.setLoopPoints(clip.startMs, clip.endMs)
                
                // Record usage
                battleArmory.recordClipUsage(clip.id, won = false) // Default false, update later
            }
        }
    }
    
    fun togglePlayPause() {
        musicController.togglePlayPause()
    }
    
    fun playNext() {
        musicController.playNext()
    }
    
    fun playPrevious() {
        musicController.playPrevious()
    }
    
    fun seekTo(position: Long) {
        musicController.seekTo(position)
    }
    
    fun seekToPercent(percent: Float) {
        musicController.seekToPercent(percent)
    }
    
    fun toggleLoop() {
        musicController.toggleLoop()
    }
    
    fun toggleShuffle() {
        musicController.toggleShuffle()
    }
    
    // ==================== SPEED CONTROL ====================
    
    fun setSpeed(speed: Float) {
        musicController.setSpeed(speed)
        _uiState.value = _uiState.value.copy(selectedPreset = null)
    }
    
    fun adjustSpeed(delta: Float) {
        musicController.adjustSpeed(delta)
        _uiState.value = _uiState.value.copy(selectedPreset = null)
    }
    
    fun resetSpeed() {
        musicController.resetSpeed()
    }
    
    // ==================== PITCH CONTROL ====================
    
    fun setPitch(semitones: Float) {
        musicController.setPitch(semitones)
        _uiState.value = _uiState.value.copy(selectedPreset = null)
    }
    
    fun adjustPitch(delta: Float) {
        musicController.adjustPitch(delta)
        _uiState.value = _uiState.value.copy(selectedPreset = null)
    }
    
    fun resetPitch() {
        musicController.resetPitch()
    }
    
    // ==================== PRESETS ====================
    
    fun applyPreset(preset: AudioPreset) {
        musicController.applyPreset(preset)
        _uiState.value = _uiState.value.copy(selectedPreset = preset)
    }
    
    fun resetAll() {
        musicController.resetAll()
        _uiState.value = _uiState.value.copy(selectedPreset = null)
    }
    
    // ==================== A-B LOOP ====================
    
    fun setABLoopStart() {
        musicController.setABLoopStart()
    }
    
    fun setABLoopEnd() {
        musicController.setABLoopEnd()
    }
    
    fun clearABLoop() {
        musicController.clearABLoop()
    }
    
    /**
     * Save current A-B loop as a counter clip to Battle Armory
     */
    fun saveClipToArmory(
        song: com.ultramusic.player.data.Song,
        startMs: Long,
        endMs: Long,
        name: String? = null,
        purpose: com.ultramusic.player.core.ClipPurpose = com.ultramusic.player.core.ClipPurpose.ALL_ROUNDER
    ) {
        val clipName = name ?: "${song.title} (${formatTime(startMs)}-${formatTime(endMs)})"
        battleArmory.createCounterClip(
            song = song,
            startMs = startMs,
            endMs = endMs,
            name = clipName,
            purpose = purpose,
            notes = "Created from Now Playing"
        )
    }
    
    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        val mins = seconds / 60
        val secs = seconds % 60
        return "$mins:${secs.toString().padStart(2, '0')}"
    }
    
    // ==================== AUTO CLIP DETECTION ====================
    
    /**
     * Auto-detect best A-B clips in current song
     */
    fun autoDetectClips() {
        val currentSong = playbackState.value.currentSong ?: return
        
        viewModelScope.launch {
            _isDetectingClips.value = true
            _detectedClips.value = emptyList()
            
            try {
                val clips = autoClipDetector.detectClips(currentSong)
                _detectedClips.value = clips
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                _isDetectingClips.value = false
            }
        }
    }
    
    /**
     * Auto-detect clips for a specific song
     */
    fun autoDetectClipsForSong(song: Song) {
        viewModelScope.launch {
            _isDetectingClips.value = true
            _detectedClips.value = emptyList()
            
            try {
                val clips = autoClipDetector.detectClips(song)
                _detectedClips.value = clips
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                _isDetectingClips.value = false
            }
        }
    }
    
    /**
     * Set A-B loop from a detected clip
     */
    fun setABFromDetectedClip(clip: com.ultramusic.player.core.DetectedClip) {
        musicController.setLoopPoints(clip.startMs, clip.endMs)
        musicController.seekTo(clip.startMs)
    }
    
    /**
     * Save detected clip directly to armory
     */
    fun saveDetectedClipToArmory(clip: com.ultramusic.player.core.DetectedClip) {
        val song = _uiState.value.songs.find { it.id == clip.songId } ?: return
        battleArmory.createCounterClip(
            song = song,
            startMs = clip.startMs,
            endMs = clip.endMs,
            name = clip.suggestedName,
            purpose = clip.purpose,
            notes = clip.reason
        )
    }
    
    /**
     * Auto-detect and save all clips from current song to armory
     */
    fun autoDetectAndSaveAllClips() {
        val currentSong = playbackState.value.currentSong ?: return
        
        viewModelScope.launch {
            _isDetectingClips.value = true
            
            try {
                val clips = autoClipDetector.detectClips(currentSong)
                clips.forEach { clip ->
                    battleArmory.createCounterClip(
                        song = currentSong,
                        startMs = clip.startMs,
                        endMs = clip.endMs,
                        name = clip.suggestedName,
                        purpose = clip.purpose,
                        notes = clip.reason
                    )
                }
                _detectedClips.value = clips
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                _isDetectingClips.value = false
            }
        }
    }
    
    /**
     * Clear detected clips
     */
    fun clearDetectedClips() {
        _detectedClips.value = emptyList()
    }
    
    // Aliases for EasyPlayerScreen compatibility
    fun setLoopStart() = setABLoopStart()
    fun setLoopEnd() = setABLoopEnd()
    fun clearLoop() = clearABLoop()
    
    // Toggle repeat mode (0=off, 1=all, 2=one)
    fun toggleRepeat() {
        musicController.toggleRepeatMode()
    }
    
    // ==================== UI PANELS ====================
    
    fun toggleSpeedPitchPanel() {
        _uiState.value = _uiState.value.copy(
            showSpeedPitchPanel = !_uiState.value.showSpeedPitchPanel,
            showPresetPanel = false,
            showABLoopPanel = false
        )
    }
    
    fun togglePresetPanel() {
        _uiState.value = _uiState.value.copy(
            showPresetPanel = !_uiState.value.showPresetPanel,
            showSpeedPitchPanel = false,
            showABLoopPanel = false
        )
    }
    
    fun toggleABLoopPanel() {
        _uiState.value = _uiState.value.copy(
            showABLoopPanel = !_uiState.value.showABLoopPanel,
            showSpeedPitchPanel = false,
            showPresetPanel = false
        )
    }
    
    fun closePanels() {
        _uiState.value = _uiState.value.copy(
            showSpeedPitchPanel = false,
            showPresetPanel = false,
            showABLoopPanel = false
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        musicController.release()
        voiceSearchManager.release()
        extremeVoiceCapture.release()
        audioQualityManager.release()
    }
}
