package com.hirahira.snowing.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityPolicyTest {

    private val interval = 1f / 60f

    private fun QualityPolicy.run(seconds: Float, missedShare: Float = 0f) {
        val missedEvery = if (missedShare > 0f) (1f / missedShare).toInt() else Int.MAX_VALUE
        var elapsed = 0f
        var frame = 0
        while (elapsed < seconds) {
            frame++
            val dt = if (frame % missedEvery == 0) interval * 2f else interval
            onFrame(dt, interval)
            elapsed += dt
        }
    }

    @Test
    fun `starts at the start level`() {
        assertEquals(QualityLevel.HIGH, QualityPolicy(QualityLevel.HIGH).level)
        assertEquals(QualityLevel.LOW, QualityPolicy(QualityLevel.LOW).level)
    }

    @Test
    fun `steps down one level after a window with too many missed frames`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.run(seconds = 5.1f, missedShare = 0.1f)
        assertEquals(QualityLevel.MEDIUM, policy.level)
        assertEquals(0.1f, policy.missedFraction, 0.02f)
    }

    @Test
    fun `ignores the first seconds after the loop starts`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.onLoopStarted()
        policy.run(seconds = 1.9f, missedShare = 0.5f)
        policy.run(seconds = 5.1f)
        assertEquals(QualityLevel.HIGH, policy.level)
        policy.run(seconds = 5.1f, missedShare = 0.5f)
        assertEquals(QualityLevel.MEDIUM, policy.level)
    }

    @Test
    fun `a few missed frames are fine`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.run(seconds = 30f, missedShare = 0.02f)
        assertEquals(QualityLevel.HIGH, policy.level)
    }

    @Test
    fun `frame drops never go below low`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.run(seconds = 60f, missedShare = 0.5f)
        assertEquals(QualityLevel.LOW, policy.level)
    }

    @Test
    fun `steps up only after a minute of stable frames`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.run(seconds = 5.1f, missedShare = 0.5f)
        assertEquals(QualityLevel.MEDIUM, policy.level)
        policy.run(seconds = 50f)
        assertEquals(QualityLevel.MEDIUM, policy.level)
        policy.run(seconds = 15f)
        assertEquals(QualityLevel.HIGH, policy.level)
    }

    @Test
    fun `a missed window restarts the stable minute`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.run(seconds = 5.1f, missedShare = 0.5f)
        policy.run(seconds = 40f)
        policy.run(seconds = 5.1f, missedShare = 0.5f)
        assertEquals(QualityLevel.LOW, policy.level)
        policy.run(seconds = 40f)
        assertEquals(QualityLevel.LOW, policy.level)
    }

    @Test
    fun `screen on tries one level better but never above the start level`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.run(seconds = 11f, missedShare = 0.5f)
        assertEquals(QualityLevel.LOW, policy.level)
        policy.onScreenOn()
        assertEquals(QualityLevel.MEDIUM, policy.level)

        val lowRam = QualityPolicy(QualityLevel.LOW)
        lowRam.onScreenOn()
        lowRam.run(seconds = 120f)
        assertEquals(QualityLevel.LOW, lowRam.level)
    }

    @Test
    fun `battery saver sets the saver level while on`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.powerSave = true
        assertEquals(QualityLevel.SAVER, policy.level)
        policy.powerSave = false
        assertEquals(QualityLevel.HIGH, policy.level)
    }

    @Test
    fun `heat lowers the level and critical heat pauses`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.thermal = ThermalState.MODERATE
        assertEquals(QualityLevel.LOW, policy.level)
        policy.thermal = ThermalState.SEVERE
        assertEquals(QualityLevel.SAVER, policy.level)
        assertFalse(policy.isPaused)
        policy.thermal = ThermalState.CRITICAL
        assertTrue(policy.isPaused)
        policy.thermal = ThermalState.NONE
        assertEquals(QualityLevel.HIGH, policy.level)
        assertFalse(policy.isPaused)
    }

    @Test
    fun `the lower of adaptive and device floors wins`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.run(seconds = 5.1f, missedShare = 0.5f)
        policy.thermal = ThermalState.MODERATE
        assertEquals(QualityLevel.LOW, policy.level)
        policy.thermal = ThermalState.NONE
        assertEquals(QualityLevel.MEDIUM, policy.level)
    }

    @Test
    fun `a forced level overrides everything but the heat pause`() {
        val policy = QualityPolicy(QualityLevel.HIGH)
        policy.powerSave = true
        policy.forced = QualityLevel.MEDIUM
        assertEquals(QualityLevel.MEDIUM, policy.level)
        policy.thermal = ThermalState.CRITICAL
        assertTrue(policy.isPaused)
        policy.forced = null
        assertEquals(QualityLevel.SAVER, policy.level)
    }

    @Test
    fun `reports each change once`() {
        var changes = 0
        val policy = QualityPolicy(QualityLevel.HIGH, onChange = { changes++ })
        policy.powerSave = true
        policy.powerSave = true
        policy.thermal = ThermalState.SEVERE
        assertEquals(1, changes)
        policy.thermal = ThermalState.CRITICAL
        assertEquals(2, changes)
    }

    @Test
    fun `a level caps density and layers but keeps the look`() {
        val config = SnowConfig(density = 4f, fallSpeed = 60f, minRadius = 1f, maxRadius = 4f, layers = 5, wind = 10f, opacity = 1f)
        val low = QualityLevel.LOW.apply(config)
        assertEquals(2f, low.density, 1e-6f)
        assertEquals(2, low.layers)
        assertEquals(config.copy(density = low.density, layers = low.layers), low)
        assertEquals(config, QualityLevel.HIGH.apply(config))
        assertEquals(1, QualityLevel.MEDIUM.apply(config.copy(layers = 1)).layers)
    }
}
