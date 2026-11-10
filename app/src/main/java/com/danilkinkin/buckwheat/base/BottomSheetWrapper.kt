package com.danilkinkin.buckwheat.base

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danilkinkin.buckwheat.data.AppViewModel
import com.danilkinkin.buckwheat.data.SystemBarState
import com.danilkinkin.buckwheat.ui.isNightMode
import com.danilkinkin.buckwheat.util.setSystemStyle
import kotlinx.coroutines.launch

data class BottomSheetScrollState(
    val topPadding: Dp,
)

val LocalBottomSheetScrollState = compositionLocalOf { BottomSheetScrollState(0.dp) }

/**
 * What sheet content gets to drive its own dismissal.
 *
 * The animation lives in Material3's [SheetState]; this only keeps the arguments and the
 * result callback that the app's PathState model passes around.
 */
@OptIn(ExperimentalMaterial3Api::class)
class ModalBottomSheetState internal constructor(
    private val sheetState: SheetState,
    private val onDismissed: () -> Unit,
) {
    var args: Map<String, Any?> = emptyMap()
        internal set

    private var callback: (result: Map<String, Any?>) -> Unit = {}

    fun bindCallback(callback: (result: Map<String, Any?>) -> Unit) {
        this.callback = callback
    }

    suspend fun hide(result: Map<String, Any?> = emptyMap()) {
        callback(result)
        sheetState.hide()
        onDismissed()
    }
}

/**
 * Hosts one sheet from the [AppViewModel] sheet map.
 *
 * This used to run a vendored Material2 swipeable and a hand-written layout. Material3's
 * [ModalBottomSheet] now owns the drag, nested scroll, fling, predictive back and window
 * handling. The sheet sits below the status bar and the drag handle stays with the content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetWrapper(
    name: String,
    appViewModel: AppViewModel = viewModel(),
    cancelable: Boolean = true,
    content: @Composable (state: ModalBottomSheetState) -> Unit,
) {
    val sheets by appViewModel.sheetStates.collectAsStateWithLifecycle()
    val entry = sheets[name] ?: return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            if (value == androidx.compose.material3.SheetValue.Hidden) cancelable else true
        },
    )
    val scope = rememberCoroutineScope()

    val state = remember(sheetState, entry) {
        ModalBottomSheetState(
            sheetState = sheetState,
            onDismissed = { appViewModel.closeSheet(name) },
        ).apply {
            args = entry.args
            bindCallback(entry.callback)
        }
    }

    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    val isNightModeNow = isNightMode()

    setSystemStyle(
        style = {
            SystemBarState(
                statusBarDarkBackground = isNightModeNow,
                navigationBarDarkBackground = isNightModeNow,
            )
        },
        key = isNightModeNow,
    )

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch { state.hide() }
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        sheetGesturesEnabled = cancelable,
        dragHandle = if (cancelable) {
            { BottomSheetDefaults.DragHandle() }
        } else {
            null
        },
        // Top only: sheet content already pads its own navigation bar area.
        contentWindowInsets = { WindowInsets.statusBars },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = cancelable,
            shouldDismissOnClickOutside = cancelable,
        ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            LocalBottomSheetScrollState provides BottomSheetScrollState(0.dp),
        ) {
            content(state)
        }
    }
}
