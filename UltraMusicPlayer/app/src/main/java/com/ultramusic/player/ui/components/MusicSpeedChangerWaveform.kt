package com.ultramusic.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max

/**
 * Music Speed Changer Style Waveform
 *
 * Features:
 * - Simple rounded bar visualization
 * - Clean A-B markers with draggable handles
 * - Draggable playhead with floating timestamp
 * - Smooth seek functionality
 */
@Composable
fun MusicSpeedChangerWaveform(
    waveformData: List<Float>,
    currentPosition: Float,  // 0-1
    durationMs: Long,
    loopStartMs: Long?,
    loopEndMs: Long?,
    onSeek: (Float) -> Unit,
    onLoopStartChange: (Long) -> Unit,
    onLoopEndChange: (Long) -> Unit,
    onClearLoop: () -> Unit,
    lockPlayheadToCenter: Boolean = true,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    barColor: Color = Color(0xFF4FC3F7),       // Light blue for unplayed
    playedColor: Color = Color(0xFF00E676),     // Green for played
    loopRegionColor: Color = Color(0xFFFF9800), // Orange for loop region
    backgroundColor: Color = Color(0xFF1A1A1A)
) {
    var isDraggingA by remember { mutableStateOf(false) }
    var isDraggingB by remember { mutableStateOf(false) }
    var isDraggingPlayhead by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(currentPosition) }

    // Update drag position when not dragging
    if (!isDraggingPlayhead) {
        dragPosition = currentPosition
    }

    val loopStartPos = loopStartMs?.let { it.toFloat() / durationMs } ?: 0f
    val loopEndPos = loopEndMs?.let { it.toFloat() / durationMs } ?: 1f

    // Drag detection threshold (percentage of width) for A/B markers
    val markerDragThreshold = 0.08f  // 8% for A/B markers

    Column(modifier = modifier) {
        // Waveform canvas with floating timestamp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height + 24.dp)  // Extra space for floating timestamp
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Floating timestamp that follows playhead
            val displayPosition = if (isDraggingPlayhead) dragPosition else currentPosition
            val displayTimeMs = (displayPosition * durationMs).toLong()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color.Transparent)
            ) {
                // Calculate offset for the timestamp pill
                val offsetFraction = (if (lockPlayheadToCenter) 0.5f else displayPosition)
                    .coerceIn(0.05f, 0.95f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth(offsetFraction)
                        .align(Alignment.CenterStart)
                ) {
                    Surface(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDraggingPlayhead) Color(0xFF00E676) else Color.White,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = formatTimeMs(displayTimeMs),
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Main waveform canvas
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .align(Alignment.BottomCenter)
                    .background(backgroundColor, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val displayPos = if (isDraggingPlayhead) dragPosition else currentPosition
                            val translateX = if (lockPlayheadToCenter) {
                                (0.5f - displayPos) * size.width
                            } else {
                                0f
                            }

                            val pos = ((offset.x - translateX) / size.width).coerceIn(0f, 1f)
                            onSeek(pos)
                        }
                    }
                    .pointerInput(loopStartMs, loopEndMs, currentPosition, lockPlayheadToCenter) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val translateX = if (lockPlayheadToCenter) {
                                    (0.5f - currentPosition) * size.width
                                } else {
                                    0f
                                }

                                val pos = ((offset.x - translateX) / size.width).coerceIn(0f, 1f)
                                val distToA = if (loopStartMs != null) abs(pos - loopStartPos) else 1f
                                val distToB = if (loopEndMs != null) abs(pos - loopEndPos) else 1f
                                val distToPlayhead = abs(pos - currentPosition)

                                when {
                                    // A marker has priority if very close
                                    distToA < markerDragThreshold && distToA <= distToB && distToA <= distToPlayhead -> {
                                        isDraggingA = true
                                    }
                                    // B marker
                                    distToB < markerDragThreshold && distToB < distToA && distToB <= distToPlayhead -> {
                                        isDraggingB = true
                                    }
                                    // Playhead is draggable anywhere else
                                    else -> {
                                        isDraggingPlayhead = true

                                        if (lockPlayheadToCenter) {
                                            dragPosition = currentPosition
                                        } else {
                                            dragPosition = pos
                                            onSeek(dragPosition)
                                        }
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                val translateX = if (lockPlayheadToCenter) {
                                    (0.5f - (if (isDraggingPlayhead) dragPosition else currentPosition)) * size.width
                                } else {
                                    0f
                                }

                                val pos = ((change.position.x - translateX) / size.width).coerceIn(0f, 1f)
                                when {
                                    isDraggingA -> {
                                        val maxPos = if (loopEndMs != null) loopEndPos - 0.01f else 1f
                                        val newPos = pos.coerceIn(0f, maxPos)
                                        onLoopStartChange((newPos * durationMs).toLong())
                                    }
                                    isDraggingB -> {
                                        val minPos = if (loopStartMs != null) loopStartPos + 0.01f else 0f
                                        val newPos = pos.coerceIn(minPos, 1f)
                                        onLoopEndChange((newPos * durationMs).toLong())
                                    }
                                    isDraggingPlayhead -> {
                                        if (lockPlayheadToCenter) {
                                            val deltaPos = (-dragAmount.x / size.width)
                                            dragPosition = (dragPosition + deltaPos).coerceIn(0f, 1f)
                                            onSeek(dragPosition)
                                        } else {
                                            dragPosition = pos
                                            onSeek(pos)
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
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerY = canvasHeight / 2

                val displayPos = if (isDraggingPlayhead) dragPosition else currentPosition
                val translateX = if (lockPlayheadToCenter) {
                    (0.5f - displayPos) * canvasWidth
                } else {
                    0f
                }

                // Number of bars to draw
                val numBars = if (waveformData.isNotEmpty()) waveformData.size else 100
                val barWidth = canvasWidth / numBars
                val barGap = 2.dp.toPx()
                val actualBarWidth = (barWidth - barGap).coerceAtLeast(2.dp.toPx())
                val cornerRadius = actualBarWidth / 2

                withTransform({
                    translate(left = translateX, top = 0f)
                }) {
                    // Draw loop region background
                    if (loopStartMs != null && loopEndMs != null) {
                        val startX = loopStartPos * canvasWidth
                        val endX = loopEndPos * canvasWidth
                        drawRect(
                            color = loopRegionColor.copy(alpha = 0.2f),
                            topLeft = Offset(startX, 0f),
                            size = Size(endX - startX, canvasHeight)
                        )
                    }

                    // Draw waveform bars
                    for (i in 0 until numBars) {
                        val amplitude = if (waveformData.isNotEmpty()) {
                            waveformData.getOrElse(i) { 0.3f }
                        } else {
                            // Generate fake waveform if no data
                            0.3f + (kotlin.math.sin(i * 0.15f) * 0.3f) + (kotlin.random.Random.nextFloat() * 0.2f)
                        }

                        val barHeight = (amplitude * (canvasHeight - 20.dp.toPx()) * 0.9f).coerceAtLeast(4.dp.toPx())
                        val x = i * barWidth + barGap / 2
                        val position = i.toFloat() / numBars

                        // Determine bar color
                        val color = when {
                            position <= displayPos -> playedColor
                            loopStartMs != null && loopEndMs != null &&
                                position >= loopStartPos && position <= loopEndPos -> loopRegionColor.copy(alpha = 0.7f)
                            else -> barColor.copy(alpha = 0.6f)
                        }

                        // Draw rounded bar (pill shape)
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, centerY - barHeight / 2),
                            size = Size(actualBarWidth, barHeight),
                            cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                        )
                    }

                    // Draw A marker (behind playhead)
                    if (loopStartMs != null) {
                        val aX = loopStartPos * canvasWidth

                        // A marker line
                        drawLine(
                            color = Color(0xFF00E5FF),
                            start = Offset(aX, 0f),
                            end = Offset(aX, canvasHeight),
                            strokeWidth = 3.dp.toPx()
                        )

                        // A marker circle handle at top (larger for easier drag)
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 12.dp.toPx(),
                            center = Offset(aX, 14.dp.toPx())
                        )
                        // Inner circle
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(aX, 14.dp.toPx())
                        )

                        // A marker circle handle at bottom (larger for easier drag)
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 12.dp.toPx(),
                            center = Offset(aX, canvasHeight - 14.dp.toPx())
                        )
                        // Inner circle
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(aX, canvasHeight - 14.dp.toPx())
                        )
                    }

                    // Draw B marker
                    if (loopEndMs != null) {
                        val bX = loopEndPos * canvasWidth

                        // B marker line
                        drawLine(
                            color = Color(0xFFFFEA00),
                            start = Offset(bX, 0f),
                            end = Offset(bX, canvasHeight),
                            strokeWidth = 3.dp.toPx()
                        )

                        // B marker circle handle at top (larger for easier drag)
                        drawCircle(
                            color = Color(0xFFFFEA00),
                            radius = 12.dp.toPx(),
                            center = Offset(bX, 14.dp.toPx())
                        )
                        // Inner circle
                        drawCircle(
                            color = Color.Black,
                            radius = 6.dp.toPx(),
                            center = Offset(bX, 14.dp.toPx())
                        )

                        // B marker circle handle at bottom (larger for easier drag)
                        drawCircle(
                            color = Color(0xFFFFEA00),
                            radius = 12.dp.toPx(),
                            center = Offset(bX, canvasHeight - 14.dp.toPx())
                        )
                        // Inner circle
                        drawCircle(
                            color = Color.Black,
                            radius = 6.dp.toPx(),
                            center = Offset(bX, canvasHeight - 14.dp.toPx())
                        )
                    }
                }

                // Draw playhead (current position) - ON TOP of markers
                val playheadX = if (lockPlayheadToCenter) canvasWidth / 2 else (displayPos * canvasWidth)
                val playheadColor = if (isDraggingPlayhead) Color(0xFF00E676) else Color.White

                // Playhead line (thicker for visibility)
                drawLine(
                    color = playheadColor,
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, canvasHeight),
                    strokeWidth = 3.dp.toPx()
                )

                // Large draggable handle at top (triangle pointing down)
                val triangleSize = 14.dp.toPx()
                val trianglePath = Path().apply {
                    moveTo(playheadX - triangleSize, 0f)
                    lineTo(playheadX + triangleSize, 0f)
                    lineTo(playheadX, triangleSize)
                    close()
                }
                drawPath(trianglePath, playheadColor, style = Fill)

                // Large draggable handle at bottom (circle)
                drawCircle(
                    color = playheadColor,
                    radius = 10.dp.toPx(),
                    center = Offset(playheadX, canvasHeight - 12.dp.toPx())
                )
                // Inner dot
                drawCircle(
                    color = Color.Black,
                    radius = 4.dp.toPx(),
                    center = Offset(playheadX, canvasHeight - 12.dp.toPx())
                )
            }

            // Duration label at bottom right corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatTimeMs(durationMs),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // A-B Loop Controls Row
        ABLoopControls(
            loopStartMs = loopStartMs,
            loopEndMs = loopEndMs,
            durationMs = durationMs,
            currentPosition = currentPosition,
            onLoopStartChange = onLoopStartChange,
            onLoopEndChange = onLoopEndChange,
            onClearLoop = onClearLoop
        )
    }
}

