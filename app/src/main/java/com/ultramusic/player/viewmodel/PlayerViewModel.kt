package com.ultramusic.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultramusic.player.audio.Algorithm
import com.ultramusic.player.audio.NativeAudioEngine
import com.ultramusic.player.audio.PlaybackState
import com.ultramusic.player.audio.QualityMode
import com.ultramusic.player.data.model.AudioPreset
import com.ultramusic.player.data.model.LoopRegion
import com.ultramusic.player.data.model.Song
import com.ultramusic.player.data.repository.MusicScanner
import com.ultramusic.player.data.repository.ScanProgress
import com.ultramusic.player.utils.VoiceCommandParser
import com.ultramusic.player.utils.VoiceSearchHandler
import com.ultramusic.player.utils.VoiceSearchState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the player
 */
data class PlayerUiState(
    // Library
    val songs: List<Song> = emptyList(),
    val isScanning: Boolean = false,
    val scanMessage: String = "",
    
    // Current playback
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    
    // Speed control (0.05 - 10.0)
    val speed: Float = 1.0f,
    
    // Pitch control (-36 to +36 semitones)
    val pitchSemitones: Float = 0.0f,
    val pitchCents: Float = 0.0f,
    
    // Formant
    val formantShift: Float = 0.0f,
    val preserveFormants: Boolean = true,
    
    // Loop
    val isLooping: Boolean = false,
    val loopRegion: LoopRegion? = null,
    
    // Quality
    val qualityMode: QualityMode = QualityMode.HIGH,
    val algorithm: Algorithm = Algorithm.PHASE_VOCODER,
    
    // Presets
    val currentPreset: AudioPreset = AudioPreset.DEFAULT,
    val customPresets: List<AudioPreset> = emptyList(),
    
    // BPM
    val detectedBpm: Float = 0f,
    val targetBpm: Float = 0f,
    
    // Voice search
    val voiceSearchState: VoiceSearchState = VoiceSearchState.Idle,
    val searchQuery: String = "",
    val searchResults: List<Song> = emptyList(),
    
    // UI
    val showSpeedControl: Boolean = false,
    val showPitchControl: Boolean = false,
    val showLoopControl: Boolean = false,
    val showPresets: Boolean = false
)

