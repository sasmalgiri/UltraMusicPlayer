package com.ultramusic.player.audio

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Software Limiter AudioProcessor
 *
 * Provides brickwall limiting for devices without DynamicsProcessing (pre-API 28).
 * Uses lookahead-free limiting with smooth attack/release characteristics.
 *
 * This prevents audio clipping when multiple effects are maxed:
 * - Bass boost
 * - Loudness enhancer
 * - EQ boosts
 * - Compressor makeup gain
 *
 * Features:
 * - Brickwall limiting (nothing exceeds ceiling)
 * - Smooth envelope following to prevent artifacts
 * - Configurable threshold and ceiling
 * - Fast attack for transient control
 * - Medium release for natural sound
 */
@UnstableApi
class SoftwareLimiter : AudioProcessor {

    companion object {
        private const val TAG = "SoftwareLimiter"

        // Default settings optimized for music
        private const val DEFAULT_THRESHOLD_DB = -1f    // Start limiting at -1dB
        private const val DEFAULT_CEILING_DB = -0.3f    // Hard limit at -0.3dB
        private const val DEFAULT_ATTACK_MS = 0.5f      // Very fast attack
        private const val DEFAULT_RELEASE_MS = 50f      // Medium release

        // PCM constants
        private const val MAX_16BIT = 32767f
        private const val MIN_16BIT = -32768f
    }

    // Limiter settings
    private var thresholdDb = DEFAULT_THRESHOLD_DB
    private var ceilingDb = DEFAULT_CEILING_DB
    private var attackMs = DEFAULT_ATTACK_MS
    private var releaseMs = DEFAULT_RELEASE_MS
    private var enabled = true

    // Audio format
    private var inputFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputFormat: AudioFormat = AudioFormat.NOT_SET
    private var sampleRate = 44100
    private var channelCount = 2

    // Processing state
    private var envelope = 0f
    private var attackCoeff = 0f
    private var releaseCoeff = 0f
    private var thresholdLinear = 0f
    private var ceilingLinear = 0f

    // Buffers
    private var outputBuffer: ByteBuffer = EMPTY_BUFFER
    private var inputEnded = false

    // Statistics for monitoring
    private var peakReductionDb = 0f
    private var samplesProcessed = 0L

    /**
     * Set the threshold where limiting begins (dB)
     * @param db Threshold in dB, typically -3 to 0
     */
    fun setThreshold(db: Float) {
        thresholdDb = db.coerceIn(-12f, 0f)
        updateCoefficients()
        Log.d(TAG, "Threshold set to ${thresholdDb}dB")
    }

    /**
     * Set the absolute ceiling - nothing exceeds this level (dB)
     * @param db Ceiling in dB, typically -0.5 to 0
     */
    fun setCeiling(db: Float) {
        ceilingDb = db.coerceIn(-3f, 0f)
        updateCoefficients()
        Log.d(TAG, "Ceiling set to ${ceilingDb}dB")
    }

    /**
     * Set attack time - how quickly limiting responds to peaks
     * @param ms Attack time in milliseconds, typically 0.1 to 10
     */
    fun setAttack(ms: Float) {
        attackMs = ms.coerceIn(0.1f, 10f)
        updateCoefficients()
    }

    /**
     * Set release time - how quickly limiting recovers
     * @param ms Release time in milliseconds, typically 10 to 500
     */
    fun setRelease(ms: Float) {
        releaseMs = ms.coerceIn(10f, 500f)
        updateCoefficients()
    }

