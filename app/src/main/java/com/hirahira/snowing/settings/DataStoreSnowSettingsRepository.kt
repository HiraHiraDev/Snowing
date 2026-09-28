package com.hirahira.snowing.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

class DataStoreSnowSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SnowSettingsRepository {

    override val settings: Flow<SnowSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (SnowSettings) -> SnowSettings) {
        dataStore.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    private fun Preferences.toSettings(): SnowSettings {
        val defaults = SnowSettings()
        return SnowSettings(
            intensity = this[Keys.INTENSITY] ?: defaults.intensity,
            speed = this[Keys.SPEED] ?: defaults.speed,
            lab = LabSettings(
                layers = this[Keys.LAB_LAYERS] ?: defaults.lab.layers,
                turbulence = this[Keys.LAB_TURBULENCE] ?: defaults.lab.turbulence,
                wind = this[Keys.LAB_WIND] ?: defaults.lab.wind,
                gusts = this[Keys.LAB_GUSTS] ?: defaults.lab.gusts,
                showHud = this[Keys.LAB_HUD] ?: defaults.lab.showHud,
                showTracer = this[Keys.LAB_TRACER] ?: defaults.lab.showTracer,
                foreground = this[Keys.LAB_FOREGROUND] ?: defaults.lab.foreground,
                instantChanges = this[Keys.LAB_INSTANT] ?: defaults.lab.instantChanges,
            ),
        )
    }

    private fun MutablePreferences.write(settings: SnowSettings) {
        this[Keys.INTENSITY] = settings.intensity
        this[Keys.SPEED] = settings.speed
        this[Keys.LAB_LAYERS] = settings.lab.layers
        this[Keys.LAB_TURBULENCE] = settings.lab.turbulence
        this[Keys.LAB_WIND] = settings.lab.wind
        this[Keys.LAB_GUSTS] = settings.lab.gusts
        this[Keys.LAB_HUD] = settings.lab.showHud
        this[Keys.LAB_TRACER] = settings.lab.showTracer
        this[Keys.LAB_FOREGROUND] = settings.lab.foreground
        this[Keys.LAB_INSTANT] = settings.lab.instantChanges
    }

    private object Keys {
        val INTENSITY = floatPreferencesKey("intensity")
        val SPEED = floatPreferencesKey("speed")
        val LAB_LAYERS = intPreferencesKey("lab_layers")
        val LAB_TURBULENCE = floatPreferencesKey("lab_turbulence")
        val LAB_WIND = floatPreferencesKey("lab_wind")
        val LAB_GUSTS = floatPreferencesKey("lab_gusts")
        val LAB_HUD = booleanPreferencesKey("lab_hud")
        val LAB_TRACER = booleanPreferencesKey("lab_tracer")
        val LAB_FOREGROUND = booleanPreferencesKey("lab_foreground")
        val LAB_INSTANT = booleanPreferencesKey("lab_instant")
    }
}
