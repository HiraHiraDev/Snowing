package com.hirahira.snowing.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NoiseTest {

    private fun samples(): Sequence<Triple<Float, Float, Float>> = sequence {
        for (i in 0 until 2000) yield(Triple(i * 0.137f, i * 0.071f + 3f, i * 0.029f + 7f))
    }

    @Test
    fun `stays within -1 to 1 and is not flat`() {
        var min = 0f
        var max = 0f
        for ((x, y, z) in samples()) {
            val n = Noise.at(x, y, z)
            assertTrue(n in -1.01f..1.01f)
            min = minOf(min, n)
            max = maxOf(max, n)
        }
        assertTrue("range $min..$max", max - min > 0.8f)
    }

    @Test
    fun `is smooth`() {
        for ((x, y, z) in samples()) {
            assertTrue(abs(Noise.at(x, y, z) - Noise.at(x + 0.001f, y, z)) < 0.01f)
        }
    }

    @Test
    fun `repeats every period so clocks can wrap without a seam`() {
        for ((x, y, z) in samples()) {
            assertEquals(Noise.at(x, y, z), Noise.at(x + Noise.PERIOD, y, z - Noise.PERIOD), 1e-3f)
        }
    }
}
