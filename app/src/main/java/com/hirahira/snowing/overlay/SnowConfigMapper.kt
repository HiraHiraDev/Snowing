package com.hirahira.snowing.overlay

import com.hirahira.snowing.engine.QualityLevel
import com.hirahira.snowing.engine.SnowConfig
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.LabQuality
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.WindStrength
import kotlin.math.sin

/**
 * The single place where the look of the snow is tuned: mood levels and
 * lab sliders map linearly onto these physical ranges (ADR-0007, ADR-0010).
 */
object SnowTuning {
    /** Flakes per 10 000 dp². */
    val DensityRange = 0.6f..7f

    /** Fall speed of the closest layer, dp/s. */
    val SpeedRange = 25f..140f

    /** Flake radius from the farthest to the closest layer, dp. */
    val RadiusRange = 1.2f..4.2f

    /** Mean wind per mood strength, dp/s, before the direction takes its share (ADR-0010). */
    fun windSpeed(strength: WindStrength): Float = when (strength) {
        WindStrength.CALM -> 0f
        WindStrength.GENTLE -> 35f
        WindStrength.GUSTY -> 80f
    }

    /** Added to the mood's gusts (0..1): a gusty wind also gusts harder. */
    fun gustBoost(strength: WindStrength): Float = when (strength) {
        WindStrength.CALM, WindStrength.GENTLE -> 0f
        WindStrength.GUSTY -> 0.3f
    }

    /** Flake radius multiplier per mood size class. */
    fun sizeFactor(size: FlakeSize): Float = when (size) {
        FlakeSize.S -> 0.75f
        FlakeSize.M -> 1f
        FlakeSize.L -> 1.35f
    }

    /** Gust strength, dp/s. */
    val GustsRange = 0f..45f

    /** Speed of the swirls in the air, dp/s. */
    val TurbulenceRange = 0f..40f

    /** Flutter of each flake around its path, dp; follows the turbulence slider. */
    val FlutterRange = 0f..5f

    const val OPACITY = 0.95f

    // Bokeh: out-of-focus flakes right in front of the lens. Rare, heavily blurred,
    // about the size of a snowflake, and faster than the snow because they are so close.

    /** Flakes per 10 000 dp²; with the respawn gap only a few are on screen at a time. */
    const val BOKEH_DENSITY = 0.15f

    /** Radius from the farther to the closer bokeh layer, dp. */
    val BokehRadiusRange = 4f..8f

    const val BOKEH_LAYERS = 2
    const val BOKEH_MOTION_FACTOR = 2.2f
    const val BOKEH_FLUTTER_FACTOR = 2f
    const val BOKEH_OPACITY = 0.4f

    /** Gap above the screen before a bokeh flake returns, in screen heights: they pass only now and then. */
    const val BOKEH_RESPAWN_SPREAD = 1.5f
}

/** Everything the overlay draws: the snowfall and the rare out-of-focus flakes in front of it. */
data class SnowScene(
    val snow: SnowConfig,
    val foreground: SnowConfig,
)

object SnowConfigMapper {

    /** The user's snow, thinned out by [level] (ADR-0008 §2). */
    fun toScene(settings: SnowSettings, level: QualityLevel): SnowScene {
        val snow = toConfig(settings)
        return SnowScene(
            snow = level.apply(snow),
            foreground = level.apply(foregroundFor(snow, enabled = settings.lab.foreground)),
        )
    }

    fun forcedQuality(settings: SnowSettings): QualityLevel? = when (settings.lab.quality) {
        LabQuality.AUTO -> null
        LabQuality.HIGH -> QualityLevel.HIGH
        LabQuality.MEDIUM -> QualityLevel.MEDIUM
        LabQuality.LOW -> QualityLevel.LOW
        LabQuality.SAVER -> QualityLevel.SAVER
    }

    /** The active mood, adjustments included; the lab may replace the mood's hidden parameters. */
    fun toConfig(settings: SnowSettings): SnowConfig {
        val mood = settings.activeMood
        val lab = settings.lab
        val speed = if (lab.overrideMood) lab.speed else mood.speed
        val layers = if (lab.overrideMood) lab.layers else mood.layers
        val turbulence = if (lab.overrideMood) lab.turbulence else mood.turbulence
        val gusts = if (lab.overrideMood) lab.gusts else mood.gusts
        val size = SnowTuning.sizeFactor(mood.flakeSize)
        // The wheel turns all the way round, but snow always falls down (ADR-0006, ADR-0010),
        // so only the sideways part of the needle counts: 90° blows right, 270° left.
        val sideways = sin(Math.toRadians(mood.windAngle.toDouble())).toFloat()
        return SnowConfig(
            density = SnowTuning.DensityRange.at(mood.intensity.fractionOf(Mood.LEVELS)),
            fallSpeed = SnowTuning.SpeedRange.at(speed.fractionOf(Mood.LEVELS)),
            minRadius = SnowTuning.RadiusRange.start * size,
            maxRadius = SnowTuning.RadiusRange.endInclusive * size,
            layers = layers.coerceAtLeast(1),
            wind = SnowTuning.windSpeed(mood.windStrength) * sideways,
            gusts = SnowTuning.GustsRange.at(gusts + SnowTuning.gustBoost(mood.windStrength)),
            turbulence = SnowTuning.TurbulenceRange.at(turbulence),
            flutter = SnowTuning.FlutterRange.at(turbulence),
            opacity = SnowTuning.OPACITY,
        )
    }

    // Density 0 when disabled, so switching bokeh off lets the last ones fall out (ADR-0006).
    private fun foregroundFor(snow: SnowConfig, enabled: Boolean): SnowConfig = SnowConfig(
        density = if (enabled) SnowTuning.BOKEH_DENSITY else 0f,
        fallSpeed = snow.fallSpeed * SnowTuning.BOKEH_MOTION_FACTOR,
        minRadius = SnowTuning.BokehRadiusRange.start,
        maxRadius = SnowTuning.BokehRadiusRange.endInclusive,
        layers = SnowTuning.BOKEH_LAYERS,
        wind = snow.wind * SnowTuning.BOKEH_MOTION_FACTOR,
        gusts = snow.gusts * SnowTuning.BOKEH_MOTION_FACTOR,
        turbulence = snow.turbulence * SnowTuning.BOKEH_MOTION_FACTOR,
        flutter = snow.flutter * SnowTuning.BOKEH_FLUTTER_FACTOR,
        opacity = SnowTuning.BOKEH_OPACITY,
        respawnSpread = SnowTuning.BOKEH_RESPAWN_SPREAD,
    )

    fun toRenderOptions(settings: SnowSettings): RenderOptions = RenderOptions(
        showHud = settings.lab.showHud,
        showTracer = settings.lab.showTracer,
    )

    private fun Int.fractionOf(levels: IntRange): Float =
        (coerceIn(levels) - levels.first).toFloat() / (levels.last - levels.first)

    private fun ClosedFloatingPointRange<Float>.at(fraction: Float): Float =
        start + (endInclusive - start) * fraction.coerceIn(0f, 1f)
}
