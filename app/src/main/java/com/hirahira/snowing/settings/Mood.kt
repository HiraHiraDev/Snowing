package com.hirahira.snowing.settings

/** Flake size class of a mood: three steps, no slider (ADR-0007). */
enum class FlakeSize { S, M, L }

/** Wind strength of a mood: three steps (ADR-0010). */
enum class WindStrength { CALM, GENTLE, GUSTY }

/**
 * The character of the snow (ADR-0007, ADR-0010), in UI steps rather than physics;
 * [com.hirahira.snowing.overlay.SnowConfigMapper] turns it into a SnowConfig.
 */
data class Mood(
    /** In [LEVELS]. */
    val intensity: Int,
    val flakeSize: FlakeSize,
    /** Where the wheel's needle points, degrees clockwise from up, in [WIND_ANGLES]. */
    val windAngle: Int,
    val windStrength: WindStrength,
    // Tuned in the Prototype lab, never edited by users.
    /** In [LEVELS]. */
    val speed: Int,
    val layers: Int,
    /** Swirls in the air and each flake's flutter, 0..1. */
    val turbulence: Float,
    /** Gusts sweeping across the screen, 0..1. */
    val gusts: Float,
) {
    /** The part a user sets on their own mood. */
    val look: MoodLook get() = MoodLook(intensity, flakeSize, windAngle, windStrength)

    fun withLook(look: MoodLook): Mood = look.normalized().let {
        copy(intensity = it.intensity, flakeSize = it.flakeSize, windAngle = it.windAngle, windStrength = it.windStrength)
    }

    companion object {
        val LEVELS = 1..10
        val WIND_ANGLES = 0..359
    }
}

/** What a user sets on their own mood (ADR-0010); the hidden parameters stay the ones it was created with. */
data class MoodLook(
    val intensity: Int,
    val flakeSize: FlakeSize,
    val windAngle: Int,
    val windStrength: WindStrength,
) {
    fun normalized(): MoodLook = copy(
        intensity = intensity.coerceIn(Mood.LEVELS),
        windAngle = windAngle.mod(Mood.WIND_ANGLES.count()),
    )
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
        val BUILT_IN = entries - CUSTOM

        fun fromKey(key: String?): MoodChoice? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Built-in moods live in code, not in the user's store: users cannot change
 * them, and a retune reaches everyone with the next update (ADR-0007).
 * Values are starting points for tuning in the lab (ADR-0010).
 */
object BuiltInMoods {
    // Calm moods point straight down, so a wind picked later starts from there.
    val FIRST_SNOW = Mood(
        intensity = 3, flakeSize = FlakeSize.S, windAngle = 180, windStrength = WindStrength.CALM,
        speed = 3, layers = 3, turbulence = 0.4f, gusts = 0.3f,
    )
    val FLUFF = Mood(
        intensity = 4, flakeSize = FlakeSize.S, windAngle = 180, windStrength = WindStrength.CALM,
        speed = 2, layers = 3, turbulence = 0.6f, gusts = 0.3f,
    )
    val HEAVY = Mood(
        intensity = 6, flakeSize = FlakeSize.M, windAngle = 200, windStrength = WindStrength.GENTLE,
        speed = 5, layers = 4, turbulence = 0.5f, gusts = 0.4f,
    )

    // The design gives no direction for the blizzard; 240° is a guess to tune in the lab.
    val BLIZZARD = Mood(
        intensity = 9, flakeSize = FlakeSize.L, windAngle = 240, windStrength = WindStrength.GUSTY,
        speed = 8, layers = 5, turbulence = 0.7f, gusts = 0.8f,
    )

    /** Null for [MoodChoice.CUSTOM], which is the user's data. */
    fun of(choice: MoodChoice): Mood? = when (choice) {
        MoodChoice.FIRST_SNOW -> FIRST_SNOW
        MoodChoice.FLUFF -> FLUFF
        MoodChoice.HEAVY -> HEAVY
        MoodChoice.BLIZZARD -> BLIZZARD
        MoodChoice.CUSTOM -> null
    }
}
