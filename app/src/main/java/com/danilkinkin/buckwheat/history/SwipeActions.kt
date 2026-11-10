package com.danilkinkin.buckwheat.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.danilkinkin.buckwheat.R
import com.danilkinkin.buckwheat.ui.BuckwheatTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class SwipeActionsConfig(
    val threshold: Float,
    val icon: Painter?,
    val iconTint: Color,
    val background: Color,
    val backgroundActive: Color,
    val stayDismissed: Boolean,
    val onDismiss: () -> Unit,
)

val DefaultSwipeActionsConfig = SwipeActionsConfig(
    threshold = 0.4f,
    icon = null,
    iconTint = Color.Transparent,
    background = Color.Transparent,
    backgroundActive = Color.Transparent,
    stayDismissed = false,
    onDismiss = {},
)

private enum class SwipeAnchor { EndToStart, Initial, StartToEnd }

/**
 * Swipe right for the start action, left for the end action.
 *
 * This used to be built on Material 2's SwipeToDismiss, which is deprecated. It now drives a
 * foundation [AnchoredDraggableState] directly, so the drag offsets, thresholds and spring-back
 * are explicit and the swipe visuals are unchanged.
 */
@Composable
fun SwipeActions(
    modifier: Modifier = Modifier,
    startActionsConfig: SwipeActionsConfig = DefaultSwipeActionsConfig,
    endActionsConfig: SwipeActionsConfig = DefaultSwipeActionsConfig,
    content: @Composable () -> Unit,
) = BoxWithConstraints(modifier) {
    val width = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val haptic = LocalHapticFeedback.current

    val currentStart by rememberUpdatedState(startActionsConfig)
    val currentEnd by rememberUpdatedState(endActionsConfig)

    val state = remember {
        AnchoredDraggableState<SwipeAnchor>(
            initialValue = SwipeAnchor.Initial,
            confirmValueChange = { target ->
                when (target) {
                    SwipeAnchor.StartToEnd -> {
                        currentStart.onDismiss()
                        currentStart.stayDismissed
                    }

                    SwipeAnchor.EndToStart -> {
                        currentEnd.onDismiss()
                        currentEnd.stayDismissed
                    }

                    SwipeAnchor.Initial -> true
                }
            },
        )
    }

    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance -> distance * currentStart.threshold },
        animationSpec = tween(durationMillis = 200),
    )

    SideEffect {
        state.updateAnchors(
            DraggableAnchors {
                SwipeAnchor.EndToStart at -width
                SwipeAnchor.Initial at 0f
                SwipeAnchor.StartToEnd at width
            },
            state.settledValue,
        )
    }

    val offset = state.offset.takeUnless { it.isNaN() } ?: 0f
    val direction = when {
        offset > 0f -> SwipeAnchor.StartToEnd
        offset < 0f -> SwipeAnchor.EndToStart
        else -> SwipeAnchor.Initial
    }

    val config = when (direction) {
        SwipeAnchor.StartToEnd -> currentStart
        SwipeAnchor.EndToStart -> currentEnd
        SwipeAnchor.Initial -> DefaultSwipeActionsConfig
    }

    val progress = (abs(offset) / width).coerceIn(0f, 1f)
    val willDismiss = progress > config.threshold

    LaunchedEffect(willDismiss, direction) {
        if (willDismiss && direction != SwipeAnchor.Initial) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val restingIconTint = contentColorFor(config.background)
    val iconScale by animateFloatAsState(
        targetValue = if (willDismiss) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "swipeIcon",
    )

    Box(
        modifier = Modifier.anchoredDraggable(
            state = state,
            orientation = Orientation.Horizontal,
            flingBehavior = flingBehavior,
        ),
    ) {
        if (direction != SwipeAnchor.Initial) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(config.background),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CirclePath(progress, direction == SwipeAnchor.StartToEnd))
                        .background(config.backgroundActive),
                )

                Box(
                    modifier = Modifier
                        .align(
                            when (direction) {
                                SwipeAnchor.StartToEnd -> Alignment.CenterStart
                                else -> Alignment.CenterEnd
                            }
                        )
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .scale(iconScale),
                    contentAlignment = Alignment.Center,
                ) {
                    config.icon?.let { icon ->
                        Image(
                            painter = icon,
                            colorFilter = ColorFilter.tint(
                                if (willDismiss) config.iconTint else restingIconTint,
                            ),
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.roundToInt(), 0) },
        ) {
            content()
        }
    }
}

class CirclePath(private val progress: Float, private val start: Boolean) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val origin = Offset(
            x = if (start) size.height / 2 else size.width - size.height / 2,
            y = size.center.y,
        )

        val radius = sqrt(size.height * size.height + size.width * size.width) * progress

        return Outline.Generic(
            Path().apply {
                addOval(
                    Rect(
                        center = origin,
                        radius = radius,
                    )
                )
            }
        )
    }
}

@Preview(widthDp = 300)
@Composable
private fun PreviewDefault() {
    BuckwheatTheme {
        SwipeActions(
            startActionsConfig = SwipeActionsConfig(
                threshold = 0.4f,
                background = MaterialTheme.colorScheme.tertiaryContainer,
                backgroundActive = MaterialTheme.colorScheme.tertiary,
                iconTint = MaterialTheme.colorScheme.onTertiary,
                icon = painterResource(R.drawable.ic_edit),
                stayDismissed = false,
                onDismiss = {},
            ),
            endActionsConfig = SwipeActionsConfig(
                threshold = 0.4f,
                background = MaterialTheme.colorScheme.errorContainer,
                backgroundActive = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.onError,
                icon = painterResource(R.drawable.ic_delete_forever),
                stayDismissed = false,
                onDismiss = {},
            ),
        ) {
            Text(
                text = "Swipe me",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(24.dp),
            )
        }
    }
}
