package com.hirahira.snowing.benchmark

import androidx.test.uiautomator.UiDevice

/** The snow presets of docs/PERFORMANCE.md: V1 is First Snow, V2 Blizzard, the heaviest built-in mood. */
enum class SnowVariant(internal val extras: String) {
    V0("--ez snow false"),
    V1("--ez snow true --es mood first_snow --ez override false"),
    V2("--ez snow true --es mood blizzard --ez override false"),
}

/**
 * Drives Snowing over adb through PerfControlActivity, which only the shell may
 * start (DUMP permission). Needs the benchmark or debug build of :app installed.
 */
internal object SnowControl {

    const val PACKAGE = "com.hirahira.snowing"

    fun set(device: UiDevice, variant: SnowVariant) {
        check(device.executeShellCommand("pm path $PACKAGE").contains("package:")) {
            "$PACKAGE is not installed: run ./gradlew :app:installBenchmark first"
        }
        device.executeShellCommand("appops set $PACKAGE SYSTEM_ALERT_WINDOW allow")
        // quality=high pins the ADR-0008 level, so frame drops cannot change the variant mid-run.
        control(device, "${variant.extras} --ez hud false --es quality high")
        // Snow enters from the top edge (ADR-0006); the slowest layers need this long to fill the screen.
        if (variant != SnowVariant.V0) Thread.sleep(FILL_SCREEN_MS)
    }

    /** Starts [variant] unless the snow process already runs, e.g. after Macrobenchmark force-stopped it. */
    fun ensure(device: UiDevice, variant: SnowVariant) {
        if (device.executeShellCommand("pidof $PACKAGE").isBlank()) set(device, variant)
    }

    fun reset(device: UiDevice) {
        control(device, "--ez snow false --es quality auto")
    }

    private fun control(device: UiDevice, extras: String) {
        device.executeShellCommand("am start -W -n $PACKAGE/.debug.PerfControlActivity $extras")
    }

    private const val FILL_SCREEN_MS = 45_000L
}
