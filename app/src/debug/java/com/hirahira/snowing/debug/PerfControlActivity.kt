package com.hirahira.snowing.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.settings.LabQuality
import com.hirahira.snowing.settings.MoodChoice
import kotlinx.coroutines.launch

/**
 * Debug-only remote control for repeatable performance runs: sets the snow
 * variant and switches it on or off without touching the UI.
 *
 *     adb shell am start -n com.hirahira.snowing/.debug.PerfControlActivity \
 *         --ez snow true --es mood blizzard --ez bokeh true --es quality low
 *
 * `mood` takes a MoodChoice key and drops any adjustment. `override` with `speed`, `layers`,
 * `turbulence` and `gusts` replaces the mood's hidden parameters. `direction` (int, degrees
 * clockwise from up, as on the wind wheel) adjusts the active mood's wind.
 *
 * Every extra is optional; only the ones given change. Guarded by the DUMP
 * permission, which adb shell holds and regular apps cannot get.
 */
class PerfControlActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = intent.extras ?: Bundle.EMPTY
        val container = appContainer
        lifecycleScope.launch {
            container.settingsRepository.update { saved ->
                val current = MoodChoice.fromKey(extras.getString(EXTRA_MOOD))
                    ?.let { saved.copy(mood = it, moodAdjustment = null) }
                    ?: saved
                current.copy(
                    moodAdjustment = if (extras.containsKey(EXTRA_DIRECTION)) {
                        current.activeMood.adjustment.copy(windAngle = extras.getInt(EXTRA_DIRECTION)).normalized()
                    } else {
                        current.moodAdjustment
                    },
                    lab = current.lab.copy(
                        overrideMood = extras.booleanOr(EXTRA_OVERRIDE, current.lab.overrideMood),
                        speed = extras.intOr(EXTRA_SPEED, current.lab.speed),
                        layers = extras.intOr(EXTRA_LAYERS, current.lab.layers),
                        turbulence = extras.floatOr(EXTRA_TURBULENCE, current.lab.turbulence),
                        gusts = extras.floatOr(EXTRA_GUSTS, current.lab.gusts),
                        showHud = extras.booleanOr(EXTRA_HUD, current.lab.showHud),
                        foreground = extras.booleanOr(EXTRA_BOKEH, current.lab.foreground),
                        quality = extras.qualityOr(current.lab.quality),
                    ),
                )
            }
            // Started while this activity is in the foreground, so the FGS start is allowed.
            if (extras.containsKey(EXTRA_SNOW)) {
                if (extras.getBoolean(EXTRA_SNOW)) container.snowSwitch.turnOn() else container.snowSwitch.turnOff()
            }
            finish()
        }
    }

    private fun Bundle.floatOr(key: String, fallback: Float) = if (containsKey(key)) getFloat(key) else fallback

    private fun Bundle.intOr(key: String, fallback: Int) = if (containsKey(key)) getInt(key) else fallback

    private fun Bundle.qualityOr(fallback: LabQuality): LabQuality {
        val name = getString(EXTRA_QUALITY) ?: return fallback
        return LabQuality.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: fallback
    }

    private fun Bundle.booleanOr(key: String, fallback: Boolean) = if (containsKey(key)) getBoolean(key) else fallback

    private companion object {
        const val EXTRA_SNOW = "snow"
        const val EXTRA_MOOD = "mood"
        const val EXTRA_DIRECTION = "direction"
        const val EXTRA_OVERRIDE = "override"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_LAYERS = "layers"
        const val EXTRA_TURBULENCE = "turbulence"
        const val EXTRA_GUSTS = "gusts"
        const val EXTRA_HUD = "hud"
        const val EXTRA_BOKEH = "bokeh"
        const val EXTRA_QUALITY = "quality"
    }
}
