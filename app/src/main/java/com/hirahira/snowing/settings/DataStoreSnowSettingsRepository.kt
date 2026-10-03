package com.hirahira.snowing.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val custom = defaults.customMood
        return SnowSettings(
            mood = MoodChoice.fromKey(this[Keys.MOOD]) ?: defaults.mood,
            customMood = BuiltInMoods.custom(
                flakeSize = this[Keys.CUSTOM_SIZE]?.let { name -> FlakeSize.entries.firstOrNull { it.name == name } }
                    ?: custom.flakeSize,
                intensity = this[Keys.CUSTOM_INTENSITY] ?: custom.intensity,
                speed = this[Keys.CUSTOM_SPEED] ?: custom.speed,
                wind = this[Keys.CUSTOM_WIND] ?: custom.wind,
            ),
            windDirection = (this[Keys.WIND_DIRECTION] ?: defaults.windDirection)
                .coerceIn(-SnowSettings.MAX_WIND_DIRECTION, SnowSettings.MAX_WIND_DIRECTION),
            lab = LabSettings(
                overrideMood = this[Keys.LAB_OVERRIDE] ?: defaults.lab.overrideMood,
                layers = this[Keys.LAB_LAYERS] ?: defaults.lab.layers,
                turbulence = this[Keys.LAB_TURBULENCE] ?: defaults.lab.turbulence,
                gusts = this[Keys.LAB_GUSTS] ?: defaults.lab.gusts,
                showHud = this[Keys.LAB_HUD] ?: defaults.lab.showHud,
                showTracer = this[Keys.LAB_TRACER] ?: defaults.lab.showTracer,
                foreground = this[Keys.LAB_FOREGROUND] ?: defaults.lab.foreground,
                instantChanges = this[Keys.LAB_INSTANT] ?: defaults.lab.instantChanges,
                quality = this[Keys.LAB_QUALITY]
                    ?.let { name -> LabQuality.entries.firstOrNull { it.name == name } }
                    ?: defaults.lab.quality,
            ),
        )
    }

    // Only the custom mood's visible values are stored; its hidden ones come from code (ADR-0007).
    private fun MutablePreferences.write(settings: SnowSettings) {
        this[Keys.MOOD] = settings.mood.key
        this[Keys.CUSTOM_SIZE] = settings.customMood.flakeSize.name
        this[Keys.CUSTOM_INTENSITY] = settings.customMood.intensity
        this[Keys.CUSTOM_SPEED] = settings.customMood.speed
        this[Keys.CUSTOM_WIND] = settings.customMood.wind
        this[Keys.WIND_DIRECTION] = settings.windDirection
        this[Keys.LAB_OVERRIDE] = settings.lab.overrideMood
        this[Keys.LAB_LAYERS] = settings.lab.layers
        this[Keys.LAB_TURBULENCE] = settings.lab.turbulence
        this[Keys.LAB_GUSTS] = settings.lab.gusts
        this[Keys.LAB_HUD] = settings.lab.showHud
        this[Keys.LAB_TRACER] = settings.lab.showTracer
        this[Keys.LAB_FOREGROUND] = settings.lab.foreground
        this[Keys.LAB_INSTANT] = settings.lab.instantChanges
        this[Keys.LAB_QUALITY] = settings.lab.quality.name
    }

    private object Keys {
        val MOOD = stringPreferencesKey("mood")
        val CUSTOM_SIZE = stringPreferencesKey("custom_size")
        val CUSTOM_INTENSITY = intPreferencesKey("custom_intensity")
        val CUSTOM_SPEED = intPreferencesKey("custom_speed")
        val CUSTOM_WIND = intPreferencesKey("custom_wind")
        val WIND_DIRECTION = floatPreferencesKey("wind_direction")
        val LAB_OVERRIDE = booleanPreferencesKey("lab_override")
        val LAB_LAYERS = intPreferencesKey("lab_layers")
        val LAB_TURBULENCE = floatPreferencesKey("lab_turbulence")
        val LAB_GUSTS = floatPreferencesKey("lab_gusts")
        val LAB_HUD = booleanPreferencesKey("lab_hud")
        val LAB_TRACER = booleanPreferencesKey("lab_tracer")
        val LAB_FOREGROUND = booleanPreferencesKey("lab_foreground")
        val LAB_INSTANT = booleanPreferencesKey("lab_instant")
        val LAB_QUALITY = stringPreferencesKey("lab_quality")
    }
}
