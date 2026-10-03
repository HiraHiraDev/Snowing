package com.hirahira.snowing.settings

/** Flake size class of a mood: three steps, no slider (ADR-0007). */
enum class FlakeSize { S, M, L }

/**
 * The character of the snow (ADR-0007), in UI steps rather than physics;
 * [com.hirahira.snowing.overlay.SnowConfigMapper] turns it into a SnowConfig.
 * [wind] is a strength only: the direction is a global setting.
 */
data class Mood(
    val flakeSize: FlakeSize,
    /** In [LEVELS]. */
    val intensity: Int,
    /** In [LEVELS]. */
    val speed: Int,
    /** In [WIND_LEVELS]; 0 means no wind. */
    val wind: Int,
    // Tuned in the Prototype lab, never edited by users.
    val layers: Int,
    /** Swirls in the air and each flake's flutter, 0..1. */
    val turbulence: Float,
    /** Gusts sweeping across the screen, 0..1. */
    val gusts: Float,
) {
    companion object {
        val LEVELS = 1..10
        val WIND_LEVELS = 0..10
    }
}

/** The active mood: one of the built-ins or the user's own. [key] is what the store keeps. */
enum class MoodChoice(val key: String) {
    FIRST_SNOW("first_snow"),
    FLUFF("fluff"),
    HEAVY("heavy"),
    BLIZZARD("blizzard"),
    CUSTOM("custom"),
    ;

    companion object {
        fun fromKey(key: String?): MoodChoice? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Built-in moods live in code, not in the user's store: users cannot change
 * them, and a retune reaches everyone with the next update (ADR-0007).
 * Values are starting points for tuning in the lab.
 */
object BuiltInMoods {
    val FIRST_SNOW = Mood(FlakeSize.M, intensity = 3, speed = 3, wind = 0, layers = 3, turbulence = 0.4f, gusts = 0.3f)
    val FLUFF = Mood(FlakeSize.L, intensity = 5, speed = 2, wind = 2, layers = 3, turbulence = 0.6f, gusts = 0.3f)
    val HEAVY = Mood(FlakeSize.M, intensity = 8, speed = 5, wind = 2, layers = 4, turbulence = 0.5f, gusts = 0.4f)
    val BLIZZARD = Mood(FlakeSize.S, intensity = 10, speed = 8, wind = 8, layers = 5, turbulence = 0.7f, gusts = 0.8f)

    /** Null for [MoodChoice.CUSTOM], which is the user's data. */
    fun of(choice: MoodChoice): Mood? = when (choice) {
        MoodChoice.FIRST_SNOW -> FIRST_SNOW
        MoodChoice.FLUFF -> FLUFF
        MoodChoice.HEAVY -> HEAVY
        MoodChoice.BLIZZARD -> BLIZZARD
        MoodChoice.CUSTOM -> null
    }

    /** The custom mood starts as Fluff and always keeps Fluff's hidden parameters. */
    val CUSTOM_BASE = FLUFF

    fun custom(flakeSize: FlakeSize, intensity: Int, speed: Int, wind: Int): Mood = CUSTOM_BASE.copy(
        flakeSize = flakeSize,
        intensity = intensity.coerceIn(Mood.LEVELS),
        speed = speed.coerceIn(Mood.LEVELS),
        wind = wind.coerceIn(Mood.WIND_LEVELS),
    )
}
