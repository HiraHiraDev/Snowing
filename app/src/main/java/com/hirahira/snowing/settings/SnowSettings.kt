package com.hirahira.snowing.settings

/**
 * What the user chose, in UI terms (normalized 0..1 sliders).
 * Translated to physics by [SnowConfigMapper]; nothing here knows about pixels.
 */
data class SnowSettings(
    val intensity: Float = 0.45f,
    val speed: Float = 0.4f,
    val lab: LabSettings = LabSettings(),
)

/**
 * Prototype knobs for tuning the snowfall. Only exposed in debug builds;
 * release builds run with the defaults.
 */
data class LabSettings(
    val layers: Int = 3,
    val sway: Float = 0.5f,
    val showHud: Boolean = false,
    val showTracer: Boolean = false,
    /** Rare out-of-focus flakes in front of the snow. */
    val foreground: Boolean = true,
    /** Apply changes to falling flakes at once instead of as a front from the top (ADR-0006). */
    val instantChanges: Boolean = false,
)
