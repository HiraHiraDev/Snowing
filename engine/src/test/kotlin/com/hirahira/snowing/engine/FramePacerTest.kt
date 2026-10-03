package com.hirahira.snowing.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class FramePacerTest {

    @Test
    fun `picks the highest divisor frame rate within the cap`() {
        val cases = listOf(
            Triple(60f, 60, 60f),
            Triple(90f, 60, 45f),
            Triple(120f, 60, 60f),
            Triple(144f, 60, 48f),
            Triple(60f, 30, 30f),
            Triple(90f, 30, 30f),
            Triple(120f, 30, 30f),
            Triple(59.94f, 60, 59.94f),
            Triple(60.000004f, 60, 60.000004f),
        )
        for ((refresh, cap, expected) in cases) {
            assertEquals("$refresh Hz, cap $cap", expected, FramePacer(refresh, cap).targetFps, 0.01f)
        }
    }

    @Test
    fun `draws evenly spaced frames`() {
        val drawn = drawnVsyncs(FramePacer(refreshRate = 90f, fpsCap = 60), refreshRate = 90f, vsyncs = 12)
        assertEquals(listOf(0, 2, 4, 6, 8, 10), drawn)
    }

    @Test
    fun `tolerates jitter in vsync timestamps`() {
        val pacer = FramePacer(refreshRate = 120f, fpsCap = 30)
        val period = 1_000_000_000L / 120
        val jitter = longArrayOf(0, 400_000, -300_000, 200_000, -400_000, 100_000, 0, -200_000, 300_000)
        val drawn = jitter.indices.filter { pacer.shouldDraw(it * period + jitter[it]) }
        assertEquals(listOf(0, 4, 8), drawn)
    }

    @Test
    fun `follows a refresh rate change`() {
        val pacer = FramePacer(refreshRate = 90f, fpsCap = 60)
        pacer.refreshRate = 60f
        assertEquals(1, pacer.divisor)
        pacer.fpsCap = 30
        assertEquals(2, pacer.divisor)
    }

    @Test
    fun `draws the first frame after a reset`() {
        val pacer = FramePacer(refreshRate = 60f, fpsCap = 30)
        pacer.shouldDraw(1_000_000_000L)
        pacer.reset()
        assertEquals(true, pacer.shouldDraw(1_000_000_001L))
    }

    private fun drawnVsyncs(pacer: FramePacer, refreshRate: Float, vsyncs: Int): List<Int> {
        val period = (1_000_000_000.0 / refreshRate).toLong()
        return (0 until vsyncs).filter { pacer.shouldDraw(1_000_000_000L + it * period) }
    }
}
