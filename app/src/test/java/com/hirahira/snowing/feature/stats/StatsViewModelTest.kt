package com.hirahira.snowing.feature.stats

import com.hirahira.snowing.testing.FakeStatsRepository
import com.hirahira.snowing.testing.FakeWallClock
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val stats = FakeStatsRepository()
    private val clock = FakeWallClock() // Saturday 2026-10-17
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): StatsViewModel {
        val vm = StatsViewModel(stats, clock) { DayOfWeek.MONDAY }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `shows today and the week in whole minutes`() = runTest {
        stats.state.value = mapOf(clock.date to 3 * 60_000L + 59_000, clock.date.minusDays(1) to 60 * 60_000L)
        val state = viewModel().uiState.value

        assertTrue(state.loaded)
        assertEquals(3, state.todayMinutes)
        assertEquals(63, state.weekMinutes)
        assertEquals(DayOfWeek.MONDAY, state.days.first().day)
        assertTrue(state.days[5].isToday)
    }

    @Test
    fun `bars scale to the busiest day but a short week does not fill the chart`() = runTest {
        stats.state.value = mapOf(clock.date to 6 * 60_000L)
        assertEquals(0.1f, viewModel().uiState.value.days[5].fraction, 1e-4f)

        stats.state.value = mapOf(clock.date to 120 * 60_000L, clock.date.minusDays(1) to 60 * 60_000L)
        val days = viewModel().uiState.value.days
        assertEquals(1f, days[5].fraction, 1e-4f)
        assertEquals(0.5f, days[4].fraction, 1e-4f)
    }

    @Test
    fun `new snow shows as it is recorded`() = runTest {
        val vm = viewModel()
        assertEquals(0, vm.uiState.value.todayMinutes)

        stats.add(clock.date, 5 * 60_000L)
        assertEquals(5, vm.uiState.value.todayMinutes)
    }
}
