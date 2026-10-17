package com.hirahira.snowing.clock

import java.time.LocalTime

/** The device's local time of day; an interface so tests can drive it. */
interface WallClock {
    fun now(): LocalTime
}

class SystemWallClock : WallClock {
    override fun now(): LocalTime = LocalTime.now()
}
