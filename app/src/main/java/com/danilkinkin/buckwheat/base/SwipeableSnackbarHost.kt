package com.danilkinkin.buckwheat.base

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

enum class SwipeDirection {
    Left,
    Initial,
    Right,
}

/**
 * A snackbar host that can be flicked left or right to dismiss it.
 *
 * Material 2's swipeable is deprecated, so this drives a foundation [AnchoredDraggableState]
 * directly. The visual result is the same: a third of the width commits the dismissal.
 */
@Composable
fun SwipeableSnackbarHost(hostState: SnackbarHostState) {
    if (hostState.currentSnackbarData == null) return

    var size by remember { mutableStateOf(Size.Zero) }
    val width = remember(size) { if (size.width == 0f) 1f else size.width }
    val state = remember {
        AnchoredDraggableState<SwipeDirection>(initialValue = SwipeDirection.Initial)
    }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance -> distance * 0.3f },
        animationSpec = tween(durationMillis = 200),
    )

    SideEffect {
        state.updateAnchors(
            DraggableAnchors {
                SwipeDirection.Left at -width
                SwipeDirection.Initial at 0f
                SwipeDirection.Right at width
            },
            state.settledValue,
        )
    }

    LaunchedEffect(state.settledValue) {
        if (state.settledValue != SwipeDirection.Initial) {
            hostState.currentSnackbarData?.dismiss()
        }
    }

    SnackbarHost(
        hostState,
        snackbar = { snackbarData ->
            Snackbar(
                snackbarData,
                // Read the swipe offset inside the placement lambda: dragging then only
                // re-places the snackbar instead of recomposing it on every frame.
                modifier = Modifier.offset {
                    val offset = state.offset.takeUnless { it.isNaN() } ?: 0f
                    IntOffset(offset.roundToInt(), 0)
                },
            )
        },
        modifier = Modifier
            .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal,
                flingBehavior = flingBehavior,
            ),
    )
}
