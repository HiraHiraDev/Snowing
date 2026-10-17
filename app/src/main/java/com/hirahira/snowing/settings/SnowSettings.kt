package com.hirahira.snowing.settings

/**
 * What the user chose, in UI terms (ADR-0007, ADR-0010). Translated to physics by
 * [com.hirahira.snowing.overlay.SnowConfigMapper]; nothing here knows about pixels.
 */
data class SnowSettings(
    val mood: MoodChoice = DEFAULT_MOOD,
    /** The one custom slot, filled by "Save as new"; null until then. */
    val customMood: Mood? = null,
    /** Changes to the chosen mood ("edited"); choosing another mood drops them. */
    val moodAdjustment: MoodAdjustment? = null,
    /** Colors the app's background only, never the snow. */
    val timeOfDay: TimeOfDay = TimeOfDay.AUTO,
    val lab: LabSettings = LabSettings(),
) {
    /** The chosen mood as designed; an empty custom slot falls back to the default mood. */
    val baseMood: Mood get() = BuiltInMoods.of(mood) ?: customMood ?: BuiltInMoods.of(DEFAULT_MOOD)!!

    val activeMood: Mood get() = moodAdjustment?.let(baseMood::adjusted) ?: baseMood

    val isMoodAdjusted: Boolean get() = moodAdjustment != null

    /** What can be picked: the built-in moods, then the custom one once it has been saved. */
    val moodChoices: List<MoodChoice>
        get() = MoodChoice.BUILT_IN + listOfNotNull(MoodChoice.CUSTOM.takeIf { customMood != null })

    /** Picking a mood drops changes to the previous one (ADR-0010); an empty custom slot cannot be picked. */
    fun choosing(choice: MoodChoice): SnowSettings {
        if (choice !in moodChoices) return this
        return copy(mood = choice, moodAdjustment = null)
    }

    companion object {
        val DEFAULT_MOOD = MoodChoice.FIRST_SNOW
    }
}

/** Sky of the app's background (ADR-0010): [AUTO] follows the device clock. */
enum class TimeOfDay { AUTO, DAWN, DAY, DUSK, NIGHT }

/**
 * Prototype knobs for tuning the snowfall. Only exposed in debug builds;
 * release builds run with the defaults.
 */
data class LabSettings(
    /** Lets the four sliders below replace the mood's hidden parameters, for tuning moods. */
    val overrideMood: Boolean = false,
    /** In [Mood.LEVELS]. */
    val speed: Int = 3,
    /** In [LAYERS]. */
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
) {
    companion object {
        val LAYERS = 1..5
    }
}

enum class LabQuality { AUTO, HIGH, MEDIUM, LOW, SAVER }
