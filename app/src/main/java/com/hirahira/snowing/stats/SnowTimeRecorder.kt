package com.hirahira.snowing.stats

import com.hirahira.snowing.clock.WallClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Counts the time snow is actually drawn on screen and adds it to the day's total.
 *
 * Durations come from a monotonic clock, so changing the phone's time cannot add or take
 * away snow; the wall clock only names the day. While snow shows, the count is written once
 * a minute, so a killed process loses at most a minute, and a minute crossing midnight goes
 * to the new day. Lives in [scope], beyond the service, so the last write survives its end.
 */
class SnowTimeRecorder(
    private val stats: SnowStatsRepository,
    private val clock: WallClock,
    private val elapsedMillis: () -> Long,
    private val scope: CoroutineScope,
) {
    private var since: Long? = null
    private var ticker: Job? = null

    /** Call on the main thread whenever the snow starts or stops being drawn. */
    fun setVisible(visible: Boolean) {
        if (visible == (since != null)) return
        if (visible) {
            since = elapsedMillis()
            ticker = scope.launch {
                while (isActive) {
                    delay(FLUSH_EVERY_MS)
                    flush()
                }
            }
        } else {
            ticker?.cancel()
            ticker = null
            flush()
            since = null
        }
    }

    private fun flush() {
        val start = since ?: return
        val now = elapsedMillis()
        since = now
        val millis = now - start
        if (millis > 0) {
            val day = clock.today()
            scope.launch { stats.add(day, millis) }
        }
    }

    companion object {
        const val FLUSH_EVERY_MS = 60_000L
    }
}
