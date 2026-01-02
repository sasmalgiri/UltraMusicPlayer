package com.ultramusic.player.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Simple edge-swipe back gesture wrapper.
 *
 * - Only activates if the drag starts near the left edge.
 * - Triggers once when the swipe passes a threshold.
 * - Designed to be applied at the nav root so all screens benefit.
 */
@Composable
fun SwipeBackContainer(
    enabled: Boolean,
    onSwipeBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val edgeWidthPx = remember(density) { with(density) { 24.dp.toPx() } }
    val triggerDistancePx = remember(density) { with(density) { 120.dp.toPx() } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                var startedFromEdge = false
                var totalDrag = 0f

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        startedFromEdge = offset.x <= edgeWidthPx
                        totalDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!startedFromEdge) return@detectHorizontalDragGestures
                        totalDrag += dragAmount
                        if (totalDrag >= triggerDistancePx) {
                            startedFromEdge = false
                            change.consume()
                            onSwipeBack()
                        }
                    },
                    onDragCancel = {
                        startedFromEdge = false
                        totalDrag = 0f
                    },
                    onDragEnd = {
                        startedFromEdge = false
                        totalDrag = 0f
                    }
                )
            }
    ) {
        content()
    }
}
