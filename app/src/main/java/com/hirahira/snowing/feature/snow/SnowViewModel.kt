package com.hirahira.snowing.feature.snow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.power.SnowSwitch
import com.hirahira.snowing.settings.SnowSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The Snow page: shows the active mood and turns the snow on and off. */
class SnowViewModel(
    settingsRepository: SnowSettingsRepository,
    private val snowSwitch: SnowSwitch,
) : ViewModel() {

    private val hasPermission = MutableStateFlow(snowSwitch.hasPermission())

    val uiState: StateFlow<SnowUiState> = combine(
        settingsRepository.settings,
        snowSwitch.enabled,
        hasPermission,
    ) { settings, enabled, permission ->
        SnowUiState(
            loaded = true,
            snowEnabled = enabled,
            hasOverlayPermission = permission,
            mood = settings.mood,
            intensity = settings.activeMood.intensity,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SnowUiState())

    fun onEvent(event: SnowEvent) {
        when (event) {
            SnowEvent.ToggleSnow -> viewModelScope.launch {
                if (!snowSwitch.toggle()) hasPermission.value = false
            }
            SnowEvent.Resumed -> {
                hasPermission.value = snowSwitch.hasPermission()
                viewModelScope.launch { snowSwitch.reconcile() }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                SnowViewModel(container.settingsRepository, container.snowSwitch)
            }
        }
    }
}