/**
 * Player ViewModel
 * 
 * Manages all player state and interactions with the native audio engine.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioEngine: NativeAudioEngine,
    private val musicScanner: MusicScanner,
    private val voiceSearchHandler: VoiceSearchHandler
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    init {
        // Initialize audio engine
        viewModelScope.launch {
            audioEngine.initialize()
        }
        
        // Observe playback state
        viewModelScope.launch {
            audioEngine.playbackState.collect { state ->
                _uiState.update { ui ->
                    ui.copy(
                        isPlaying = state.isPlaying,
                        currentPositionMs = state.currentPositionMs,
                        durationMs = state.durationMs,
                        speed = state.speed,
                        pitchSemitones = state.pitchSemitones,
                        pitchCents = state.pitchCents,
                        isLooping = state.isLooping
                    )
                }
            }
        }
        
        // Observe voice search state
        viewModelScope.launch {
            voiceSearchHandler.state.collect { state ->
                _uiState.update { it.copy(voiceSearchState = state) }
                
                // Handle successful voice search
                if (state is VoiceSearchState.Success) {
                    handleVoiceCommand(state.result)
                }
            }
        }
    }
    
    // ========================================================================
    // Library Management
    // ========================================================================
    
    fun scanMusic() {
        viewModelScope.launch {
            musicScanner.scanAllMusic().collect { progress ->
                when (progress) {
                    is ScanProgress.Started -> {
                        _uiState.update { it.copy(isScanning = true, scanMessage = "Starting scan...") }
                    }
                    is ScanProgress.ScanningVolume -> {
                        _uiState.update { 
                            it.copy(scanMessage = "Scanning ${progress.volumeName}...") 
                        }
                    }
                    is ScanProgress.VolumeComplete -> {
                        _uiState.update { 
                            it.copy(scanMessage = "Found ${progress.songCount} songs in ${progress.volumeName}") 
                        }
                    }
                    is ScanProgress.Complete -> {
                        _uiState.update { 
                            it.copy(
                                isScanning = false, 
                                songs = progress.songs,
                                scanMessage = "Found ${progress.songs.size} total songs"
                            ) 
                        }
                    }
                    is ScanProgress.Error -> {
                        _uiState.update { 
                            it.copy(isScanning = false, scanMessage = progress.message) 
                        }
                    }
                }
            }
        }
    }
    
    // ========================================================================
    // Playback Control
    // ========================================================================
    
    fun playSong(song: Song) {
        viewModelScope.launch {
            if (audioEngine.loadFile(song.filePath)) {
                _uiState.update { it.copy(currentSong = song) }
                audioEngine.play()
            }
        }
    }
    
    fun play() = audioEngine.play()
    fun pause() = audioEngine.pause()
    fun stop() = audioEngine.stop()
    
    fun togglePlayPause() {
        if (_uiState.value.isPlaying) pause() else play()
    }
    
    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
    }
    
    fun seekToPercent(percent: Float) {
        val positionMs = (percent * _uiState.value.durationMs).toLong()
        seekTo(positionMs)
    }
    
    // ========================================================================
    // Speed Control (0.05x - 10.0x)
    // ========================================================================
    
    fun setSpeed(speed: Float) {
        audioEngine.setSpeed(speed)
        _uiState.update { it.copy(speed = speed) }
    }
    
    fun incrementSpeed(delta: Float = 0.05f) {
        setSpeed(_uiState.value.speed + delta)
    }
    
    fun decrementSpeed(delta: Float = 0.05f) {
        setSpeed(_uiState.value.speed - delta)
    }
    
    fun resetSpeed() {
        setSpeed(1.0f)
    }
    
    // ========================================================================
    // Pitch Control (-36 to +36 semitones)
    // ========================================================================
    
    fun setPitchSemitones(semitones: Float) {
        audioEngine.setPitchSemitones(semitones)
        _uiState.update { it.copy(pitchSemitones = semitones) }
    }
    
    fun setPitchCents(cents: Float) {
        audioEngine.setPitchCents(cents)
        _uiState.update { it.copy(pitchCents = cents) }
    }
    
    fun incrementPitch(deltaSemitones: Float = 1f) {
        setPitchSemitones(_uiState.value.pitchSemitones + deltaSemitones)
    }
    
    fun decrementPitch(deltaSemitones: Float = 1f) {
        setPitchSemitones(_uiState.value.pitchSemitones - deltaSemitones)
    }
    
    fun resetPitch() {
        setPitchSemitones(0f)
        setPitchCents(0f)
    }
    
    // ========================================================================
    // Formant Control
    // ========================================================================
    
    fun setFormantShift(shift: Float) {
        audioEngine.setFormantShift(shift)
        _uiState.update { it.copy(formantShift = shift) }
    }
    
    fun setPreserveFormants(preserve: Boolean) {
        audioEngine.setPreserveFormants(preserve)
        _uiState.update { it.copy(preserveFormants = preserve) }
    }
    
    // ========================================================================
    // Loop Control
    // ========================================================================
    
    fun setLoopRegion(startMs: Long, endMs: Long, name: String = "") {
        audioEngine.setLoopRegion(startMs, endMs)
        _uiState.update { 
            it.copy(loopRegion = LoopRegion(startMs, endMs, name))
        }
    }
    
    fun enableLoop(enable: Boolean) {
        audioEngine.enableLoop(enable)
        _uiState.update { it.copy(isLooping = enable) }
    }
    
    fun toggleLoop() {
        enableLoop(!_uiState.value.isLooping)
    }
    
    fun clearLoop() {
        enableLoop(false)
        _uiState.update { it.copy(loopRegion = null) }
    }
    
    // ========================================================================
    // Quality Settings
    // ========================================================================
    
    fun setQualityMode(mode: QualityMode) {
        audioEngine.setQualityMode(mode)
        _uiState.update { it.copy(qualityMode = mode) }
    }
    
    fun setAlgorithm(algorithm: Algorithm) {
        audioEngine.setAlgorithm(algorithm)
        _uiState.update { it.copy(algorithm = algorithm) }
    }
    
    // ========================================================================
    // Presets
    // ========================================================================
    
    fun applyPreset(preset: AudioPreset) {
        setSpeed(preset.speed)
        setPitchSemitones(preset.pitchSemitones)
        setPitchCents(preset.pitchCents)
        setFormantShift(preset.formantShift)
        setPreserveFormants(preset.preserveFormants)
        _uiState.update { it.copy(currentPreset = preset) }
    }
    
    fun applyNightcore() = applyPreset(AudioPreset.NIGHTCORE)
    fun applySlowed() = applyPreset(AudioPreset.SLOWED)
    fun applyVaporwave() = applyPreset(AudioPreset.VAPORWAVE)
    
    fun resetToDefault() {
        applyPreset(AudioPreset.DEFAULT)
    }
    
    // ========================================================================
    // BPM
    // ========================================================================
    
    fun detectBpm() {
        viewModelScope.launch {
            val bpm = audioEngine.detectBPM()
            _uiState.update { it.copy(detectedBpm = bpm) }
        }
    }
    
    fun setTargetBpm(bpm: Float) {
        audioEngine.setTargetBPM(bpm)
        _uiState.update { it.copy(targetBpm = bpm) }
    }
    
    // ========================================================================
    // Voice Search
    // ========================================================================
    
    fun startVoiceSearch() {
        voiceSearchHandler.startListening()
    }
    
    fun stopVoiceSearch() {
        voiceSearchHandler.stopListening()
    }
    
    fun cancelVoiceSearch() {
        voiceSearchHandler.cancel()
    }
    
    private fun handleVoiceCommand(text: String) {
        when (val command = VoiceCommandParser.parse(text)) {
            is VoiceCommandParser.VoiceCommand.Search -> {
                searchSongs(command.query)
            }
            is VoiceCommandParser.VoiceCommand.Play -> {
                val song = _uiState.value.songs.find { 
                    it.title.contains(command.songName, ignoreCase = true) 
                }
                song?.let { playSong(it) }
            }
            is VoiceCommandParser.VoiceCommand.Pause -> pause()
            is VoiceCommandParser.VoiceCommand.Resume -> play()
            is VoiceCommandParser.VoiceCommand.Next -> playNext()
            is VoiceCommandParser.VoiceCommand.Previous -> playPrevious()
            is VoiceCommandParser.VoiceCommand.SpeedUp -> incrementSpeed(0.1f)
            is VoiceCommandParser.VoiceCommand.SlowDown -> decrementSpeed(0.1f)
            is VoiceCommandParser.VoiceCommand.SetSpeed -> setSpeed(command.speed)
            is VoiceCommandParser.VoiceCommand.PitchUp -> incrementPitch(1f)
            is VoiceCommandParser.VoiceCommand.PitchDown -> decrementPitch(1f)
            is VoiceCommandParser.VoiceCommand.SetPitch -> setPitchSemitones(command.semitones)
            is VoiceCommandParser.VoiceCommand.EnableLoop -> enableLoop(true)
            is VoiceCommandParser.VoiceCommand.DisableLoop -> enableLoop(false)
            is VoiceCommandParser.VoiceCommand.Unknown -> {
                // Treat as search
                searchSongs(text)
            }
        }
    }
    
    // ========================================================================
    // Search
    // ========================================================================
    
    fun searchSongs(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        
        val results = _uiState.value.songs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
            song.artist.contains(query, ignoreCase = true) ||
            song.album.contains(query, ignoreCase = true)
        }
        
        _uiState.update { it.copy(searchResults = results) }
    }
    
    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }
    
    // ========================================================================
    // Navigation
    // ========================================================================
    
    fun playNext() {
        val currentIndex = _uiState.value.songs.indexOf(_uiState.value.currentSong)
        if (currentIndex >= 0 && currentIndex < _uiState.value.songs.size - 1) {
            playSong(_uiState.value.songs[currentIndex + 1])
        }
    }
    
    fun playPrevious() {
        val currentIndex = _uiState.value.songs.indexOf(_uiState.value.currentSong)
        if (currentIndex > 0) {
            playSong(_uiState.value.songs[currentIndex - 1])
        }
    }
    
    // ========================================================================
    // UI State
    // ========================================================================
    
    fun toggleSpeedControl() {
        _uiState.update { it.copy(showSpeedControl = !it.showSpeedControl) }
    }
    
    fun togglePitchControl() {
        _uiState.update { it.copy(showPitchControl = !it.showPitchControl) }
    }
    
    fun toggleLoopControl() {
        _uiState.update { it.copy(showLoopControl = !it.showLoopControl) }
    }
    
    fun togglePresets() {
        _uiState.update { it.copy(showPresets = !it.showPresets) }
    }
    
    // ========================================================================
    // Cleanup
    // ========================================================================
    
    override fun onCleared() {
        super.onCleared()
        audioEngine.shutdown()
        voiceSearchHandler.destroy()
    }
}
