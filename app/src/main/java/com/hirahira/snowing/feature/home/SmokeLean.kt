package com.hirahira.snowing.feature.home

import com.hirahira.snowing.overlay.SnowTuning
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.WindStrength
import kotlin.math.sin

/** How the active mood's wind bends the cabin's chimney smoke (scene update 6.1), like it carries the snow. */
object SmokeLean {
    /** -1..1: the wind's sideways share of the gusty wind's speed; calm air lets the smoke drift a little right. */
    fun of(mood: Mood): Float {
        if (mood.windStrength == WindStrength.CALM) return CALM
        val sideways = sin(Math.toRadians(mood.windAngle.toDouble())).toFloat()
        return sideways * SnowTuning.windSpeed(mood.windStrength) / SnowTuning.windSpeed(WindStrength.GUSTY)
    }

    private const val CALM = 0.4f
}
