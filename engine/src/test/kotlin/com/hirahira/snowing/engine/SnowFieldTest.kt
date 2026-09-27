package com.hirahira.snowing.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private fun field(seed: Int = 7, config: SnowConfig = this.config) =
        SnowField(config, pxPerDp = 3f, random = Random(seed)).apply { resize(1080, 2400) }

    /** A field that has been snowing long enough for every flake to have been reborn once. */
    private fun steadyField(seed: Int = 7) = field(seed).apply { run(60f) }

    private fun SnowField.run(seconds: Float, hz: Int = 60) = repeat((seconds * hz).toInt()) { step(1f / hz) }

    @Test
    fun `snow starts falling from the top edge`() {
        val snow = field()
        assertTrue(snow.count > 0)
        for (i in 0 until snow.count) {
            assertTrue("flake $i at ${snow.y(i)}", snow.y(i) + snow.radius(i) <= 0f)
        }
    }

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

        snow.updateConfig(config.copy(density = 10_000f))
        assertEquals(SnowField.MAX_FLAKES, snow.count)
    }

    @Test
    fun `a config change leaves every falling flake untouched`() {
        val snow = steadyField()
        val n = snow.count
        val x = FloatArray(n) { snow.x(it) }
        val y = FloatArray(n) { snow.y(it) }
        val radius = FloatArray(n) { snow.radius(it) }
        val alpha = FloatArray(n) { snow.alpha(it) }
        val speed = FloatArray(n) { snow.fallSpeed(it) }

        snow.updateConfig(
            config.copy(
                fallSpeed = 300f, wind = -50f, swayAmplitude = 40f, swayFrequency = 2f,
                minRadius = 3f, maxRadius = 9f, layers = 1, opacity = 0.5f,
            ),
        )

        assertEquals(n, snow.count)
        for (i in 0 until n) {
            assertEquals("x of $i", x[i], snow.x(i), 0f)
            assertEquals("y of $i", y[i], snow.y(i), 0f)
            assertEquals("radius of $i", radius[i], snow.radius(i), 0f)
            assertEquals("alpha of $i", alpha[i], snow.alpha(i), 0f)
            assertEquals("speed of $i", speed[i], snow.fallSpeed(i), 0f)
        }

        // And the next frame moves each flake by its own old speed.
        snow.step(1f / 60)
        for (i in 0 until n) {
            if (snow.y(i) < y[i]) continue // fell out and was reborn
            assertEquals("step of $i", speed[i] / 60, snow.y(i) - y[i], 0.01f)
        }
    }

    @Test
    fun `a new config enters from the top like a front`() {
        val snow = steadyField()
        val oldMaxSpeed = (0 until snow.count).maxOf { snow.fallSpeed(it) }
        val newMaxSpeed = 200f * 3f

        snow.updateConfig(config.copy(fallSpeed = 200f))
        val elapsed = 2f
        snow.run(elapsed)

        var reborn = 0
        var oldBelowFront = 0
        for (i in 0 until snow.count) {
            if (snow.fallSpeed(i) > oldMaxSpeed + 0.01f) {
                reborn++
                // Born above the top after the change, so it cannot be further than the front.
                assertTrue("new flake $i at ${snow.y(i)}", snow.y(i) <= newMaxSpeed * elapsed)
            } else if (snow.y(i) > newMaxSpeed * elapsed) {
                oldBelowFront++
            }
        }
        assertTrue("no flake took the new speed", reborn > 0)
        assertTrue("old snow below the front is gone", oldBelowFront > 0)
    }

    private fun SnowField.visible(): Int =
        (0 until count).count { y(it) + radius(it) >= 0f && y(it) - radius(it) <= height }

    private fun SnowField.waitingAbove(): Int = (0 until count).count { y(it) + radius(it) < 0f }

    @Test
    fun `lower density thins out through the bottom instead of cutting flakes`() {
        val snow = steadyField()
        assertEquals(115, snow.count)
        val visible = snow.visible()
        assertTrue(snow.waitingAbove() > 0)

        snow.updateConfig(config.copy(density = 2f))
        assertEquals("no visible flake disappears at once", visible, snow.visible())
        assertEquals("unseen flakes above the screen go first", 0, snow.waitingAbove())

        snow.run(60f)
        assertEquals(58, snow.count)
    }

    @Test
    fun `stopping lets the last flakes fall out without new ones`() {
        val snow = steadyField()
        val visible = snow.visible()
        snow.stopFalling()
        assertEquals("no visible flake disappears at once", visible, snow.count)

        var minY = (0 until snow.count).minOf { snow.y(it) }
        var seconds = 0f
        while (!snow.isEmpty && seconds < 60f) {
            snow.step(1f / 60)
            seconds += 1f / 60
            if (snow.count > 0) {
                val newMin = (0 until snow.count).minOf { snow.y(it) }
                assertTrue("a flake was reborn at the top", newMin >= minY)
                minY = newMin
            }
        }
        assertTrue(snow.isEmpty)
    }

    @Test
    fun `fading out clears the field in FADE_SECONDS`() {
        val snow = steadyField()
        val alphaBefore = snow.alpha(0)
        snow.fadeOut()

        snow.run(SnowField.FADE_SECONDS / 2)
        assertTrue(snow.count > 0)
        assertTrue(snow.alpha(0) < alphaBefore)

        snow.run(SnowField.FADE_SECONDS / 2 + 0.1f)
        assertTrue(snow.isEmpty)
    }

    @Test
    fun `resuming after a stop brings snow back from the top`() {
        val snow = steadyField()
        snow.stopFalling()
        snow.run(60f)
        assertTrue(snow.isEmpty)

        snow.resumeFalling()
        assertTrue(snow.isFalling)
        assertEquals(115, snow.count)
        for (i in 0 until snow.count) assertTrue(snow.y(i) < 0f)
    }

    @Test
    fun `immediate update re-derives every flake for tuning`() {
        val snow = steadyField()
        val speed = FloatArray(snow.count) { snow.fallSpeed(it) }

        snow.updateConfig(config.copy(fallSpeed = 200f), immediate = true)

        for (i in 0 until snow.count) assertEquals(speed[i] * 2, snow.fallSpeed(i), 0.01f)
    }

    @Test
    fun `flakes keep recycling within the screen band`() {
        val snow = steadyField()
        snow.run(60f)

        val maxRadiusPx = config.maxRadius * 1.2f * 3f
        for (i in 0 until snow.count) {
            assertTrue("flake $i at ${snow.y(i)}", snow.y(i) <= snow.height + maxRadiusPx)
            assertTrue("flake $i at ${snow.y(i)}", snow.y(i) >= -snow.height * SnowField.SPAWN_BAND - maxRadiusPx)
        }
    }

    @Test
    fun `respawn spread keeps sparse flakes away for longer`() {
        val snow = field(config = config.copy(density = 0.2f, respawnSpread = 2f)).apply { run(120f) }

        val maxRadiusPx = config.maxRadius * 1.2f * 3f
        val lowest = -snow.height * (SnowField.SPAWN_BAND + 2f) - maxRadiusPx
        var aboveScreen = 0
        for (i in 0 until snow.count) {
            assertTrue(snow.y(i) >= lowest)
            if (snow.y(i) < 0f) aboveScreen++
        }
        assertFalse("every sparse flake is on screen", aboveScreen == 0)
    }

    @Test
    fun `resize keeps flakes at the same relative position`() {
        val snow = steadyField()
        val relativeY = FloatArray(snow.count) { snow.y(it) / snow.height }
        snow.resize(2400, 1080)

        for (i in 0 until relativeY.size) {
            assertEquals(relativeY[i], snow.y(i) / snow.height, 0.0001f)
        }
    }
}
