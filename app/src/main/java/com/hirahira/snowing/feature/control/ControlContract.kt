package com.hirahira.snowing.feature.control

import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.LabQuality
import com.hirahira.snowing.settings.MoodChoice

data class ControlUiState(
    /** The user's choice, not whether the overlay is up this instant (ADR-0005). */
    val snowEnabled: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val moods: List<MoodItem> = emptyList(),
    /** The custom mood's editor; null while a built-in mood is active. */
    val customMood: CustomMoodUiState? = null,
    /** Null hides the prototype lab (release builds). */
    val lab: LabUiState? = null,
) {
    val canToggle: Boolean get() = snowEnabled || hasOverlayPermission
}

data class MoodItem(
    val choice: MoodChoice,
    val intensity: Int,
    val selected: Boolean,
)

data class CustomMoodUiState(
    val flakeSize: FlakeSize,
    val intensity: Int,
    val speed: Int,
    val wind: Int,
)

data class LabUiState(
    val windDirection: Float,
    val overrideMood: Boolean,
    val layers: Int,
    val turbulence: Float,
    val gusts: Float,
    val showHud: Boolean,
    val showTracer: Boolean,
    val foreground: Boolean,
    val instantChanges: Boolean,
    val quality: LabQuality,
)

sealed interface ControlEvent {
    data object ToggleSnow : ControlEvent
    /** Screen came to the foreground: re-check permission, restore snow if it was lost. */
    data object Resumed : ControlEvent
    data class MoodSelected(val choice: MoodChoice) : ControlEvent
    data class CustomFlakeSizeChanged(val value: FlakeSize) : ControlEvent
    data class CustomIntensityChanged(val value: Int) : ControlEvent
    data class CustomSpeedChanged(val value: Int) : ControlEvent
    data class CustomWindChanged(val value: Int) : ControlEvent
    /** Degrees from straight down; the lab sets it until the wind wheel exists (ADR-0007). */
    data class WindDirectionChanged(val value: Float) : ControlEvent
    data class MoodOverrideToggled(val enabled: Boolean) : ControlEvent
    data class LayersChanged(val value: Int) : ControlEvent
    data class TurbulenceChanged(val value: Float) : ControlEvent
    data class GustsChanged(val value: Float) : ControlEvent
    data class QualityChanged(val value: LabQuality) : ControlEvent
    /** The finger left a slider: write the final value now. */
    data object SliderReleased : ControlEvent
    data class HudToggled(val enabled: Boolean) : ControlEvent
    data class TracerToggled(val enabled: Boolean) : ControlEvent
    data class ForegroundToggled(val enabled: Boolean) : ControlEvent
    data class InstantChangesToggled(val enabled: Boolean) : ControlEvent
}
