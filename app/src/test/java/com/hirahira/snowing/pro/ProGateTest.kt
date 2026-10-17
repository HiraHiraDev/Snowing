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
    private val withOwn = saved.creatingOwnMood().copy(timeOfDay = TimeOfDay.DUSK)

    @Test
    fun `with Pro everything is written`() {
        assertEquals(withOwn, ProGate.persistable(withOwn, saved, hasPro = true))
    }

    @Test
    fun `without Pro no own mood is made but free settings are written`() {
        assertFalse(ProGate.canOwnMood(hasPro = false))
        val written = ProGate.persistable(withOwn, saved, hasPro = false)
        assertEquals(MoodChoice.HEAVY, written.mood)
        assertNull(written.customMood)
        assertEquals(TimeOfDay.DUSK, written.timeOfDay)
    }

    @Test
    fun `without Pro an own mood is not changed`() {
        val own = SnowSettings(mood = MoodChoice.CUSTOM, customMood = BuiltInMoods.HEAVY)
        val changed = own.editingOwnMood { it.copy(intensity = 1) }
        assertEquals(own.customMood, ProGate.persistable(changed, own, hasPro = false).customMood)
    }

    @Test
    fun `without Pro a built-in mood is still picked`() {
        val picked = saved.copy(mood = MoodChoice.BLIZZARD)
        assertEquals(picked, ProGate.persistable(picked, saved, hasPro = false))
    }
}
