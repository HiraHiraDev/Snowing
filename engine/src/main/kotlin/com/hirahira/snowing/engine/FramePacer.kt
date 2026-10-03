package com.hirahira.snowing.engine

import kotlin.math.ceil

/**
 * Picks which vsyncs to draw on (ADR-0008 §1): the highest frame rate that
 * divides the display refresh rate and stays within [fpsCap], so frames stay
 * evenly spaced. 60 fps on a 90 Hz screen would alternate 11 and 22 ms, which
 * reads as stutter on slow snow; 45 fps does not.
 */
class FramePacer(refreshRate: Float = 60f, fpsCap: Int = 60) {

    var refreshRate: Float = refreshRate
        set(value) {
            require(value > 0f) { "refreshRate must be > 0, was $value" }
            field = value
            recompute()
        }

    var fpsCap: Int = fpsCap
        set(value) {
            require(value > 0) { "fpsCap must be > 0, was $value" }
            field = value
            recompute()
        }

    /** Draw every n-th vsync. */
    var divisor: Int = 1
        private set

    val targetFps: Float get() = refreshRate / divisor

    val targetIntervalSeconds: Float get() = divisor / refreshRate

    private var vsyncNanos = 0L
    private var targetNanos = 0L
    private var lastDrawNanos = NOT_DRAWN

    init {
        recompute()
    }

    /** True when the frame at [frameTimeNanos] should be drawn; remembers it if so. */
    fun shouldDraw(frameTimeNanos: Long): Boolean {
        // Half a vsync of slack absorbs jitter in the callback timestamps.
        if (lastDrawNanos != NOT_DRAWN && frameTimeNanos - lastDrawNanos < targetNanos - vsyncNanos / 2) return false
        lastDrawNanos = frameTimeNanos
        return true
    }

    /** Forget the last drawn frame, e.g. after a pause. */
    fun reset() {
        lastDrawNanos = NOT_DRAWN
    }

    private fun recompute() {
        divisor = ceil(refreshRate / (fpsCap * CAP_TOLERANCE)).toInt().coerceAtLeast(1)
        vsyncNanos = (NANOS_PER_SECOND / refreshRate).toLong()
        targetNanos = vsyncNanos * divisor
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000.0
        const val NOT_DRAWN = Long.MIN_VALUE

        // Displays report e.g. 60.000004 or 59.94 Hz; neither should halve the frame rate.
        const val CAP_TOLERANCE = 1.01f
    }
}
