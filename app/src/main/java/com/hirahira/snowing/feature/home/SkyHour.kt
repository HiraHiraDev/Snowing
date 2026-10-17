package com.hirahira.snowing.feature.home

import com.hirahira.snowing.settings.TimeOfDay
import com.hirahira.snowing.ui.theme.Sky
import java.time.LocalTime

/** The hour the background sky shows (ADR-0010): the clock on Auto, else the chosen time's pinned hour. */
object SkyHour {
    fun of(timeOfDay: TimeOfDay, now: LocalTime): Float = when (timeOfDay) {
        TimeOfDay.AUTO -> now.hour + now.minute / MINUTES_PER_HOUR
        TimeOfDay.DAWN -> Sky.HOUR_DAWN
        TimeOfDay.DAY -> Sky.HOUR_DAY
        TimeOfDay.DUSK -> Sky.HOUR_DUSK
        TimeOfDay.NIGHT -> Sky.HOUR_NIGHT
    }

    /** Time left until the next full minute, when the Auto sky moves on. */
    fun millisToNextMinute(now: LocalTime): Long =
        MILLIS_PER_MINUTE - now.second * MILLIS_PER_SECOND - now.nano / NANOS_PER_MILLI

    private const val MINUTES_PER_HOUR = 60f
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MILLIS_PER_SECOND = 1_000L
    private const val NANOS_PER_MILLI = 1_000_000L
}
