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
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Extreme Noise Voice Capture
 * 
 * Designed for VERY LOUD music competition environments where:
 * - Background music is 90-110 dB
 * - User speaks 60-80 dB at close range
 * - Standard voice recognition WILL FAIL
 * 
 * Solution approach:
 * 1. Multiple audio processing layers
 * 2. Bengali + English language support
 * 3. Adaptive noise learning
 * 4. Voice activity detection (VAD)
 * 5. Multiple recognition attempts with confidence scoring
 * 6. Visual feedback for optimal timing
 */

/**
 * Voice capture state with detailed feedback
 */
sealed class ExtremeCaptureState {
    object Idle : ExtremeCaptureState()
    object Calibrating : ExtremeCaptureState()  // Learning noise profile
    object WaitingForQuiet : ExtremeCaptureState()  // Waiting for quieter moment
    object ReadyToListen : ExtremeCaptureState()  // Good time to speak
    object Listening : ExtremeCaptureState()
    object Processing : ExtremeCaptureState()
    data class Result(
        val text: String, 
        val confidence: Float,
        val language: String,
        val alternatives: List<String>
    ) : ExtremeCaptureState()
    data class Error(val message: String, val suggestion: String) : ExtremeCaptureState()
}

/**
 * Noise level classification
 */
enum class NoiseLevel {
    QUIET,      // < 50 dB - Good for voice
    MODERATE,   // 50-70 dB - Acceptable
    LOUD,       // 70-85 dB - Difficult
    VERY_LOUD,  // 85-100 dB - Very hard
    EXTREME     // > 100 dB - Nearly impossible
}

