package com.hirahira.snowing.feature.control

import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.SnowSettingsRepository
import com.hirahira.snowing.settings.TimeOfDay
import com.hirahira.snowing.settings.WindStrength
import com.hirahira.snowing.testing.FakeEntitlements
import com.hirahira.snowing.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val entitlements = FakeEntitlements()

    // Shared with runTest(main) so delays in viewModelScope run on virtual time.
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(labEnabled: Boolean = false) =
        ControlViewModel(settings, entitlements, labEnabled)

    private fun TestScope.collect(vm: ControlViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `state reflects settings and choice`() = runTest {
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY, timeOfDay = TimeOfDay.DUSK)
        val vm = viewModel()
        collect(vm)

        val state = vm.uiState.value
        assertTrue(state.loaded)
        assertEquals(MoodChoice.BUILT_IN, state.moods.map { it.choice })
        assertEquals(listOf(MoodChoice.HEAVY), state.moods.filter { it.selected }.map { it.choice })
        assertEquals(BuiltInMoods.HEAVY.intensity, state.moods.single { it.selected }.intensity)
        assertEquals(TimeOfDay.DUSK, state.timeOfDay)
        val adjust = state.adjust!!
        assertEquals(MoodChoice.HEAVY, adjust.mood)
        assertFalse(adjust.edited)
        assertEquals(BuiltInMoods.HEAVY.windAngle, adjust.windAngle)
    }

    @Test
    fun `the custom mood is listed once it has been saved`() = runTest {
        settings.state.value = SnowSettings(customMood = BuiltInMoods.BLIZZARD)
        val vm = viewModel()
        collect(vm)

        assertEquals(MoodChoice.CUSTOM, vm.uiState.value.moods.last().choice)
    }

    @Test
    fun `an empty custom slot cannot be chosen`() = runTest(main) {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.MoodSelected(MoodChoice.CUSTOM))

        assertEquals(SnowSettings.DEFAULT_MOOD, settings.state.value.mood)
    }

    @Test
    fun `adjusting changes the active mood and marks it edited`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.WindStrengthChanged(WindStrength.GUSTY))

        val written = settings.state.value
        assertEquals(WindStrength.GUSTY, written.activeMood.windStrength)
        assertEquals(BuiltInMoods.HEAVY.layers, written.activeMood.layers)
        assertTrue(vm.uiState.value.adjust!!.edited)
    }

    @Test
    fun `adjusting back to the mood as designed is no change`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.FlakeSizeChanged(FlakeSize.L))
        vm.onEvent(ControlEvent.FlakeSizeChanged(BuiltInMoods.HEAVY.flakeSize))

        assertNull(settings.state.value.moodAdjustment)
        assertFalse(vm.uiState.value.adjust!!.edited)
    }

    @Test
    fun `levels and angles stay in range`() = runTest(main) {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.IntensityChanged(42))
        vm.onEvent(ControlEvent.WindAngleChanged(-90))
        vm.onEvent(ControlEvent.SliderReleased)

        val mood = settings.state.value.activeMood
        assertEquals(Mood.LEVELS.last, mood.intensity)
        assertEquals(270, mood.windAngle)
    }

    @Test
    fun `restoring drops the changes`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.FlakeSizeChanged(FlakeSize.S))
        vm.onEvent(ControlEvent.RestoreMood)

        assertEquals(BuiltInMoods.HEAVY, settings.state.value.activeMood)
    }

    @Test
    fun `choosing another mood drops the changes`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.FlakeSizeChanged(FlakeSize.S))
        vm.onEvent(ControlEvent.MoodSelected(MoodChoice.BLIZZARD))
        vm.onEvent(ControlEvent.MoodSelected(MoodChoice.HEAVY))

        assertNull(settings.state.value.moodAdjustment)
        assertEquals(BuiltInMoods.HEAVY, settings.state.value.activeMood)
    }

    @Test
    fun `saving as new fills the custom slot with the adjusted mood and its hidden parameters`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.BLIZZARD)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.FlakeSizeChanged(FlakeSize.S))
        vm.onEvent(ControlEvent.SaveMoodAsNew)

        val written = settings.state.value
        assertEquals(MoodChoice.CUSTOM, written.mood)
        assertNull(written.moodAdjustment)
        assertEquals(BuiltInMoods.BLIZZARD.copy(flakeSize = FlakeSize.S), written.customMood)
        assertEquals(MoodChoice.CUSTOM, vm.uiState.value.moods.single { it.selected }.choice)
    }

    @Test
    fun `without Pro moods are picked but adjusting is locked`() = runTest(main) {
        entitlements.hasPro.value = false
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.MoodSelected(MoodChoice.BLIZZARD))
        vm.onEvent(ControlEvent.IntensityChanged(2))
        vm.onEvent(ControlEvent.SliderReleased)
        vm.onEvent(ControlEvent.SaveMoodAsNew)

        assertEquals(MoodChoice.BLIZZARD, settings.state.value.mood)
        assertEquals(BuiltInMoods.BLIZZARD, settings.state.value.activeMood)
        assertNull(settings.state.value.customMood)
        val adjust = vm.uiState.value.adjust!!
        assertTrue(adjust.locked)
        assertEquals(BuiltInMoods.BLIZZARD.intensity, adjust.intensity)
    }

    @Test
    fun `slider changes are persisted after the throttle`() = runTest(main) {
        val vm = viewModel(labEnabled = true)
        vm.onEvent(ControlEvent.IntensityChanged(9))
        vm.onEvent(ControlEvent.LayersChanged(5))
        advanceTimeBy(ControlViewModel.SAVE_THROTTLE_MS + 1)

        assertEquals(9, settings.state.value.activeMood.intensity)
        assertEquals(5, settings.state.value.lab.layers)
    }

    @Test
    fun `dragging shows every value at once but writes at most once per throttle`() = runTest(main) {
        val vm = viewModel()
        collect(vm)
        // One second of dragging the wind needle at 60 Hz.
        for (frame in 1..60) {
            val angle = frame * 6
            vm.onEvent(ControlEvent.WindAngleChanged(angle))
            assertEquals(angle % 360, vm.uiState.value.adjust?.windAngle)
            advanceTimeBy(16)
        }

        val maxWrites = (60 * 16 / ControlViewModel.SAVE_THROTTLE_MS + 1).toInt()
        assertTrue("wrote ${settings.writes} times", settings.writes <= maxWrites)
    }

    @Test
    fun `releasing a slider writes the final value at once`() = runTest(main) {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(ControlEvent.IntensityChanged(5))
        vm.onEvent(ControlEvent.IntensityChanged(7))
        vm.onEvent(ControlEvent.SliderReleased)

        assertEquals(7, settings.state.value.activeMood.intensity)
        assertEquals(1, settings.writes)
        assertEquals(7, vm.uiState.value.adjust?.intensity)
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
        assertEquals(BuiltInMoods.BLIZZARD.speed, lab.speed)
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
        assertEquals(BuiltInMoods.HEAVY.speed, lab.speed)
        assertEquals(BuiltInMoods.HEAVY.layers, lab.layers)
        assertEquals(BuiltInMoods.HEAVY.turbulence, lab.turbulence)
        assertEquals(BuiltInMoods.HEAVY.gusts, lab.gusts)
    }

    @Test
    fun `an outside change shows once no edit is pending`() = runTest(main) {
        val vm = viewModel(labEnabled = true)
        collect(vm)
        vm.onEvent(ControlEvent.TurbulenceChanged(0.2f))
        vm.onEvent(ControlEvent.SliderReleased)

        settings.state.value = settings.state.value.copy(mood = MoodChoice.BLIZZARD) // e.g. a tile or the schedule
        assertEquals(MoodChoice.BLIZZARD, vm.uiState.value.moods.single { it.selected }.choice)
        assertEquals(0.2f, settings.state.value.lab.turbulence)
    }

    @Test
    fun `nothing that depends on stored state shows before it loads`() = runTest {
        val neverLoads = object : SnowSettingsRepository {
            override val settings = MutableSharedFlow<SnowSettings>()
            override suspend fun update(transform: (SnowSettings) -> SnowSettings) = Unit
        }
        val vm = ControlViewModel(neverLoads, entitlements, labEnabled = false)
        collect(vm)

        assertFalse(vm.uiState.value.loaded)
        assertTrue(vm.uiState.value.moods.isEmpty())
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
