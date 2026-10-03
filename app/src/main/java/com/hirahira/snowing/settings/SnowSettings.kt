package com.hirahira.snowing.settings

/**
 * What the user chose, in UI terms (ADR-0007). Translated to physics by
 * [com.hirahira.snowing.overlay.SnowConfigMapper]; nothing here knows about pixels.
 */
data class SnowSettings(
    val mood: MoodChoice = MoodChoice.FIRST_SNOW,
    /** The user's own mood, kept even while a built-in one is active. */
    val customMood: Mood = BuiltInMoods.CUSTOM_BASE,
    /**
     * Degrees from straight down, positive blows to the right, within
     * ±[MAX_WIND_DIRECTION]. Applies to every mood; a mood only sets the strength.
     */
    val windDirection: Float = DEFAULT_WIND_DIRECTION,
    val lab: LabSettings = LabSettings(),
) {
    val activeMood: Mood get() = BuiltInMoods.of(mood) ?: customMood

    companion object {
        // Not straight down, so a mood's wind shows before the wind wheel exists.
        const val DEFAULT_WIND_DIRECTION = 20f
        const val MAX_WIND_DIRECTION = 75f
    }
}

/**
 * Prototype knobs for tuning the snowfall. Only exposed in debug builds;
 * release builds run with the defaults.
 */
data class LabSettings(
    /** Lets the three sliders below replace the mood's hidden parameters, for tuning moods. */
    val overrideMood: Boolean = false,
    val layers: Int = 3,
    /** Swirls in the air and each flake's flutter, 0..1. */
    val turbulence: Float = 0.5f,
    /** Gusts sweeping across the screen, 0..1. */
    val gusts: Float = 0.4f,
    val showHud: Boolean = false,
    val showTracer: Boolean = false,
    /** Rare out-of-focus flakes in front of the snow. */
    val foreground: Boolean = true,
    /** Apply changes to falling flakes at once instead of as a front from the top (ADR-0006). */
    val instantChanges: Boolean = false,
    /** Pins the quality level for testing; [LabQuality.AUTO] lets the device decide (ADR-0008). */
    val quality: LabQuality = LabQuality.AUTO,
)

enum class LabQuality { AUTO, HIGH, MEDIUM, LOW, SAVER }
