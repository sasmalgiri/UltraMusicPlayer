package com.ultramusic.player.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice search state
 */
sealed class VoiceSearchState {
    object Idle : VoiceSearchState()
    object Listening : VoiceSearchState()
    data class Processing(val partialResult: String) : VoiceSearchState()
    data class Success(val result: String) : VoiceSearchState()
    data class Error(val message: String) : VoiceSearchState()
}

/**
 * Voice Search Handler
 * 
 * Provides voice-to-text functionality for searching music.
 * Uses Android's built-in SpeechRecognizer for high accuracy.
 */
@Singleton
class VoiceSearchHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    
    private val _state = MutableStateFlow<VoiceSearchState>(VoiceSearchState.Idle)
    val state: StateFlow<VoiceSearchState> = _state.asStateFlow()
    
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()
    
    init {
        checkAvailability()
    }
    
    /**
     * Check if speech recognition is available
     */
    private fun checkAvailability() {
        _isAvailable.value = SpeechRecognizer.isRecognitionAvailable(context)
    }
    
    /**
     * Start listening for voice input
     */
    fun startListening(
        language: Locale = Locale.getDefault(),
        onResult: (String) -> Unit = {}
    ) {
        if (!_isAvailable.value) {
            _state.value = VoiceSearchState.Error("Speech recognition not available")
            return
        }
        
        // Create recognizer if needed
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        
        // Create intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                1500L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                1000L
            )
        }
        
        // Set listener
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceSearchState.Listening
            }
            
            override fun onBeginningOfSpeech() {
                // Speech started
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Volume level changed - can be used for visual feedback
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }
            
            override fun onEndOfSpeech() {
                // Speech ended, waiting for results
            }
            
            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                _state.value = VoiceSearchState.Error(message)
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                val result = matches?.firstOrNull() ?: ""
                
                if (result.isNotEmpty()) {
                    _state.value = VoiceSearchState.Success(result)
                    onResult(result)
                } else {
                    _state.value = VoiceSearchState.Error("No speech detected")
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                val partial = matches?.firstOrNull() ?: ""
                
                if (partial.isNotEmpty()) {
                    _state.value = VoiceSearchState.Processing(partial)
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Additional events
            }
        })
        
        // Start listening
        speechRecognizer?.startListening(intent)
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = VoiceSearchState.Idle
    }
    
    /**
     * Cancel recognition
     */
    fun cancel() {
        speechRecognizer?.cancel()
        _state.value = VoiceSearchState.Idle
    }
    
    /**
     * Reset state
     */
    fun reset() {
        _state.value = VoiceSearchState.Idle
    }
    
    /**
     * Destroy recognizer
     */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

/**
 * Voice command parser
 * 
 * Parses voice commands for playback control:
 * - "Play [song name]"
 * - "Search [query]"
 * - "Speed up" / "Slow down"
 * - "Pitch up" / "Pitch down"
 * - "Loop this"
 * - "Next" / "Previous"
 */
object VoiceCommandParser {
    
    sealed class VoiceCommand {
        data class Search(val query: String) : VoiceCommand()
        data class Play(val songName: String) : VoiceCommand()
        object Pause : VoiceCommand()
        object Resume : VoiceCommand()
        object Next : VoiceCommand()
        object Previous : VoiceCommand()
        data class SetSpeed(val speed: Float) : VoiceCommand()
        object SpeedUp : VoiceCommand()
        object SlowDown : VoiceCommand()
        data class SetPitch(val semitones: Float) : VoiceCommand()
        object PitchUp : VoiceCommand()
        object PitchDown : VoiceCommand()
        object EnableLoop : VoiceCommand()
        object DisableLoop : VoiceCommand()
        data class Unknown(val text: String) : VoiceCommand()
    }
    
    fun parse(input: String): VoiceCommand {
        val normalized = input.lowercase().trim()
        
        return when {
            // Playback control
            normalized.startsWith("play ") -> {
                VoiceCommand.Play(input.substringAfter("play ").trim())
            }
            normalized == "pause" || normalized == "stop" -> VoiceCommand.Pause
            normalized == "resume" || normalized == "continue" -> VoiceCommand.Resume
            normalized == "next" || normalized == "skip" -> VoiceCommand.Next
            normalized == "previous" || normalized == "back" -> VoiceCommand.Previous
            
            // Speed control
            normalized.contains("speed up") || normalized.contains("faster") -> {
                VoiceCommand.SpeedUp
            }
            normalized.contains("slow down") || normalized.contains("slower") -> {
                VoiceCommand.SlowDown
            }
            normalized.startsWith("set speed") || normalized.startsWith("speed") -> {
                val speed = extractNumber(normalized)
                if (speed != null) {
                    VoiceCommand.SetSpeed(speed)
                } else {
                    VoiceCommand.Unknown(input)
                }
            }
            
            // Pitch control
            normalized.contains("pitch up") || normalized.contains("higher pitch") -> {
                VoiceCommand.PitchUp
            }
            normalized.contains("pitch down") || normalized.contains("lower pitch") -> {
                VoiceCommand.PitchDown
            }
            normalized.startsWith("set pitch") || normalized.startsWith("pitch") -> {
                val pitch = extractNumber(normalized)
                if (pitch != null) {
                    VoiceCommand.SetPitch(pitch)
                } else {
                    VoiceCommand.Unknown(input)
                }
            }
            
            // Loop control
            normalized.contains("loop") && 
                    (normalized.contains("enable") || normalized.contains("on") || 
                     normalized.contains("start") || normalized == "loop this") -> {
                VoiceCommand.EnableLoop
            }
            normalized.contains("loop") && 
                    (normalized.contains("disable") || normalized.contains("off") || 
                     normalized.contains("stop")) -> {
                VoiceCommand.DisableLoop
            }
            
            // Search
            normalized.startsWith("search ") || 
                    normalized.startsWith("find ") ||
                    normalized.startsWith("look for ") -> {
                val query = normalized
                    .removePrefix("search ")
                    .removePrefix("find ")
                    .removePrefix("look for ")
                    .trim()
                VoiceCommand.Search(query)
            }
            
            // Default: treat as search query
            else -> VoiceCommand.Search(input)
        }
    }
    
    private fun extractNumber(text: String): Float? {
        val regex = Regex("[-+]?\\d*\\.?\\d+")
        val match = regex.find(text)
        return match?.value?.toFloatOrNull()
    }
}
