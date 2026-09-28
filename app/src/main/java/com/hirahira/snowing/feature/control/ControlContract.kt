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
    val turbulence: Float,
    val wind: Float,
    val gusts: Float,
    val showHud: Boolean,
    val showTracer: Boolean,
    val foreground: Boolean,
    val instantChanges: Boolean,
)

sealed interface ControlEvent {
    data object ToggleSnow : ControlEvent
    /** Screen came to the foreground: re-check permission, restore snow if it was lost. */
    data object Resumed : ControlEvent
    data class IntensityChanged(val value: Float) : ControlEvent
    data class SpeedChanged(val value: Float) : ControlEvent
    data class LayersChanged(val value: Int) : ControlEvent
    data class TurbulenceChanged(val value: Float) : ControlEvent
    data class WindChanged(val value: Float) : ControlEvent
    data class GustsChanged(val value: Float) : ControlEvent
    /** The finger left a slider: write the final value now. */
    data object SliderReleased : ControlEvent
    data class HudToggled(val enabled: Boolean) : ControlEvent
    data class TracerToggled(val enabled: Boolean) : ControlEvent
    data class ForegroundToggled(val enabled: Boolean) : ControlEvent
    data class InstantChangesToggled(val enabled: Boolean) : ControlEvent
}
