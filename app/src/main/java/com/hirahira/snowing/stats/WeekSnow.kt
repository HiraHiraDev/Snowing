package com.hirahira.snowing.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Snow time over the current week, day by day. */
data class WeekSnow(
    val days: List<DaySnow>,
    val todayMillis: Long,
    val weekMillis: Long,
) {
    companion object {
        /** The week holding [today], starting on [firstDay] as the user's locale counts weeks. */
        fun of(dailyMillis: Map<LocalDate, Long>, today: LocalDate, firstDay: DayOfWeek): WeekSnow {
            val start = today.with(TemporalAdjusters.previousOrSame(firstDay))
            val days = (0L until DAYS_IN_WEEK).map { offset ->
                val date = start.plusDays(offset)
                DaySnow(
                    date = date,
                    millis = if (date > today) 0L else dailyMillis[date] ?: 0L,
                    isToday = date == today,
                )
            }
            return WeekSnow(days = days, todayMillis = dailyMillis[today] ?: 0L, weekMillis = days.sumOf { it.millis })
        }

        private const val DAYS_IN_WEEK = 7L
    }
}

data class DaySnow(
    val date: LocalDate,
    val millis: Long,
    val isToday: Boolean,
)
