package com.hirahira.snowing.debug.lab

import com.hirahira.snowing.settings.LabQuality

data class LabUiState(
    /** False until the stored settings arrive; until then the screen shows nothing. */
    val loaded: Boolean = false,
    val overrideMood: Boolean = false,
    val speed: Int = 0,
    val layers: Int = 0,
    val turbulence: Float = 0f,
    val gusts: Float = 0f,
    val showHud: Boolean = false,
    val showTracer: Boolean = false,
    val foreground: Boolean = false,
    val instantChanges: Boolean = false,
    val quality: LabQuality = LabQuality.AUTO,
)

sealed interface LabEvent {
    data class MoodOverrideToggled(val enabled: Boolean) : LabEvent
    data class SpeedChanged(val value: Int) : LabEvent
    data class LayersChanged(val value: Int) : LabEvent
    data class TurbulenceChanged(val value: Float) : LabEvent
    data class GustsChanged(val value: Float) : LabEvent
    data class QualityChanged(val value: LabQuality) : LabEvent
    /** The finger left a slider: write the final value now. */
    data object DragFinished : LabEvent
    data class HudToggled(val enabled: Boolean) : LabEvent
    data class TracerToggled(val enabled: Boolean) : LabEvent
    data class ForegroundToggled(val enabled: Boolean) : LabEvent
    data class InstantChangesToggled(val enabled: Boolean) : LabEvent
}
