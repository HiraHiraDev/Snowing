package com.hirahira.snowing.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SnowSettingsTest {

    @Test
    fun `the custom mood can be picked only once saved`() {
        assertEquals(MoodChoice.BUILT_IN, SnowSettings().moodChoices)
        assertEquals(SnowSettings(), SnowSettings().choosing(MoodChoice.CUSTOM))
        val withSlot = SnowSettings(customMood = BuiltInMoods.HEAVY)
        assertEquals(MoodChoice.CUSTOM, withSlot.choosing(MoodChoice.CUSTOM).mood)
    }

    @Test
    fun `the own mood starts as what is falling now`() {
        val created = SnowSettings(mood = MoodChoice.BLIZZARD).creatingOwnMood()
        assertEquals(MoodChoice.CUSTOM, created.mood)
        assertEquals(BuiltInMoods.BLIZZARD, created.customMood)
        assertEquals(BuiltInMoods.BLIZZARD, created.activeMood)
    }

    @Test
    fun `only the active own mood can be changed`() {
        val withOwn = SnowSettings(mood = MoodChoice.HEAVY, customMood = BuiltInMoods.FIRST_SNOW)
        assertEquals(withOwn, withOwn.editingOwnMood { it.copy(intensity = 9) })
        val active = withOwn.choosing(MoodChoice.CUSTOM).editingOwnMood { it.copy(intensity = 9) }
        assertEquals(9, active.customMood!!.intensity)
    }
}
