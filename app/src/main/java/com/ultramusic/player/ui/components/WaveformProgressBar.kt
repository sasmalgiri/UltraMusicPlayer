package com.ultramusic.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ultramusic.player.ui.theme.*
import kotlin.math.abs
import kotlin.math.sin

/**
 * Waveform Progress Bar
 * 
 * Displays audio waveform with:
 * - Progress indicator
 * - Loop region overlay
 * - Touch-to-seek functionality
 * - Loop point selection
 */
@Composable
fun WaveformProgressBar(
    currentPosition: Long,
    duration: Long,
    isLooping: Boolean,
    loopStart: Long,
    loopEnd: Long,
    onSeek: (Long) -> Unit,
    onLoopRegionChange: (Long, Long) -> Unit,
    modifier: Modifier = Modifier,
    waveformData: FloatArray? = null  // Actual waveform data if available
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
    val loopStartProgress = if (duration > 0) loopStart.toFloat() / duration else 0f
    val loopEndProgress = if (duration > 0) loopEnd.toFloat() / duration else 0f
    
    var isDragging by remember { mutableStateOf(false) }
    var isSettingLoopStart by remember { mutableStateOf(false) }
    
    val playedColor = WaveformPlayedColor
    val unplayedColor = WaveformColor.copy(alpha = 0.4f)
    val loopColor = LoopRegionColor
    val cursorColor = MaterialTheme.colorScheme.primary
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val seekProgress = offset.x / size.width
                        val seekPosition = (seekProgress * duration).toLong()
                        onSeek(seekPosition)
                    },
                    onDoubleTap = { offset ->
                        // Double tap to set loop point
                        val tapProgress = offset.x / size.width
                        val tapPosition = (tapProgress * duration).toLong()
                        
                        if (isSettingLoopStart) {
                            // Setting loop end
                            val newEnd = maxOf(loopStart, tapPosition)
                            val newStart = minOf(loopStart, tapPosition)
                            onLoopRegionChange(newStart, newEnd)
                            isSettingLoopStart = false
                        } else {
                            // Setting loop start
                            onLoopRegionChange(tapPosition, tapPosition)
                            isSettingLoopStart = true
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDrag = { change, _ ->
                        val seekProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        val seekPosition = (seekProgress * duration).toLong()
                        onSeek(seekPosition)
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        // Draw loop region if active
        if (isLooping && loopEnd > loopStart) {
            drawRect(
                color = loopColor,
                topLeft = Offset(width * loopStartProgress, 0f),
                size = Size(width * (loopEndProgress - loopStartProgress), height)
            )
        }
        
        // Draw waveform
        if (waveformData != null && waveformData.isNotEmpty()) {
            drawActualWaveform(waveformData, progress, playedColor, unplayedColor, width, height, centerY)
        } else {
            drawSimulatedWaveform(progress, playedColor, unplayedColor, width, height, centerY)
        }
        
        // Draw progress cursor
        val cursorX = width * progress
        drawLine(
            color = cursorColor,
            start = Offset(cursorX, 0f),
            end = Offset(cursorX, height),
            strokeWidth = 3.dp.toPx()
        )
        
        // Draw cursor head
        drawCircle(
            color = cursorColor,
            radius = 6.dp.toPx(),
            center = Offset(cursorX, 0f)
        )
        
        // Draw loop markers
        if (isLooping && loopEnd > loopStart) {
            // Loop start marker
            drawLine(
                color = MaterialTheme.colorScheme.secondary,
                start = Offset(width * loopStartProgress, 0f),
                end = Offset(width * loopStartProgress, height),
                strokeWidth = 2.dp.toPx()
            )
            
            // Loop end marker
            drawLine(
                color = MaterialTheme.colorScheme.secondary,
                start = Offset(width * loopEndProgress, 0f),
                end = Offset(width * loopEndProgress, height),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

private fun DrawScope.drawSimulatedWaveform(
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    width: Float,
    height: Float,
    centerY: Float
) {
    val barCount = 100
    val barWidth = width / barCount * 0.6f
    val spacing = width / barCount
    
    for (i in 0 until barCount) {
        val x = i * spacing
        val normalizedX = i.toFloat() / barCount
        
        // Generate pseudo-random but consistent waveform
        val amplitude = (
            0.3f + 
            0.4f * abs(sin(i * 0.5f)) + 
            0.2f * abs(sin(i * 1.3f)) + 
            0.1f * abs(sin(i * 2.7f))
        ).coerceIn(0.1f, 1f)
        
        val barHeight = height * 0.8f * amplitude
        
        val color = if (normalizedX <= progress) playedColor else unplayedColor
        
        // Draw bar (symmetric around center)
        drawRect(
            color = color,
            topLeft = Offset(x, centerY - barHeight / 2),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawActualWaveform(
    waveformData: FloatArray,
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    width: Float,
    height: Float,
    centerY: Float
) {
    val barCount = minOf(waveformData.size, 200)
    val samplesPerBar = waveformData.size / barCount
    val barWidth = width / barCount * 0.7f
    val spacing = width / barCount
    
    for (i in 0 until barCount) {
        val x = i * spacing
        val normalizedX = i.toFloat() / barCount
        
        // Calculate average amplitude for this bar
        var amplitude = 0f
        val startSample = i * samplesPerBar
        val endSample = minOf(startSample + samplesPerBar, waveformData.size)
        
        for (j in startSample until endSample) {
            amplitude += abs(waveformData[j])
        }
        amplitude /= (endSample - startSample).coerceAtLeast(1)
        amplitude = amplitude.coerceIn(0.05f, 1f)
        
        val barHeight = height * 0.9f * amplitude
        val color = if (normalizedX <= progress) playedColor else unplayedColor
        
        drawRect(
            color = color,
            topLeft = Offset(x, centerY - barHeight / 2),
            size = Size(barWidth, barHeight)
        )
    }
}

/**
 * Circular progress indicator for mini player
 */
@Composable
fun CircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 4f,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val diameter = minOf(size.width, size.height)
        val radius = diameter / 2 - strokeWidth
        
        // Background circle
        drawCircle(
            color = backgroundColor,
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        
        // Progress arc
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(strokeWidth, strokeWidth),
            size = Size(diameter - strokeWidth * 2, diameter - strokeWidth * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
    }
}
