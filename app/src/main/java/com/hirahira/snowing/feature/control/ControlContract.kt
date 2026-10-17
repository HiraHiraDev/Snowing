package com.hirahira.snowing.feature.control

import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.LabQuality
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.TimeOfDay
import com.hirahira.snowing.settings.WindStrength

data class ControlUiState(
    /** False until the stored settings arrive; until then nothing below is real (B-008). */
    val loaded: Boolean = false,
    /** The user's choice, not whether the overlay is up this instant (ADR-0005). */
    val snowEnabled: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    /** Built-in moods, then the custom one once it has been saved. */
    val moods: List<MoodItem> = emptyList(),
    val adjust: AdjustUiState? = null,
    val timeOfDay: TimeOfDay = TimeOfDay.AUTO,
    /** Null hides the prototype lab (release builds). */
    val lab: LabUiState? = null,
) {
    val canToggle: Boolean get() = snowEnabled || hasOverlayPermission
}

data class MoodItem(
    val choice: MoodChoice,
    /** As designed, without the user's changes. */
    val intensity: Int,
    val selected: Boolean,
)

/** "Adjust": the active mood with the user's changes (ADR-0010). */
data class AdjustUiState(
    /** The mood the changes apply to. */
    val mood: MoodChoice,
    val edited: Boolean,
    /** Without Pro the controls show the mood but do not change it. */
    val locked: Boolean,
    val intensity: Int,
    val flakeSize: FlakeSize,
    val windAngle: Int,
    val windStrength: WindStrength,
)

data class LabUiState(
    val overrideMood: Boolean,
    val speed: Int,
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
    /** Drops any changes to the previous mood. */
    data class MoodSelected(val choice: MoodChoice) : ControlEvent
    data class IntensityChanged(val value: Int) : ControlEvent
    data class FlakeSizeChanged(val value: FlakeSize) : ControlEvent
    /** Degrees clockwise from up, as the wheel's needle points. */
    data class WindAngleChanged(val value: Int) : ControlEvent
    data class WindStrengthChanged(val value: WindStrength) : ControlEvent
    data object RestoreMood : ControlEvent
    /** Writes the adjusted mood to the custom slot and makes it the active one. */
    data object SaveMoodAsNew : ControlEvent
    data class TimeOfDayChanged(val value: TimeOfDay) : ControlEvent
    data class MoodOverrideToggled(val enabled: Boolean) : ControlEvent
    data class SpeedChanged(val value: Int) : ControlEvent
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
