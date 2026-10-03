package com.hirahira.snowing.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * S2: jank of another app scrolled under the snow. The budget is at most +1 p.p.
 * of janky frames against V0 (docs/PERFORMANCE.md).
 */
@RunWith(Parameterized::class)
class ScrollUnderSnowBenchmark(private val variant: SnowVariant) {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun startSnow() {
        SnowControl.set(device, variant)
    }

    @After
    fun stopSnow() {
        SnowControl.reset(device)
    }

    @Test
    fun scroll() = rule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.DEFAULT,
        iterations = ITERATIONS,
        setupBlock = {
            startActivityAndWait()
        },
    ) {
        val list = device.wait(Until.findObject(By.res(LIST_ID)), FIND_TIMEOUT_MS)
            ?: error("List not found in $TARGET_PACKAGE")
        // Keeps the gesture away from the system gesture areas at the edges.
        list.setGestureMargin(device.displayWidth / 5)
        repeat(FLINGS) {
            list.fling(Direction.DOWN)
            list.fling(Direction.UP)
        }
    }

    companion object {
        private const val TARGET_PACKAGE = "com.hirahira.snowing.scrollapp"
        private const val LIST_ID = "list"
        private const val ITERATIONS = 3
        private const val FLINGS = 5
        private const val FIND_TIMEOUT_MS = 5_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun variants() = SnowVariant.entries.toList()
    }
}
