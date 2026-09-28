package com.hirahira.snowing.overlay

import com.hirahira.snowing.engine.SnowConfig
import com.hirahira.snowing.settings.SnowSettings

/**
 * The single place where the look of the snow is tuned: slider positions
 * (0..1) map linearly onto these physical ranges.
 */
object SnowTuning {
    /** Flakes per 10 000 dp². */
    val DensityRange = 0.6f..7f

    /** Fall speed of the closest layer, dp/s. */
    val SpeedRange = 25f..140f

    /** Flake radius from the farthest to the closest layer, dp. */
    val RadiusRange = 1.2f..4.2f

    /** Strongest mean wind either way, dp/s (lab slider -1..1). */
    const val WIND_MAX_DP_PER_S = 60f

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

    fun toScene(settings: SnowSettings): SnowScene {
        val snow = toConfig(settings)
        return SnowScene(snow = snow, foreground = foregroundFor(snow, enabled = settings.lab.foreground))
    }

    fun toConfig(settings: SnowSettings): SnowConfig = SnowConfig(
        density = SnowTuning.DensityRange.at(settings.intensity),
        fallSpeed = SnowTuning.SpeedRange.at(settings.speed),
        minRadius = SnowTuning.RadiusRange.start,
        maxRadius = SnowTuning.RadiusRange.endInclusive,
        layers = settings.lab.layers.coerceAtLeast(1),
        wind = settings.lab.wind.coerceIn(-1f, 1f) * SnowTuning.WIND_MAX_DP_PER_S,
        gusts = SnowTuning.GustsRange.at(settings.lab.gusts),
        turbulence = SnowTuning.TurbulenceRange.at(settings.lab.turbulence),
        flutter = SnowTuning.FlutterRange.at(settings.lab.turbulence),
        opacity = SnowTuning.OPACITY,
    )

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

    private fun ClosedFloatingPointRange<Float>.at(fraction: Float): Float =
        start + (endInclusive - start) * fraction.coerceIn(0f, 1f)
}
