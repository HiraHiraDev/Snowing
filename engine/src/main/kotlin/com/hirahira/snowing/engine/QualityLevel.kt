package com.hirahira.snowing.engine

/**
 * How much snow the device is asked to draw (ADR-0008 §2). A level changes the
 * amount and depth only, never flake size, speed or wind, so a mood keeps its
 * character on a weak phone. Declared from the best to the cheapest.
 */
enum class QualityLevel(
    val densityFactor: Float,
    /** Upper bound for depth layers; the mood may ask for fewer. */
    val maxLayers: Int,
    val fpsCap: Int,
) {
    HIGH(densityFactor = 1f, maxLayers = Int.MAX_VALUE, fpsCap = 60),
    MEDIUM(densityFactor = 0.75f, maxLayers = 3, fpsCap = 60),
    LOW(densityFactor = 0.5f, maxLayers = 2, fpsCap = 30),
    SAVER(densityFactor = 0.5f, maxLayers = 2, fpsCap = 30),
    ;

    fun apply(config: SnowConfig): SnowConfig = config.copy(
        density = config.density * densityFactor,
        layers = minOf(config.layers, maxLayers),
    )
}

/** The platform thermal status, reduced to what the snow reacts to (ADR-0008 §4). */
enum class ThermalState { NONE, MODERATE, SEVERE, CRITICAL }
