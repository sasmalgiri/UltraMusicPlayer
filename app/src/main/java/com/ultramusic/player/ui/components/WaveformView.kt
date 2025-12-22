package com.ultramusic.player.ui.components

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * REAL WAVEFORM VISUALIZATION
 * 
 * Decodes audio file and displays actual waveform.
 * Shows:
 * - Full waveform in background
 * - Progress position
 * - A-B loop markers
 * - Tap to seek
 */
@Composable
fun WaveformView(
    audioPath: String?,
    currentPosition: Long,
    duration: Long,
    abLoopStart: Long? = null,
    abLoopEnd: Long? = null,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    waveformColor: Color = Color(0xFF4CAF50),
    progressColor: Color = Color(0xFFE91E63),
    backgroundColor: Color = Color(0xFF1A1A1A),
    loopColor: Color = Color(0xFF2196F3)
) {
    val context = LocalContext.current
    var waveformData by remember { mutableStateOf<FloatArray?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Load waveform when path changes
    LaunchedEffect(audioPath) {
        if (audioPath != null && waveformData == null) {
            isLoading = true
            waveformData = withContext(Dispatchers.Default) {
                extractWaveform(context, audioPath)
            }
            isLoading = false
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    if (duration > 0) {
                        val progress = offset.x / size.width
                        val seekPosition = (progress * duration).toLong()
                        onSeek(seekPosition.coerceIn(0, duration))
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            
            // Draw waveform
            waveformData?.let { data ->
                val samplesPerPixel = data.size / width.toInt()
                
                for (x in 0 until width.toInt()) {
                    val startSample = (x * samplesPerPixel).coerceIn(0, data.size - 1)
                    val endSample = ((x + 1) * samplesPerPixel).coerceIn(0, data.size)
                    
                    // Get max amplitude in this pixel range
                    var maxAmp = 0f
                    for (i in startSample until endSample) {
                        maxAmp = max(maxAmp, abs(data[i]))
                    }
                    
                    val barHeight = maxAmp * height * 0.8f
                    
                    // Determine color based on progress
                    val xProgress = x / width
                    val currentProgress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                    
                    val color = if (xProgress <= currentProgress) {
                        progressColor
                    } else {
                        waveformColor.copy(alpha = 0.5f)
                    }
                    
                    // Draw bar
                    drawLine(
                        color = color,
                        start = Offset(x.toFloat(), centerY - barHeight / 2),
                        end = Offset(x.toFloat(), centerY + barHeight / 2),
                        strokeWidth = 2f
                    )
                }
            } ?: run {
                // Placeholder waveform when loading
                for (x in 0 until width.toInt() step 3) {
                    val progress = x / width
                    val barHeight = (20 + (kotlin.math.sin(x * 0.1) * 15)).toFloat()
                    
                    val currentProgress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                    val color = if (progress <= currentProgress) {
                        progressColor.copy(alpha = 0.3f)
                    } else {
                        waveformColor.copy(alpha = 0.2f)
                    }
                    
                    drawLine(
                        color = color,
                        start = Offset(x.toFloat(), centerY - barHeight),
                        end = Offset(x.toFloat(), centerY + barHeight),
                        strokeWidth = 2f
                    )
                }
            }
            
            // Draw A-B loop markers
            if (abLoopStart != null && duration > 0) {
                val startX = (abLoopStart.toFloat() / duration) * width
                
                // A marker line
                drawLine(
                    color = loopColor,
                    start = Offset(startX, 0f),
                    end = Offset(startX, height),
                    strokeWidth = 3f
                )
                
                // A label background
                drawCircle(
                    color = loopColor,
                    radius = 10f,
                    center = Offset(startX, 12f)
                )
            }
            
            if (abLoopEnd != null && duration > 0) {
                val endX = (abLoopEnd.toFloat() / duration) * width
                
                // B marker line
                drawLine(
                    color = loopColor,
                    start = Offset(endX, 0f),
                    end = Offset(endX, height),
                    strokeWidth = 3f
                )
                
                // B label background
                drawCircle(
                    color = loopColor,
                    radius = 10f,
                    center = Offset(endX, 12f)
                )
            }
            
            // Draw loop region highlight
            if (abLoopStart != null && abLoopEnd != null && duration > 0) {
                val startX = (abLoopStart.toFloat() / duration) * width
                val endX = (abLoopEnd.toFloat() / duration) * width
                
                drawRect(
                    color = loopColor.copy(alpha = 0.15f),
                    topLeft = Offset(startX, 0f),
                    size = androidx.compose.ui.geometry.Size(endX - startX, height)
                )
            }
            
            // Draw playhead
            if (duration > 0) {
                val playheadX = (currentPosition.toFloat() / duration) * width
                drawLine(
                    color = Color.White,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, height),
                    strokeWidth = 2f
                )
            }
        }
    }
}

/**
 * Extract waveform data from audio file
 * Returns normalized float array of amplitudes
 */
private fun extractWaveform(context: Context, path: String): FloatArray? {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    
    try {
        extractor.setDataSource(path)
        
        // Find audio track
        var audioTrackIndex = -1
        var format: MediaFormat? = null
        
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
                format = trackFormat
                break
            }
        }
        
        if (audioTrackIndex < 0 || format == null) {
            return null
        }
        
        extractor.selectTrack(audioTrackIndex)
        
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        
        // Create decoder
        decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()
        
        val samples = mutableListOf<Float>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        
        // Limit to reasonable size for waveform (downsample to ~4000 points)
        val targetSamples = 4000
        val maxRawSamples = sampleRate * channels * 300 // Max 5 min
        
        while (!outputDone && samples.size < maxRawSamples) {
            // Feed input
            if (!inputDone) {
                val inputIndex = decoder.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = decoder.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
            }
            
            // Get output
            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val outputBuffer = decoder.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    val shortBuffer = outputBuffer.asShortBuffer()
                    while (shortBuffer.hasRemaining() && samples.size < maxRawSamples) {
                        samples.add(shortBuffer.get() / 32768f)
                    }
                }
                decoder.releaseOutputBuffer(outputIndex, false)
                
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone = true
                }
            }
        }
        
        if (samples.isEmpty()) return null
        
        // Downsample to target size
        val result = FloatArray(targetSamples)
        val samplesPerBucket = samples.size / targetSamples
        
        for (i in 0 until targetSamples) {
            val start = i * samplesPerBucket
            val end = min((i + 1) * samplesPerBucket, samples.size)
            
            var maxAmp = 0f
            for (j in start until end) {
                maxAmp = max(maxAmp, abs(samples[j]))
            }
            result[i] = maxAmp
        }
        
        // Normalize
        val maxValue = result.maxOrNull() ?: 1f
        if (maxValue > 0) {
            for (i in result.indices) {
                result[i] = result[i] / maxValue
            }
        }
        
        return result
        
    } catch (e: Exception) {
        Log.e("WaveformView", "Failed to extract waveform", e)
        return null
    } finally {
        try {
            decoder?.stop()
            decoder?.release()
        } catch (e: Exception) { }
        extractor.release()
    }
}

/**
 * Mini waveform for song list items
 */
@Composable
fun MiniWaveform(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF4CAF50),
    inactiveColor: Color = Color(0xFF333333)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barCount = 30
        val barWidth = width / barCount * 0.6f
        val gap = width / barCount * 0.4f
        
        for (i in 0 until barCount) {
            val x = i * (barWidth + gap) + barWidth / 2
            val barProgress = i.toFloat() / barCount
            
            // Pseudo-random height based on position
            val seed = (i * 7 + 13) % 17
            val barHeight = (height * 0.3f + (seed / 17f) * height * 0.5f)
            
            val color = if (barProgress <= progress) activeColor else inactiveColor
            
            drawLine(
                color = color,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth
            )
        }
    }
}
