package com.hirahira.snowing.power

import com.hirahira.snowing.testing.FakeOverlayController
import com.hirahira.snowing.testing.FakeSnowStateRepository
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
}
