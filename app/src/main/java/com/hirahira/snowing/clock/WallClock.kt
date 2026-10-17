package com.hirahira.snowing.clock

import java.time.LocalDate
import java.time.LocalTime

/** The device's local date and time of day; an interface so tests can drive it. */
interface WallClock {
    fun now(): LocalTime

    fun today(): LocalDate
}

class SystemWallClock : WallClock {
    override fun now(): LocalTime = LocalTime.now()

    override fun today(): LocalDate = LocalDate.now()
}
