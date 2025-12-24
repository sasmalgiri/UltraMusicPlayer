package com.ultramusic.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.calculatePan
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.audio.BeatMarker
import com.ultramusic.player.audio.BeatStrength
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Zoomable Waveform Visualizer with A-B Loop Markers and Beat Detection
 *
 * Features:
 * - Real-time waveform display
 * - Pinch to zoom (1x to 10x)
 * - Pan/scroll when zoomed
 * - Draggable A and B markers for loop selection
 * - Beat markers overlay
 * - Playhead position indicator
 * - Tap to seek
 * - Time ruler with timestamps
 */
@Composable
fun WaveformVisualizer(
    waveformData: List<Float>,  // Normalized amplitude data (0-1)
    currentPosition: Float,      // Current playback position (0-1)
    durationMs: Long,           // Total duration in ms
    loopStartMs: Long?,         // A marker position in ms
    loopEndMs: Long?,           // B marker position in ms
    isLooping: Boolean,
    onSeek: (Float) -> Unit,    // Seek to position (0-1)
    onLoopStartChange: (Long) -> Unit,   // Set A marker
    onLoopEndChange: (Long) -> Unit,     // Set B marker
    onClearLoop: () -> Unit,
    modifier: Modifier = Modifier,
    beatMarkers: List<BeatMarker> = emptyList(),  // Beat markers
    estimatedBpm: Float = 0f,                      // Estimated BPM
    showBeatMarkers: Boolean = true,
    waveformColor: Color = Color(0xFF2196F3),
    playedColor: Color = Color(0xFF4CAF50),
    loopColor: Color = Color(0xFFFF9800),
    beatColor: Color = Color(0xFFE91E63),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 140.dp
) {
    val density = LocalDensity.current
    var canvasWidth by remember { mutableFloatStateOf(0f) }

    // Zoom and pan state
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableFloatStateOf(0f) }

    // Animated zoom
    val animatedZoom by animateFloatAsState(
        targetValue = zoomLevel,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "zoom_animation"
    )

    // Convert ms to position (0-1)
    val loopStartPosition = loopStartMs?.let { it.toFloat() / durationMs } ?: 0f
    val loopEndPosition = loopEndMs?.let { it.toFloat() / durationMs } ?: 1f

    // Marker drag states
    var isDraggingA by remember { mutableStateOf(false) }
    var isDraggingB by remember { mutableStateOf(false) }
    var dragPositionA by remember { mutableFloatStateOf(loopStartPosition) }
    var dragPositionB by remember { mutableFloatStateOf(loopEndPosition) }

    LaunchedEffect(loopStartMs, loopEndMs) {
        dragPositionA = loopStartMs?.let { it.toFloat() / durationMs } ?: 0f
        dragPositionB = loopEndMs?.let { it.toFloat() / durationMs } ?: 1f
    }

    // Auto-scroll to keep playhead visible when zoomed
    LaunchedEffect(currentPosition, animatedZoom) {
        if (animatedZoom > 1f) {
            val visibleRange = 1f / animatedZoom
            val playheadInView = currentPosition >= panOffset && currentPosition <= panOffset + visibleRange
            if (!playheadInView) {
                panOffset = (currentPosition - visibleRange / 2).coerceIn(0f, 1f - visibleRange)
            }
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with zoom controls and BPM
            WaveformHeader(
                currentPosition = currentPosition,
                durationMs = durationMs,
                zoomLevel = animatedZoom,
                estimatedBpm = estimatedBpm,
                playedColor = playedColor,
                beatColor = beatColor,
                onZoomIn = { zoomLevel = (zoomLevel * 1.5f).coerceAtMost(10f) },
                onZoomOut = { zoomLevel = (zoomLevel / 1.5f).coerceAtLeast(1f) },
                onResetZoom = { zoomLevel = 1f; panOffset = 0f }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Waveform canvas with zoom and pan
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Handle pinch to zoom and pan
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()

                                    if (zoomChange != 1f) {
                                        zoomLevel = (zoomLevel * zoomChange).coerceIn(1f, 10f)
                                    }

                                    if (zoomLevel > 1f && panChange.x != 0f) {
                                        val visibleRange = 1f / zoomLevel
                                        val panDelta = panChange.x / size.width / zoomLevel
                                        panOffset = (panOffset - panDelta).coerceIn(0f, 1f - visibleRange)
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val visibleRange = 1f / zoomLevel
                                val tappedPosition = panOffset + (offset.x / size.width) * visibleRange
                                onSeek(tappedPosition.coerceIn(0f, 1f))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val visibleRange = 1f / animatedZoom
                                    val position = panOffset + (offset.x / size.width) * visibleRange

                                    // Calculate marker positions in current view
                                    val markerAViewX = (dragPositionA - panOffset) / visibleRange * size.width
                                    val markerBViewX = (dragPositionB - panOffset) / visibleRange * size.width

                                    // Check if near A or B marker (within 40px for easier touch)
                                    if (abs(offset.x - markerAViewX) < 40) {
                                        isDraggingA = true
                                    } else if (abs(offset.x - markerBViewX) < 40) {
                                        isDraggingB = true
                                    }
                                },
                                onDrag = { change, _ ->
                                    val visibleRange = 1f / animatedZoom
                                    val position = panOffset + (change.position.x / size.width) * visibleRange

                                    if (isDraggingA) {
                                        dragPositionA = position.coerceIn(0f, dragPositionB - 0.01f)
                                        onLoopStartChange((dragPositionA * durationMs).toLong())
                                    } else if (isDraggingB) {
                                        dragPositionB = position.coerceIn(dragPositionA + 0.01f, 1f)
                                        onLoopEndChange((dragPositionB * durationMs).toLong())
                                    }
                                },
                                onDragEnd = {
                                    isDraggingA = false
                                    isDraggingB = false
                                }
                            )
                        }
                ) {
                    canvasWidth = size.width
                    val canvasHeight = size.height
                    val centerY = canvasHeight / 2

                    val visibleRange = 1f / animatedZoom
                    val startPos = panOffset
                    val endPos = panOffset + visibleRange

                    // Draw time ruler at top
                    drawTimeRuler(
                        canvasWidth = size.width,
                        durationMs = durationMs,
                        startPos = startPos,
                        endPos = endPos,
                        color = Color.White.copy(alpha = 0.3f)
                    )

                    // Draw loop region highlight
                    if (loopStartMs != null && loopEndMs != null) {
                        val loopStartX = ((dragPositionA - startPos) / visibleRange * size.width).coerceIn(0f, size.width)
                        val loopEndX = ((dragPositionB - startPos) / visibleRange * size.width).coerceIn(0f, size.width)

                        if (loopEndX > loopStartX) {
                            drawRect(
                                color = loopColor.copy(alpha = 0.15f),
                                topLeft = Offset(loopStartX, 0f),
                                size = Size(loopEndX - loopStartX, canvasHeight)
                            )
                        }
                    }

                    // Draw waveform
                    if (waveformData.isNotEmpty()) {
                        val totalBars = waveformData.size
                        val startIndex = (startPos * totalBars).toInt().coerceIn(0, totalBars - 1)
                        val endIndex = (endPos * totalBars).toInt().coerceIn(0, totalBars)
                        val visibleBars = endIndex - startIndex

                        if (visibleBars > 0) {
                            val barWidth = size.width / visibleBars
                            val barGap = (1.dp.toPx() / animatedZoom).coerceAtLeast(0.5f)

                            for (i in startIndex until endIndex) {
                                val amplitude = waveformData.getOrElse(i) { 0f }
                                val localIndex = i - startIndex
                                val x = localIndex * barWidth
                                val barHeight = amplitude * (canvasHeight - 30.dp.toPx()) * 0.8f
                                val position = i.toFloat() / totalBars

                                // Determine color based on position
                                val barColor = when {
                                    position < currentPosition -> playedColor
                                    loopStartMs != null && loopEndMs != null &&
                                            position >= dragPositionA && position <= dragPositionB -> loopColor
                                    else -> waveformColor
                                }

                                // Draw bar (top half)
                                drawRect(
                                    color = barColor.copy(alpha = 0.9f),
                                    topLeft = Offset(x + barGap / 2, centerY - barHeight / 2),
                                    size = Size((barWidth - barGap).coerceAtLeast(1f), barHeight / 2)
                                )

                                // Draw bar (bottom half - mirror)
                                drawRect(
                                    color = barColor.copy(alpha = 0.5f),
                                    topLeft = Offset(x + barGap / 2, centerY),
                                    size = Size((barWidth - barGap).coerceAtLeast(1f), barHeight / 2)
                                )
                            }
                        }
                    }

                    // Draw beat markers
                    if (showBeatMarkers && beatMarkers.isNotEmpty()) {
                        beatMarkers.forEach { beat ->
                            val beatPos = beat.getPosition(durationMs)
                            if (beatPos >= startPos && beatPos <= endPos) {
                                val beatX = ((beatPos - startPos) / visibleRange * size.width)

                                // Draw beat marker line
                                val markerHeight = when (beat.strength) {
                                    BeatStrength.STRONG -> canvasHeight * 0.9f
                                    BeatStrength.MEDIUM -> canvasHeight * 0.6f
                                    BeatStrength.WEAK -> canvasHeight * 0.3f
                                }

                                val markerAlpha = when (beat.strength) {
                                    BeatStrength.STRONG -> 0.8f
                                    BeatStrength.MEDIUM -> 0.5f
                                    BeatStrength.WEAK -> 0.3f
                                }

                                drawLine(
                                    color = beatColor.copy(alpha = markerAlpha),
                                    start = Offset(beatX, centerY - markerHeight / 2),
                                    end = Offset(beatX, centerY + markerHeight / 2),
                                    strokeWidth = when (beat.strength) {
                                        BeatStrength.STRONG -> 3.dp.toPx()
                                        BeatStrength.MEDIUM -> 2.dp.toPx()
                                        BeatStrength.WEAK -> 1.dp.toPx()
                                    }
                                )

                                // Draw beat dot for strong beats
                                if (beat.strength == BeatStrength.STRONG) {
                                    drawCircle(
                                        color = beatColor,
                                        radius = 4.dp.toPx(),
                                        center = Offset(beatX, canvasHeight - 10.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    // Draw playhead
                    if (currentPosition >= startPos && currentPosition <= endPos) {
                        val playheadX = ((currentPosition - startPos) / visibleRange * size.width)

                        // Playhead glow
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.5f),
                                    Color.White,
                                    Color.White.copy(alpha = 0.5f)
                                )
                            ),
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, canvasHeight),
                            strokeWidth = 4.dp.toPx()
                        )

                        // Playhead line
                        drawLine(
                            color = Color.White,
                            start = Offset(playheadX, 0f),
                            end = Offset(playheadX, canvasHeight),
                            strokeWidth = 2.dp.toPx()
                        )

                        // Playhead triangle
                        val trianglePath = Path().apply {
                            moveTo(playheadX - 6.dp.toPx(), 0f)
                            lineTo(playheadX + 6.dp.toPx(), 0f)
                            lineTo(playheadX, 8.dp.toPx())
                            close()
                        }
                        drawPath(trianglePath, Color.White)
                    }

                    // Draw A marker
                    if (loopStartMs != null && dragPositionA >= startPos && dragPositionA <= endPos) {
                        val markerX = ((dragPositionA - startPos) / visibleRange * size.width)
                        drawLoopMarker(
                            x = markerX,
                            canvasHeight = canvasHeight,
                            color = Color(0xFF4CAF50),
                            label = "A",
                            isDragging = isDraggingA
                        )
                    }

                    // Draw B marker
                    if (loopEndMs != null && dragPositionB >= startPos && dragPositionB <= endPos) {
                        val markerX = ((dragPositionB - startPos) / visibleRange * size.width)
                        drawLoopMarker(
                            x = markerX,
                            canvasHeight = canvasHeight,
                            color = Color(0xFFF44336),
                            label = "B",
                            isDragging = isDraggingB
                        )
                    }
                }

                // Zoom indicator overlay
                if (animatedZoom > 1.05f) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${String.format("%.1f", animatedZoom)}x",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // A-B Loop controls
            LoopControlRow(
                loopStartMs = loopStartMs,
                loopEndMs = loopEndMs,
                currentPosition = currentPosition,
                durationMs = durationMs,
                loopColor = loopColor,
                onLoopStartChange = onLoopStartChange,
                onLoopEndChange = onLoopEndChange,
                onClearLoop = onClearLoop
            )

            // Instructions
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pinch to zoom | Pan when zoomed | Tap to seek | Drag A/B markers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WaveformHeader(
    currentPosition: Float,
    durationMs: Long,
    zoomLevel: Float,
    estimatedBpm: Float,
    playedColor: Color,
    beatColor: Color,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and BPM
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "WAVEFORM",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )

            if (estimatedBpm > 0) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = beatColor.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = beatColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${estimatedBpm.toInt()} BPM",
                            style = MaterialTheme.typography.labelSmall,
                            color = beatColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Time display and zoom controls
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Current time
            Text(
                text = formatTime((currentPosition * durationMs).toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = playedColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " / ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = formatTime(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Zoom controls
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(28.dp),
                enabled = zoomLevel > 1f
            ) {
                Icon(
                    Icons.Default.ZoomOut,
                    contentDescription = "Zoom out",
                    modifier = Modifier.size(18.dp),
                    tint = if (zoomLevel > 1f)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            // Zoom reset button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp)),
                color = if (zoomLevel > 1f)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                onClick = onResetZoom
            ) {
                Text(
                    text = "${String.format("%.1f", zoomLevel)}x",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(28.dp),
                enabled = zoomLevel < 10f
            ) {
                Icon(
                    Icons.Default.ZoomIn,
                    contentDescription = "Zoom in",
                    modifier = Modifier.size(18.dp),
                    tint = if (zoomLevel < 10f)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun LoopControlRow(
    loopStartMs: Long?,
    loopEndMs: Long?,
    currentPosition: Float,
    durationMs: Long,
    loopColor: Color,
    onLoopStartChange: (Long) -> Unit,
    onLoopEndChange: (Long) -> Unit,
    onClearLoop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Loop time display
        if (loopStartMs != null && loopEndMs != null) {
            Column {
                Text(
                    text = "Loop: ${formatTime(loopStartMs)} - ${formatTime(loopEndMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = loopColor
                )
                Text(
                    text = "Duration: ${formatTime(loopEndMs - loopStartMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        } else {
            Text(
                text = "Set A-B points to create loop",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // Control buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Set A button
            OutlinedButton(
                onClick = { onLoopStartChange((currentPosition * durationMs).toLong()) },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF4CAF50)
                )
            ) {
                Text("A", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Set B button
            OutlinedButton(
                onClick = { onLoopEndChange((currentPosition * durationMs).toLong()) },
                modifier = Modifier.height(32.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF44336)
                )
            ) {
                Text("B", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            // Clear loop button
            if (loopStartMs != null || loopEndMs != null) {
                OutlinedButton(
                    onClick = onClearLoop,
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Clear Loop",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Draw time ruler at top of waveform
 */
private fun DrawScope.drawTimeRuler(
    canvasWidth: Float,
    durationMs: Long,
    startPos: Float,
    endPos: Float,
    color: Color
) {
    val visibleDurationMs = ((endPos - startPos) * durationMs).toLong()

    // Determine tick interval based on visible duration
    val tickIntervalMs = when {
        visibleDurationMs < 5000 -> 500L      // < 5s: every 0.5s
        visibleDurationMs < 30000 -> 2000L    // < 30s: every 2s
        visibleDurationMs < 120000 -> 5000L   // < 2m: every 5s
        visibleDurationMs < 300000 -> 10000L  // < 5m: every 10s
        else -> 30000L                         // > 5m: every 30s
    }

    val startTimeMs = (startPos * durationMs).toLong()
    val endTimeMs = (endPos * durationMs).toLong()

    var tickTime = (startTimeMs / tickIntervalMs) * tickIntervalMs
    while (tickTime <= endTimeMs) {
        if (tickTime >= startTimeMs) {
            val tickPos = (tickTime - startTimeMs).toFloat() / (endTimeMs - startTimeMs)
            val tickX = tickPos * canvasWidth

            // Draw tick mark
            drawLine(
                color = color,
                start = Offset(tickX, 0f),
                end = Offset(tickX, 8.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        tickTime += tickIntervalMs
    }
}

/**
 * Draw loop marker (A or B)
 */
private fun DrawScope.drawLoopMarker(
    x: Float,
    canvasHeight: Float,
    color: Color,
    label: String,
    isDragging: Boolean
) {
    val markerWidth = if (isDragging) 4.dp.toPx() else 3.dp.toPx()

    // Marker line
    drawLine(
        color = color,
        start = Offset(x, 0f),
        end = Offset(x, canvasHeight),
        strokeWidth = markerWidth
    )

    // Marker circle at top
    val circleRadius = if (isDragging) 14.dp.toPx() else 12.dp.toPx()
    drawCircle(
        color = color,
        radius = circleRadius,
        center = Offset(x, 16.dp.toPx())
    )

    // Dragging highlight
    if (isDragging) {
        drawCircle(
            color = color.copy(alpha = 0.3f),
            radius = 20.dp.toPx(),
            center = Offset(x, 16.dp.toPx())
        )
    }
}

/**
 * Compact waveform for mini player or Now Playing
 */
@Composable
fun CompactWaveform(
    waveformData: List<Float>,
    currentPosition: Float,
    loopStartPosition: Float?,
    loopEndPosition: Float?,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    waveformColor: Color = Color(0xFF2196F3),
    playedColor: Color = Color(0xFF4CAF50),
    beatMarkers: List<BeatMarker> = emptyList(),
    durationMs: Long = 0L,
    beatColor: Color = Color(0xFFE91E63)
) {
    Canvas(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val position = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(position)
                }
            }
    ) {
        val centerY = size.height / 2

        if (waveformData.isNotEmpty()) {
            val barWidth = size.width / waveformData.size
            val barGap = 0.5.dp.toPx()

            waveformData.forEachIndexed { index, amplitude ->
                val x = index * barWidth
                val barHeight = amplitude * size.height * 0.9f
                val position = index.toFloat() / waveformData.size

                val barColor = if (position < currentPosition) playedColor else waveformColor

                drawRect(
                    color = barColor.copy(alpha = 0.8f),
                    topLeft = Offset(x + barGap / 2, centerY - barHeight / 2),
                    size = Size(barWidth - barGap, barHeight)
                )
            }
        }

        // Draw beat markers (simplified for compact view)
        if (beatMarkers.isNotEmpty() && durationMs > 0) {
            beatMarkers.filter { it.strength == BeatStrength.STRONG }.forEach { beat ->
                val beatX = beat.getPosition(durationMs) * size.width
                drawCircle(
                    color = beatColor.copy(alpha = 0.6f),
                    radius = 2.dp.toPx(),
                    center = Offset(beatX, size.height - 4.dp.toPx())
                )
            }
        }

        // Draw playhead
        val playheadX = currentPosition * size.width
        drawLine(
            color = Color.White,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, size.height),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
