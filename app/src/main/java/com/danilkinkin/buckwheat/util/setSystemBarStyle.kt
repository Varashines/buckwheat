package com.danilkinkin.buckwheat.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.danilkinkin.buckwheat.data.AppViewModel
import com.danilkinkin.buckwheat.data.SystemBarState

/**
 * Keeps the system bar icon contrast in sync with whatever content is currently on top.
 *
 * This used to go through Accompanist's SystemUiController, which is archived and explicitly
 * deprecated in favour of edge-to-edge. Bar colours are intentionally not set any more: every
 * call site passed Color.Transparent, and Window#setStatusBarColor is a no-op from Android 15
 * once the window is edge-to-edge - the app paints its own bar backgrounds in Compose.
 */
@Composable
fun setSystemStyle(
    style: () -> SystemBarState,
    key: Any = Unit,
    confirmChange: () -> Boolean = { true },
    appViewModel: AppViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val window = view.context.findActivity()?.window ?: return
    val insetsController = WindowCompat.getInsetsController(window, view)

    DisposableEffect(key) {
        if (!confirmChange()) {
            return@DisposableEffect onDispose { }
        }

        appViewModel.statusBarStack.add(style)
        insetsController.applyState(style())

        onDispose {
            appViewModel.statusBarStack.removeLastOrNull()

            // Restore the style of the screen underneath this one.
            appViewModel.statusBarStack.lastOrNull()?.let { previous ->
                insetsController.applyState(previous())
            }
        }
    }
}

private fun WindowInsetsControllerCompat.applyState(state: SystemBarState) {
    // "Light bars" is the platform flag for a light background, which draws dark icons.
    isAppearanceLightStatusBars = !state.statusBarDarkBackground
    isAppearanceLightNavigationBars = !state.navigationBarDarkBackground
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
