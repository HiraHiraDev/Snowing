package com.hirahira.snowing.pro

import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SnowSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ProGateTest {

    private val saved = SnowSettings(mood = MoodChoice.FLUFF)
    private val customEdit = saved.copy(
        mood = MoodChoice.CUSTOM,
        customMood = BuiltInMoods.custom(FlakeSize.S, intensity = 9, speed = 9, wind = 9),
        windDirection = -40f,
    )

    @Test
    fun `with Pro everything is written`() {
        assertEquals(customEdit, ProGate.persistable(customEdit, saved, hasPro = true))
    }

    @Test
    fun `without Pro the custom mood stays a preview but free settings are written`() {
        val written = ProGate.persistable(customEdit, saved, hasPro = false)
        assertEquals(MoodChoice.FLUFF, written.mood)
        assertEquals(saved.customMood, written.customMood)
        assertEquals(-40f, written.windDirection)
    }

    @Test
    fun `without Pro a built-in mood is still written`() {
        val edit = saved.copy(mood = MoodChoice.BLIZZARD)
        assertEquals(edit, ProGate.persistable(edit, saved, hasPro = false))
    }
}
