package com.danilkinkin.buckwheat.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Intent-named haptics, so screens do not have to pick a raw [HapticFeedbackType].
 *
 * [tap] is the light tick for selections, [toggle] is the firmer pulse for a state change.
 */
class Haptics(private val feedback: HapticFeedback) {
    fun tap() = feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)

    fun toggle() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)

    fun confirm() = toggle()
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
