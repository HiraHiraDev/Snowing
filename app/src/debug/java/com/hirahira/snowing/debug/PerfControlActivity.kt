package com.hirahira.snowing.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.hirahira.snowing.appContainer
import kotlinx.coroutines.launch

/**
 * Debug-only remote control for repeatable performance runs: sets the snow
 * variant and switches it on or off without touching the UI.
 *
 *     adb shell am start -n com.hirahira.snowing/.debug.PerfControlActivity \
 *         --ez snow true --ef intensity 1.0 --ef speed 1.0 --ei layers 5
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
            container.settingsRepository.update { current ->
                current.copy(
                    intensity = extras.floatOr(EXTRA_INTENSITY, current.intensity),
                    speed = extras.floatOr(EXTRA_SPEED, current.speed),
                    lab = current.lab.copy(
                        layers = extras.intOr(EXTRA_LAYERS, current.lab.layers),
                        sway = extras.floatOr(EXTRA_SWAY, current.lab.sway),
                        showHud = extras.booleanOr(EXTRA_HUD, current.lab.showHud),
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

    private fun Bundle.booleanOr(key: String, fallback: Boolean) = if (containsKey(key)) getBoolean(key) else fallback

    private companion object {
        const val EXTRA_SNOW = "snow"
        const val EXTRA_INTENSITY = "intensity"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_LAYERS = "layers"
        const val EXTRA_SWAY = "sway"
        const val EXTRA_HUD = "hud"
    }
}
