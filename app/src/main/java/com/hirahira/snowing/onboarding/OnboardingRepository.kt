package com.hirahira.snowing.onboarding

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

/** Whether the first-run onboarding is behind the user (ADR-0011). It shows once. */
interface OnboardingRepository {
    val completed: Flow<Boolean>

    suspend fun complete()
}

class DataStoreOnboardingRepository(
    private val dataStore: DataStore<Preferences>,
) : OnboardingRepository {

    override val completed: Flow<Boolean> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it[COMPLETED] ?: false }
        .distinctUntilChanged()

    override suspend fun complete() {
        dataStore.edit { it[COMPLETED] = true }
    }

    private companion object {
        val COMPLETED = booleanPreferencesKey("completed")
    }
}
