package com.hirahira.snowing.feature.home

import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.WindStrength
import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeLeanTest {

    private val mood = BuiltInMoods.of(MoodChoice.FIRST_SNOW)!!

    @Test
    fun `a gusty wind from the side bends the smoke all the way`() {
        assertEquals(1f, SmokeLean.of(mood.copy(windAngle = 90, windStrength = WindStrength.GUSTY)), 1e-4f)
        assertEquals(-1f, SmokeLean.of(mood.copy(windAngle = 270, windStrength = WindStrength.GUSTY)), 1e-4f)
    }

    @Test
    fun `a gentle wind bends it by its share of the gusty speed`() {
        assertEquals(35f / 80f, SmokeLean.of(mood.copy(windAngle = 90, windStrength = WindStrength.GENTLE)), 1e-4f)
        assertEquals(0f, SmokeLean.of(mood.copy(windAngle = 180, windStrength = WindStrength.GENTLE)), 1e-4f)
    }

    @Test
    fun `calm air lets it drift a little right whatever the dial says`() {
        assertEquals(0.4f, SmokeLean.of(mood.copy(windAngle = 270, windStrength = WindStrength.CALM)), 1e-4f)
    }
}
