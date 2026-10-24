package com.hirahira.snowing.feature.home

import com.hirahira.snowing.clock.WallClock
import com.hirahira.snowing.clock.millisToNextMinute
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.TimeOfDay
import com.hirahira.snowing.testing.FakeOnboardingRepository
import com.hirahira.snowing.testing.FakeSettingsRepository
import com.hirahira.snowing.ui.theme.Sky
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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val settings = FakeSettingsRepository()
    private val onboarding = FakeOnboardingRepository(completed = true)
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Starts at [start] and moves with the test's virtual time. */
    private fun TestScope.clockFrom(start: LocalTime) = object : WallClock {
        override fun now(): LocalTime = start.plusNanos(testScheduler.currentTime * 1_000_000)

        override fun today(): LocalDate = LocalDate.of(2026, 10, 17)
    }

    private fun TestScope.collect(vm: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `auto follows the clock minute by minute`() = runTest(main) {
        val vm = HomeViewModel(settings, onboarding, clockFrom(LocalTime.of(17, 29, 30)))
        collect(vm)
        assertEquals(17f + 29f / 60, vm.uiState.value.skyHour, 1e-4f)

        advanceTimeBy(30_001)
        assertEquals(17.5f, vm.uiState.value.skyHour, 1e-4f)

        advanceTimeBy(60_000)
        assertEquals(17f + 31f / 60, vm.uiState.value.skyHour, 1e-4f)
    }

    @Test
    fun `a chosen time of day pins its hour`() = runTest(main) {
        settings.state.value = SnowSettings(timeOfDay = TimeOfDay.DAWN)
        val vm = HomeViewModel(settings, onboarding, clockFrom(LocalTime.of(23, 0)))
        collect(vm)
        assertEquals(Sky.HOUR_DAWN, vm.uiState.value.skyHour)

        settings.state.value = SnowSettings(timeOfDay = TimeOfDay.AUTO)
        assertEquals(23f, vm.uiState.value.skyHour)
    }

    @Test
    fun `onboarding comes first, then the pages`() = runTest(main) {
        onboarding.completed.value = false
        val vm = HomeViewModel(settings, onboarding, clockFrom(LocalTime.NOON))
        assertEquals(HomeContent.LOADING, vm.uiState.value.content)
        collect(vm)
        assertEquals(HomeContent.ONBOARDING, vm.uiState.value.content)

        onboarding.complete()
        assertEquals(HomeContent.PAGES, vm.uiState.value.content)
    }

    @Test
    fun `the wait reaches exactly the next full minute`() {
        assertEquals(60_000L, millisToNextMinute(LocalTime.of(8, 0)))
        assertEquals(500L, millisToNextMinute(LocalTime.of(8, 0, 59, 500_000_000)))
    }
}
