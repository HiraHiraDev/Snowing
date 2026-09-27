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
}

object SnowConfigMapper {

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

    fun toRenderOptions(settings: SnowSettings): RenderOptions = RenderOptions(
        showHud = settings.lab.showHud,
        showTracer = settings.lab.showTracer,
    )

    private fun ClosedFloatingPointRange<Float>.at(fraction: Float): Float =
        start + (endInclusive - start) * fraction.coerceIn(0f, 1f)
}
