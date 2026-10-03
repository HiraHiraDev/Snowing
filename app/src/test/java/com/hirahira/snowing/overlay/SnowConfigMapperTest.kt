package com.hirahira.snowing.overlay

import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.LabSettings
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SnowSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class SnowConfigMapperTest {

    private fun config(settings: SnowSettings) = SnowConfigMapper.toConfig(settings)

    @Test
    fun `levels span the tuning ranges from end to end`() {
        val lowest = BuiltInMoods.custom(FlakeSize.M, intensity = 1, speed = 1, wind = 0)
        val highest = BuiltInMoods.custom(FlakeSize.M, intensity = 10, speed = 10, wind = 10)

        val low = config(SnowSettings(mood = MoodChoice.CUSTOM, customMood = lowest))
        val high = config(SnowSettings(mood = MoodChoice.CUSTOM, customMood = highest))

        assertEquals(SnowTuning.DensityRange.start, low.density, 1e-4f)
        assertEquals(SnowTuning.SpeedRange.start, low.fallSpeed, 1e-4f)
        assertEquals(0f, low.wind, 1e-4f)
        assertEquals(SnowTuning.DensityRange.endInclusive, high.density, 1e-4f)
        assertEquals(SnowTuning.SpeedRange.endInclusive, high.fallSpeed, 1e-4f)
    }

    @Test
    fun `wind is the mood's strength times the sideways part of the direction`() {
        val blizzard = SnowSettings(mood = MoodChoice.BLIZZARD, windDirection = 30f)
        val expected = SnowTuning.WIND_MAX_DP_PER_S * 0.8f * sin(Math.toRadians(30.0)).toFloat()
        assertEquals(expected, config(blizzard).wind, 1e-3f)
        assertEquals(-expected, config(blizzard.copy(windDirection = -30f)).wind, 1e-3f)
        assertEquals(0f, config(blizzard.copy(windDirection = 0f)).wind, 1e-4f)
        assertEquals(0f, config(SnowSettings(mood = MoodChoice.FIRST_SNOW)).wind, 1e-4f)
    }

    @Test
    fun `the default direction lets a windy mood blow`() {
        assertTrue(config(SnowSettings(mood = MoodChoice.FLUFF)).wind > 0f)
    }

    @Test
    fun `the direction never goes past the wheel's limit`() {
        val far = config(SnowSettings(mood = MoodChoice.BLIZZARD, windDirection = 170f))
        val limit = config(SnowSettings(mood = MoodChoice.BLIZZARD, windDirection = SnowSettings.MAX_WIND_DIRECTION))
        assertEquals(limit.wind, far.wind, 1e-4f)
    }

    @Test
    fun `flake size scales the radius`() {
        fun radius(size: FlakeSize) = config(
            SnowSettings(mood = MoodChoice.CUSTOM, customMood = BuiltInMoods.CUSTOM_BASE.copy(flakeSize = size)),
        ).maxRadius
        assertTrue(radius(FlakeSize.S) < radius(FlakeSize.M))
        assertTrue(radius(FlakeSize.M) < radius(FlakeSize.L))
        assertEquals(SnowTuning.RadiusRange.endInclusive, radius(FlakeSize.M), 1e-4f)
    }

    @Test
    fun `built-in moods grow heavier from first snow to blizzard`() {
        val densities = listOf(MoodChoice.FIRST_SNOW, MoodChoice.FLUFF, MoodChoice.HEAVY, MoodChoice.BLIZZARD)
            .map { config(SnowSettings(mood = it)).density }
        assertEquals(densities.sorted(), densities)
    }

    @Test
    fun `hidden parameters come from the mood unless the lab overrides them`() {
        val lab = LabSettings(layers = 1, turbulence = 0f, gusts = 0f)
        val fromMood = config(SnowSettings(mood = MoodChoice.BLIZZARD, lab = lab))
        val overridden = config(SnowSettings(mood = MoodChoice.BLIZZARD, lab = lab.copy(overrideMood = true)))

        assertEquals(BuiltInMoods.BLIZZARD.layers, fromMood.layers)
        assertTrue(fromMood.turbulence > 0f)
        assertEquals(1, overridden.layers)
        assertEquals(0f, overridden.turbulence, 0f)
        assertEquals(0f, overridden.gusts, 0f)
    }
}
