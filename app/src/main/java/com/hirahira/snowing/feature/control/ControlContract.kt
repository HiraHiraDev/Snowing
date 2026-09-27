package com.hirahira.snowing.feature.control

data class ControlUiState(
    /** The user's choice, not whether the overlay is up this instant (ADR-0005). */
    val snowEnabled: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val intensity: Float = 0f,
    val speed: Float = 0f,
    /** Null hides the prototype lab (release builds). */
    val lab: LabUiState? = null,
) {
    val canToggle: Boolean get() = snowEnabled || hasOverlayPermission
}

data class LabUiState(
    val layers: Int,
    val sway: Float,
    val showHud: Boolean,
    val showTracer: Boolean,
)

sealed interface ControlEvent {
    data object ToggleSnow : ControlEvent
    /** Screen came to the foreground: re-check permission, restore snow if it was lost. */
    data object Resumed : ControlEvent
    data class IntensityChanged(val value: Float) : ControlEvent
    data class SpeedChanged(val value: Float) : ControlEvent
    data class LayersChanged(val value: Int) : ControlEvent
    data class SwayChanged(val value: Float) : ControlEvent
    data class HudToggled(val enabled: Boolean) : ControlEvent
    data class TracerToggled(val enabled: Boolean) : ControlEvent
}
