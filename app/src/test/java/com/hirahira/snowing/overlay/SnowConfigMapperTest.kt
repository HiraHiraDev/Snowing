package com.hirahira.snowing.overlay

import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.LabSettings
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.WindStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class SnowConfigMapperTest {

    private fun config(settings: SnowSettings) = SnowConfigMapper.toConfig(settings)

    private fun config(mood: Mood) = config(SnowSettings(mood = MoodChoice.CUSTOM, customMood = mood))

    private fun Mood.withWind(angle: Int, strength: WindStrength) =
        adjusted(adjustment.copy(windAngle = angle, windStrength = strength))

    @Test
    fun `levels span the tuning ranges from end to end`() {
        val low = config(BuiltInMoods.FIRST_SNOW.copy(intensity = 1, speed = 1))
        val high = config(BuiltInMoods.FIRST_SNOW.copy(intensity = 10, speed = 10))

        assertEquals(SnowTuning.DensityRange.start, low.density, 1e-4f)
        assertEquals(SnowTuning.SpeedRange.start, low.fallSpeed, 1e-4f)
        assertEquals(SnowTuning.DensityRange.endInclusive, high.density, 1e-4f)
        assertEquals(SnowTuning.SpeedRange.endInclusive, high.fallSpeed, 1e-4f)
    }

    @Test
    fun `wind is the strength times the sideways part of the needle`() {
        val heavy = BuiltInMoods.HEAVY
        val gentle = SnowTuning.windSpeed(WindStrength.GENTLE)
        assertEquals(gentle, config(heavy.withWind(90, WindStrength.GENTLE)).wind, 1e-3f)
        assertEquals(-gentle, config(heavy.withWind(270, WindStrength.GENTLE)).wind, 1e-3f)
        val expected = gentle * sin(Math.toRadians(30.0)).toFloat()
        assertEquals(expected, config(heavy.withWind(30, WindStrength.GENTLE)).wind, 1e-3f)
    }

    @Test
    fun `an up or down needle does not blow sideways and never lifts the snow`() {
        val up = config(BuiltInMoods.BLIZZARD.withWind(0, WindStrength.GUSTY))
        val down = config(BuiltInMoods.BLIZZARD.withWind(180, WindStrength.GUSTY))
        assertEquals(0f, up.wind, 1e-3f)
        assertEquals(0f, down.wind, 1e-3f)
        assertEquals(down.fallSpeed, up.fallSpeed, 0f)
    }

    @Test
    fun `calm means no wind whatever the angle`() {
        assertEquals(0f, config(BuiltInMoods.HEAVY.withWind(90, WindStrength.CALM)).wind, 0f)
        assertEquals(0f, config(SnowSettings(mood = MoodChoice.FIRST_SNOW)).wind, 0f)
    }

    @Test
    fun `wind grows with strength and a gusty wind gusts harder`() {
        val gentle = config(BuiltInMoods.HEAVY.withWind(90, WindStrength.GENTLE))
        val gusty = config(BuiltInMoods.HEAVY.withWind(90, WindStrength.GUSTY))
        assertTrue(gusty.wind > gentle.wind)
        assertTrue(gusty.gusts > gentle.gusts)
    }

    @Test
    fun `heavy snow drifts left as its needle points down and left`() {
        assertTrue(config(SnowSettings(mood = MoodChoice.HEAVY)).wind < 0f)
    }

    @Test
    fun `adjustments change the snow`() {
        val settings = SnowSettings(mood = MoodChoice.HEAVY)
        val denser = settings.copy(moodAdjustment = BuiltInMoods.HEAVY.adjustment.copy(intensity = 10))
        assertEquals(SnowTuning.DensityRange.endInclusive, config(denser).density, 1e-4f)
    }

    @Test
    fun `flake size scales the radius`() {
        fun radius(size: FlakeSize) = config(BuiltInMoods.HEAVY.copy(flakeSize = size)).maxRadius
        assertTrue(radius(FlakeSize.S) < radius(FlakeSize.M))
        assertTrue(radius(FlakeSize.M) < radius(FlakeSize.L))
        assertEquals(SnowTuning.RadiusRange.endInclusive, radius(FlakeSize.M), 1e-4f)
    }

    @Test
    fun `built-in moods grow heavier from first snow to blizzard`() {
        val densities = MoodChoice.BUILT_IN.map { config(SnowSettings(mood = it)).density }
        assertEquals(densities.sorted(), densities)
    }

    @Test
    fun `an empty custom slot falls back to the default mood`() {
        assertEquals(
            config(SnowSettings(mood = SnowSettings.DEFAULT_MOOD)),
            config(SnowSettings(mood = MoodChoice.CUSTOM, customMood = null)),
        )
    }

    @Test
    fun `hidden parameters come from the mood unless the lab overrides them`() {
        val lab = LabSettings(speed = 1, layers = 1, turbulence = 0f, gusts = 0f)
        val fromMood = config(SnowSettings(mood = MoodChoice.FIRST_SNOW, lab = lab))
        val overridden = config(SnowSettings(mood = MoodChoice.FIRST_SNOW, lab = lab.copy(overrideMood = true)))

        assertEquals(BuiltInMoods.FIRST_SNOW.layers, fromMood.layers)
        assertTrue(fromMood.turbulence > 0f)
        assertTrue(fromMood.fallSpeed > SnowTuning.SpeedRange.start)
        assertEquals(1, overridden.layers)
        assertEquals(SnowTuning.SpeedRange.start, overridden.fallSpeed, 1e-4f)
        assertEquals(0f, overridden.turbulence, 0f)
        assertEquals(0f, overridden.gusts, 0f)
    }
}
