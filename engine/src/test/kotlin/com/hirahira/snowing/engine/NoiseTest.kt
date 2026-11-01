package com.hirahira.snowing.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

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
    @Test
    fun `the 2D noise is the z = 0 slice of the 3D one`() {
        for ((x, y, _) in samples()) {
            assertEquals(Noise.at(x, y, 0f), Noise.at(x, y), 1e-6f)
        }
    }

    @Test
    fun `the 2D slice wobbles about as much as the 3D noise`() {
        // Flutter was tuned on 3D noise; a 2D slice must not shrink or grow it noticeably.
        var sum2 = 0.0
        var sum3 = 0.0
        for ((x, y, z) in samples()) {
            sum2 += Noise.at(x, y + 0.37f).let { it * it }
            sum3 += Noise.at(x, y, z).let { it * it }
        }
        val ratio = sqrt(sum2 / sum3)
        assertTrue("rms ratio $ratio", ratio in 0.8..1.25)
    }

    @Test
    fun `the analytic gradient matches a finite difference`() {
        val out = FloatArray(2)
        val h = 1e-3f
        for ((x, y, z) in samples()) {
            Noise.gradientXY(x, y, z, out)
            val dx = (Noise.at(x + h, y, z) - Noise.at(x - h, y, z)) / (2 * h)
            val dy = (Noise.at(x, y + h, z) - Noise.at(x, y - h, z)) / (2 * h)
            assertEquals("d/dx at $x, $y, $z", dx, out[0], 0.02f)
            assertEquals("d/dy at $x, $y, $z", dy, out[1], 0.02f)
        }
    }
}
