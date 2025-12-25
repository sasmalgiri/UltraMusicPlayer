package com.ultramusic.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.CornerRadius
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
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Saved A-B marker for quick recall (like Music Speed Changer)
 */
data class SavedMarker(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startMs: Long,
    val endMs: Long,
    val songId: Long  // Associate with specific song
)

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
    height: Dp = 200.dp,  // Balanced height for visibility and screen space
    // Saved markers functionality
    savedMarkers: List<SavedMarker> = emptyList(),
    onSaveMarker: ((String, Long, Long) -> Unit)? = null,  // name, startMs, endMs
    onLoadMarker: ((SavedMarker) -> Unit)? = null,
    onDeleteMarker: ((SavedMarker) -> Unit)? = null
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
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
    var isDraggingPlayhead by remember { mutableStateOf(false) }  // For scrubbing
    var dragPositionA by remember { mutableFloatStateOf(loopStartPosition) }
    var dragPositionB by remember { mutableFloatStateOf(loopEndPosition) }

    // Track which marker to set next (for long-press to set markers)
    // false = set A next, true = set B next
    var setMarkerB by remember { mutableStateOf(false) }

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
                            detectTapGestures(
                                onTap = { offset ->
                                    val visibleRange = 1f / zoomLevel
                                    val tappedPosition = panOffset + (offset.x / size.width) * visibleRange
                                    onSeek(tappedPosition.coerceIn(0f, 1f))
                                },
                                onLongPress = { offset ->
                                    // Long press sets A or B marker at that position
                                    val visibleRange = 1f / zoomLevel
                                    val tappedPosition = panOffset + (offset.x / size.width) * visibleRange
                                    val positionMs = (tappedPosition.coerceIn(0f, 1f) * durationMs).toLong()

                                    if (!setMarkerB || loopStartMs == null) {
                                        // Set A marker
                                        onLoopStartChange(positionMs)
                                        setMarkerB = true
                                    } else {
                                        // Set B marker (must be after A)
                                        if (positionMs > (loopStartMs ?: 0)) {
                                            onLoopEndChange(positionMs)
                                        } else {
                                            // If B is before A, swap them
                                            val currentA = loopStartMs ?: 0
                                            onLoopStartChange(positionMs)
                                            onLoopEndChange(currentA)
                                        }
                                        setMarkerB = false
                                    }
                                }
                            )
                        }
                        .pointerInput(loopStartMs, loopEndMs) {
                            // Enable dragging for markers and playhead scrubbing
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val visibleRange = 1f / animatedZoom

                                    // Calculate marker positions in current view
                                    val markerAViewX = if (loopStartMs != null) {
                                        (dragPositionA - panOffset) / visibleRange * size.width
                                    } else -1000f
                                    val markerBViewX = if (loopEndMs != null) {
                                        (dragPositionB - panOffset) / visibleRange * size.width
                                    } else -1000f

                                    // Larger touch target (60px) for easier marker selection
                                    val distanceToA = abs(offset.x - markerAViewX)
                                    val distanceToB = abs(offset.x - markerBViewX)

                                    // Choose the closest marker if within threshold, otherwise scrub playhead
                                    when {
                                        distanceToA < 60 && distanceToA <= distanceToB -> {
                                            isDraggingA = true
                                        }
                                        distanceToB < 60 && distanceToB < distanceToA -> {
                                            isDraggingB = true
                                        }
                                        else -> {
                                            // Not near any marker - drag to scrub playhead
                                            isDraggingPlayhead = true
                                            val position = panOffset + (offset.x / size.width) * visibleRange
                                            onSeek(position.coerceIn(0f, 1f))
                                        }
                                    }
                                },
                                onDrag = { change, _ ->
                                    val visibleRange = 1f / animatedZoom
                                    val position = panOffset + (change.position.x / size.width) * visibleRange

                                    when {
                                        isDraggingA -> {
                                            val maxA = if (loopEndMs != null) dragPositionB - 0.01f else 1f
                                            dragPositionA = position.coerceIn(0f, maxA)
                                            onLoopStartChange((dragPositionA * durationMs).toLong())
                                        }
                                        isDraggingB -> {
                                            val minB = if (loopStartMs != null) dragPositionA + 0.01f else 0f
                                            dragPositionB = position.coerceIn(minB, 1f)
                                            onLoopEndChange((dragPositionB * durationMs).toLong())
                                        }
                                        isDraggingPlayhead -> {
                                            // Scrub through the song
                                            onSeek(position.coerceIn(0f, 1f))
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

                    // Draw playhead with floating timestamp
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

                        // Floating timestamp label - positioned at bottom of playhead
                        val currentTimeMs = (currentPosition * durationMs).toLong()
                        val timeText = formatTime(currentTimeMs)
                        val labelWidth = 44.dp.toPx()
                        val labelHeight = 18.dp.toPx()
                        val labelY = canvasHeight - labelHeight - 4.dp.toPx()

                        // Calculate x position, keeping label within bounds
                        val labelX = (playheadX - labelWidth / 2)
                            .coerceIn(2.dp.toPx(), size.width - labelWidth - 2.dp.toPx())

                        // Draw label background
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.85f),
                            topLeft = Offset(labelX, labelY),
                            size = Size(labelWidth, labelHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )

                        // Draw label border
                        drawRoundRect(
                            color = playedColor.copy(alpha = 0.8f),
                            topLeft = Offset(labelX, labelY),
                            size = Size(labelWidth, labelHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Draw timestamp text
                        val textLayoutResult = textMeasurer.measure(
                            text = timeText,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                labelX + (labelWidth - textLayoutResult.size.width) / 2,
                                labelY + (labelHeight - textLayoutResult.size.height) / 2
                            )
                        )

                        // Playhead triangle at top
                        val trianglePath = Path().apply {
                            moveTo(playheadX - 6.dp.toPx(), 0f)
                            lineTo(playheadX + 6.dp.toPx(), 0f)
                            lineTo(playheadX, 8.dp.toPx())
                            close()
                        }
                        drawPath(trianglePath, Color.White)
                    }

                    // Draw A marker - Bright Cyan for high visibility
                    if (loopStartMs != null && dragPositionA >= startPos && dragPositionA <= endPos) {
                        val markerX = ((dragPositionA - startPos) / visibleRange * size.width)
                        drawLoopMarker(
                            x = markerX,
                            canvasHeight = canvasHeight,
                            color = Color(0xFF00E5FF),  // Bright Cyan
                            label = "A",
                            isDragging = isDraggingA
                        )
                    }

                    // Draw B marker - Bright Yellow for high visibility
                    if (loopEndMs != null && dragPositionB >= startPos && dragPositionB <= endPos) {
                        val markerX = ((dragPositionB - startPos) / visibleRange * size.width)
                        drawLoopMarker(
                            x = markerX,
                            canvasHeight = canvasHeight,
                            color = Color(0xFFFFEA00),  // Bright Yellow
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
                onClearLoop = onClearLoop,
                savedMarkers = savedMarkers,
                onSaveMarker = onSaveMarker,
                onLoadMarker = onLoadMarker,
                onDeleteMarker = onDeleteMarker
            )

            // Instructions
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap to seek | Drag markers | HOLD A/B buttons & release to set",
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

/**
 * Music Speed Changer style A-B Loop Controls
 * Features:
 * - Hold A/B buttons and release to set marker at that moment
 * - Skip loop forward/backward by loop duration
 * - Fine-tune markers with +/- ms adjustment
 * - Save/load markers
 */
@Composable
private fun LoopControlRow(
    loopStartMs: Long?,
    loopEndMs: Long?,
    currentPosition: Float,
    durationMs: Long,
    loopColor: Color,
    onLoopStartChange: (Long) -> Unit,
    onLoopEndChange: (Long) -> Unit,
    onClearLoop: () -> Unit,
    savedMarkers: List<SavedMarker> = emptyList(),
    onSaveMarker: ((String, Long, Long) -> Unit)? = null,
    onLoadMarker: ((SavedMarker) -> Unit)? = null,
    onDeleteMarker: ((SavedMarker) -> Unit)? = null
) {
    // Hold-to-set state
    var isHoldingA by remember { mutableStateOf(false) }
    var isHoldingB by remember { mutableStateOf(false) }
    var holdStartTimeA by remember { mutableFloatStateOf(0f) }
    var holdStartTimeB by remember { mutableFloatStateOf(0f) }

    // Markers menu state
    var showMarkersMenu by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var markerName by remember { mutableStateOf("") }

    // Fine-tune step (in milliseconds)
    val finetuneStep = 100L  // 100ms steps

    val loopDuration = if (loopStartMs != null && loopEndMs != null) loopEndMs - loopStartMs else 0L

    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: Loop info and main A-B buttons
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
                        // Fine-tune A backward
                        IconButton(
                            onClick = {
                                val newA = (loopStartMs - finetuneStep).coerceAtLeast(0)
                                onLoopStartChange(newA)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "A -100ms",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF00E5FF)
                            )
                        }

                        Text(
                            text = "A: ${formatTimeWithMs(loopStartMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold
                        )

                        // Fine-tune A forward
                        IconButton(
                            onClick = {
                                val newA = (loopStartMs + finetuneStep).coerceAtMost(loopEndMs - 100)
                                onLoopStartChange(newA)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "A +100ms",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    }

                    // B marker with fine-tune
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Fine-tune B backward
                        IconButton(
                            onClick = {
                                val newB = (loopEndMs - finetuneStep).coerceAtLeast(loopStartMs + 100)
                                onLoopEndChange(newB)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "B -100ms",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFEA00)
                            )
                        }

                        Text(
                            text = "B: ${formatTimeWithMs(loopEndMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFEA00),
                            fontWeight = FontWeight.Bold
                        )

                        // Fine-tune B forward
                        IconButton(
                            onClick = {
                                val newB = (loopEndMs + finetuneStep).coerceAtMost(durationMs)
                                onLoopEndChange(newB)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "B +100ms",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFEA00)
                            )
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
                        text = "HOLD & RELEASE to set A-B",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Or long-press waveform",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        fontSize = 10.sp
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
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Skip loop backward",
                            modifier = Modifier.size(18.dp),
                            tint = loopColor
                        )
                    }
                }

                // Hold-to-set A button - Bright Cyan
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isHoldingA) Color(0xFF00E5FF)
                            else Color(0xFF00E5FF).copy(alpha = 0.2f)
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                // Wait for press
                                awaitFirstDown(requireUnconsumed = false)
                                isHoldingA = true
                                holdStartTimeA = currentPosition

                                // Wait for release
                                try {
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    // Set marker at RELEASE position (current playback position)
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
                            Text(
                                text = " ✓",
                                fontSize = 10.sp,
                                color = if (isHoldingA) Color.Black else Color(0xFF00E5FF)
                            )
                        }
                    }
                }

                // Hold-to-set B button - Bright Yellow
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isHoldingB) Color(0xFFFFEA00)
                            else Color(0xFFFFEA00).copy(alpha = 0.2f)
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                isHoldingB = true
                                holdStartTimeB = currentPosition

                                try {
                                    do {
                                        val event = awaitPointerEvent()
                                    } while (event.changes.any { it.pressed })
                                } finally {
                                    if (isHoldingB) {
                                        val releaseTimeMs = (currentPosition * durationMs).toLong()
                                        // Ensure B is after A
                                        if (loopStartMs == null || releaseTimeMs > loopStartMs) {
                                            onLoopEndChange(releaseTimeMs)
                                        } else {
                                            // Swap: current A becomes B, new position becomes A
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
                            Text(
                                text = " ✓",
                                fontSize = 10.sp,
                                color = if (isHoldingB) Color.Black else Color(0xFFFFEA00)
                            )
                        }
                    }
                }

                // Skip forward (move loop ahead by its duration)
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
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Skip loop forward",
                            modifier = Modifier.size(18.dp),
                            tint = loopColor
                        )
                    }
                }

                // Clear loop button
                if (loopStartMs != null || loopEndMs != null) {
                    IconButton(
                        onClick = onClearLoop,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Clear Loop",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Saved Markers button (Music Speed Changer style)
                if (onSaveMarker != null) {
                    Box {
                        IconButton(
                            onClick = { showMarkersMenu = !showMarkersMenu },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (savedMarkers.isNotEmpty()) Icons.Default.Bookmark
                                else Icons.Default.BookmarkAdd,
                                contentDescription = "Saved Markers",
                                modifier = Modifier.size(18.dp),
                                tint = if (savedMarkers.isNotEmpty())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        // Badge for marker count
                        if (savedMarkers.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(14.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${savedMarkers.size}",
                                        fontSize = 8.sp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Markers dropdown menu
        if (showMarkersMenu && onSaveMarker != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📌 Saved Markers",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Save current button
                        if (loopStartMs != null && loopEndMs != null) {
                            OutlinedButton(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.height(28.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (savedMarkers.isEmpty()) {
                        Text(
                            text = "No saved markers yet.\nSet A-B loop and save it!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    } else {
                        // Markers list
                        savedMarkers.forEach { marker ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        onLoadMarker?.invoke(marker)
                                        showMarkersMenu = false
                                    }
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = marker.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${formatTimeWithMs(marker.startMs)} → ${formatTimeWithMs(marker.endMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = { onDeleteMarker?.invoke(marker) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Stop,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Close button
                    TextButton(
                        onClick = { showMarkersMenu = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close", fontSize = 12.sp)
                    }
                }
            }
        }

        // Save marker dialog (simple inline input)
        if (showSaveDialog && loopStartMs != null && loopEndMs != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Save Marker",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Marker name input
                    androidx.compose.material3.OutlinedTextField(
                        value = markerName,
                        onValueChange = { markerName = it },
                        label = { Text("Marker Name", fontSize = 12.sp) },
                        placeholder = { Text("e.g., Chorus, Verse 1", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${formatTimeWithMs(loopStartMs)} → ${formatTimeWithMs(loopEndMs)} (${formatTimeWithMs(loopEndMs - loopStartMs)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            showSaveDialog = false
                            markerName = ""
                        }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val name = markerName.ifBlank {
                                    "Marker ${savedMarkers.size + 1}"
                                }
                                onSaveMarker?.invoke(name, loopStartMs, loopEndMs)
                                showSaveDialog = false
                                markerName = ""
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format time with milliseconds for precise display
 */
private fun formatTimeWithMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 10  // Show centiseconds
    return "%d:%02d.%02d".format(minutes, seconds, millis)
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
 * Draw loop marker (A or B) with high visibility
 */
private fun DrawScope.drawLoopMarker(
    x: Float,
    canvasHeight: Float,
    color: Color,
    label: String,
    isDragging: Boolean
) {
    val markerWidth = if (isDragging) 6.dp.toPx() else 4.dp.toPx()

    // Glow effect behind marker line
    drawLine(
        color = color.copy(alpha = 0.4f),
        start = Offset(x, 0f),
        end = Offset(x, canvasHeight),
        strokeWidth = markerWidth + 6.dp.toPx()
    )

    // Black outline for contrast
    drawLine(
        color = Color.Black,
        start = Offset(x, 0f),
        end = Offset(x, canvasHeight),
        strokeWidth = markerWidth + 2.dp.toPx()
    )

    // Main marker line
    drawLine(
        color = color,
        start = Offset(x, 0f),
        end = Offset(x, canvasHeight),
        strokeWidth = markerWidth
    )

    // Marker circle at top with black outline
    val circleRadius = if (isDragging) 16.dp.toPx() else 14.dp.toPx()
    val circleCenter = Offset(x, 18.dp.toPx())

    // Black outline circle
    drawCircle(
        color = Color.Black,
        radius = circleRadius + 2.dp.toPx(),
        center = circleCenter
    )

    // Main circle
    drawCircle(
        color = color,
        radius = circleRadius,
        center = circleCenter
    )

    // Label text (A or B) in black for contrast
    // Draw a smaller inner circle for text background
    drawCircle(
        color = Color.Black.copy(alpha = 0.7f),
        radius = circleRadius - 4.dp.toPx(),
        center = circleCenter
    )

    // Dragging highlight
    if (isDragging) {
        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = 24.dp.toPx(),
            center = circleCenter
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
