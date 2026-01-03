package com.ultramusic.player.ui.components

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * - A-B loop markers with labels
 * - Drag to seek or drag markers to adjust loop
 */
@Composable
fun WaveformView(
    audioPath: String?,
    currentPosition: Long,
    duration: Long,
    abLoopStart: Long? = null,
    abLoopEnd: Long? = null,
    onSeek: (Long) -> Unit = {},
    onLoopStartChange: (Long) -> Unit = {},
    onLoopEndChange: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    lockPlayheadToCenter: Boolean = false,
    enableTapToSeek: Boolean = true,
    waveformColor: Color = Color(0xFF4CAF50),
    progressColor: Color = Color(0xFFE91E63),
    backgroundColor: Color = Color(0xFF1A1A1A),
    loopColor: Color = Color(0xFF2196F3)
) {
    val context = LocalContext.current
    var waveformData by remember { mutableStateOf<FloatArray?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Drag state
    var isDraggingA by remember { mutableStateOf(false) }
    var isDraggingB by remember { mutableStateOf(false) }
    var isDraggingPlayhead by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    // Update drag position when not dragging
    val currentProgress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    if (!isDraggingPlayhead) {
        dragPosition = currentProgress
    }

    // Center progress used for centered-playhead mode.
    // When not scrubbing, it tracks the actual playback position.
    val centerProgress = if (lockPlayheadToCenter) {
        if (isDraggingPlayhead) dragPosition else currentProgress
    } else {
        currentProgress
    }

    // Marker positions as fractions
    val loopStartPos = if (abLoopStart != null && duration > 0) abLoopStart.toFloat() / duration else null
    val loopEndPos = if (abLoopEnd != null && duration > 0) abLoopEnd.toFloat() / duration else null

    // Marker colors
    val markerAColor = Color(0xFF00E5FF)  // Cyan
    val markerBColor = Color(0xFFFFEA00)  // Yellow

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
            .height(100.dp)  // Increased height for floating timestamp
    ) {
        // Floating timestamp during drag
        if (isDraggingPlayhead || isDraggingA || isDraggingB) {
            val displayPos = when {
                isDraggingA -> loopStartPos ?: 0f
                isDraggingB -> loopEndPos ?: 1f
                else -> dragPosition
            }
            val displayTimeMs = (displayPos * duration).toLong()
            val offsetFraction = displayPos.coerceIn(0.05f, 0.95f)

            Box(
                modifier = Modifier
                    .fillMaxWidth(offsetFraction)
                    .align(Alignment.TopStart)
            ) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isDraggingA -> markerAColor
                        isDraggingB -> markerBColor
                        else -> Color.White
                    },
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = formatTimeWithMs(displayTimeMs),
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Main waveform canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        if (duration > 0 && (!lockPlayheadToCenter || enableTapToSeek)) {
                            val progress = offset.x / size.width
                            val seekPosition = (progress * duration).toLong()
                            onSeek(seekPosition.coerceIn(0, duration))
                        }
                    }
                }
                .pointerInput(duration, abLoopStart, abLoopEnd) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (duration > 0) {
                                val pos = offset.x / size.width
                                val threshold = 0.08f  // 8% touch target

                                val distToA = loopStartPos?.let {
                                    val markerPos = if (lockPlayheadToCenter) {
                                        (0.5f + (it - centerProgress)).coerceIn(0f, 1f)
                                    } else {
                                        it
                                    }
                                    abs(pos - markerPos)
                                } ?: Float.MAX_VALUE
                                val distToB = loopEndPos?.let {
                                    val markerPos = if (lockPlayheadToCenter) {
                                        (0.5f + (it - centerProgress)).coerceIn(0f, 1f)
                                    } else {
                                        it
                                    }
                                    abs(pos - markerPos)
                                } ?: Float.MAX_VALUE

                                when {
                                    distToA < threshold && distToA <= distToB -> {
                                        isDraggingA = true
                                    }
                                    distToB < threshold && distToB < distToA -> {
                                        isDraggingB = true
                                    }
                                    else -> {
                                        isDraggingPlayhead = true
                                        if (lockPlayheadToCenter) {
                                            // Center playhead stays fixed; waveform scrubs under it.
                                            // Initialize dragPosition to currentProgress so subsequent deltas adjust it.
                                            dragPosition = currentProgress
                                        } else {
                                            dragPosition = pos.coerceIn(0f, 1f)
                                            onSeek((dragPosition * duration).toLong())
                                        }
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (duration > 0) {
                                val pos = (change.position.x / size.width).coerceIn(0f, 1f)
                                when {
                                    isDraggingA -> {
                                        val rawPos = if (lockPlayheadToCenter) {
                                            (centerProgress + (pos - 0.5f)).coerceIn(0f, 1f)
                                        } else {
                                            pos
                                        }
                                        val maxPos = loopEndPos?.minus(0.0005f) ?: 1f
                                        val newPos = rawPos.coerceIn(0f, maxPos)
                                        onLoopStartChange((newPos * duration).toLong())
                                    }
                                    isDraggingB -> {
                                        val rawPos = if (lockPlayheadToCenter) {
                                            (centerProgress + (pos - 0.5f)).coerceIn(0f, 1f)
                                        } else {
                                            pos
                                        }
                                        val minPos = loopStartPos?.plus(0.0005f) ?: 0f
                                        val newPos = rawPos.coerceIn(minPos, 1f)
                                        onLoopEndChange((newPos * duration).toLong())
                                    }
                                    isDraggingPlayhead -> {
                                        if (lockPlayheadToCenter) {
                                            // Dragging waveform: deltaX > 0 means move waveform right => go backwards.
                                            val deltaProgress = (dragAmount.x / size.width)
                                            dragPosition = (dragPosition - deltaProgress).coerceIn(0f, 1f)
                                            onSeek((dragPosition * duration).toLong())
                                        } else {
                                            dragPosition = pos
                                            onSeek((pos * duration).toLong())
                                        }
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            isDraggingA = false
                            isDraggingB = false
                            isDraggingPlayhead = false
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2

            fun progressToX(progress: Float): Float {
                val clamped = progress.coerceIn(0f, 1f)
                return if (lockPlayheadToCenter) {
                    width * (0.5f + (clamped - centerProgress))
                } else {
                    clamped * width
                }
            }
            
            // Draw waveform
            waveformData?.let { data ->
                val wInt = max(1, width.toInt())
                val samplesPerPixel = max(1, data.size / wInt)

                for (x in 0 until wInt) {
                    val timeFraction = if (lockPlayheadToCenter) {
                        (centerProgress + (x.toFloat() / width - 0.5f)).coerceIn(0f, 1f)
                    } else {
                        (x.toFloat() / width).coerceIn(0f, 1f)
                    }

                    val centerSample = (timeFraction * (data.size - 1)).toInt().coerceIn(0, data.size - 1)
                    val startSample = (centerSample - samplesPerPixel / 2).coerceIn(0, data.size - 1)
                    val endSample = (centerSample + samplesPerPixel / 2).coerceIn(0, data.size)

                    var maxAmp = 0f
                    for (i in startSample until endSample) {
                        maxAmp = max(maxAmp, abs(data[i]))
                    }

                    val barHeight = maxAmp * height * 0.8f
                    val color = if (timeFraction <= centerProgress) {
                        progressColor
                    } else {
                        waveformColor.copy(alpha = 0.5f)
                    }

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
                    val color = if (!lockPlayheadToCenter && progress <= currentProgress) {
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
            
            // Draw loop region highlight (behind markers)
            if (abLoopStart != null && abLoopEnd != null && duration > 0) {
                val startX = progressToX(abLoopStart.toFloat() / duration)
                val endX = progressToX(abLoopEnd.toFloat() / duration)
                val left = min(startX, endX).coerceIn(0f, width)
                val right = max(startX, endX).coerceIn(0f, width)

                drawRect(
                    color = Color(0xFFFF9800).copy(alpha = 0.2f),  // Orange highlight
                    topLeft = Offset(left, 0f),
                    size = Size((right - left).coerceAtLeast(0f), height)
                )
            }

            // Draw A marker with label
            if (abLoopStart != null && duration > 0) {
                val startX = progressToX(abLoopStart.toFloat() / duration).coerceIn(0f, width)

                // A marker line
                drawLine(
                    color = markerAColor,
                    start = Offset(startX, 0f),
                    end = Offset(startX, height),
                    strokeWidth = 3f
                )

                // A label background (rounded rect)
                val labelSize = 20f
                drawRoundRect(
                    color = markerAColor,
                    topLeft = Offset(startX - labelSize / 2, height / 2 - labelSize / 2),
                    size = Size(labelSize, labelSize),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Draw "A" text using native canvas
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 14f * density
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawText("A", startX, height / 2 + 5f * density, paint)
                }
            }

            // Draw B marker with label
            if (abLoopEnd != null && duration > 0) {
                val endX = progressToX(abLoopEnd.toFloat() / duration).coerceIn(0f, width)

                // B marker line
                drawLine(
                    color = markerBColor,
                    start = Offset(endX, 0f),
                    end = Offset(endX, height),
                    strokeWidth = 3f
                )

                // B label background (rounded rect)
                val labelSize = 20f
                drawRoundRect(
                    color = markerBColor,
                    topLeft = Offset(endX - labelSize / 2, height / 2 - labelSize / 2),
                    size = Size(labelSize, labelSize),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Draw "B" text using native canvas
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 14f * density
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    drawText("B", endX, height / 2 + 5f * density, paint)
                }
            }

            // Draw playhead
            if (duration > 0) {
                val playheadX = if (lockPlayheadToCenter) width / 2f else (currentPosition.toFloat() / duration) * width
                drawLine(
                    color = Color.White,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, height),
                    strokeWidth = 2f
                )

                // Playhead triangle at top
                val triangleSize = 8f
                val trianglePath = Path().apply {
                    moveTo(playheadX, 0f)
                    lineTo(playheadX - triangleSize, -triangleSize)
                    lineTo(playheadX + triangleSize, -triangleSize)
                    close()
                }
                // Note: Triangle is above canvas, not visible in current height
            }
        }
    }
}

/**
 * Format time with milliseconds: m:ss.ms
 */
private fun formatTimeWithMs(ms: Long): String {
    val minutes = ms / 60000
    val seconds = (ms % 60000) / 1000
    val millis = ms % 1000
    return "%d:%02d.%03d".format(minutes, seconds, millis)
}

/**
 * Extract waveform data from audio file
 * Returns normalized float array of amplitudes
 *
 * MEMORY OPTIMIZED: Samples on-the-fly to avoid OOM
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

        // Get duration to calculate sampling interval
        val durationUs = format.getLong(MediaFormat.KEY_DURATION)
        val durationSec = durationUs / 1_000_000.0

        // Create decoder
        decoder = MediaCodec.createDecoderByType(mime)
        decoder.configure(format, null, null, 0)
        decoder.start()

        // MEMORY OPTIMIZED: Fixed-size result array, sample on-the-fly
        val targetSamples = 2000  // Reduced from 4000 for less memory
        val result = FloatArray(targetSamples)
        val sampleCounts = IntArray(targetSamples)  // Count samples per bucket

        // Calculate samples per bucket (total expected samples / target buckets)
        val totalExpectedSamples = (sampleRate * channels * durationSec).toLong()
        val samplesPerBucket = max(1L, totalExpectedSamples / targetSamples)

        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var sampleIndex = 0L

        // Limit processing time (max 10 seconds of processing)
        val startTime = System.currentTimeMillis()
        val maxProcessingTime = 10_000L

        while (!outputDone && (System.currentTimeMillis() - startTime) < maxProcessingTime) {
            // Feed input
            if (!inputDone) {
                val inputIndex = decoder.dequeueInputBuffer(5000)
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

            // Get output and sample directly into result array
            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 5000)
            if (outputIndex >= 0) {
                val outputBuffer = decoder.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    val shortBuffer = outputBuffer.asShortBuffer()

                    while (shortBuffer.hasRemaining()) {
                        val sample = abs(shortBuffer.get() / 32768f)
                        val bucketIndex = ((sampleIndex / samplesPerBucket) % targetSamples).toInt()

                        // Keep max amplitude in bucket
                        if (sample > result[bucketIndex]) {
                            result[bucketIndex] = sample
                        }
                        sampleCounts[bucketIndex]++
                        sampleIndex++
                    }
                }
                decoder.releaseOutputBuffer(outputIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone = true
                }
            }
        }

        // Check if we got any data
        val validBuckets = sampleCounts.count { it > 0 }
        if (validBuckets < 10) return null

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
