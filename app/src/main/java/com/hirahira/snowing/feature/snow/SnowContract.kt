package com.hirahira.snowing.feature.snow

import com.hirahira.snowing.settings.MoodChoice

data class SnowUiState(
    /** False until the stored settings arrive; until then the screen shows nothing (B-008). */
    val loaded: Boolean = false,
    /** The user's choice, not whether the overlay is up this instant (ADR-0005). */
    val snowEnabled: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val mood: MoodChoice = MoodChoice.FIRST_SNOW,
    /** Of the active mood, adjustments included. */
    val intensity: Int = 0,
) {
    /** Without the permission the button leads to system settings instead of starting snow. */
    val startNeedsPermission: Boolean get() = !snowEnabled && !hasOverlayPermission
}

sealed interface SnowEvent {
    data object ToggleSnow : SnowEvent
    /** Screen came to the foreground: re-check permission, restore snow if it was lost. */
    data object Resumed : SnowEvent
}
