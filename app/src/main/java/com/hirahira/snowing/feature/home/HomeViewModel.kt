package com.hirahira.snowing.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.clock.WallClock
import com.hirahira.snowing.clock.minutes
import com.hirahira.snowing.settings.SnowSettingsRepository
import com.hirahira.snowing.settings.TimeOfDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The shell around the three pages: today only the sky of the shared background. */
class HomeViewModel(
    settingsRepository: SnowSettingsRepository,
    private val clock: WallClock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settings.map { it.timeOfDay }.distinctUntilChanged(),
        clock.minutes(),
    ) { timeOfDay, now -> HomeUiState(skyHour = SkyHour.of(timeOfDay, now)) }
        .distinctUntilChanged()
        // Auto until the store answers: right for most users, and the store is quick.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState(SkyHour.of(TimeOfDay.AUTO, clock.now())))

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                HomeViewModel(container.settingsRepository, container.wallClock)
            }
        }
    }
}
