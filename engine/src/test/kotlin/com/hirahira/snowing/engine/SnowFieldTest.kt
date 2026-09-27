package com.hirahira.snowing.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SnowFieldTest {

    private val config = SnowConfig(
        density = 4f,
        fallSpeed = 100f,
        minRadius = 1f,
        maxRadius = 4f,
        layers = 3,
        swayAmplitude = 10f,
        swayFrequency = 0.5f,
        wind = 5f,
        opacity = 1f,
    )

    // 1080 x 2400 px at 3 px/dp = 360 x 800 dp.
    private fun field(seed: Int = 7) =
        SnowField(config, pxPerDp = 3f, random = Random(seed)).apply { resize(1080, 2400) }

    @Test
    fun `flakes fall at their nominal speed at any refresh rate`() {
        for (hz in listOf(30, 60, 90, 120, 144)) {
            val snow = field()
            val start = FloatArray(snow.count) { snow.y(it) }
            repeat(hz) { snow.step(1f / hz) }

            var checked = 0
            for (i in 0 until snow.count) {
                if (snow.y(i) < start[i]) continue // respawned at the top
                assertEquals("hz=$hz flake=$i", snow.fallSpeed(i), snow.y(i) - start[i], 0.05f)
                checked++
            }
            assertTrue("hz=$hz checked only $checked", checked > snow.count / 2)
        }
    }

    @Test
    fun `positions after one second match between 60 and 120 Hz`() {
        val at60 = field()
        val at120 = field()
        val start = FloatArray(at60.count) { at60.y(it) }
        repeat(60) { at60.step(1f / 60) }
        repeat(120) { at120.step(1f / 120) }

        for (i in 0 until at60.count) {
            if (at60.y(i) < start[i] || at120.y(i) < start[i]) continue
            assertEquals("y of $i", at60.y(i), at120.y(i), 0.05f)
            assertEquals("x of $i", at60.x(i), at120.x(i), 0.05f)
        }
    }

    @Test
    fun `a long hitch is clamped instead of teleporting flakes`() {
        val snow = field()
        val start = FloatArray(snow.count) { snow.y(it) }
        snow.step(5f)

        for (i in 0 until snow.count) {
            if (snow.y(i) < start[i]) continue
            assertEquals(snow.fallSpeed(i) * SnowField.MAX_STEP_SECONDS, snow.y(i) - start[i], 0.05f)
        }
    }

    @Test
    fun `flake count follows density and screen area`() {
        val snow = field()
        // 360 * 800 dp² = 28.8 area units * 4 flakes.
        assertEquals(115, snow.count)

        snow.updateConfig(config.copy(density = 2f))
        assertEquals(58, snow.count)

        snow.updateConfig(config.copy(density = 10_000f))
        assertEquals(SnowField.MAX_FLAKES, snow.count)
    }

    @Test
    fun `flakes keep recycling within the screen band`() {
        val snow = field()
        repeat(60 * 60) { snow.step(1f / 60) }

        val maxRadiusPx = config.maxRadius * 1.2f * 3f
        for (i in 0 until snow.count) {
            assertTrue("flake $i at ${snow.y(i)}", snow.y(i) <= snow.height + maxRadiusPx)
            assertTrue("flake $i at ${snow.y(i)}", snow.y(i) >= -snow.height * 0.3f - maxRadiusPx)
        }
    }

    @Test
    fun `resize keeps flakes at the same relative position`() {
        val snow = field()
        val relativeY = FloatArray(snow.count) { snow.y(it) / snow.height }
        snow.resize(2400, 1080)

        for (i in 0 until relativeY.size) {
            assertEquals(relativeY[i], snow.y(i) / snow.height, 0.0001f)
        }
    }
}
