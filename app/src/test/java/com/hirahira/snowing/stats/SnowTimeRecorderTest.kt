package com.hirahira.snowing.stats

import com.hirahira.snowing.testing.FakeStatsRepository
import com.hirahira.snowing.testing.FakeWallClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnowTimeRecorderTest {

    private val stats = FakeStatsRepository()
    private val clock = FakeWallClock()

    private fun TestScope.recorder() = SnowTimeRecorder(stats, clock, { testScheduler.currentTime }, backgroundScope)

    @Test
    fun `counts only while the snow shows`() = runTest {
        val recorder = recorder()
        recorder.setVisible(true)
        advanceTimeBy(90_000)
        recorder.setVisible(false)
        advanceTimeBy(300_000)
        runCurrent()

        assertEquals(90_000L, stats.state.value[clock.date])
    }

    @Test
    fun `writes every minute while the snow shows`() = runTest {
        val recorder = recorder()
        recorder.setVisible(true)
        advanceTimeBy(SnowTimeRecorder.FLUSH_EVERY_MS * 3 + 1)

        assertEquals(SnowTimeRecorder.FLUSH_EVERY_MS * 3, stats.state.value[clock.date])
    }

    @Test
    fun `time after midnight goes to the new day`() = runTest {
        val recorder = recorder()
        recorder.setVisible(true)
        advanceTimeBy(SnowTimeRecorder.FLUSH_EVERY_MS + 1)
        val evening = clock.date
        clock.date = evening.plusDays(1)
        advanceTimeBy(SnowTimeRecorder.FLUSH_EVERY_MS)
        recorder.setVisible(false)
        runCurrent()

        assertEquals(SnowTimeRecorder.FLUSH_EVERY_MS, stats.state.value[evening])
        assertTrue(stats.state.value.getValue(clock.date) >= SnowTimeRecorder.FLUSH_EVERY_MS - 1)
    }

    @Test
    fun `repeated signals do not double count`() = runTest {
        val recorder = recorder()
        recorder.setVisible(true)
        advanceTimeBy(10_000)
        recorder.setVisible(true)
        advanceTimeBy(10_000)
        recorder.setVisible(false)
        recorder.setVisible(false)
        runCurrent()

        assertEquals(20_000L, stats.state.value[clock.date])
    }
}
