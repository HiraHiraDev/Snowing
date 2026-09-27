package com.hirahira.snowing.feature.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hirahira.snowing.BuildConfig
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.power.SnowSwitch
import com.hirahira.snowing.settings.LabSettings
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.SnowSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ControlViewModel(
    private val settingsRepository: SnowSettingsRepository,
    private val snowSwitch: SnowSwitch,
    private val labEnabled: Boolean,
) : ViewModel() {

    private val hasPermission = MutableStateFlow(snowSwitch.hasPermission())

    val uiState: StateFlow<ControlUiState> = combine(
        settingsRepository.settings,
        snowSwitch.enabled,
        hasPermission,
    ) { settings, enabled, permission ->
        ControlUiState(
            snowEnabled = enabled,
            hasOverlayPermission = permission,
            intensity = settings.intensity,
            speed = settings.speed,
            lab = if (labEnabled) settings.lab.toUiState() else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ControlUiState())

    fun onEvent(event: ControlEvent) {
        when (event) {
            ControlEvent.ToggleSnow -> viewModelScope.launch {
                if (!snowSwitch.toggle()) hasPermission.value = false
            }
            ControlEvent.Resumed -> {
                hasPermission.value = snowSwitch.hasPermission()
                viewModelScope.launch { snowSwitch.reconcile() }
            }
            is ControlEvent.IntensityChanged -> update { it.copy(intensity = event.value) }
            is ControlEvent.SpeedChanged -> update { it.copy(speed = event.value) }
            is ControlEvent.LayersChanged -> updateLab { it.copy(layers = event.value) }
            is ControlEvent.SwayChanged -> updateLab { it.copy(sway = event.value) }
            is ControlEvent.HudToggled -> updateLab { it.copy(showHud = event.enabled) }
            is ControlEvent.TracerToggled -> updateLab { it.copy(showTracer = event.enabled) }
        }
    }

    private fun update(transform: (SnowSettings) -> SnowSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    private fun updateLab(transform: (LabSettings) -> LabSettings) =
        update { it.copy(lab = transform(it.lab)) }

    private fun LabSettings.toUiState() = LabUiState(
        layers = layers,
        sway = sway,
        showHud = showHud,
        showTracer = showTracer,
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                ControlViewModel(
                    settingsRepository = container.settingsRepository,
                    snowSwitch = container.snowSwitch,
                    labEnabled = BuildConfig.DEBUG,
                )
            }
        }
    }
}
