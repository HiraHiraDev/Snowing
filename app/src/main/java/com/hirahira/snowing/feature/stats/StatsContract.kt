package com.hirahira.snowing.feature.stats

import java.time.DayOfWeek

data class StatsUiState(
    /** False until the stored stats arrive; until then the screen shows nothing. */
    val loaded: Boolean = false,
    val weekMinutes: Int = 0,
    val todayMinutes: Int = 0,
    /** The current week from its first day, as the user's locale counts weeks. */
    val days: List<DayBar> = emptyList(),
)

data class DayBar(
    val day: DayOfWeek,
    val minutes: Int,
    /** Height in the chart, 0..1. */
    val fraction: Float,
    val isToday: Boolean,
)