@Composable
private fun ABLoopControls(
    loopStartMs: Long?,
    loopEndMs: Long?,
    durationMs: Long,
    currentPosition: Float,
    onLoopStartChange: (Long) -> Unit,
    onLoopEndChange: (Long) -> Unit,
    onClearLoop: () -> Unit
) {
    val finetuneStep = 100L  // 100ms steps
    val loopDuration = if (loopStartMs != null && loopEndMs != null) loopEndMs - loopStartMs else 0L

    // Hold-to-set state
    var isHoldingA by remember { mutableStateOf(false) }
    var isHoldingB by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        // Loop time display with fine-tune controls
        if (loopStartMs != null && loopEndMs != null) {
            Column {
                // A marker with fine-tune
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onLoopStartChange((loopStartMs - finetuneStep).coerceAtLeast(0)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, "A -100ms", Modifier.size(14.dp), tint = Color(0xFF00E5FF))
                    }
                    Text(
                        text = "A: ${formatTimeWithMs(loopStartMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onLoopStartChange((loopStartMs + finetuneStep).coerceAtMost(loopEndMs - 100)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, "A +100ms", Modifier.size(14.dp), tint = Color(0xFF00E5FF))
                    }
                }

                // B marker with fine-tune
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onLoopEndChange((loopEndMs - finetuneStep).coerceAtLeast(loopStartMs + 100)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Remove, "B -100ms", Modifier.size(14.dp), tint = Color(0xFFFFEA00))
                    }
                    Text(
                        text = "B: ${formatTimeWithMs(loopEndMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFEA00),
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onLoopEndChange((loopEndMs + finetuneStep).coerceAtMost(durationMs)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Add, "B +100ms", Modifier.size(14.dp), tint = Color(0xFFFFEA00))
                    }
                }

                // Duration
                Text(
                    text = "Loop: ${formatTimeWithMs(loopDuration)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        } else {
            Column {
                Text(
                    text = "HOLD A/B to set loop points",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Or tap waveform to seek",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        // Main control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip backward (move loop back by its duration)
            if (loopStartMs != null && loopEndMs != null && loopDuration > 0) {
                IconButton(
                    onClick = {
                        val newStart = (loopStartMs - loopDuration).coerceAtLeast(0)
                        val newEnd = (loopEndMs - loopDuration).coerceAtLeast(loopDuration)
                        onLoopStartChange(newStart)
                        onLoopEndChange(newEnd)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, "Skip loop back", Modifier.size(18.dp), tint = Color(0xFFFF9800))
                }
            }

            // Hold-to-set A button
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHoldingA) Color(0xFF00E5FF) else Color(0xFF00E5FF).copy(alpha = 0.2f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isHoldingA = true
                            try {
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                            } finally {
                                if (isHoldingA) {
                                    val releaseTimeMs = (currentPosition * durationMs).toLong()
                                    onLoopStartChange(releaseTimeMs)
                                }
                                isHoldingA = false
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isHoldingA) "▶ A" else "A",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isHoldingA) Color.Black else Color(0xFF00E5FF)
                    )
                    if (loopStartMs != null) {
                        Text(" ✓", fontSize = 10.sp, color = if (isHoldingA) Color.Black else Color(0xFF00E5FF))
                    }
                }
            }

            // Hold-to-set B button
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHoldingB) Color(0xFFFFEA00) else Color(0xFFFFEA00).copy(alpha = 0.2f))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isHoldingB = true
                            try {
                                do {
                                    val event = awaitPointerEvent()
                                } while (event.changes.any { it.pressed })
                            } finally {
                                if (isHoldingB) {
                                    val releaseTimeMs = (currentPosition * durationMs).toLong()
                                    if (loopStartMs == null || releaseTimeMs > loopStartMs) {
                                        onLoopEndChange(releaseTimeMs)
                                    } else {
                                        onLoopEndChange(loopStartMs)
                                        onLoopStartChange(releaseTimeMs)
                                    }
                                }
                                isHoldingB = false
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isHoldingB) "▶ B" else "B",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isHoldingB) Color.Black else Color(0xFFFFEA00)
                    )
                    if (loopEndMs != null) {
                        Text(" ✓", fontSize = 10.sp, color = if (isHoldingB) Color.Black else Color(0xFFFFEA00))
                    }
                }
            }

            // Skip forward
            if (loopStartMs != null && loopEndMs != null && loopDuration > 0) {
                IconButton(
                    onClick = {
                        val newStart = (loopStartMs + loopDuration).coerceAtMost(durationMs - loopDuration)
                        val newEnd = (loopEndMs + loopDuration).coerceAtMost(durationMs)
                        if (newEnd > newStart) {
                            onLoopStartChange(newStart)
                            onLoopEndChange(newEnd)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.SkipNext, "Skip loop forward", Modifier.size(18.dp), tint = Color(0xFFFF9800))
                }
            }

            // Clear loop button
            if (loopStartMs != null || loopEndMs != null) {
                IconButton(onClick = onClearLoop, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Stop, "Clear Loop", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        }

        // Manual time edit (always clamped within valid range)
        Spacer(modifier = Modifier.height(8.dp))

        val safeDuration = durationMs.coerceAtLeast(0L)
        val effectiveA = (loopStartMs ?: 0L).coerceIn(0L, safeDuration)
        val effectiveB = (loopEndMs ?: safeDuration).coerceIn(0L, safeDuration)
        val minGapMs = 1L

        LoopPointEditorRowCompact(
            label = "A",
            valueMs = effectiveA,
            minMs = 0L,
            maxMs = (effectiveB - minGapMs).coerceAtLeast(0L),
            onValueMsChange = { newA ->
                val maxA = (effectiveB - minGapMs).coerceAtLeast(0L)
                onLoopStartChange(newA.coerceIn(0L, maxA))
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        LoopPointEditorRowCompact(
            label = "B",
            valueMs = effectiveB,
            minMs = (effectiveA + minGapMs).coerceAtMost(safeDuration),
            maxMs = safeDuration,
            onValueMsChange = { newB ->
                val minB = (effectiveA + minGapMs).coerceAtMost(safeDuration)
                onLoopEndChange(newB.coerceIn(minB, safeDuration))
            }
        )
    }
}

@Composable
private fun LoopPointEditorRowCompact(
    label: String,
    valueMs: Long,
    minMs: Long,
    maxMs: Long,
    onValueMsChange: (Long) -> Unit
) {
    val safeMax = maxMs.coerceAtLeast(minMs)
    val safeValue = valueMs.coerceIn(minMs, safeMax)

    val minutes = (safeValue / 60000).toInt()
    val seconds = ((safeValue % 60000) / 1000).toInt()
    val millis = (safeValue % 1000).toInt()

    var minText by remember(safeValue) { mutableStateOf(minutes.toString()) }
    var secText by remember(safeValue) { mutableStateOf(seconds.toString().padStart(2, '0')) }
    var msText by remember(safeValue) { mutableStateOf(millis.toString().padStart(3, '0')) }

    fun tryUpdate() {
        val m = minText.toIntOrNull() ?: return
        val s = secText.toIntOrNull() ?: return
        val ms = msText.toIntOrNull() ?: return
        if (m < 0) return
        if (s !in 0..59) return
        if (ms !in 0..999) return
        val newValue = (m * 60_000L) + (s * 1000L) + ms
        onValueMsChange(newValue.coerceIn(minMs, safeMax))
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(24.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = {
                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                            minText = it
                            tryUpdate()
                        }
                    },
                    singleLine = true,
                    label = { Text("Min") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(84.dp)
                )

                Text(":", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = secText,
                    onValueChange = {
                        if (it.length <= 2 && it.all { ch -> ch.isDigit() }) {
                            secText = it
                            tryUpdate()
                        }
                    },
                    singleLine = true,
                    label = { Text("Sec") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(84.dp)
                )

                Text(".", style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = msText,
                    onValueChange = {
                        if (it.length <= 3 && it.all { ch -> ch.isDigit() }) {
                            msText = it
                            tryUpdate()
                        }
                    },
                    singleLine = true,
                    label = { Text("ms") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(92.dp)
                )
            }
        }

        Text(
            text = "Range: ${formatTimeWithMs(minMs)} – ${formatTimeWithMs(safeMax)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 32.dp, top = 2.dp)
        )
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatTimeWithMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val centis = (ms % 1000) / 10
    return "%d:%02d.%02d".format(minutes, seconds, centis)
}
