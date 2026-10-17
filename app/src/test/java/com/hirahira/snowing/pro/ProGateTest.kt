package com.hirahira.snowing.pro

import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ProGateTest {

    private val saved = SnowSettings(mood = MoodChoice.HEAVY)
    private val adjusted = saved.copy(
        moodAdjustment = BuiltInMoods.HEAVY.adjustment.copy(intensity = 9),
        timeOfDay = TimeOfDay.DUSK,
    )
    private val savedAsNew = saved.copy(mood = MoodChoice.CUSTOM, customMood = adjusted.activeMood)

    @Test
    fun `with Pro everything is written`() {
        assertEquals(adjusted, ProGate.persistable(adjusted, saved, hasPro = true))
        assertEquals(savedAsNew, ProGate.persistable(savedAsNew, saved, hasPro = true))
    }

    @Test
    fun `without Pro moods cannot be adjusted`() {
        assertFalse(ProGate.canAdjustMoods(hasPro = false))
        val written = ProGate.persistable(adjusted, saved, hasPro = false)
        assertNull(written.moodAdjustment)
        assertEquals(TimeOfDay.DUSK, written.timeOfDay)
    }

    @Test
    fun `without Pro nothing is saved as a new mood`() {
        val written = ProGate.persistable(savedAsNew, saved, hasPro = false)
        assertEquals(MoodChoice.HEAVY, written.mood)
        assertNull(written.customMood)
    }

    @Test
    fun `without Pro a built-in mood is still picked and old changes can be dropped`() {
        val picked = adjusted.copy(mood = MoodChoice.BLIZZARD, moodAdjustment = null)
        assertEquals(picked, ProGate.persistable(picked, adjusted, hasPro = false))
    }
}
