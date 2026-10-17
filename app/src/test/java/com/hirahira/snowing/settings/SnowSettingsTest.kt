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
}
