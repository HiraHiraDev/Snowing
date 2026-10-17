package com.hirahira.snowing.debug.lab

import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.LabSettings
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SettingsEditor
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.testing.FakeSettingsRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LabViewModelTest {

    private val settings = FakeSettingsRepository()
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): LabViewModel {
        val vm = LabViewModel(settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `switches are written at once`() = runTest(main) {
        val vm = viewModel()
        vm.onEvent(LabEvent.HudToggled(true))
        vm.onEvent(LabEvent.MoodOverrideToggled(true))

        assertTrue(settings.state.value.lab.showHud)
        assertTrue(settings.state.value.lab.overrideMood)
    }

    @Test
    fun `without the override the lab shows the active mood's hidden parameters`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.BLIZZARD)
        val lab = viewModel().uiState.value

        assertEquals(BuiltInMoods.BLIZZARD.speed, lab.speed)
        assertEquals(BuiltInMoods.BLIZZARD.layers, lab.layers)
        assertEquals(BuiltInMoods.BLIZZARD.turbulence, lab.turbulence)
        assertEquals(BuiltInMoods.BLIZZARD.gusts, lab.gusts)
    }

    @Test
    fun `turning the override on starts from the active mood`() = runTest(main) {
        settings.state.value = SnowSettings(mood = MoodChoice.HEAVY)
        viewModel().onEvent(LabEvent.MoodOverrideToggled(true))

        val lab = settings.state.value.lab
        assertEquals(BuiltInMoods.HEAVY.speed, lab.speed)
        assertEquals(BuiltInMoods.HEAVY.layers, lab.layers)
        assertEquals(BuiltInMoods.HEAVY.turbulence, lab.turbulence)
        assertEquals(BuiltInMoods.HEAVY.gusts, lab.gusts)
    }

    @Test
    fun `sliders are written after the throttle and stay in range`() = runTest(main) {
        val vm = viewModel()
        vm.onEvent(LabEvent.LayersChanged(42))
        vm.onEvent(LabEvent.SpeedChanged(0))
        advanceTimeBy(SettingsEditor.THROTTLE_MS + 1)

        assertEquals(LabSettings.LAYERS.last, settings.state.value.lab.layers)
        assertEquals(1, settings.state.value.lab.speed)
    }
}
