package com.ultramusic.player.core

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.ultramusic.player.data.Song
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
import kotlin.math.sqrt

/**
 * LOCAL BATTLE ANALYZER
 * 
 * The REAL battle system for sound system clashes:
 * 
 * 1. LISTENS to opponent's LOUD music via microphone
 * 2. ANALYZES frequencies, BPM, energy in real-time
 * 3. INSTANT counter from PRE-INDEXED library (no searching!)
 * 4. SUGGESTS best counter based on opponent's weaknesses
 * 
 * Works with:
 * - Custom dubplates
 * - DJ mixes
 * - Unknown/unreleased tracks
 * - Raw beats/bass music
 * - ANY audio the opponent plays
 * 
 * NO external API needed - 100% LOCAL analysis!
 */
@Singleton
class LocalBattleAnalyzer @Inject constructor(
    private val context: Context,
    private val battleSongDatabase: BattleSongDatabase
) {
    companion object {
        private const val TAG = "LocalBattleAnalyzer"
        private const val SAMPLE_RATE = 44100
        private const val FFT_SIZE = 4096
        private const val HOP_SIZE = 1024
        
        // Frequency bands for analysis
        private const val SUB_BASS_LOW = 20
        private const val SUB_BASS_HIGH = 60
        private const val BASS_LOW = 60
        private const val BASS_HIGH = 250
        private const val LOW_MID_LOW = 250
        private const val LOW_MID_HIGH = 500
        private const val MID_LOW = 500
        private const val MID_HIGH = 2000
        private const val HIGH_MID_LOW = 2000
        private const val HIGH_MID_HIGH = 4000
        private const val HIGH_LOW = 4000
        private const val HIGH_HIGH = 20000
    }
    
    private val scope = CoroutineScope(Dispatchers.Default)
    private var analysisJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var vibrator: Vibrator? = null
    
    // Using BattleSongDatabase for INSTANT pre-indexed lookups!
    // No more searching - songs are already analyzed and categorized
    
    // ==================== OPPONENT ANALYSIS STATE ====================
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _opponentProfile = MutableStateFlow(OpponentAudioProfile())
    val opponentProfile: StateFlow<OpponentAudioProfile> = _opponentProfile.asStateFlow()
    
    private val _frequencyBands = MutableStateFlow(FrequencyBands())
    val frequencyBands: StateFlow<FrequencyBands> = _frequencyBands.asStateFlow()
    
    private val _detectedBPM = MutableStateFlow(0.0)
    val detectedBPM: StateFlow<Double> = _detectedBPM.asStateFlow()
    
    private val _bpmConfidence = MutableStateFlow(0f)
    val bpmConfidence: StateFlow<Float> = _bpmConfidence.asStateFlow()
    
    private val _overallLoudness = MutableStateFlow(-60f)
    val overallLoudness: StateFlow<Float> = _overallLoudness.asStateFlow()
    
    private val _dominantFrequencyRange = MutableStateFlow(FrequencyRange.UNKNOWN)
    val dominantFrequencyRange: StateFlow<FrequencyRange> = _dominantFrequencyRange.asStateFlow()
    
    private val _weakFrequencyRange = MutableStateFlow(FrequencyRange.UNKNOWN)
    val weakFrequencyRange: StateFlow<FrequencyRange> = _weakFrequencyRange.asStateFlow()
    
    // ==================== COUNTER SUGGESTIONS ====================
    
    private val _counterSuggestions = MutableStateFlow<List<CounterSuggestion>>(emptyList())
    val counterSuggestions: StateFlow<List<CounterSuggestion>> = _counterSuggestions.asStateFlow()
    
    private val _topCounter = MutableStateFlow<CounterSuggestion?>(null)
    val topCounter: StateFlow<CounterSuggestion?> = _topCounter.asStateFlow()
    
    private val _counterStrategy = MutableStateFlow<CounterStrategy?>(null)
    val counterStrategy: StateFlow<CounterStrategy?> = _counterStrategy.asStateFlow()
    
    // ==================== BATTLE STATUS ====================
    
    private val _battleAdvice = MutableStateFlow("")
    val battleAdvice: StateFlow<String> = _battleAdvice.asStateFlow()
    
    private val _attackOpportunity = MutableStateFlow(false)
    val attackOpportunity: StateFlow<Boolean> = _attackOpportunity.asStateFlow()
    
    // Internal buffers
    private var audioBuffer = DoubleArray(FFT_SIZE)
    private var onsetBuffer = mutableListOf<Double>()
    private var bpmHistory = mutableListOf<Double>()
    private var loudnessHistory = mutableListOf<Float>()
    
    // ==================== INITIALIZATION ====================
    
    init {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    /**
     * Index your music library for battle (call once when app starts)
     * Uses BattleSongDatabase for persistent, pre-indexed storage
     */
    suspend fun indexLibrary(songs: List<Song>) {
        battleSongDatabase.indexLibrary(songs)
        Log.i(TAG, "Library indexed: ${battleSongDatabase.indexedCount.value} songs ready for battle")
    }
    
    /**
     * Check if library is indexed
     */
    fun isLibraryIndexed(): Boolean = battleSongDatabase.indexedSongs.value.isNotEmpty()
    
    /**
     * Get indexing progress
     */
    val indexingProgress = battleSongDatabase.indexProgress
    val isIndexing = battleSongDatabase.isIndexing
    val indexedCount = battleSongDatabase.indexedCount
    
    /**
     * Analyze a song file and create its profile
     * Call this to build your battle library
     */
    suspend fun analyzeSongForBattle(song: Song, audioSamples: FloatArray): SongProfile {
        val doubles = DoubleArray(audioSamples.size) { audioSamples[it].toDouble() }
        
        // Detect BPM
        val bpm = detectBPM(doubles)
        
        // Analyze frequency distribution
        val bands = analyzeFrequencyBands(doubles)
        
        // Calculate energy
        val energy = calculateEnergy(doubles)
        
        return SongProfile(
            bpm = bpm,
            energy = energy,
            bassStrength = (bands.subBass + bands.bass) / 2f,
            midStrength = (bands.lowMid + bands.mid) / 2f,
            highStrength = (bands.highMid + bands.high) / 2f,
            tags = extractTagsFromTitle(song.title)
        )
    }
    
    // ==================== MAIN LISTENING FUNCTIONS ====================
    
    /**
     * Start listening to opponent's music
     */
    fun startListening() {
        if (_isListening.value) {
            Log.w(TAG, "Already listening")
            return
        }
        
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
            _isListening.value = true
            
            // Clear history
            onsetBuffer.clear()
            bpmHistory.clear()
            loudnessHistory.clear()
            
            analysisJob = scope.launch {
                val buffer = ShortArray(HOP_SIZE)
                
                while (isActive && _isListening.value) {
                    val read = audioRecord?.read(buffer, 0, HOP_SIZE) ?: 0
                    
                    if (read > 0) {
                        // Shift buffer and add new samples
                        System.arraycopy(audioBuffer, read, audioBuffer, 0, FFT_SIZE - read)
                        for (i in 0 until read) {
                            audioBuffer[FFT_SIZE - read + i] = buffer[i] / 32768.0
                        }
                        
                        // Analyze opponent audio
                        analyzeOpponentAudio()
                        
                        // Update counter suggestions periodically
                        if (onsetBuffer.size % 20 == 0) {
                            updateCounterSuggestions()
                        }
                    }
                    
                    delay(20) // ~50 FPS analysis
                }
            }
            
            Log.i(TAG, "Started listening to opponent")
            _battleAdvice.value = "🎧 Listening to opponent..."
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Microphone permission denied", e)
            _battleAdvice.value = "❌ Microphone permission needed!"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
        }
    }
    
    /**
     * Stop listening
     */
    fun stopListening() {
        _isListening.value = false
        analysisJob?.cancel()
        
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping", e)
        }
        
        audioRecord = null
        _battleAdvice.value = "⏹️ Stopped listening"
        Log.i(TAG, "Stopped listening")
    }
    
    // ==================== REAL-TIME ANALYSIS ====================
    
    private fun analyzeOpponentAudio() {
        // Apply Hanning window
        val windowed = DoubleArray(FFT_SIZE) { i ->
            val window = 0.5 * (1 - cos(2 * PI * i / (FFT_SIZE - 1)))
            audioBuffer[i] * window
        }
        
        // Compute FFT
        val spectrum = computeFFT(windowed)
        
        // ===== LOUDNESS =====
        val rms = sqrt(audioBuffer.map { it * it }.average())
        val db = if (rms > 0) (20 * log10(rms)).toFloat() else -60f
        _overallLoudness.value = db.coerceIn(-60f, 0f)
        
        loudnessHistory.add(db)
        if (loudnessHistory.size > 100) loudnessHistory.removeAt(0)
        
        // ===== FREQUENCY BANDS =====
        val bands = calculateBandsFromSpectrum(spectrum)
        _frequencyBands.value = bands
        
        // ===== DOMINANT & WEAK FREQUENCIES =====
        val (dominant, weak) = findDominantAndWeak(bands)
        _dominantFrequencyRange.value = dominant
        _weakFrequencyRange.value = weak
        
        // ===== ONSET DETECTION (for BPM) =====
        val onset = calculateOnsetStrength(spectrum)
        onsetBuffer.add(onset)
        if (onsetBuffer.size > 500) onsetBuffer.removeAt(0)
        
        // ===== BPM DETECTION =====
        if (onsetBuffer.size >= 200) {
            val bpm = detectBPMFromOnsets()
            if (bpm > 0) {
                bpmHistory.add(bpm)
                if (bpmHistory.size > 20) bpmHistory.removeAt(0)
                
                val avgBpm = bpmHistory.average()
                _detectedBPM.value = avgBpm
                _bpmConfidence.value = calculateBPMConfidence()
            }
        }
        
        // ===== UPDATE OPPONENT PROFILE =====
        _opponentProfile.value = OpponentAudioProfile(
            loudnessDb = db,
            bpm = _detectedBPM.value,
            bpmConfidence = _bpmConfidence.value,
            dominantRange = dominant,
            weakRange = weak,
            bands = bands,
            energy = ((db + 60) / 60f).coerceIn(0f, 1f),
            isPlaying = db > -40f,
            timestamp = System.currentTimeMillis()
        )
        
        // ===== ATTACK OPPORTUNITY =====
        detectAttackOpportunity()
        
        // ===== UPDATE ADVICE =====
        updateBattleAdvice()
    }
    
    private fun calculateBandsFromSpectrum(spectrum: DoubleArray): FrequencyBands {
        val binWidth = SAMPLE_RATE.toDouble() / FFT_SIZE
        
        fun bandEnergy(lowFreq: Int, highFreq: Int): Float {
            val lowBin = (lowFreq / binWidth).toInt().coerceIn(0, spectrum.size / 2 - 1)
            val highBin = (highFreq / binWidth).toInt().coerceIn(0, spectrum.size / 2 - 1)
            
            var sum = 0.0
            for (i in lowBin..highBin) {
                sum += spectrum[i] * spectrum[i]
            }
            return sqrt(sum / (highBin - lowBin + 1)).toFloat()
        }
        
        val subBass = bandEnergy(SUB_BASS_LOW, SUB_BASS_HIGH)
        val bass = bandEnergy(BASS_LOW, BASS_HIGH)
        val lowMid = bandEnergy(LOW_MID_LOW, LOW_MID_HIGH)
        val mid = bandEnergy(MID_LOW, MID_HIGH)
        val highMid = bandEnergy(HIGH_MID_LOW, HIGH_MID_HIGH)
        val high = bandEnergy(HIGH_LOW, HIGH_HIGH)
        
        // Normalize
        val max = maxOf(subBass, bass, lowMid, mid, highMid, high, 0.001f)
        
        return FrequencyBands(
            subBass = subBass / max,
            bass = bass / max,
            lowMid = lowMid / max,
            mid = mid / max,
            highMid = highMid / max,
            high = high / max
        )
    }
    
    private fun findDominantAndWeak(bands: FrequencyBands): Pair<FrequencyRange, FrequencyRange> {
        val values = listOf(
            FrequencyRange.SUB_BASS to bands.subBass,
            FrequencyRange.BASS to bands.bass,
            FrequencyRange.LOW_MID to bands.lowMid,
            FrequencyRange.MID to bands.mid,
            FrequencyRange.HIGH_MID to bands.highMid,
            FrequencyRange.HIGH to bands.high
        )
        
        val sorted = values.sortedByDescending { it.second }
        return Pair(sorted.first().first, sorted.last().first)
    }
    
    private var previousSpectrum = DoubleArray(FFT_SIZE / 2)
    
    private fun calculateOnsetStrength(spectrum: DoubleArray): Double {
        var flux = 0.0
        for (i in 0 until minOf(spectrum.size / 2, previousSpectrum.size)) {
            val diff = spectrum[i] - previousSpectrum[i]
            if (diff > 0) flux += diff * diff
        }
        
        System.arraycopy(spectrum, 0, previousSpectrum, 0, minOf(spectrum.size / 2, previousSpectrum.size))
        return sqrt(flux)
    }
    
    private fun detectBPMFromOnsets(): Double {
        if (onsetBuffer.size < 100) return 0.0
        
        val onsets = onsetBuffer.toDoubleArray()
        
        // Normalize
        val max = onsets.maxOrNull() ?: 1.0
        val normalized = onsets.map { it / max }.toDoubleArray()
        
        // Auto-correlation
        val minLag = (60.0 / 200.0 * SAMPLE_RATE / HOP_SIZE).toInt() // 200 BPM max
        val maxLag = (60.0 / 60.0 * SAMPLE_RATE / HOP_SIZE).toInt() // 60 BPM min
        
        var bestLag = minLag
        var bestCorr = 0.0
        
        for (lag in minLag until minOf(maxLag, normalized.size / 2)) {
            var corr = 0.0
            for (i in 0 until normalized.size - lag) {
                corr += normalized[i] * normalized[i + lag]
            }
            corr /= (normalized.size - lag)
            
            if (corr > bestCorr) {
                bestCorr = corr
                bestLag = lag
            }
        }
        
        val bpm = 60.0 * SAMPLE_RATE / (bestLag * HOP_SIZE)
        
        // Adjust to reasonable range
        return when {
            bpm < 70 -> bpm * 2
            bpm > 180 -> bpm / 2
            else -> bpm
        }
    }
    
    private fun calculateBPMConfidence(): Float {
        if (bpmHistory.size < 5) return 0f
        
        val mean = bpmHistory.average()
        val variance = bpmHistory.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)
        
        // Lower std dev = higher confidence
        return (100 - stdDev * 5).toFloat().coerceIn(0f, 100f)
    }
    
    // ==================== COUNTER SUGGESTIONS ====================
    
    private fun updateCounterSuggestions() {
        val indexed = battleSongDatabase.indexedSongs.value
        if (indexed.isEmpty()) {
            _counterSuggestions.value = emptyList()
            _battleAdvice.value = "⚠️ Library not indexed - tap 'Index Library' first!"
            return
        }
        
        val opponent = _opponentProfile.value
        if (!opponent.isPlaying) {
            _battleAdvice.value = "⏸️ Opponent is quiet - prepare your attack!"
            return
        }
        
        // INSTANT lookup from pre-indexed database! No searching!
        val matches = battleSongDatabase.getInstantCounters(
            opponentBpm = opponent.bpm,
            opponentDominant = opponent.dominantRange,
            opponentWeak = opponent.weakRange,
            opponentEnergy = opponent.energy,
            limit = 5
        )
        
        // Convert to CounterSuggestion format
        val suggestions = matches.map { match ->
            CounterSuggestion(
                song = Song(
                    id = match.song.songId,
                    title = match.song.title,
                    artist = match.song.artist,
                    album = "",
                    duration = match.song.duration,
                    uri = android.net.Uri.parse(match.song.path),
                    albumArtUri = null,
                    path = match.song.path,
                    dateAdded = 0
                ),
                profile = SongProfile(
                    bpm = match.song.bpm,
                    energy = match.song.energy,
                    bassStrength = match.song.bassStrength,
                    midStrength = match.song.midStrength,
                    highStrength = match.song.highStrength,
                    tags = match.song.tags
                ),
                score = match.score,
                reasons = match.matchReasons,
                strategy = match.song.category.displayName
            )
        }
        
        _counterSuggestions.value = suggestions
        _topCounter.value = suggestions.firstOrNull()
        
        // Determine counter strategy
        _counterStrategy.value = determineStrategy(opponent)
    }
    
    /**
     * Get songs by category (instant - no analysis needed)
     */
    fun getBassKillers() = battleSongDatabase.bassKillers.value
    fun getMidCutters() = battleSongDatabase.midCutters.value
    fun getEnergyBombs() = battleSongDatabase.energyBombs.value
    fun getTopBattleSongs(limit: Int = 20) = battleSongDatabase.getTopBattleSongs(limit)
    fun getSongsByBpm(targetBpm: Double) = battleSongDatabase.getSongsByBpm(targetBpm)
    
    private fun determineStrategy(opponent: OpponentAudioProfile): CounterStrategy {
        return when {
            opponent.dominantRange in listOf(FrequencyRange.SUB_BASS, FrequencyRange.BASS) &&
            opponent.weakRange in listOf(FrequencyRange.MID, FrequencyRange.HIGH_MID) -> {
                CounterStrategy(
                    name = "MID CUT THROUGH",
                    description = "Opponent is bass-heavy with weak mids. Use tracks with strong presence (1-4kHz) to cut through their bass wall.",
                    eqAdvice = "Boost 1kHz-3kHz, keep your bass solid",
                    icon = "🔪"
                )
            }
            opponent.dominantRange in listOf(FrequencyRange.MID, FrequencyRange.HIGH_MID) -> {
                CounterStrategy(
                    name = "BASS OVERPOWER",
                    description = "Opponent lacks low end. Overwhelm them with sub-bass and bass.",
                    eqAdvice = "Max bass boost, cut mids slightly",
                    icon = "💣"
                )
            }
            opponent.energy < 0.5f -> {
                CounterStrategy(
                    name = "ENERGY DOMINATION",
                    description = "Opponent is playing soft. Hit them with high energy tracks!",
                    eqAdvice = "Full loudness, balanced EQ",
                    icon = "⚡"
                )
            }
            else -> {
                CounterStrategy(
                    name = "FULL ASSAULT",
                    description = "Match their energy and exceed on all frequencies.",
                    eqAdvice = "Balanced boost across all bands",
                    icon = "🔥"
                )
            }
        }
    }
    
    // ==================== BATTLE ADVICE ====================
    
    private fun detectAttackOpportunity() {
        val opponent = _opponentProfile.value
        val current = _overallLoudness.value
        
        // Detect drop in opponent's energy (end of track, breakdown, etc.)
        val avgLoudness = if (loudnessHistory.isNotEmpty()) loudnessHistory.average() else current.toDouble()
        
        val isOpportunity = current < avgLoudness - 10 || // Sudden drop
                           current < -30f || // Very quiet
                           !opponent.isPlaying // Not playing
        
        if (isOpportunity && !_attackOpportunity.value) {
            // New opportunity!
            _attackOpportunity.value = true
            vibrateAlert()
        } else if (!isOpportunity) {
            _attackOpportunity.value = false
        }
    }
    
    private fun updateBattleAdvice() {
        val opponent = _opponentProfile.value
        
        _battleAdvice.value = when {
            !opponent.isPlaying -> "⏸️ Opponent quiet - ATTACK NOW!"
            _attackOpportunity.value -> "🎯 ATTACK OPPORTUNITY! Drop your track!"
            opponent.bpmConfidence > 70f -> "🎵 Opponent BPM: ${opponent.bpm.roundToInt()} | ${opponent.dominantRange.displayName} heavy"
            opponent.energy > 0.7f -> "🔊 Opponent going HARD - match their energy!"
            else -> "👂 Analyzing opponent: ${opponent.dominantRange.displayName} dominant"
        }
    }
    
    private fun vibrateAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 100, 50, 100), -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }
    
    // ==================== FFT ====================
    
    private fun computeFFT(input: DoubleArray): DoubleArray {
        val n = input.size
        val real = input.copyOf()
        val imag = DoubleArray(n)
        
        // Bit reversal
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
        
        // FFT
        var mmax = 1
        while (n > mmax) {
            val step = mmax * 2
            val theta = -PI / mmax
            var wr = 1.0
            var wi = 0.0
            val wpr = cos(theta)
            val wpi = kotlin.math.sin(theta)
            
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
        
        return DoubleArray(n) { sqrt(real[it] * real[it] + imag[it] * imag[it]) }
    }
    
    // ==================== HELPERS ====================
    
    private fun detectBPM(audio: DoubleArray): Double {
        // Simplified BPM detection for file analysis
        val hopSize = 512
        val frameSize = 2048
        val onsets = mutableListOf<Double>()
        var prev = DoubleArray(frameSize / 2)
        
        var pos = 0
        while (pos + frameSize <= audio.size) {
            val frame = DoubleArray(frameSize) { i ->
                if (pos + i < audio.size) audio[pos + i] else 0.0
            }
            val spectrum = computeFFT(frame)
            
            var flux = 0.0
            for (i in 0 until frameSize / 2) {
                val diff = spectrum[i] - prev[i]
                if (diff > 0) flux += diff
            }
            onsets.add(flux)
            prev = spectrum.take(frameSize / 2).toDoubleArray()
            pos += hopSize
        }
        
        if (onsets.size < 50) return 0.0
        
        // Simple peak detection for BPM
        val threshold = onsets.average() + onsets.standardDeviation()
        val peaks = mutableListOf<Int>()
        for (i in 1 until onsets.size - 1) {
            if (onsets[i] > threshold && onsets[i] > onsets[i-1] && onsets[i] > onsets[i+1]) {
                peaks.add(i)
            }
        }
        
        if (peaks.size < 4) return 0.0
        
        // Calculate intervals
        val intervals = mutableListOf<Int>()
        for (i in 1 until peaks.size) {
            intervals.add(peaks[i] - peaks[i-1])
        }
        
        val avgInterval = intervals.average()
        val bpm = 60.0 * SAMPLE_RATE / (avgInterval * hopSize)
        
        return when {
            bpm < 70 -> bpm * 2
            bpm > 180 -> bpm / 2
            else -> bpm
        }
    }
    
    private fun analyzeFrequencyBands(audio: DoubleArray): FrequencyBands {
        val frameSize = 4096
        val bands = mutableListOf<FrequencyBands>()
        
        var pos = 0
        while (pos + frameSize <= audio.size) {
            val frame = DoubleArray(frameSize) { audio[pos + it] }
            val spectrum = computeFFT(frame)
            bands.add(calculateBandsFromSpectrum(spectrum))
            pos += frameSize / 2
        }
        
        if (bands.isEmpty()) return FrequencyBands()
        
        return FrequencyBands(
            subBass = bands.map { it.subBass }.average().toFloat(),
            bass = bands.map { it.bass }.average().toFloat(),
            lowMid = bands.map { it.lowMid }.average().toFloat(),
            mid = bands.map { it.mid }.average().toFloat(),
            highMid = bands.map { it.highMid }.average().toFloat(),
            high = bands.map { it.high }.average().toFloat()
        )
    }
    
    private fun calculateEnergy(audio: DoubleArray): Float {
        val rms = sqrt(audio.map { it * it }.average())
        return (rms * 10).toFloat().coerceIn(0f, 1f)
    }
    
    private fun estimateBPMFromTitle(title: String): Double? {
        // Try to extract BPM from filename like "Track_140bpm.mp3"
        val bpmRegex = Regex("(\\d{2,3})\\s*bpm", RegexOption.IGNORE_CASE)
        val match = bpmRegex.find(title)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
    
    private fun extractTagsFromTitle(title: String): List<String> {
        val tags = mutableListOf<String>()
        val lower = title.lowercase()
        
        if (lower.contains("bass")) tags.add("bass")
        if (lower.contains("dub")) tags.add("dubplate")
        if (lower.contains("battle")) tags.add("battle")
        if (lower.contains("remix")) tags.add("remix")
        if (lower.contains("drop")) tags.add("drop")
        if (lower.contains("heavy")) tags.add("heavy")
        
        return tags
    }
    
    private fun List<Double>.standardDeviation(): Double {
        if (size < 2) return 0.0
        val mean = average()
        return sqrt(map { (it - mean).pow(2) }.average())
    }
}

// ==================== DATA CLASSES ====================

data class OpponentAudioProfile(
    val loudnessDb: Float = -60f,
    val bpm: Double = 0.0,
    val bpmConfidence: Float = 0f,
    val dominantRange: FrequencyRange = FrequencyRange.UNKNOWN,
    val weakRange: FrequencyRange = FrequencyRange.UNKNOWN,
    val bands: FrequencyBands = FrequencyBands(),
    val energy: Float = 0f,
    val isPlaying: Boolean = false,
    val timestamp: Long = 0
)

data class FrequencyBands(
    val subBass: Float = 0f,  // 20-60 Hz
    val bass: Float = 0f,     // 60-250 Hz
    val lowMid: Float = 0f,   // 250-500 Hz
    val mid: Float = 0f,      // 500-2000 Hz
    val highMid: Float = 0f,  // 2000-4000 Hz
    val high: Float = 0f      // 4000-20000 Hz
)

enum class FrequencyRange(val displayName: String) {
    SUB_BASS("Sub Bass (20-60Hz)"),
    BASS("Bass (60-250Hz)"),
    LOW_MID("Low Mid (250-500Hz)"),
    MID("Mid (500-2kHz)"),
    HIGH_MID("High Mid (2-4kHz)"),
    HIGH("High (4-20kHz)"),
    UNKNOWN("Unknown")
}

data class SongProfile(
    val bpm: Double?,
    val energy: Float,
    val bassStrength: Float,
    val midStrength: Float,
    val highStrength: Float,
    val tags: List<String> = emptyList()
)

data class CounterSuggestion(
    val song: Song,
    val profile: SongProfile,
    val score: Float,
    val reasons: List<String>,
    val strategy: String
)

data class CounterStrategy(
    val name: String,
    val description: String,
    val eqAdvice: String,
    val icon: String
)
