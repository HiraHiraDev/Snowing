package com.hirahira.snowing.power

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * The user's choice "I want snow". Persisted, so it outlives the process,
 * reboots and updates. The single source of truth for whether it should snow;
 * whether it actually does is [com.hirahira.snowing.overlay.OverlayController.isRunning].
 */
interface SnowStateRepository {
    val enabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}

class DataStoreSnowStateRepository(
    private val dataStore: DataStore<Preferences>,
) : SnowStateRepository {

    override val enabled: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[ENABLED] ?: false }
        .distinctUntilChanged()

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[ENABLED] = enabled }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("snow_enabled")
    }
}
