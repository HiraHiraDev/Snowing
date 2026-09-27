package com.hirahira.snowing.feature.control

import com.hirahira.snowing.power.SnowSwitch
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.testing.FakeOverlayController
import com.hirahira.snowing.testing.FakeSettingsRepository
import com.hirahira.snowing.testing.FakeSnowStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ControlViewModelTest {

    private val settings = FakeSettingsRepository()
    private val snowState = FakeSnowStateRepository()
    private val overlay = FakeOverlayController()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(labEnabled: Boolean = false) =
        ControlViewModel(settings, SnowSwitch(snowState, overlay), labEnabled)

    private fun TestScope.collect(vm: ControlViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `toggle turns snow on when permission is granted`() = runTest {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.ToggleSnow)

        assertTrue(vm.uiState.value.snowEnabled)
        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `toggle without permission leaves snow off and shows the permission card`() = runTest {
        overlay.permission = false
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.ToggleSnow)

        assertFalse(vm.uiState.value.snowEnabled)
        assertFalse(vm.uiState.value.hasOverlayPermission)
        assertFalse(overlay.isRunning.value)
    }

    @Test
    fun `toggle turns running snow off`() = runTest {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.ToggleSnow)
        vm.onEvent(ControlEvent.ToggleSnow)

        assertFalse(vm.uiState.value.snowEnabled)
        assertFalse(overlay.isRunning.value)
    }

    @Test
    fun `resume restores snow lost while the app was away`() = runTest {
        snowState.state.value = true // e.g. force-stopped from the task manager
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.Resumed)

        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `resume re-checks the permission`() = runTest {
        overlay.permission = false
        val vm = viewModel()
        collect(vm)
        assertFalse(vm.uiState.value.hasOverlayPermission)

        overlay.permission = true
        vm.onEvent(ControlEvent.Resumed)
        assertTrue(vm.uiState.value.hasOverlayPermission)
    }

    @Test
    fun `state reflects settings, choice and permission`() = runTest {
        snowState.state.value = true
        settings.state.value = SnowSettings(intensity = 0.8f, speed = 0.2f)
        val vm = viewModel()
        collect(vm)

        val state = vm.uiState.value
        assertTrue(state.snowEnabled)
        assertTrue(state.hasOverlayPermission)
        assertEquals(0.8f, state.intensity)
        assertEquals(0.2f, state.speed)
    }

    @Test
    fun `slider changes are persisted`() {
        val vm = viewModel(labEnabled = true)
        vm.onEvent(ControlEvent.IntensityChanged(0.9f))
        vm.onEvent(ControlEvent.LayersChanged(5))

        assertEquals(0.9f, settings.state.value.intensity)
        assertEquals(5, settings.state.value.lab.layers)
    }

    @Test
    fun `lab is hidden unless enabled`() = runTest {
        val hidden = viewModel(labEnabled = false)
        val shown = viewModel(labEnabled = true)
        collect(hidden)
        collect(shown)

        assertNull(hidden.uiState.value.lab)
        assertNotNull(shown.uiState.value.lab)
    }
}
