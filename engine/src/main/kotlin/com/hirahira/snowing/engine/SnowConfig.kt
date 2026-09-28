package com.hirahira.snowing.engine

/**
 * Physical description of a snowfall.
 *
 * Distances are in dp and times in seconds, so the same config looks the same
 * on every screen and at every refresh rate. [SnowField] converts to pixels.
 * Values describe the front (closest) depth layer; deeper layers are derived
 * from them — smaller, slower and fainter.
 *
 * The air is shared by all flakes (docs/adr/0009-wind.md): [wind], [gusts] and
 * [turbulence] say how strongly a flake born with this config responds to it.
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
    /** Mean horizontal wind, dp/s. Positive blows to the right. */
    val wind: Float,
    /** Opacity of a flake, 0..1. */
    val opacity: Float,
    /** Strength of the gusts that sweep across the screen, dp/s. */
    val gusts: Float = 0f,
    /** Speed of the swirls in the air that carry flakes along, dp/s. */
    val turbulence: Float = 0f,
    /** Small irregular wobble of each flake around its path, dp. */
    val flutter: Float = 0f,
    /**
     * Extra gap above the screen before a flake is reborn, in screen heights.
     * 0 keeps a steady snowfall; larger values make flakes pass only now and then.
     */
    val respawnSpread: Float = 0f,
) {
    init {
        require(density >= 0f) { "density must be >= 0, was $density" }
        require(fallSpeed >= 0f) { "fallSpeed must be >= 0, was $fallSpeed" }
        require(minRadius > 0f && maxRadius >= minRadius) { "invalid radius range $minRadius..$maxRadius" }
        require(layers >= 1) { "layers must be >= 1, was $layers" }
        require(gusts >= 0f && turbulence >= 0f && flutter >= 0f) { "gusts, turbulence and flutter must be >= 0" }
        require(opacity in 0f..1f) { "opacity must be in 0..1, was $opacity" }
        require(respawnSpread >= 0f) { "respawnSpread must be >= 0, was $respawnSpread" }
    }
}
