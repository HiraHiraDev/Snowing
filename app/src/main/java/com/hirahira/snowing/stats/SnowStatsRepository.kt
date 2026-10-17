package com.hirahira.snowing.stats

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

/** How long it snowed on screen, per local day (ADR-0010: simple stats in v1). */
interface SnowStatsRepository {
    /** Milliseconds of snow per day, for the last [KEEP_DAYS] days at most. */
    val dailyMillis: Flow<Map<LocalDate, Long>>

    suspend fun add(date: LocalDate, millis: Long)

    companion object {
        /** Two weeks: enough for this week and last, little enough to stay a tiny file. */
        const val KEEP_DAYS = 14L
    }
}

class DataStoreSnowStatsRepository(
    private val dataStore: DataStore<Preferences>,
) : SnowStatsRepository {

    override val dailyMillis: Flow<Map<LocalDate, Long>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            prefs.asMap().mapNotNull { (key, value) ->
                val date = key.name.removePrefix(PREFIX).takeIf { key.name.startsWith(PREFIX) }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                if (date != null && value is Long) date to value else null
            }.toMap()
        }
        .distinctUntilChanged()

    override suspend fun add(date: LocalDate, millis: Long) {
        dataStore.edit { prefs ->
            val key = keyFor(date)
            prefs[key] = (prefs[key] ?: 0L) + millis
            val oldest = date.minusDays(SnowStatsRepository.KEEP_DAYS - 1)
            prefs.asMap().keys
                .filter { it.name.startsWith(PREFIX) && LocalDate.parse(it.name.removePrefix(PREFIX)) < oldest }
                .forEach { prefs.remove(it) }
        }
    }

    private fun keyFor(date: LocalDate) = longPreferencesKey("$PREFIX$date")

    private companion object {
        const val PREFIX = "day_"
    }
}
