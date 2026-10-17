package com.hirahira.snowing.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.clock.WallClock
import com.hirahira.snowing.clock.minutes
import com.hirahira.snowing.stats.SnowStatsRepository
import com.hirahira.snowing.stats.WeekSnow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

/** The Stats page: how much it snowed on screen this week (ADR-0010, simple stats in v1). */
class StatsViewModel(
    statsRepository: SnowStatsRepository,
    private val clock: WallClock,
    private val firstDayOfWeek: () -> DayOfWeek,
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        statsRepository.dailyMillis,
        // The week moves on at midnight even while the screen stays open.
        clock.minutes().map { clock.today() }.distinctUntilChanged(),
    ) { daily, today ->
        val week = WeekSnow.of(daily, today, firstDayOfWeek())
        val minutes = week.days.map { it.millis.toMinutes() }
        // A short day must not fill the chart: the scale is at least an hour.
        val scale = maxOf(minutes.max(), MIN_SCALE_MINUTES).toFloat()
        StatsUiState(
            loaded = true,
            weekMinutes = week.weekMillis.toMinutes(),
            todayMinutes = week.todayMillis.toMinutes(),
            days = week.days.mapIndexed { i, day ->
                DayBar(day.date.dayOfWeek, minutes[i], minutes[i] / scale, day.isToday)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    private fun Long.toMinutes(): Int = (this / MILLIS_PER_MINUTE).toInt()

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val MIN_SCALE_MINUTES = 60

        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                StatsViewModel(
                    statsRepository = container.statsRepository,
                    clock = container.wallClock,
                    firstDayOfWeek = { WeekFields.of(Locale.getDefault()).firstDayOfWeek },
                )
            }
        }
    }
}
