package com.hirahira.snowing.feature.moods

import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SettingsEditor
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoodsViewModelTest {

    private val settings = FakeSettingsRepository()
    private val entitlements = FakeEntitlements()

    // Shared with runTest(main) so delays in viewModelScope run on virtual time.
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = MoodsViewModel(settings, entitlements)

    private fun TestScope.collect(vm: MoodsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    /** The user's own mood, active: the only mood whose controls show. */
    private val ownActive = SnowSettings(mood = MoodChoice.CUSTOM, customMood = BuiltInMoods.HEAVY)

    @Test
    fun `state reflects settings and choice`() = runTest {
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY, timeOfDay = TimeOfDay.DUSK)
        val vm = viewModel()
        collect(vm)

        val state = vm.uiState.value
        assertTrue(state.loaded)
        assertEquals(MoodChoice.BUILT_IN, state.moods.map { it.choice })
        assertEquals(listOf(MoodChoice.HEAVY), state.moods.filter { it.selected }.map { it.choice })
        assertEquals(TimeOfDay.DUSK, state.timeOfDay)
        assertEquals(OwnMoodUiState.CanCreate, state.ownMood)
    }

    @Test
    fun `the own mood is listed once it exists`() = runTest {
        settings.state.value = SnowSettings(customMood = BuiltInMoods.BLIZZARD)
        val vm = viewModel()
        collect(vm)

        assertEquals(MoodChoice.CUSTOM, vm.uiState.value.moods.last().choice)
    }

    @Test
    fun `an empty custom slot cannot be chosen`() = runTest(main) {
        val vm = viewModel()
        collect(vm)
        vm.onEvent(MoodsEvent.MoodSelected(MoodChoice.CUSTOM))

        assertEquals(SnowSettings.DEFAULT_MOOD, settings.state.value.mood)
    }

    @Test
    fun `creating the own mood copies the active one and makes it active`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.BLIZZARD)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(MoodsEvent.CreateOwnMood)

        val written = settings.state.value
        assertEquals(MoodChoice.CUSTOM, written.mood)
        assertEquals(BuiltInMoods.BLIZZARD, written.customMood)
        assertTrue(vm.uiState.value.ownMood is OwnMoodUiState.Editing)
    }

    @Test
    fun `built-in moods cannot be changed`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY, customMood = BuiltInMoods.FIRST_SNOW)
        val vm = viewModel()
        collect(vm)
        vm.onEvent(MoodsEvent.WindStrengthChanged(WindStrength.GUSTY))
        vm.onEvent(MoodsEvent.IntensityChanged(1))
        vm.onEvent(MoodsEvent.DragFinished)

        assertEquals(BuiltInMoods.HEAVY, settings.state.value.activeMood)
        assertEquals(BuiltInMoods.FIRST_SNOW, settings.state.value.customMood)
        assertEquals(OwnMoodUiState.Hidden, vm.uiState.value.ownMood)
    }

    @Test
    fun `changing the own mood keeps its hidden parameters`() = runTest(main) {
        settings.state.value = ownActive
        val vm = viewModel()
        collect(vm)
        vm.onEvent(MoodsEvent.WindStrengthChanged(WindStrength.GUSTY))
        vm.onEvent(MoodsEvent.FlakeSizeChanged(FlakeSize.L))

        val own = settings.state.value.customMood!!
        assertEquals(WindStrength.GUSTY, own.windStrength)
        assertEquals(FlakeSize.L, own.flakeSize)
        assertEquals(BuiltInMoods.HEAVY.layers, own.layers)
        assertEquals(BuiltInMoods.HEAVY.speed, own.speed)
    }

    @Test
    fun `levels and angles stay in range`() = runTest(main) {
        settings.state.value = ownActive
        val vm = viewModel()
        collect(vm)
        vm.onEvent(MoodsEvent.IntensityChanged(42))
        vm.onEvent(MoodsEvent.WindAngleChanged(-90))
        vm.onEvent(MoodsEvent.DragFinished)

        val mood = settings.state.value.activeMood
        assertEquals(Mood.LEVELS.last, mood.intensity)
        assertEquals(270, mood.windAngle)
    }

    @Test
    fun `the wind arrow points to the nearest eighth of a turn`() = runTest(main) {
        settings.state.value = ownActive
        val vm = viewModel()
        collect(vm)
        for ((angle, arrow) in listOf(200 to 180, 205 to 225, 350 to 0, 90 to 90)) {
            vm.onEvent(MoodsEvent.WindAngleChanged(angle))
            assertEquals(arrow, (vm.uiState.value.ownMood as OwnMoodUiState.Editing).windArrow)
        }
    }

    @Test
    fun `without Pro moods are picked but no own mood is made`() = runTest(main) {
        entitlements.hasPro.value = false
        val vm = viewModel()
        collect(vm)
        assertEquals(OwnMoodUiState.Locked, vm.uiState.value.ownMood)

        vm.onEvent(MoodsEvent.MoodSelected(MoodChoice.BLIZZARD))
        vm.onEvent(MoodsEvent.CreateOwnMood)

        assertEquals(MoodChoice.BLIZZARD, settings.state.value.mood)
        assertNull(settings.state.value.customMood)
    }

    @Test
    fun `dragged changes are persisted after the throttle`() = runTest(main) {
        settings.state.value = ownActive
        val vm = viewModel()
        vm.onEvent(MoodsEvent.IntensityChanged(9))
        advanceTimeBy(SettingsEditor.THROTTLE_MS + 1)

        assertEquals(9, settings.state.value.activeMood.intensity)
    }

    @Test
    fun `dragging shows every value at once but writes at most once per throttle`() = runTest(main) {
        settings.state.value = ownActive
        val vm = viewModel()
        collect(vm)
        // One second of dragging the wind needle at 60 Hz.
        for (frame in 1..60) {
            val angle = frame * 6
            vm.onEvent(MoodsEvent.WindAngleChanged(angle))
            assertEquals(angle % 360, (vm.uiState.value.ownMood as OwnMoodUiState.Editing).windAngle)
            advanceTimeBy(16)
        }

        val maxWrites = (60 * 16 / SettingsEditor.THROTTLE_MS + 1).toInt()
        assertTrue("wrote ${settings.writes} times", settings.writes <= maxWrites)
    }

    @Test
    fun `releasing a slider writes the final value at once`() = runTest(main) {
        settings.state.value = ownActive
        val vm = viewModel()
        collect(vm)
        vm.onEvent(MoodsEvent.IntensityChanged(5))
        vm.onEvent(MoodsEvent.IntensityChanged(7))
        vm.onEvent(MoodsEvent.DragFinished)

        assertEquals(7, settings.state.value.activeMood.intensity)
        assertEquals(1, settings.writes)
        assertEquals(7, (vm.uiState.value.ownMood as OwnMoodUiState.Editing).intensity)
    }

    @Test
    fun `an outside change shows once no edit is pending`() = runTest(main) {
        settings.state.value = ownActive
        val vm = viewModel()
        collect(vm)
        vm.onEvent(MoodsEvent.IntensityChanged(4))
        vm.onEvent(MoodsEvent.DragFinished)

        settings.state.value = settings.state.value.copy(mood = MoodChoice.BLIZZARD) // e.g. a tile or the schedule
        assertEquals(MoodChoice.BLIZZARD, vm.uiState.value.moods.single { it.selected }.choice)
        assertEquals(4, settings.state.value.customMood!!.intensity)
    }

    @Test
    fun `nothing that depends on stored state shows before it loads`() = runTest {
        val neverLoads = object : SnowSettingsRepository {
            override val settings = MutableSharedFlow<SnowSettings>()
            override suspend fun update(transform: (SnowSettings) -> SnowSettings) = Unit
        }
        val vm = MoodsViewModel(neverLoads, entitlements)
        collect(vm)

        assertFalse(vm.uiState.value.loaded)
        assertTrue(vm.uiState.value.moods.isEmpty())
    }
}
