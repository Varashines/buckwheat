package com.danilkinkin.buckwheat.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-wide confirmation toast, themed instead of the OS one.
 *
 * The system toast is the last unthemed surface in the app: it sits behind the sheet stack and
 * cannot follow the palette. Any screen can call [show]; a single [AppToastHost] near the root
 * renders it above everything.
 */
object AppToast {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var token = 0

    fun show(text: String) {
        val mine = ++token
        _message.value = text

        scope.launch {
            delay(2400)
            if (token == mine) _message.value = null
        }
    }
}

@Composable
fun AppToastHost(modifier: Modifier = Modifier) {
    val message by AppToast.message.collectAsState()

    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            ),
            shadowElevation = 12.dp,
        ) {
            Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)) {
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