@Singleton
class ExtremeNoiseVoiceCapture @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var autoGainControl: AutomaticGainControl? = null
    
    private val scope = CoroutineScope(Dispatchers.Main)
    private var noiseMonitorJob: Job? = null
    private var recognitionJob: Job? = null
    
    private val _state = MutableStateFlow<ExtremeCaptureState>(ExtremeCaptureState.Idle)
    val state: StateFlow<ExtremeCaptureState> = _state.asStateFlow()
    
    private val _noiseLevel = MutableStateFlow(NoiseLevel.QUIET)
    val noiseLevel: StateFlow<NoiseLevel> = _noiseLevel.asStateFlow()
    
    private val _noiseLevelDb = MutableStateFlow(0f)
    val noiseLevelDb: StateFlow<Float> = _noiseLevelDb.asStateFlow()
    
    private val _voiceDetected = MutableStateFlow(false)
    val voiceDetected: StateFlow<Boolean> = _voiceDetected.asStateFlow()
    
    private val _canSpeak = MutableStateFlow(false)
    val canSpeak: StateFlow<Boolean> = _canSpeak.asStateFlow()
    
    // Noise profile learned during calibration
    private var baselineNoiseLevel = 70f
    private var noiseVariance = 10f
    
    // Recognition results from multiple attempts
    private val recognitionAttempts = mutableListOf<RecognitionAttempt>()
    
    companion object {
        private const val TAG = "ExtremeNoiseVoiceCapture"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        
        // Noise thresholds (in dB)
        private const val QUIET_THRESHOLD = 50f
        private const val MODERATE_THRESHOLD = 70f
        private const val LOUD_THRESHOLD = 85f
        private const val VERY_LOUD_THRESHOLD = 100f
        
        // Voice detection
        private const val VOICE_FREQUENCY_MIN = 85f  // Hz
        private const val VOICE_FREQUENCY_MAX = 255f // Hz for Bengali/English speech
        
        // Calibration
        private const val CALIBRATION_DURATION_MS = 2000L
        private const val NOISE_SAMPLE_INTERVAL_MS = 50L
    }
    
    /**
     * Start the extreme noise voice capture process
     * 
     * @param languages List of language codes to recognize (e.g., ["bn-IN", "en-IN", "hi-IN"])
     */
    fun startCapture(languages: List<String> = listOf("bn-IN", "en-IN", "hi-IN")) {
        if (!hasPermission()) {
            _state.value = ExtremeCaptureState.Error(
                "Microphone permission required",
                "Please grant microphone permission in settings"
            )
            return
        }
        
        // Start with calibration
        _state.value = ExtremeCaptureState.Calibrating
        
        scope.launch {
            // Step 1: Calibrate noise level
            calibrateNoiseLevel()
            
            // Step 2: Wait for relatively quiet moment
            waitForOptimalMoment()
            
            // Step 3: Start recognition
            startRecognition(languages)
        }
    }
    
    /**
     * Calibrate the noise level by sampling background audio
     */
    private suspend fun calibrateNoiseLevel() {
        initializeAudioRecord()
        
        val samples = mutableListOf<Float>()
        val startTime = System.currentTimeMillis()
        
        audioRecord?.startRecording()
        
        while (System.currentTimeMillis() - startTime < CALIBRATION_DURATION_MS) {
            val db = measureCurrentNoiseDb()
            samples.add(db)
            delay(NOISE_SAMPLE_INTERVAL_MS)
        }
        
        // Calculate baseline and variance
        if (samples.isNotEmpty()) {
            baselineNoiseLevel = samples.average().toFloat()
            noiseVariance = samples.map { (it - baselineNoiseLevel) * (it - baselineNoiseLevel) }
                .average().toFloat().let { sqrt(it) }
        }
        
        audioRecord?.stop()
    }
    
    /**
     * Wait for a relatively quiet moment in the music
     * (e.g., between songs, during a quiet passage)
     */
    private suspend fun waitForOptimalMoment() {
        _state.value = ExtremeCaptureState.WaitingForQuiet
        
        initializeAudioRecord()
        audioRecord?.startRecording()
        
        var quietMomentCount = 0
        val requiredQuietMoments = 3  // Need 3 consecutive quiet samples
        
        // Monitor for up to 10 seconds
        val timeout = System.currentTimeMillis() + 10000
        
        while (System.currentTimeMillis() < timeout) {
            val currentDb = measureCurrentNoiseDb()
            _noiseLevelDb.value = currentDb
            _noiseLevel.value = classifyNoiseLevel(currentDb)
            
            // Check if this is a good moment to speak
            // Good = current noise is below baseline or has dropped significantly
            val isQuietMoment = currentDb < baselineNoiseLevel - noiseVariance * 0.5f ||
                               currentDb < MODERATE_THRESHOLD
            
            if (isQuietMoment) {
                quietMomentCount++
                if (quietMomentCount >= requiredQuietMoments) {
                    _canSpeak.value = true
                    _state.value = ExtremeCaptureState.ReadyToListen
                    delay(500)  // Give user time to see the "ready" state
                    break
                }
            } else {
                quietMomentCount = 0
                _canSpeak.value = false
            }
            
            delay(NOISE_SAMPLE_INTERVAL_MS)
        }
        
        audioRecord?.stop()
        
        // If we timed out, proceed anyway with a warning
        if (_state.value == ExtremeCaptureState.WaitingForQuiet) {
            _state.value = ExtremeCaptureState.ReadyToListen
        }
    }
    
    /**
     * Start actual speech recognition with multiple language support
     */
    private fun startRecognition(languages: List<String>) {
        _state.value = ExtremeCaptureState.Listening
        recognitionAttempts.clear()
        
        initializeAudioProcessing()
        
        // Create recognizer for each language and pick best result
        for (language in languages) {
            startRecognizerForLanguage(language)
        }
    }
    
    private fun startRecognizerForLanguage(language: String) {
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceDetected.value = false
            }
            
            override fun onBeginningOfSpeech() {
                _voiceDetected.value = true
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Update visual feedback
                val normalized = ((rmsdB + 2) / 12).coerceIn(0f, 1f)
                // Could update a visual indicator here
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                _state.value = ExtremeCaptureState.Processing
            }
            
            override fun onError(error: Int) {
                val (message, suggestion) = getErrorDetails(error)
                
                // If this is just one language failing, try next
                if (recognitionAttempts.isEmpty()) {
                    _state.value = ExtremeCaptureState.Error(message, suggestion)
                }
                
                releaseAudioProcessing()
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                
                if (!matches.isNullOrEmpty()) {
                    val confidence = confidences?.getOrNull(0) ?: 0.5f
                    
                    recognitionAttempts.add(
                        RecognitionAttempt(
                            text = matches[0],
                            confidence = confidence,
                            language = language,
                            alternatives = matches.drop(1)
                        )
                    )
                    
                    // Use best result
                    val best = recognitionAttempts.maxByOrNull { it.confidence }
                    if (best != null) {
                        _state.value = ExtremeCaptureState.Result(
                            text = best.text,
                            confidence = best.confidence,
                            language = best.language,
                            alternatives = best.alternatives
                        )
                    }
                }
                
                releaseAudioProcessing()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        val intent = createRecognitionIntent(language)
        speechRecognizer?.startListening(intent)
    }
    
    /**
     * Create recognition intent optimized for noisy environment
     */
    private fun createRecognitionIntent(language: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            
            // Request multiple results for better accuracy
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            
            // Partial results for feedback
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            // Prefer offline for faster response
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            
            // Longer timeouts for noisy environment
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
    }
    
    /**
     * Initialize audio record for noise monitoring
     */
    private fun initializeAudioRecord() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
            )
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing audio record", e)
        }
    }

    /**
     * Initialize audio processing for voice capture
     */
    private fun initializeAudioProcessing() {
        try {
            val audioSessionId = audioRecord?.audioSessionId ?: return
            
            // Noise Suppressor - CRITICAL
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioSessionId)
                noiseSuppressor?.enabled = true
            }
            
            // Echo Canceler - Removes music feedback
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(audioSessionId)
                echoCanceler?.enabled = true
            }
            
            // Auto Gain Control - Normalizes voice
            if (AutomaticGainControl.isAvailable()) {
                autoGainControl = AutomaticGainControl.create(audioSessionId)
                autoGainControl?.enabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing audio processing", e)
        }
    }

    /**
     * Measure current noise level in dB
     */
    private fun measureCurrentNoiseDb(): Float {
        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)
        
        val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
        if (read <= 0) return 0f
        
        // Calculate RMS
        var sum = 0.0
        for (i in 0 until read) {
            sum += buffer[i] * buffer[i]
        }
        val rms = sqrt(sum / read)
        
        // Convert to dB (reference: max short value)
        val db = if (rms > 0) 20 * kotlin.math.log10(rms / Short.MAX_VALUE) + 90 else 0.0
        
        return db.toFloat().coerceIn(0f, 120f)
    }
    
    /**
     * Classify noise level
     */
    private fun classifyNoiseLevel(db: Float): NoiseLevel {
        return when {
            db < QUIET_THRESHOLD -> NoiseLevel.QUIET
            db < MODERATE_THRESHOLD -> NoiseLevel.MODERATE
            db < LOUD_THRESHOLD -> NoiseLevel.LOUD
            db < VERY_LOUD_THRESHOLD -> NoiseLevel.VERY_LOUD
            else -> NoiseLevel.EXTREME
        }
    }
    
    /**
     * Get error details with helpful suggestions
     */
    private fun getErrorDetails(error: Int): Pair<String, String> {
        return when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> 
                "Couldn't understand" to "Try speaking louder and closer to the mic"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 
                "No speech detected" to "Cup your hand around the mic and speak clearly"
            SpeechRecognizer.ERROR_AUDIO -> 
                "Audio error" to "Check if another app is using the microphone"
            SpeechRecognizer.ERROR_NETWORK -> 
                "Network error" to "Check internet connection or try offline mode"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 
                "Permission denied" to "Grant microphone permission in settings"
            else -> 
                "Recognition failed" to "Wait for a quieter moment and try again"
        }
    }
    
    /**
     * Check if microphone permission is granted
     */
    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Release audio processing resources
     */
    private fun releaseAudioProcessing() {
        noiseSuppressor?.release()
        noiseSuppressor = null
        
        echoCanceler?.release()
        echoCanceler = null
        
        autoGainControl?.release()
        autoGainControl = null
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    /**
     * Cancel ongoing capture
     */
    fun cancel() {
        noiseMonitorJob?.cancel()
        recognitionJob?.cancel()
        speechRecognizer?.cancel()
        releaseAudioProcessing()
        _state.value = ExtremeCaptureState.Idle
    }
    
    /**
     * Reset state
     */
    fun reset() {
        cancel()
        _noiseLevel.value = NoiseLevel.QUIET
        _noiseLevelDb.value = 0f
        _voiceDetected.value = false
        _canSpeak.value = false
        recognitionAttempts.clear()
    }
    
    /**
     * Release all resources
     */
    fun release() {
        cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
    
    /**
     * Get tips for the current noise level
     */
    fun getTipsForNoiseLevel(): List<String> {
        return when (_noiseLevel.value) {
            NoiseLevel.QUIET -> listOf(
                "Good conditions for voice input",
                "Speak naturally"
            )
            NoiseLevel.MODERATE -> listOf(
                "Speak clearly and closer to the phone",
                "Hold phone 10-15cm from mouth"
            )
            NoiseLevel.LOUD -> listOf(
                "Cup your hand around the microphone",
                "Speak during quieter moments in music",
                "Get closer to the phone"
            )
            NoiseLevel.VERY_LOUD -> listOf(
                "Very loud! Cup hand tightly around mic",
                "Wait for a break in the music",
                "Move to a slightly quieter spot if possible",
                "Speak as loud as comfortable"
            )
            NoiseLevel.EXTREME -> listOf(
                "Extremely loud! Voice input very difficult",
                "Try using text search instead",
                "Wait for song to end",
                "Move away from speakers if possible"
            )
        }
    }
}

/**
 * Single recognition attempt result
 */
data class RecognitionAttempt(
    val text: String,
    val confidence: Float,
    val language: String,
    val alternatives: List<String>
)
