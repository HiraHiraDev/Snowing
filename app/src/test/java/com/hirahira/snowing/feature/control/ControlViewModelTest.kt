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
import kotlinx.coroutines.test.advanceTimeBy
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

    // Shared with runTest(main) so delays in viewModelScope run on virtual time.
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

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
    fun `slider changes are persisted after the throttle`() = runTest(main) {
        val vm = viewModel(labEnabled = true)
        vm.onEvent(ControlEvent.IntensityChanged(0.9f))
        vm.onEvent(ControlEvent.LayersChanged(5))
        advanceTimeBy(ControlViewModel.SAVE_THROTTLE_MS + 1)

        assertEquals(0.9f, settings.state.value.intensity)
        assertEquals(5, settings.state.value.lab.layers)
    }

    @Test
    fun `dragging shows every value at once but writes at most once per throttle`() = runTest(main) {
        val vm = viewModel()
        collect(vm)
        // One second of dragging at 60 Hz.
        for (frame in 1..60) {
            vm.onEvent(ControlEvent.IntensityChanged(frame / 60f))
            assertEquals(frame / 60f, vm.uiState.value.intensity)
            advanceTimeBy(16)
        }

        val maxWrites = (60 * 16 / ControlViewModel.SAVE_THROTTLE_MS + 1).toInt()
        assertTrue("wrote ${settings.writes} times", settings.writes <= maxWrites)
    }

    @Test
    fun `releasing a slider writes the final value at once`() = runTest(main) {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.IntensityChanged(0.3f))
        vm.onEvent(ControlEvent.IntensityChanged(0.7f))
        vm.onEvent(ControlEvent.SliderReleased)

        assertEquals(0.7f, settings.state.value.intensity)
        assertEquals(1, settings.writes)
        assertEquals(0.7f, vm.uiState.value.intensity)
    }

    @Test
    fun `switches are written at once`() = runTest(main) {
        val vm = viewModel(labEnabled = true)
        collect(vm)
        vm.onEvent(ControlEvent.HudToggled(true))

        assertTrue(settings.state.value.lab.showHud)
    }

    @Test
    fun `an outside change shows once no edit is pending`() = runTest(main) {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.IntensityChanged(0.3f))
        vm.onEvent(ControlEvent.SliderReleased)

        settings.state.value = settings.state.value.copy(intensity = 0.95f) // e.g. a tile or a mood
        assertEquals(0.95f, vm.uiState.value.intensity)
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
