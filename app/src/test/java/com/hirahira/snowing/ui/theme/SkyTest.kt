package com.hirahira.snowing.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyTest {

    @Test
    fun `keyframe hours give their palettes`() {
        assertEquals(Sky.Night, Sky.at(2f))
        assertEquals(Sky.Dawn, Sky.at(Sky.HOUR_DAWN))
        assertEquals(Sky.Day, Sky.at(Sky.HOUR_DAY))
        assertEquals(Sky.Dusk, Sky.at(Sky.HOUR_DUSK))
        assertEquals(Sky.Night, Sky.at(Sky.HOUR_NIGHT))
    }

    @Test
    fun `hours between keyframes blend smoothly`() {
        val early = Sky.at(7.1f)
        val half = Sky.at(8.25f)
        assertNotEquals(Sky.Dawn, half)
        assertNotEquals(Sky.Day, half)
        // Smoothstep: barely moving right after a keyframe, halfway in the middle.
        assertEquals(Sky.Dawn.windowGlow, early.windowGlow, 0.01f)
        assertEquals((Sky.Dawn.windowGlow + Sky.Day.windowGlow) / 2, half.windowGlow, 1e-4f)
    }

    @Test
    fun `hours outside the day are clamped`() {
        assertEquals(Sky.Night, Sky.at(-3f))
        assertEquals(Sky.Night, Sky.at(30f))
    }

    @Test
    fun `ink turns dark only on the light day sky`() {
        assertTrue(SnowingColors.forSky(Sky.Day).isLightSky)
        assertFalse(SnowingColors.forSky(Sky.Night).isLightSky)
        assertFalse(SnowingColors.forSky(Sky.Dawn).isLightSky)
        assertFalse(SnowingColors.forSky(Sky.Dusk).isLightSky)
        assertNotEquals(SnowingColors.forSky(Sky.Day).ink, SnowingColors.forSky(Sky.Night).ink)
    }
}
