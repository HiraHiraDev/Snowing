package com.hirahira.snowing.power

import com.hirahira.snowing.overlay.StopStyle
import com.hirahira.snowing.testing.FakeOverlayController
import com.hirahira.snowing.testing.FakeSnowStateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnowSwitchTest {

    private val state = FakeSnowStateRepository()
    private val overlay = FakeOverlayController()
    private val snowSwitch = SnowSwitch(state, overlay)

    @Test
    fun `turning on records the choice and starts the overlay`() = runTest {
        assertTrue(snowSwitch.turnOn())
        assertTrue(state.state.value)
        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `turning on without permission changes nothing`() = runTest {
        overlay.permission = false
        assertFalse(snowSwitch.turnOn())
        assertFalse(state.state.value)
        assertFalse(overlay.isRunning.value)
    }

    @Test
    fun `a start refused in the background keeps the choice for reconcile`() = runTest {
        overlay.refuseStart = true
        assertFalse(snowSwitch.turnOn())
        assertTrue(state.state.value)
        assertFalse(overlay.isRunning.value)

        // The app comes to the front.
        overlay.refuseStart = false
        snowSwitch.reconcile()
        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `turning off records the choice and stops the overlay`() = runTest {
        snowSwitch.turnOn()
        snowSwitch.turnOff()
        assertFalse(state.state.value)
        assertFalse(overlay.isRunning.value)
    }

    @Test
    fun `toggle follows the recorded choice`() = runTest {
        snowSwitch.toggle()
        assertTrue(state.state.value)
        snowSwitch.toggle()
        assertFalse(state.state.value)
    }

    @Test
    fun `reconcile restores snow the process lost`() = runTest {
        // Choice survived on disk, process was killed (force stop, reboot, update).
        state.state.value = true
        snowSwitch.reconcile()
        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `reconcile keeps the choice while permission is missing`() = runTest {
        state.state.value = true
        overlay.permission = false
        snowSwitch.reconcile()
        assertFalse(overlay.isRunning.value)
        assertTrue(state.state.value)

        // User grants it again and comes back to the app.
        overlay.permission = true
        snowSwitch.reconcile()
        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `reconcile does not restart a running overlay`() = runTest {
        snowSwitch.turnOn()
        snowSwitch.reconcile()
        snowSwitch.reconcile()
        assertEquals(1, overlay.startCalls)
    }

    @Test
    fun `reconcile stops an overlay the user turned off`() = runTest {
        overlay.start()
        snowSwitch.reconcile()
        assertFalse(overlay.isRunning.value)
    }

    @Test
    fun `snowing needs both the choice and a running overlay`() = runTest {
        snowSwitch.turnOn()
        assertTrue(snowSwitch.snowing.first())

        // Force stop: the overlay is gone, the choice stays.
        overlay.isRunning.value = false
        assertTrue(state.state.value)
        assertFalse(snowSwitch.snowing.first())
    }

    @Test
    fun `while the last flakes fall out it no longer counts as snowing`() = runTest {
        snowSwitch.turnOn()
        state.setEnabled(false)
        assertTrue(overlay.isRunning.value)
        assertFalse(snowSwitch.snowing.first())
    }

    @Test
    fun `the app stops gently, shortcuts at once`() = runTest {
        snowSwitch.turnOn()
        snowSwitch.turnOff()
        assertEquals(StopStyle.GENTLE, overlay.lastStop)

        snowSwitch.turnOn()
        snowSwitch.turnOff(StopStyle.AT_ONCE)
        assertEquals(StopStyle.AT_ONCE, overlay.lastStop)
    }
}
