package com.hirahira.snowing.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeekSnowTest {

    // A Saturday.
    private val today = LocalDate.of(2026, 10, 17)

    @Test
    fun `the week starts on the locale's first day and holds today`() {
        val monday = WeekSnow.of(emptyMap(), today, DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 10, 12), monday.days.first().date)
        assertEquals(7, monday.days.size)
        assertTrue(monday.days[5].isToday)

        val sunday = WeekSnow.of(emptyMap(), today, DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 10, 11), sunday.days.first().date)
        assertTrue(sunday.days.last().isToday)
    }

    @Test
    fun `days outside the week do not count`() {
        val daily = mapOf(
            LocalDate.of(2026, 10, 11) to 99_000L, // Sunday before a Monday week
            LocalDate.of(2026, 10, 12) to 60_000L,
            today to 120_000L,
        )
        val week = WeekSnow.of(daily, today, DayOfWeek.MONDAY)
        assertEquals(180_000L, week.weekMillis)
        assertEquals(120_000L, week.todayMillis)
    }

    @Test
    fun `days after today stay empty`() {
        val week = WeekSnow.of(mapOf(today.plusDays(1) to 60_000L), today, DayOfWeek.MONDAY)
        assertEquals(0L, week.days.last().millis)
        assertEquals(0L, week.weekMillis)
    }
}
