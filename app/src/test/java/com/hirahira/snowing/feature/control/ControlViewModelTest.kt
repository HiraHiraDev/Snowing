package com.hirahira.snowing.feature.control

import com.hirahira.snowing.power.SnowSwitch
import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.testing.FakeEntitlements
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
    private val entitlements = FakeEntitlements()

    // Shared with runTest(main) so delays in viewModelScope run on virtual time.
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(labEnabled: Boolean = false) =
        ControlViewModel(settings, SnowSwitch(snowState, overlay), entitlements, labEnabled)

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
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY)
        val vm = viewModel()
        collect(vm)

        val state = vm.uiState.value
        assertTrue(state.snowEnabled)
        assertTrue(state.hasOverlayPermission)
        assertEquals(MoodChoice.entries, state.moods.map { it.choice })
        assertEquals(listOf(MoodChoice.HEAVY), state.moods.filter { it.selected }.map { it.choice })
        assertEquals(BuiltInMoods.HEAVY.intensity, state.moods.single { it.selected }.intensity)
        assertNull(state.customMood)
    }

    @Test
    fun `choosing a mood is written at once and opens the editor only for the custom one`() = runTest(main) {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.MoodSelected(MoodChoice.BLIZZARD))
        assertEquals(MoodChoice.BLIZZARD, settings.state.value.mood)
        assertNull(vm.uiState.value.customMood)

        vm.onEvent(ControlEvent.MoodSelected(MoodChoice.CUSTOM))
        assertEquals(MoodChoice.CUSTOM, settings.state.value.mood)
        assertEquals(BuiltInMoods.CUSTOM_BASE.intensity, vm.uiState.value.customMood?.intensity)
    }

    @Test
    fun `slider changes are persisted after the throttle`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.CUSTOM)
        val vm = viewModel(labEnabled = true)
        vm.onEvent(ControlEvent.CustomIntensityChanged(9))
        vm.onEvent(ControlEvent.LayersChanged(5))
        advanceTimeBy(ControlViewModel.SAVE_THROTTLE_MS + 1)

        assertEquals(9, settings.state.value.customMood.intensity)
        assertEquals(5, settings.state.value.lab.layers)
    }

    @Test
    fun `custom levels stay in range and keep the hidden parameters`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.CUSTOM)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.CustomWindChanged(42))
        vm.onEvent(ControlEvent.CustomFlakeSizeChanged(FlakeSize.S))

        val custom = settings.state.value.customMood
        assertEquals(10, custom.wind)
        assertEquals(FlakeSize.S, custom.flakeSize)
        assertEquals(BuiltInMoods.CUSTOM_BASE.layers, custom.layers)
    }

    @Test
    fun `without Pro the custom mood is shown but not written`() = runTest(main) {
        entitlements.hasPro.value = false
        settings.state.value = SnowSettings(mood = MoodChoice.FLUFF)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.MoodSelected(MoodChoice.CUSTOM))
        vm.onEvent(ControlEvent.CustomIntensityChanged(9))
        vm.onEvent(ControlEvent.SliderReleased)

        assertEquals(MoodChoice.FLUFF, settings.state.value.mood)
        assertEquals(BuiltInMoods.CUSTOM_BASE, settings.state.value.customMood)
        assertEquals(9, vm.uiState.value.customMood?.intensity)
    }

    @Test
    fun `dragging shows every value at once but writes at most once per throttle`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.CUSTOM)
        val vm = viewModel()
        collect(vm)
        // One second of dragging at 60 Hz, back and forth across the levels.
        for (frame in 1..60) {
            val level = frame % 10 + 1
            vm.onEvent(ControlEvent.CustomIntensityChanged(level))
            assertEquals(level, vm.uiState.value.customMood?.intensity)
            advanceTimeBy(16)
        }

        val maxWrites = (60 * 16 / ControlViewModel.SAVE_THROTTLE_MS + 1).toInt()
        assertTrue("wrote ${settings.writes} times", settings.writes <= maxWrites)
    }

    @Test
    fun `releasing a slider writes the final value at once`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.CUSTOM)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.CustomSpeedChanged(3))
        vm.onEvent(ControlEvent.CustomSpeedChanged(7))
        vm.onEvent(ControlEvent.SliderReleased)

        assertEquals(7, settings.state.value.customMood.speed)
        assertEquals(1, settings.writes)
        assertEquals(7, vm.uiState.value.customMood?.speed)
    }

    @Test
    fun `switches are written at once`() = runTest(main) {
        val vm = viewModel(labEnabled = true)
        collect(vm)
        vm.onEvent(ControlEvent.HudToggled(true))
        vm.onEvent(ControlEvent.MoodOverrideToggled(true))

        assertTrue(settings.state.value.lab.showHud)
        assertTrue(settings.state.value.lab.overrideMood)
    }

    @Test
    fun `without the override the lab shows the active mood's hidden parameters`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.BLIZZARD)
        val vm = viewModel(labEnabled = true)
        collect(vm)

        val lab = vm.uiState.value.lab!!
        assertEquals(BuiltInMoods.BLIZZARD.layers, lab.layers)
        assertEquals(BuiltInMoods.BLIZZARD.turbulence, lab.turbulence)
        assertEquals(BuiltInMoods.BLIZZARD.gusts, lab.gusts)
    }

    @Test
    fun `turning the override on starts from the active mood`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY)
        val vm = viewModel(labEnabled = true)
        collect(vm)
        vm.onEvent(ControlEvent.MoodOverrideToggled(true))

        val lab = settings.state.value.lab
        assertEquals(BuiltInMoods.HEAVY.layers, lab.layers)
        assertEquals(BuiltInMoods.HEAVY.turbulence, lab.turbulence)
        assertEquals(BuiltInMoods.HEAVY.gusts, lab.gusts)
    }

    @Test
    fun `an outside change shows once no edit is pending`() = runTest(main) {
        val vm = viewModel(labEnabled = true)
        collect(vm)
        vm.onEvent(ControlEvent.WindDirectionChanged(-30f))
        vm.onEvent(ControlEvent.SliderReleased)

        settings.state.value = settings.state.value.copy(mood = MoodChoice.BLIZZARD) // e.g. a tile or the schedule
        assertEquals(MoodChoice.BLIZZARD, vm.uiState.value.moods.single { it.selected }.choice)
        assertEquals(-30f, vm.uiState.value.lab?.windDirection)
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
