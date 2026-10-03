package com.hirahira.snowing.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMacrobenchmarkApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Frame timing of the snow window itself over an idle home screen (a short S1).
 * V0 is left out: with the snow off there are no frames to time.
 *
 * Macrobenchmark force-stops the measured package before the first iteration,
 * whatever the compilation mode, so the snow starts inside the setup block.
 */
@RunWith(Parameterized::class)
class SnowFramesBenchmark(private val variant: SnowVariant) {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @After
    fun stopSnow() {
        SnowControl.reset(device)
    }

    @OptIn(ExperimentalMacrobenchmarkApi::class)
    @Test
    fun idleHome() = rule.measureRepeated(
        packageName = SnowControl.PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        // Measures the snow as installed; a compile step would only restart the process again.
        compilationMode = CompilationMode.Ignore(),
        iterations = ITERATIONS,
        setupBlock = {
            device.pressHome()
            SnowControl.ensure(device, variant)
        },
    ) {
        Thread.sleep(MEASURE_MS)
    }

    companion object {
        private const val ITERATIONS = 3
        private const val MEASURE_MS = 10_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants() = listOf(SnowVariant.V1, SnowVariant.V2)
    }
}
