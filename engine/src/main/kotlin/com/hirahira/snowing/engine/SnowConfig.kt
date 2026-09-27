package com.hirahira.snowing.engine

/**
 * Physical description of a snowfall.
 *
 * Distances are in dp and times in seconds, so the same config looks the same
 * on every screen and at every refresh rate. [SnowField] converts to pixels.
 * Values describe the front (closest) depth layer; deeper layers are derived
 * from them — smaller, slower and fainter.
 */
data class SnowConfig(
    /** Flakes per 10 000 dp² of screen. A typical phone is ~30 such units. */
    val density: Float,
    /** Fall speed, dp/s. */
    val fallSpeed: Float,
    /** Radius of the farthest flakes, dp. */
    val minRadius: Float,
    /** Radius of the closest flakes, dp. */
    val maxRadius: Float,
    /** Number of depth layers (parallax). 1 = flat snowfall. */
    val layers: Int,
    /** Horizontal sway amplitude, dp. */
    val swayAmplitude: Float,
    /** Sway oscillations per second. */
    val swayFrequency: Float,
    /** Constant horizontal drift, dp/s. Positive blows to the right. */
    val wind: Float,
    /** Flake alpha, 0..1. */
    val opacity: Float,
) {
    init {
        require(density >= 0f) { "density must be >= 0, was $density" }
        require(fallSpeed >= 0f) { "fallSpeed must be >= 0, was $fallSpeed" }
        require(minRadius > 0f && maxRadius >= minRadius) { "invalid radius range $minRadius..$maxRadius" }
        require(layers >= 1) { "layers must be >= 1, was $layers" }
        require(swayAmplitude >= 0f && swayFrequency >= 0f) { "sway must be >= 0" }
        require(opacity in 0f..1f) { "opacity must be in 0..1, was $opacity" }
    }
}
