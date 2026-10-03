package com.hirahira.snowing.benchmark

import androidx.test.uiautomator.UiDevice

/** The snow presets of docs/PERFORMANCE.md, with the same extras as docs/perf/measure.sh. */
enum class SnowVariant(internal val extras: String) {
    V0("--ez snow false"),
    V1("--ez snow true --ef intensity 0.45 --ef speed 0.4 --ei layers 3 --ef turbulence 0.5 --ef gusts 0.4 --ef wind 0"),
    V2("--ez snow true --ef intensity 1.0 --ef speed 1.0 --ei layers 5 --ef turbulence 0.5 --ef gusts 0.4 --ef wind 0"),
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

    private const val FILL_SCREEN_MS = 30_000L
}
