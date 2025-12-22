package com.ultramusic.player.core

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * PRODUCTION-GRADE AUDIO ANALYZER
 * 
 * Real DSP algorithms for:
 * - BPM Detection (Beat tracking via onset detection)
 * - Key Detection (Chroma features + Krumhansl-Schmuckler algorithm)
 * - Energy Analysis (RMS, spectral centroid, spectral flux)
 * - Frequency Analysis (FFT-based)
 * - Onset Detection (Complex domain / spectral flux)
 * 
 * Based on established MIR (Music Information Retrieval) algorithms
 * References: TarsosDSP, Essentia, Librosa methodologies
 */
@Singleton
class RealAudioAnalyzer @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "RealAudioAnalyzer"
        private const val SAMPLE_RATE = 44100
        private const val FFT_SIZE = 4096
        private const val HOP_SIZE = 512
        private const val MIN_BPM = 60.0
        private const val MAX_BPM = 200.0
        
        // Krumhansl-Schmuckler key profiles
        private val MAJOR_PROFILE = doubleArrayOf(
            6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88
        )
        private val MINOR_PROFILE = doubleArrayOf(
            6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17
        )
        
        private val KEY_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    }
    
    private val scope = CoroutineScope(Dispatchers.Default)
    private var analysisJob: Job? = null
    private var audioRecord: AudioRecord? = null
    
    // ==================== STATE ====================
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private val _currentBPM = MutableStateFlow(0.0)
    val currentBPM: StateFlow<Double> = _currentBPM.asStateFlow()
    
    private val _bpmConfidence = MutableStateFlow(0f)
    val bpmConfidence: StateFlow<Float> = _bpmConfidence.asStateFlow()
    
    private val _detectedKey = MutableStateFlow<MusicalKey?>(null)
    val detectedKey: StateFlow<MusicalKey?> = _detectedKey.asStateFlow()
    
    private val _keyConfidence = MutableStateFlow(0f)
    val keyConfidence: StateFlow<Float> = _keyConfidence.asStateFlow()
    
    private val _energy = MutableStateFlow(0f)
    val energy: StateFlow<Float> = _energy.asStateFlow()
    
    private val _spectralCentroid = MutableStateFlow(0f)
    val spectralCentroid: StateFlow<Float> = _spectralCentroid.asStateFlow()
    
    private val _loudnessDb = MutableStateFlow(-60f)
    val loudnessDb: StateFlow<Float> = _loudnessDb.asStateFlow()
    
    private val _frequencySpectrum = MutableStateFlow(FloatArray(32))
    val frequencySpectrum: StateFlow<FloatArray> = _frequencySpectrum.asStateFlow()
    
    private val _onsetDetected = MutableStateFlow(false)
    val onsetDetected: StateFlow<Boolean> = _onsetDetected.asStateFlow()
    
    private val _analysisResult = MutableStateFlow<AudioAnalysisResult?>(null)
    val analysisResult: StateFlow<AudioAnalysisResult?> = _analysisResult.asStateFlow()
    
    // Internal buffers
    private var audioBuffer = ShortArray(FFT_SIZE)
    private var fftBuffer = DoubleArray(FFT_SIZE)
    private var previousSpectrum = DoubleArray(FFT_SIZE / 2)
    private var onsetFunction = mutableListOf<Double>()
    private var chromaHistory = mutableListOf<DoubleArray>()
    
    // ==================== PUBLIC API ====================
    
    /**
     * Start real-time audio analysis from microphone
     */
    fun startRealTimeAnalysis() {
        if (_isAnalyzing.value) return
        
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(FFT_SIZE * 2)
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return
            }
            
            audioRecord?.startRecording()
            _isAnalyzing.value = true
            
            analysisJob = scope.launch {
                val buffer = ShortArray(HOP_SIZE)
                var bufferPosition = 0
                
                while (isActive && _isAnalyzing.value) {
                    val read = audioRecord?.read(buffer, 0, HOP_SIZE) ?: 0
                    
                    if (read > 0) {
                        // Shift and add new samples
                        System.arraycopy(audioBuffer, read, audioBuffer, 0, FFT_SIZE - read)
                        for (i in 0 until read) {
                            audioBuffer[FFT_SIZE - read + i] = buffer[i]
                        }
                        
                        // Perform analysis
                        analyzeFrame()
                    }
                    
                    delay(10) // ~100 FPS
                }
            }
            
            Log.i(TAG, "Real-time analysis started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for audio recording", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start analysis", e)
        }
    }
    
    /**
     * Stop real-time analysis
     */
    fun stopAnalysis() {
        _isAnalyzing.value = false
        analysisJob?.cancel()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio record", e)
        }
        
        audioRecord = null
        Log.i(TAG, "Analysis stopped")
    }
    
    /**
     * Analyze audio file and return complete analysis
     */
    suspend fun analyzeAudioFile(
        samples: FloatArray,
        sampleRate: Int = SAMPLE_RATE
    ): AudioAnalysisResult {
        Log.i(TAG, "Analyzing ${samples.size} samples at $sampleRate Hz")
        
        val startTime = System.currentTimeMillis()
        
        // Convert to doubles
        val audioData = DoubleArray(samples.size) { samples[it].toDouble() }
        
        // Extract features
        val bpmResult = detectBPM(audioData, sampleRate)
        val keyResult = detectKey(audioData, sampleRate)
        val energyResult = calculateEnergy(audioData)
        val spectralFeatures = analyzeSpectralFeatures(audioData, sampleRate)
        
        val analysisTime = System.currentTimeMillis() - startTime
        Log.i(TAG, "Analysis completed in ${analysisTime}ms")
        
        return AudioAnalysisResult(
            bpm = bpmResult.first,
            bpmConfidence = bpmResult.second,
            key = keyResult.first,
            keyConfidence = keyResult.second,
            energy = energyResult,
            spectralCentroid = spectralFeatures.centroid,
            spectralFlatness = spectralFeatures.flatness,
            zeroCrossingRate = spectralFeatures.zeroCrossingRate,
            danceability = calculateDanceability(bpmResult.first, energyResult, spectralFeatures),
            valence = estimateValence(keyResult.first, spectralFeatures)
        )
    }
    
    // ==================== REAL-TIME ANALYSIS ====================
    
    private fun analyzeFrame() {
        // Convert to doubles with Hanning window
        for (i in 0 until FFT_SIZE) {
            val window = 0.5 * (1 - cos(2 * PI * i / (FFT_SIZE - 1)))
            fftBuffer[i] = audioBuffer[i] / 32768.0 * window
        }
        
        // Compute FFT
        val spectrum = computeFFT(fftBuffer)
        
        // Calculate loudness (RMS to dB)
        val rms = sqrt(fftBuffer.map { it * it }.average())
        val db = if (rms > 0) 20 * log10(rms) else -60.0
        _loudnessDb.value = db.toFloat().coerceIn(-60f, 0f)
        
        // Calculate energy (normalized RMS)
        _energy.value = (rms * 10).toFloat().coerceIn(0f, 1f)
        
        // Spectral centroid
        var centroidNum = 0.0
        var centroidDen = 0.0
        for (i in 0 until spectrum.size / 2) {
            val freq = i * SAMPLE_RATE.toDouble() / FFT_SIZE
            centroidNum += freq * spectrum[i]
            centroidDen += spectrum[i]
        }
        val centroid = if (centroidDen > 0) centroidNum / centroidDen else 0.0
        _spectralCentroid.value = centroid.toFloat()
        
        // Onset detection using spectral flux
        var flux = 0.0
        for (i in 0 until spectrum.size / 2) {
            val diff = spectrum[i] - previousSpectrum[i]
            if (diff > 0) flux += diff
        }
        
        // Adaptive threshold for onset
        onsetFunction.add(flux)
        if (onsetFunction.size > 50) onsetFunction.removeAt(0)
        
        val threshold = if (onsetFunction.isNotEmpty()) {
            onsetFunction.average() + onsetFunction.standardDeviation() * 1.5
        } else 0.0
        
        _onsetDetected.value = flux > threshold && flux > 0.1
        
        // Update spectrum visualization (32 bands)
        val bandSpectrum = FloatArray(32)
        val bandsPerGroup = (spectrum.size / 2) / 32
        for (i in 0 until 32) {
            var sum = 0.0
            for (j in 0 until bandsPerGroup) {
                sum += spectrum[i * bandsPerGroup + j]
            }
            bandSpectrum[i] = (sum / bandsPerGroup).toFloat()
        }
        _frequencySpectrum.value = bandSpectrum
        
        // Calculate chroma features for key detection
        val chroma = calculateChroma(spectrum, SAMPLE_RATE)
        chromaHistory.add(chroma)
        if (chromaHistory.size > 100) chromaHistory.removeAt(0)
        
        // Estimate key from chroma history
        if (chromaHistory.size >= 20) {
            val avgChroma = DoubleArray(12)
            for (c in chromaHistory) {
                for (i in 0 until 12) avgChroma[i] += c[i]
            }
            for (i in 0 until 12) avgChroma[i] = avgChroma[i] / chromaHistory.size
            
            val key = detectKeyFromChroma(avgChroma)
            _detectedKey.value = key.first
            _keyConfidence.value = key.second
        }
        
        // Save previous spectrum
        System.arraycopy(spectrum, 0, previousSpectrum, 0, spectrum.size / 2)
    }
    
    // ==================== BPM DETECTION ====================
    
    /**
     * Detect BPM using onset detection + auto-correlation
     * Algorithm: 
     * 1. Compute onset strength envelope
     * 2. Auto-correlate to find periodicity
     * 3. Convert lag to BPM
     */
    private fun detectBPM(audio: DoubleArray, sampleRate: Int): Pair<Double, Float> {
        // Calculate onset strength envelope
        val hopSize = 512
        val frameSize = 2048
        val onsetEnvelope = mutableListOf<Double>()
        
        var prevSpectrum = DoubleArray(frameSize / 2)
        
        var pos = 0
        while (pos + frameSize <= audio.size) {
            // Extract frame with window
            val frame = DoubleArray(frameSize)
            for (i in 0 until frameSize) {
                val window = 0.5 * (1 - cos(2 * PI * i / (frameSize - 1)))
                frame[i] = audio[pos + i] * window
            }
            
            // Compute magnitude spectrum
            val spectrum = computeFFT(frame)
            
            // Spectral flux (half-wave rectified)
            var flux = 0.0
            for (i in 0 until frameSize / 2) {
                val diff = spectrum[i] - prevSpectrum[i]
                if (diff > 0) flux += diff * diff
            }
            onsetEnvelope.add(sqrt(flux))
            
            System.arraycopy(spectrum, 0, prevSpectrum, 0, frameSize / 2)
            pos += hopSize
        }
        
        if (onsetEnvelope.size < 100) {
            return Pair(0.0, 0f)
        }
        
        // Normalize onset envelope
        val maxOnset = onsetEnvelope.maxOrNull() ?: 1.0
        val normalizedOnset = onsetEnvelope.map { it / maxOnset }.toDoubleArray()
        
        // Auto-correlation for tempo estimation
        val minLag = (60.0 / MAX_BPM * sampleRate / hopSize).toInt()
        val maxLag = (60.0 / MIN_BPM * sampleRate / hopSize).toInt().coerceAtMost(normalizedOnset.size / 2)
        
        var bestLag = minLag
        var bestCorr = 0.0
        
        for (lag in minLag until maxLag) {
            var corr = 0.0
            var count = 0
            for (i in 0 until normalizedOnset.size - lag) {
                corr += normalizedOnset[i] * normalizedOnset[i + lag]
                count++
            }
            corr /= count
            
            // Weight towards common tempos (120-130 BPM)
            val bpm = 60.0 * sampleRate / (lag * hopSize)
            val tempoWeight = 1.0 - abs(bpm - 125) / 100
            corr *= (1 + tempoWeight * 0.2)
            
            if (corr > bestCorr) {
                bestCorr = corr
                bestLag = lag
            }
        }
        
        val bpm = 60.0 * sampleRate / (bestLag * hopSize)
        
        // Adjust BPM to common range (double or halve if needed)
        val adjustedBpm = when {
            bpm < 70 -> bpm * 2
            bpm > 170 -> bpm / 2
            else -> bpm
        }
        
        val confidence = (bestCorr * 100).toFloat().coerceIn(0f, 100f)
        
        Log.d(TAG, "Detected BPM: $adjustedBpm (confidence: $confidence%)")
        return Pair(adjustedBpm, confidence)
    }
    
    // ==================== KEY DETECTION ====================
    
    /**
     * Detect musical key using Krumhansl-Schmuckler algorithm
     * 1. Extract chroma features from audio
     * 2. Correlate with major/minor key profiles
     * 3. Return best matching key
     */
    private fun detectKey(audio: DoubleArray, sampleRate: Int): Pair<MusicalKey, Float> {
        val frameSize = 4096
        val hopSize = 2048
        
        // Accumulate chroma features
        val chromaSum = DoubleArray(12)
        var frameCount = 0
        
        var pos = 0
        while (pos + frameSize <= audio.size) {
            val frame = DoubleArray(frameSize)
            for (i in 0 until frameSize) {
                val window = 0.5 * (1 - cos(2 * PI * i / (frameSize - 1)))
                frame[i] = audio[pos + i] * window
            }
            
            val spectrum = computeFFT(frame)
            val chroma = calculateChroma(spectrum, sampleRate)
            
            for (i in 0 until 12) {
                chromaSum[i] += chroma[i]
            }
            frameCount++
            pos += hopSize
        }
        
        // Average chroma
        if (frameCount > 0) {
            for (i in 0 until 12) chromaSum[i] = chromaSum[i] / frameCount
        }
        
        return detectKeyFromChroma(chromaSum)
    }
    
    /**
     * Calculate 12-bin chroma feature from spectrum
     */
    private fun calculateChroma(spectrum: DoubleArray, sampleRate: Int): DoubleArray {
        val chroma = DoubleArray(12)
        
        for (bin in 1 until spectrum.size / 2) {
            val freq = bin * sampleRate.toDouble() / (spectrum.size * 2)
            
            // Skip frequencies outside musical range
            if (freq < 60 || freq > 4000) continue
            
            // Convert frequency to pitch class (0-11)
            val midiNote = 12 * (ln(freq / 440.0) / ln(2.0)) + 69
            val pitchClass = ((midiNote.roundToInt() % 12) + 12) % 12
            
            chroma[pitchClass] += spectrum[bin]
        }
        
        // Normalize
        val sum = chroma.sum()
        if (sum > 0) {
            for (i in 0 until 12) chroma[i] /= sum
        }
        
        return chroma
    }
    
    /**
     * Detect key using Krumhansl-Schmuckler algorithm
     */
    private fun detectKeyFromChroma(chroma: DoubleArray): Pair<MusicalKey, Float> {
        var bestKey = MusicalKey("C", true)
        var bestCorr = -1.0
        
        // Test all 24 keys (12 major + 12 minor)
        for (root in 0 until 12) {
            // Rotate chroma to current root
            val rotatedChroma = DoubleArray(12) { chroma[(it + root) % 12] }
            
            // Correlate with major profile
            val majorCorr = pearsonCorrelation(rotatedChroma, MAJOR_PROFILE)
            if (majorCorr > bestCorr) {
                bestCorr = majorCorr
                bestKey = MusicalKey(KEY_NAMES[root], true)
            }
            
            // Correlate with minor profile
            val minorCorr = pearsonCorrelation(rotatedChroma, MINOR_PROFILE)
            if (minorCorr > bestCorr) {
                bestCorr = minorCorr
                bestKey = MusicalKey(KEY_NAMES[root], false)
            }
        }
        
        val confidence = ((bestCorr + 1) / 2 * 100).toFloat().coerceIn(0f, 100f)
        Log.d(TAG, "Detected key: $bestKey (confidence: $confidence%)")
        
        return Pair(bestKey, confidence)
    }
    
    // ==================== SPECTRAL FEATURES ====================
    
    private fun calculateEnergy(audio: DoubleArray): Float {
        val rms = sqrt(audio.map { it * it }.average())
        return (rms * 10).toFloat().coerceIn(0f, 1f)
    }
    
    private fun analyzeSpectralFeatures(
        audio: DoubleArray, 
        sampleRate: Int
    ): SpectralFeatures {
        val frameSize = 2048
        var centroidSum = 0.0
        var flatnessSum = 0.0
        var zcrSum = 0.0
        var frameCount = 0
        
        var pos = 0
        while (pos + frameSize <= audio.size) {
            val frame = DoubleArray(frameSize) { audio[pos + it] }
            val spectrum = computeFFT(frame)
            
            // Spectral centroid
            var num = 0.0
            var den = 0.0
            for (i in 0 until frameSize / 2) {
                val freq = i * sampleRate.toDouble() / frameSize
                num += freq * spectrum[i]
                den += spectrum[i]
            }
            if (den > 0) centroidSum += num / den
            
            // Spectral flatness (geometric mean / arithmetic mean)
            val magnitudes = spectrum.take(frameSize / 2).filter { it > 1e-10 }
            if (magnitudes.isNotEmpty()) {
                val geometricMean = kotlin.math.exp(magnitudes.map { ln(it) }.average())
                val arithmeticMean = magnitudes.average()
                if (arithmeticMean > 0) {
                    flatnessSum += geometricMean / arithmeticMean
                }
            }
            
            // Zero crossing rate
            var zcr = 0
            for (i in 1 until frameSize) {
                if ((frame[i] >= 0 && frame[i-1] < 0) || (frame[i] < 0 && frame[i-1] >= 0)) {
                    zcr++
                }
            }
            zcrSum += zcr.toDouble() / frameSize
            
            frameCount++
            pos += frameSize / 2
        }
        
        return SpectralFeatures(
            centroid = if (frameCount > 0) (centroidSum / frameCount).toFloat() else 0f,
            flatness = if (frameCount > 0) (flatnessSum / frameCount).toFloat() else 0f,
            zeroCrossingRate = if (frameCount > 0) (zcrSum / frameCount).toFloat() else 0f
        )
    }
    
    // ==================== HIGH-LEVEL FEATURES ====================
    
    private fun calculateDanceability(
        bpm: Double,
        energy: Float,
        spectral: SpectralFeatures
    ): Float {
        // Danceability formula based on BPM, energy, rhythm strength
        val bpmScore = when {
            bpm in 115.0..135.0 -> 1.0  // Optimal dance range
            bpm in 100.0..150.0 -> 0.8
            bpm in 90.0..160.0 -> 0.6
            else -> 0.4
        }
        
        val energyScore = energy.coerceIn(0f, 1f)
        val rhythmScore = 1 - spectral.flatness.coerceIn(0f, 1f)
        
        return ((bpmScore * 0.4 + energyScore * 0.3 + rhythmScore * 0.3) * 100).toFloat()
    }
    
    private fun estimateValence(key: MusicalKey, spectral: SpectralFeatures): Float {
        // Valence estimation (happiness) based on:
        // - Major keys tend to sound happier
        // - Higher spectral centroid = brighter sound
        val keyValence = if (key.isMajor) 0.7f else 0.3f
        val brightnessValence = (spectral.centroid / 4000f).coerceIn(0f, 1f)
        
        return (keyValence * 0.6f + brightnessValence * 0.4f) * 100
    }
    
    // ==================== DSP UTILITIES ====================
    
    /**
     * Compute FFT using Cooley-Tukey algorithm
     * Returns magnitude spectrum
     */
    private fun computeFFT(input: DoubleArray): DoubleArray {
        val n = input.size
        if (n == 1) return doubleArrayOf(abs(input[0]))
        
        // Bit-reversal permutation
        val real = input.copyOf()
        val imag = DoubleArray(n)
        
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val temp = real[i]
                real[i] = real[j]
                real[j] = temp
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }
        
        // Cooley-Tukey FFT
        var mmax = 1
        while (n > mmax) {
            val step = mmax * 2
            val theta = -PI / mmax
            var wr = 1.0
            var wi = 0.0
            val wpr = cos(theta)
            val wpi = sin(theta)
            
            for (m in 0 until mmax) {
                for (i in m until n step step) {
                    val j2 = i + mmax
                    val tr = wr * real[j2] - wi * imag[j2]
                    val ti = wr * imag[j2] + wi * real[j2]
                    real[j2] = real[i] - tr
                    imag[j2] = imag[i] - ti
                    real[i] += tr
                    imag[i] += ti
                }
                val wtemp = wr
                wr = wr * wpr - wi * wpi
                wi = wi * wpr + wtemp * wpi
            }
            mmax = step
        }
        
        // Return magnitude spectrum
        return DoubleArray(n) { sqrt(real[it] * real[it] + imag[it] * imag[it]) }
    }
    
    /**
     * Pearson correlation coefficient
     */
    private fun pearsonCorrelation(x: DoubleArray, y: DoubleArray): Double {
        val n = minOf(x.size, y.size)
        val meanX = x.take(n).average()
        val meanY = y.take(n).average()
        
        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        
        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        
        val den = sqrt(denX * denY)
        return if (den > 0) num / den else 0.0
    }
    
    private fun List<Double>.standardDeviation(): Double {
        if (size < 2) return 0.0
        val mean = average()
        return sqrt(map { (it - mean) * (it - mean) }.average())
    }
}

