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
        val customMood = readCustomMood()
        val mood = MoodChoice.fromKey(this[Keys.MOOD])
            ?.takeUnless { it == MoodChoice.CUSTOM && customMood == null }
            ?: defaults.mood
        return SnowSettings(
            mood = mood,
            customMood = customMood,
            timeOfDay = this[Keys.TIME_OF_DAY].toEnum(defaults.timeOfDay),
            lab = LabSettings(
                overrideMood = this[Keys.LAB_OVERRIDE] ?: defaults.lab.overrideMood,
                speed = this[Keys.LAB_SPEED] ?: defaults.lab.speed,
                layers = this[Keys.LAB_LAYERS] ?: defaults.lab.layers,
                turbulence = this[Keys.LAB_TURBULENCE] ?: defaults.lab.turbulence,
                gusts = this[Keys.LAB_GUSTS] ?: defaults.lab.gusts,
                showHud = this[Keys.LAB_HUD] ?: defaults.lab.showHud,
                showTracer = this[Keys.LAB_TRACER] ?: defaults.lab.showTracer,
                foreground = this[Keys.LAB_FOREGROUND] ?: defaults.lab.foreground,
                instantChanges = this[Keys.LAB_INSTANT] ?: defaults.lab.instantChanges,
                quality = this[Keys.LAB_QUALITY].toEnum(defaults.lab.quality),
            ),
        )
    }

    // The custom mood keeps the hidden parameters of the mood it was saved from (ADR-0010), so all are stored.
    private fun Preferences.readCustomMood(): Mood? {
        val look = readLook(Keys.Custom) ?: return null
        val base = BuiltInMoods.of(SnowSettings.DEFAULT_MOOD)!!
        return base.copy(
            speed = this[Keys.CUSTOM_SPEED] ?: base.speed,
            layers = this[Keys.CUSTOM_LAYERS] ?: base.layers,
            turbulence = this[Keys.CUSTOM_TURBULENCE] ?: base.turbulence,
            gusts = this[Keys.CUSTOM_GUSTS] ?: base.gusts,
        ).withLook(look)
    }

    private fun Preferences.readLook(keys: Keys.Look): MoodLook? {
        val intensity = this[keys.intensity] ?: return null
        val defaults = BuiltInMoods.of(SnowSettings.DEFAULT_MOOD)!!
        return MoodLook(
            intensity = intensity,
            flakeSize = this[keys.size].toEnum(defaults.flakeSize),
            windAngle = this[keys.windAngle] ?: defaults.windAngle,
            windStrength = this[keys.windStrength].toEnum(defaults.windStrength),
        ).normalized()
    }

    private fun MutablePreferences.write(settings: SnowSettings) {
        this[Keys.MOOD] = settings.mood.key
        writeLook(Keys.Custom, settings.customMood?.look)
        val custom = settings.customMood
        if (custom == null) {
            remove(Keys.CUSTOM_SPEED)
            remove(Keys.CUSTOM_LAYERS)
            remove(Keys.CUSTOM_TURBULENCE)
            remove(Keys.CUSTOM_GUSTS)
        } else {
            this[Keys.CUSTOM_SPEED] = custom.speed
            this[Keys.CUSTOM_LAYERS] = custom.layers
            this[Keys.CUSTOM_TURBULENCE] = custom.turbulence
            this[Keys.CUSTOM_GUSTS] = custom.gusts
        }
        // Changes to built-in moods are gone (ADR-0010); drop what older builds stored.
        writeLook(Keys.Adjustment, null)
        this[Keys.TIME_OF_DAY] = settings.timeOfDay.name
        this[Keys.LAB_OVERRIDE] = settings.lab.overrideMood
        this[Keys.LAB_SPEED] = settings.lab.speed
        this[Keys.LAB_LAYERS] = settings.lab.layers
        this[Keys.LAB_TURBULENCE] = settings.lab.turbulence
        this[Keys.LAB_GUSTS] = settings.lab.gusts
        this[Keys.LAB_HUD] = settings.lab.showHud
        this[Keys.LAB_TRACER] = settings.lab.showTracer
        this[Keys.LAB_FOREGROUND] = settings.lab.foreground
        this[Keys.LAB_INSTANT] = settings.lab.instantChanges
        this[Keys.LAB_QUALITY] = settings.lab.quality.name
    }

    private fun MutablePreferences.writeLook(keys: Keys.Look, look: MoodLook?) {
        if (look == null) {
            remove(keys.intensity)
            remove(keys.size)
            remove(keys.windAngle)
            remove(keys.windStrength)
        } else {
            this[keys.intensity] = look.intensity
            this[keys.size] = look.flakeSize.name
            this[keys.windAngle] = look.windAngle
            this[keys.windStrength] = look.windStrength.name
        }
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(fallback: E): E =
        enumValues<E>().firstOrNull { it.name == this } ?: fallback

    private object Keys {
        /** The visible part of a mood; its intensity key doubles as "present". */
        class Look(prefix: String) {
            val intensity = intPreferencesKey("${prefix}_intensity")
            val size = stringPreferencesKey("${prefix}_size")
            val windAngle = intPreferencesKey("${prefix}_wind_angle")
            val windStrength = stringPreferencesKey("${prefix}_wind_strength")
        }

        val MOOD = stringPreferencesKey("mood")
        val Custom = Look("custom_mood")
        val CUSTOM_SPEED = intPreferencesKey("custom_mood_speed")
        val CUSTOM_LAYERS = intPreferencesKey("custom_mood_layers")
        val CUSTOM_TURBULENCE = floatPreferencesKey("custom_mood_turbulence")
        val CUSTOM_GUSTS = floatPreferencesKey("custom_mood_gusts")
        /** Written by builds before built-in moods stopped being adjustable; only cleared. */
        val Adjustment = Look("adjust")
        val TIME_OF_DAY = stringPreferencesKey("time_of_day")
        val LAB_OVERRIDE = booleanPreferencesKey("lab_override")
        val LAB_SPEED = intPreferencesKey("lab_speed")
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
