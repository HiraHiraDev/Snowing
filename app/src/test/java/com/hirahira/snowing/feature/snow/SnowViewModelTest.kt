package com.hirahira.snowing.feature.snow

import com.hirahira.snowing.power.SnowSwitch
import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.MoodChoice
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnowViewModelTest {

    private val settings = FakeSettingsRepository()
    private val snowState = FakeSnowStateRepository()
    private val overlay = FakeOverlayController()
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SnowViewModel(settings, SnowSwitch(snowState, overlay))

    private fun TestScope.collect(vm: SnowViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `toggle turns snow on when permission is granted`() = runTest {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(SnowEvent.ToggleSnow)

        assertTrue(vm.uiState.value.snowEnabled)
        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `without permission starting is not possible and the screen says why`() = runTest {
        overlay.permission = false
        val vm = viewModel()
        collect(vm)
        assertTrue(vm.uiState.value.startNeedsPermission)

        vm.onEvent(SnowEvent.ToggleSnow)
        assertFalse(vm.uiState.value.snowEnabled)
        assertFalse(overlay.isRunning.value)
    }

    @Test
    fun `toggle turns running snow off`() = runTest {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(SnowEvent.ToggleSnow)
        vm.onEvent(SnowEvent.ToggleSnow)

        assertFalse(vm.uiState.value.snowEnabled)
        assertFalse(overlay.isRunning.value)
    }

    @Test
    fun `resume restores snow lost while the app was away`() = runTest {
        snowState.state.value = true // e.g. force-stopped from the task manager
        val vm = viewModel()
        collect(vm)
        vm.onEvent(SnowEvent.Resumed)

        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `resume re-checks the permission`() = runTest {
        overlay.permission = false
        val vm = viewModel()
        collect(vm)
        assertFalse(vm.uiState.value.hasOverlayPermission)

        overlay.permission = true
        vm.onEvent(SnowEvent.Resumed)
        assertTrue(vm.uiState.value.hasOverlayPermission)
    }

    @Test
    fun `shows the active mood`() = runTest {
        settings.state.value = SnowSettings(
            mood = MoodChoice.CUSTOM,
            customMood = BuiltInMoods.HEAVY.copy(intensity = 2, flakeSize = FlakeSize.L),
        )
        val vm = viewModel()
        collect(vm)

        val state = vm.uiState.value
        assertEquals(MoodChoice.CUSTOM, state.mood)
        assertEquals(2, state.intensity)
    }

    @Test
    fun `nothing shows before the store loads`() = runTest {
        assertFalse(SnowUiState().loaded)
    }
}