    /**
     * Enable or disable the limiter
     */
    fun setEnabled(isEnabled: Boolean) {
        enabled = isEnabled
        Log.d(TAG, "Limiter ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Get the peak gain reduction applied (for metering)
     */
    fun getPeakReductionDb(): Float = peakReductionDb

    /**
     * Reset peak reduction meter
     */
    fun resetPeakMeter() {
        peakReductionDb = 0f
    }

    private fun updateCoefficients() {
        // Convert dB to linear amplitude
        thresholdLinear = dbToLinear(thresholdDb)
        ceilingLinear = dbToLinear(ceilingDb)

        // Calculate attack/release coefficients
        // Using exponential smoothing: y[n] = coeff * y[n-1] + (1 - coeff) * x[n]
        // Time constant tau = -1 / (sampleRate * ln(coeff))
        // coeff = exp(-1 / (tau * sampleRate))
        val attackSamples = (attackMs * sampleRate / 1000f)
        val releaseSamples = (releaseMs * sampleRate / 1000f)

        attackCoeff = if (attackSamples > 0.1f) {
            exp(-1f / attackSamples).toFloat()
        } else {
            0f // Instant attack
        }

        releaseCoeff = if (releaseSamples > 0.1f) {
            exp(-1f / releaseSamples).toFloat()
        } else {
            0.99f // Very slow release
        }

        Log.d(TAG, "Coefficients updated: threshold=${thresholdLinear}, ceiling=${ceilingLinear}, " +
                  "attack=${attackCoeff}, release=${releaseCoeff}")
    }

    private fun dbToLinear(db: Float): Float = 10.0.pow(db / 20.0).toFloat()

    private fun linearToDb(linear: Float): Float {
        return if (linear > 0.0001f) {
            (20 * kotlin.math.log10(linear.toDouble())).toFloat()
        } else {
            -80f // Floor
        }
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        // We only support 16-bit PCM
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            Log.w(TAG, "Unsupported encoding: ${inputAudioFormat.encoding}, limiter disabled")
            return AudioFormat.NOT_SET
        }

        inputFormat = inputAudioFormat
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        outputFormat = inputAudioFormat

        // Recalculate coefficients with new sample rate
        updateCoefficients()

        // Reset state
        envelope = 0f
        peakReductionDb = 0f
        samplesProcessed = 0L

        Log.i(TAG, "Configured: ${sampleRate}Hz, ${channelCount}ch, 16-bit PCM")
        return outputFormat
    }

    override fun isActive(): Boolean = enabled && outputFormat != AudioFormat.NOT_SET

    override fun queueInput(buffer: ByteBuffer) {
        if (!enabled || buffer.remaining() == 0) {
            // Pass through unchanged
            outputBuffer = buffer
            return
        }

        // Process the buffer
        val inputBytes = ByteArray(buffer.remaining())
        buffer.get(inputBytes)

        val processedBytes = processAudio(inputBytes)

        outputBuffer = ByteBuffer.wrap(processedBytes).order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun processAudio(inputBytes: ByteArray): ByteArray {
        val outputBytes = ByteArray(inputBytes.size)
        val inputBuffer = ByteBuffer.wrap(inputBytes).order(ByteOrder.LITTLE_ENDIAN)
        val outputByteBuffer = ByteBuffer.wrap(outputBytes).order(ByteOrder.LITTLE_ENDIAN)

        val numSamples = inputBytes.size / 2 // 16-bit = 2 bytes per sample
        val numFrames = numSamples / channelCount

        var maxReduction = 0f

        for (frame in 0 until numFrames) {
            // Find peak amplitude across all channels for this frame
            var peak = 0f
            val frameStart = frame * channelCount

            for (ch in 0 until channelCount) {
                val sampleIndex = frameStart + ch
                if (sampleIndex < numSamples) {
                    val sample = inputBuffer.getShort(sampleIndex * 2)
                    val normalized = sample / MAX_16BIT
                    peak = max(peak, abs(normalized))
                }
            }

            // Update envelope (peak follower with attack/release)
            val coeff = if (peak > envelope) attackCoeff else releaseCoeff
            envelope = coeff * envelope + (1 - coeff) * peak

            // Calculate gain reduction needed
            val gainReduction = if (envelope > thresholdLinear) {
                // Soft knee limiting: smoothly transition into limiting
                val ratio = thresholdLinear / envelope
                // Scale to ceiling
                min(ratio * (ceilingLinear / thresholdLinear), 1f)
            } else {
                1f // No reduction needed
            }

            // Track peak reduction for metering
            if (gainReduction < 1f) {
                val reductionDb = linearToDb(gainReduction)
                maxReduction = min(maxReduction, reductionDb)
            }

            // Apply gain reduction to all channels
            for (ch in 0 until channelCount) {
                val sampleIndex = frameStart + ch
                if (sampleIndex < numSamples) {
                    val inputSample = inputBuffer.getShort(sampleIndex * 2).toFloat()
                    var outputSample = inputSample * gainReduction

                    // Hard clip at ceiling as absolute safety (should rarely trigger)
                    val maxOutput = MAX_16BIT * ceilingLinear
                    outputSample = outputSample.coerceIn(-maxOutput, maxOutput)

                    outputByteBuffer.putShort(sampleIndex * 2, outputSample.toInt().toShort())
                }
            }
        }

        samplesProcessed += numSamples

        // Update peak reduction meter (keep the worst case)
        if (maxReduction < peakReductionDb) {
            peakReductionDb = maxReduction
        }

        return outputBytes
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === EMPTY_BUFFER

    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        inputEnded = false
        envelope = 0f
    }

    override fun reset() {
        flush()
        enabled = true
        thresholdDb = DEFAULT_THRESHOLD_DB
        ceilingDb = DEFAULT_CEILING_DB
        attackMs = DEFAULT_ATTACK_MS
        releaseMs = DEFAULT_RELEASE_MS
        peakReductionDb = 0f
        samplesProcessed = 0L
        updateCoefficients()
        Log.d(TAG, "Limiter reset to defaults")
    }

    /**
     * Get statistics string for debugging
     */
    fun getStats(): String {
        return buildString {
            append("SoftwareLimiter Stats:\n")
            append("  Enabled: $enabled\n")
            append("  Threshold: ${thresholdDb}dB\n")
            append("  Ceiling: ${ceilingDb}dB\n")
            append("  Peak Reduction: ${String.format("%.1f", peakReductionDb)}dB\n")
            append("  Samples Processed: $samplesProcessed\n")
            append("  Current Envelope: ${String.format("%.4f", envelope)}")
        }
    }
}

private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.LITTLE_ENDIAN)
