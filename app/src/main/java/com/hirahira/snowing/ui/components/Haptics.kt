package com.hirahira.snowing.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * What touching a control feels like, matched to how it moves; the one place to retune it.
 * Below API 34 Android falls back to older effects (a clock tick, a context click), and it
 * skips all of them when the user turns touch feedback off.
 */
@Stable
internal class ControlHaptics(private val feedback: HapticFeedback) {
    /** A drag passes a step, e.g. a level of the ruler: the lightest tick, it may come often. */
    fun step() = feedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)

    /** A drag passes a marked step, e.g. a direction with a tick on the dial. */
    fun notch() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** A tap picks another option. */
    fun pick() = feedback.performHapticFeedback(HapticFeedbackType.SegmentTick)

    /** A value reaches either end of its range: firmer, so the end is felt. */
    fun limit() = feedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
}

@Composable
internal fun rememberControlHaptics(): ControlHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { ControlHaptics(feedback) }
}
