package com.hirahira.snowing.stats

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSnowStatsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.repository() = DataStoreSnowStatsRepository(
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { folder.root.resolve("snow_stats.preferences_pb") },
        ),
    )

    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun `time adds up per day`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        repository.add(today, 60_000)
        repository.add(today, 30_000)
        repository.add(today.minusDays(1), 5_000)

        assertEquals(mapOf(today to 90_000L, today.minusDays(1) to 5_000L), repository.dailyMillis.first())
    }

    @Test
    fun `days older than two weeks are dropped`() = runTest(UnconfinedTestDispatcher()) {
        val repository = repository()
        repository.add(today.minusDays(SnowStatsRepository.KEEP_DAYS), 1_000)
        repository.add(today.minusDays(SnowStatsRepository.KEEP_DAYS - 1), 2_000)
        repository.add(today, 3_000)

        assertEquals(setOf(today.minusDays(SnowStatsRepository.KEEP_DAYS - 1), today), repository.dailyMillis.first().keys)
    }
}
