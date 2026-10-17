package com.hirahira.snowing.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSnowSettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.store() = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { folder.root.resolve("snow_settings.preferences_pb") },
    )

    @Test
    fun `an empty store gives the defaults`() = runTest(UnconfinedTestDispatcher()) {
        assertEquals(SnowSettings(), DataStoreSnowSettingsRepository(store()).settings.first())
    }

    @Test
    fun `everything the user chose survives a round trip`() = runTest(UnconfinedTestDispatcher()) {
        val repository = DataStoreSnowSettingsRepository(store())
        val chosen = SnowSettings(
            mood = MoodChoice.CUSTOM,
            customMood = BuiltInMoods.BLIZZARD.copy(intensity = 5, flakeSize = FlakeSize.S, windAngle = 100),
            moodAdjustment = MoodAdjustment(2, FlakeSize.L, windAngle = 10, WindStrength.GENTLE),
            timeOfDay = TimeOfDay.NIGHT,
            lab = LabSettings(overrideMood = true, speed = 9, layers = 2, quality = LabQuality.LOW),
        )
        repository.update { chosen }

        assertEquals(chosen, repository.settings.first())
    }

    @Test
    fun `clearing the adjustment and the slot removes them`() = runTest(UnconfinedTestDispatcher()) {
        val repository = DataStoreSnowSettingsRepository(store())
        repository.update {
            it.copy(customMood = BuiltInMoods.HEAVY, moodAdjustment = BuiltInMoods.FIRST_SNOW.adjustment.copy(intensity = 8))
        }
        repository.update { it.copy(customMood = null, moodAdjustment = null) }

        val read = repository.settings.first()
        assertNull(read.customMood)
        assertNull(read.moodAdjustment)
    }

    @Test
    fun `custom chosen without a saved slot falls back to the default mood`() = runTest(UnconfinedTestDispatcher()) {
        val dataStore = store()
        dataStore.edit { it[stringPreferencesKey("mood")] = MoodChoice.CUSTOM.key }

        assertEquals(SnowSettings.DEFAULT_MOOD, DataStoreSnowSettingsRepository(dataStore).settings.first().mood)
    }

    @Test
    fun `stored values out of range are brought back into range`() = runTest(UnconfinedTestDispatcher()) {
        val dataStore = store()
        dataStore.edit {
            it[intPreferencesKey("adjust_intensity")] = 40
            it[intPreferencesKey("adjust_wind_angle")] = 400
        }

        val adjustment = DataStoreSnowSettingsRepository(dataStore).settings.first().moodAdjustment!!
        assertEquals(Mood.LEVELS.last, adjustment.intensity)
        assertEquals(40, adjustment.windAngle)
    }
}
