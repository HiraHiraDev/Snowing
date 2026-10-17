package com.hirahira.snowing.clock

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalTime

/**
 * The time now, then again at every full minute, for screens that follow the clock (the
 * sky, today's date). Ticks only while collected, so a backgrounded app does not wake up.
 */
fun WallClock.minutes(): Flow<LocalTime> = flow {
    while (true) {
        val now = now()
        emit(now)
        delay(millisToNextMinute(now))
    }
}

internal fun millisToNextMinute(now: LocalTime): Long =
    MILLIS_PER_MINUTE - now.second * MILLIS_PER_SECOND - now.nano / NANOS_PER_MILLI

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_SECOND = 1_000L
private const val NANOS_PER_MILLI = 1_000_000L
