package com.hirahira.snowing.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.management.ManagementFactory
import kotlin.random.Random

/**
 * Hours of snowfall must look like the first minutes: same number of flakes,
 * same density on screen, nothing drifting off or degrading, and no garbage
 * per frame (a GC pause on the overlay is a visible hitch over every app).
 */
class SnowFieldSoakTest {

    private val config = SnowConfig(
        density = 7f,
        fallSpeed = 140f,
        minRadius = 1.2f,
        maxRadius = 4.2f,
        layers = 5,
        wind = 10f,
        opacity = 1f,
        gusts = 45f,
        turbulence = 40f,
        flutter = 5f,
    )

    // 1080 x 2400 px at 2.625 px/dp, a typical phone.
    private fun field() = SnowField(config, pxPerDp = 2.625f, random = Random(11)).apply { resize(1080, 2400) }

    @Test
    fun `an hour of snow keeps its count, bounds and density`() {
        val snow = field()
        val initialCount = snow.count
        val margin = MAX_REACH_DP * 2.625f
        var earlyVisible = 0L
        var lateVisible = 0L
        val frames = SOAK_HOURS * 3600 * HZ
        val sampleEvery = HZ * 10

        for (frame in 1..frames) {
            snow.step(1f / HZ)
            if (frame % sampleEvery != 0) continue
            assertEquals("count at frame $frame", initialCount, snow.count)
            var visible = 0
            for (i in 0 until snow.count) {
                val x = snow.x(i)
                val y = snow.y(i)
                assertTrue("flake $i x=$x at frame $frame", x.isFinite() && x > -margin && x < snow.width + margin)
                assertTrue("flake $i y=$y at frame $frame", y.isFinite() && y > -snow.height && y < snow.height + margin)
                if (y > 0f && y < snow.height) visible++
            }
            // Steady state is reached after the first screen-height of fall; compare minutes 5-15 with the last ten.
            val minute = frame / (HZ * 60)
            if (minute in 5 until 15) earlyVisible += visible
            if (minute >= SOAK_HOURS * 60 - 10) lateVisible += visible
        }
        val drift = (lateVisible - earlyVisible).toFloat() / earlyVisible
        assertTrue("visible flakes drifted by ${drift * 100}%", kotlin.math.abs(drift) < 0.05f)
    }

    @Test
    fun `a frame allocates nothing`() {
        val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val snow = field()
        // Warm-up: JIT compilation and the first rebirths may allocate once.
        repeat(HZ * 120) { snow.step(1f / HZ) }

        val before = threads.getThreadAllocatedBytes(Thread.currentThread().id)
        repeat(HZ * 60) { snow.step(1f / HZ) }
        val allocated = threads.getThreadAllocatedBytes(Thread.currentThread().id) - before

        assertEquals("bytes allocated over 3600 frames", 0L, allocated)
    }

    private companion object {
        const val SOAK_HOURS = 1
        const val HZ = 60

        /** Widest a flake may sit outside the screen sideways before it wraps: radius + flutter, dp. */
        const val MAX_REACH_DP = 20f
    }
}
