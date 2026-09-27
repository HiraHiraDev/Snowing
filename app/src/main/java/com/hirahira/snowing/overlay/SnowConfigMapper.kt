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

    /** Sway amplitude of the closest layer, dp. */
    val SwayRange = 0f..28f

    const val SWAY_FREQUENCY_HZ = 0.35f
    const val WIND_DP_PER_S = 0f
    const val OPACITY = 0.95f

    // Bokeh: out-of-focus flakes right in front of the lens. Rare, large, faint,
    // and faster and swayier than the snow because they are so close.

    /** Flakes per 10 000 dp²; with the respawn gap about one is on screen at a time. */
    const val BOKEH_DENSITY = 0.08f

    /** Radius from the farther to the closer bokeh layer, dp. */
    val BokehRadiusRange = 16f..30f

    const val BOKEH_LAYERS = 2
    const val BOKEH_MOTION_FACTOR = 2.2f
    const val BOKEH_SWAY_FACTOR = 2f
    const val BOKEH_SWAY_FREQUENCY_HZ = 0.2f
    const val BOKEH_OPACITY = 0.28f

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
        swayAmplitude = SnowTuning.SwayRange.at(settings.lab.sway),
        swayFrequency = SnowTuning.SWAY_FREQUENCY_HZ,
        wind = SnowTuning.WIND_DP_PER_S,
        opacity = SnowTuning.OPACITY,
    )

    // Density 0 when disabled, so switching bokeh off lets the last ones fall out (ADR-0006).
    private fun foregroundFor(snow: SnowConfig, enabled: Boolean): SnowConfig = SnowConfig(
        density = if (enabled) SnowTuning.BOKEH_DENSITY else 0f,
        fallSpeed = snow.fallSpeed * SnowTuning.BOKEH_MOTION_FACTOR,
        minRadius = SnowTuning.BokehRadiusRange.start,
        maxRadius = SnowTuning.BokehRadiusRange.endInclusive,
        layers = SnowTuning.BOKEH_LAYERS,
        swayAmplitude = snow.swayAmplitude * SnowTuning.BOKEH_SWAY_FACTOR,
        swayFrequency = SnowTuning.BOKEH_SWAY_FREQUENCY_HZ,
        wind = snow.wind * SnowTuning.BOKEH_MOTION_FACTOR,
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
