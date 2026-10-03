package com.hirahira.snowing.engine

/**
 * Chooses the [QualityLevel] from device signals and frame timing (ADR-0008 §2-4).
 *
 * The adaptive part steps down one level when too many frames are missed and
 * back up only after a long stable run or a screen-on, never above [startLevel].
 * Battery saver and heat set a floor on top of it; critical heat pauses the snow.
 * Nothing is persisted: every snow session starts from [startLevel].
 */
class QualityPolicy(
    private val startLevel: QualityLevel,
    /** Called after [level] or [isPaused] changed. */
    private val onChange: () -> Unit = {},
) {
    var level: QualityLevel = startLevel
        private set

    /** Too hot to draw at all; the user's choice to have snow stands (ADR-0005). */
    var isPaused: Boolean = false
        private set

    /** Share of missed frames in the last completed window, for the HUD. */
    var missedFraction: Float = 0f
        private set

    /** Debug override; null lets the policy decide. */
    var forced: QualityLevel? = null
        set(value) {
            field = value
            update()
        }

    var powerSave: Boolean = false
        set(value) {
            field = value
            update()
        }

    var thermal: ThermalState = ThermalState.NONE
        set(value) {
            field = value
            update()
        }

    private var adaptive = startLevel
    private var windowSeconds = 0f
    private var windowFrames = 0
    private var windowMissed = 0
    private var stableSeconds = 0f
    private var warmupSeconds = 0f

    /**
     * Records one drawn frame. [dtSeconds] is the time since the previous drawn
     * frame; it counts as missed when it exceeds the target interval by half.
     */
    fun onFrame(dtSeconds: Float, targetIntervalSeconds: Float) {
        if (warmupSeconds > 0f) {
            warmupSeconds -= dtSeconds
            return
        }
        windowSeconds += dtSeconds
        windowFrames++
        if (dtSeconds > targetIntervalSeconds * MISSED_FACTOR) windowMissed++
        if (windowSeconds < WINDOW_SECONDS) return

        missedFraction = windowMissed.toFloat() / windowFrames
        if (missedFraction > MAX_MISSED_FRACTION) {
            stableSeconds = 0f
            stepAdaptive(down = true)
        } else {
            stableSeconds += windowSeconds
            if (stableSeconds >= STABLE_SECONDS_TO_STEP_UP) {
                stableSeconds = 0f
                stepAdaptive(down = false)
            }
        }
        resetWindow()
    }

    /**
     * The frame loop (re)started. Its first frames pay one-off costs (window
     * setup, the home screen animation), which say nothing about the device.
     */
    fun onLoopStarted() {
        warmupSeconds = WARMUP_SECONDS
        resetWindow()
    }

    /** A fresh start after the screen was off: worth trying one level better. */
    fun onScreenOn() {
        stableSeconds = 0f
        resetWindow()
        stepAdaptive(down = false)
    }

    private fun stepAdaptive(down: Boolean) {
        val next = if (down) {
            minOf(adaptive.ordinal + 1, ADAPTIVE_FLOOR.ordinal)
        } else {
            maxOf(adaptive.ordinal - 1, startLevel.ordinal)
        }
        if (next == adaptive.ordinal) return
        adaptive = QualityLevel.entries[next]
        update()
    }

    private fun update() {
        val newLevel = forced ?: listOf(adaptive, powerSaveFloor(), thermalFloor()).maxBy { it.ordinal }
        val newPaused = thermal == ThermalState.CRITICAL
        if (newLevel == level && newPaused == isPaused) return
        // Frames drawn under the old level say nothing about the new one.
        if (newLevel != level) resetWindow()
        level = newLevel
        isPaused = newPaused
        onChange()
    }

    private fun powerSaveFloor() = if (powerSave) QualityLevel.SAVER else QualityLevel.HIGH

    private fun thermalFloor() = when (thermal) {
        ThermalState.NONE -> QualityLevel.HIGH
        ThermalState.MODERATE -> QualityLevel.LOW
        ThermalState.SEVERE, ThermalState.CRITICAL -> QualityLevel.SAVER
    }

    private fun resetWindow() {
        windowSeconds = 0f
        windowFrames = 0
        windowMissed = 0
    }

    private companion object {
        const val WINDOW_SECONDS = 5f
        const val WARMUP_SECONDS = 2f
        const val MISSED_FACTOR = 1.5f
        const val MAX_MISSED_FRACTION = 0.05f
        const val STABLE_SECONDS_TO_STEP_UP = 60f

        /** Frame drops alone never go past Low; Saver is for battery and heat. */
        val ADAPTIVE_FLOOR = QualityLevel.LOW
    }
}
