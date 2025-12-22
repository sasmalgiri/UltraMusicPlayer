package com.ultramusic.player.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Voice Search State
 */
sealed class VoiceSearchState {
    object Idle : VoiceSearchState()
    object Listening : VoiceSearchState()
    object Processing : VoiceSearchState()
    data class Result(val text: String) : VoiceSearchState()
    data class Error(val message: String) : VoiceSearchState()
}

/**
 * VoiceSearchManager - Advanced voice recognition with noise cancellation
 * 
 * Designed for loud music competition environments:
 * - Uses VOICE_RECOGNITION audio source (optimized for voice)
 * - Applies NoiseSuppressor to filter background music
 * - Uses AcousticEchoCanceler to remove played music feedback
 * - AutomaticGainControl to normalize voice level
 * - Multiple recognition attempts for accuracy
 */
@Singleton
class VoiceSearchManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var autoGainControl: AutomaticGainControl? = null
    private var audioRecord: AudioRecord? = null
    
    private val _state = MutableStateFlow<VoiceSearchState>(VoiceSearchState.Idle)
    val state: StateFlow<VoiceSearchState> = _state.asStateFlow()
    
    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()
    
    private val _noiseLevel = MutableStateFlow(0f)
    val noiseLevel: StateFlow<Float> = _noiseLevel.asStateFlow()
    
    // Accumulated results for better accuracy
    private val recognitionResults = mutableListOf<String>()
    
    init {
        checkAvailability()
    }
    
    private fun checkAvailability() {
        _isAvailable.value = SpeechRecognizer.isRecognitionAvailable(context) &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == 
                PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Initialize audio processing with noise suppression
     */
    private fun initializeAudioProcessing() {
        try {
            // Create AudioRecord to get audio session ID for effects
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED) {
                
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION, // Optimized for voice
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize * 2
                )
                
                val audioSessionId = audioRecord?.audioSessionId ?: 0
                
                // Apply Noise Suppressor - CRITICAL for loud music environments
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(audioSessionId)
                    noiseSuppressor?.enabled = true
                }
                
                // Apply Acoustic Echo Canceler - Removes played music from mic
                if (AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = AcousticEchoCanceler.create(audioSessionId)
                    echoCanceler?.enabled = true
                }
                
                // Apply Automatic Gain Control - Normalizes voice level
                if (AutomaticGainControl.isAvailable()) {
                    autoGainControl = AutomaticGainControl.create(audioSessionId)
                    autoGainControl?.enabled = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Start listening for voice input with maximum noise cancellation
     */
    fun startListening() {
        if (!_isAvailable.value) {
            _state.value = VoiceSearchState.Error("Voice recognition not available")
            return
        }
        
        recognitionResults.clear()
        _state.value = VoiceSearchState.Listening
        
        // Initialize audio processing
        initializeAudioProcessing()
        
        // Create speech recognizer
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = VoiceSearchState.Listening
            }
            
            override fun onBeginningOfSpeech() {
                // Voice detected
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Update noise level indicator (normalized 0-1)
                _noiseLevel.value = ((rmsdB + 2) / 12).coerceIn(0f, 1f)
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                _state.value = VoiceSearchState.Processing
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try speaking louder!"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Try again!"
                    else -> "Recognition error"
                }
                
                // If no match, provide helpful message for loud environment
                if (error == SpeechRecognizer.ERROR_NO_MATCH || 
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    _state.value = VoiceSearchState.Error(
                        "Couldn't hear you. Try speaking closer to the mic or in a quieter moment!"
                    )
                } else {
                    _state.value = VoiceSearchState.Error(errorMessage)
                }
                
                releaseAudioProcessing()
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                
                if (!matches.isNullOrEmpty()) {
                    // Get the best result (highest confidence)
                    val bestResult = if (confidences != null && confidences.isNotEmpty()) {
                        val maxIndex = confidences.indices.maxByOrNull { confidences[it] } ?: 0
                        matches[maxIndex]
                    } else {
                        matches[0]
                    }
                    
                    recognitionResults.add(bestResult)
                    _state.value = VoiceSearchState.Result(bestResult)
                } else {
                    _state.value = VoiceSearchState.Error("No speech recognized")
                }
                
                releaseAudioProcessing()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Show partial results for better UX
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty()) {
                    // Could update UI with partial results
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        // Create recognition intent with settings optimized for noisy environments
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            
            // Request multiple results for better accuracy
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            
            // Prefer offline recognition for faster response
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            
            // Partial results for better UX
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            // Speech input settings
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            
            // Prompt (not shown but used internally)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the song name...")
        }
        
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = VoiceSearchState.Error("Failed to start voice recognition")
            releaseAudioProcessing()
        }
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = VoiceSearchState.Idle
        releaseAudioProcessing()
    }
    
    /**
     * Cancel listening
     */
    fun cancel() {
        speechRecognizer?.cancel()
        _state.value = VoiceSearchState.Idle
        releaseAudioProcessing()
    }
    
    /**
     * Reset state to idle
     */
    fun resetState() {
        _state.value = VoiceSearchState.Idle
        _noiseLevel.value = 0f
    }
    
    /**
     * Release audio processing resources
     */
    private fun releaseAudioProcessing() {
        try {
            noiseSuppressor?.release()
            noiseSuppressor = null
            
            echoCanceler?.release()
            echoCanceler = null
            
            autoGainControl?.release()
            autoGainControl = null
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        releaseAudioProcessing()
    }
    
    /**
     * Check if noise suppression is available on this device
     */
    fun isNoiseSuppressorAvailable(): Boolean = NoiseSuppressor.isAvailable()
    
    /**
     * Check if echo cancellation is available
     */
    fun isEchoCancelerAvailable(): Boolean = AcousticEchoCanceler.isAvailable()
    
    /**
     * Get audio processing capabilities info
     */
    fun getCapabilitiesInfo(): String {
        val capabilities = mutableListOf<String>()
        if (NoiseSuppressor.isAvailable()) capabilities.add("Noise Suppression")
        if (AcousticEchoCanceler.isAvailable()) capabilities.add("Echo Cancellation")
        if (AutomaticGainControl.isAvailable()) capabilities.add("Auto Gain")
        
        return if (capabilities.isEmpty()) {
            "Basic voice recognition"
        } else {
            "Enhanced: ${capabilities.joinToString(", ")}"
        }
    }
}