// ==================== DATA CLASSES ====================

data class MusicalKey(
    val root: String,
    val isMajor: Boolean
) {
    override fun toString(): String = "$root ${if (isMajor) "Major" else "Minor"}"
    
    val camelotCode: String
        get() {
            val majorCodes = mapOf(
                "C" to "8B", "G" to "9B", "D" to "10B", "A" to "11B",
                "E" to "12B", "B" to "1B", "F#" to "2B", "C#" to "3B",
                "G#" to "4B", "D#" to "5B", "A#" to "6B", "F" to "7B"
            )
            val minorCodes = mapOf(
                "A" to "8A", "E" to "9A", "B" to "10A", "F#" to "11A",
                "C#" to "12A", "G#" to "1A", "D#" to "2A", "A#" to "3A",
                "F" to "4A", "C" to "5A", "G" to "6A", "D" to "7A"
            )
            return if (isMajor) majorCodes[root] ?: "?" else minorCodes[root] ?: "?"
        }
    
    /**
     * Check if this key is harmonically compatible with another key
     */
    fun isCompatibleWith(other: MusicalKey): Boolean {
        val thisCamelot = camelotCode
        val otherCamelot = other.camelotCode
        
        if (thisCamelot == "?" || otherCamelot == "?") return false
        
        val thisNum = thisCamelot.dropLast(1).toIntOrNull() ?: return false
        val otherNum = otherCamelot.dropLast(1).toIntOrNull() ?: return false
        val thisLetter = thisCamelot.last()
        val otherLetter = otherCamelot.last()
        
        // Same key
        if (thisNum == otherNum && thisLetter == otherLetter) return true
        
        // Relative major/minor
        if (thisNum == otherNum && thisLetter != otherLetter) return true
        
        // Adjacent keys (+/-1 on wheel)
        val diff = abs(thisNum - otherNum)
        if ((diff == 1 || diff == 11) && thisLetter == otherLetter) return true
        
        return false
    }
}

data class SpectralFeatures(
    val centroid: Float,
    val flatness: Float,
    val zeroCrossingRate: Float
)

data class AudioAnalysisResult(
    val bpm: Double,
    val bpmConfidence: Float,
    val key: MusicalKey,
    val keyConfidence: Float,
    val energy: Float,
    val spectralCentroid: Float,
    val spectralFlatness: Float,
    val zeroCrossingRate: Float,
    val danceability: Float,
    val valence: Float
) {
    val energyLevel: EnergyLevel
        get() = when {
            energy < 0.3f -> EnergyLevel.LOW
            energy < 0.6f -> EnergyLevel.MEDIUM
            else -> EnergyLevel.HIGH
        }
    
    val mood: Mood
        get() = when {
            valence > 60 && energy > 0.5 -> Mood.HAPPY
            valence > 60 && energy <= 0.5 -> Mood.CHILL
            valence <= 60 && energy > 0.5 -> Mood.AGGRESSIVE
            else -> Mood.SAD
        }
}

enum class EnergyLevel { LOW, MEDIUM, HIGH }
enum class Mood { HAPPY, SAD, AGGRESSIVE, CHILL }
