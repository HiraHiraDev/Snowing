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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val saved: StateFlow<SnowSettings?> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Settings being edited but not yet written. The UI and the snow follow them at once,
    // while the store gets at most one write per SAVE_THROTTLE_MS (B-002).
    private val draft = MutableStateFlow<SnowSettings?>(null)
    private var saveJob: Job? = null

    init {
        // A draft only bridges the gap until the store catches up; then the store wins again.
        viewModelScope.launch {
            saved.collect { if (it != null && it == draft.value) draft.value = null }
        }
    }

    val uiState: StateFlow<ControlUiState> = combine(
        saved,
        draft,
        snowSwitch.enabled,
        hasPermission,
    ) { saved, draft, enabled, permission ->
        val settings = draft ?: saved ?: SnowSettings()
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
            is ControlEvent.IntensityChanged -> edit { it.copy(intensity = event.value) }
            is ControlEvent.SpeedChanged -> edit { it.copy(speed = event.value) }
            is ControlEvent.LayersChanged -> editLab { it.copy(layers = event.value) }
            is ControlEvent.TurbulenceChanged -> editLab { it.copy(turbulence = event.value) }
            is ControlEvent.WindChanged -> editLab { it.copy(wind = event.value) }
            is ControlEvent.GustsChanged -> editLab { it.copy(gusts = event.value) }
            ControlEvent.SliderReleased -> saveNow()
            is ControlEvent.HudToggled -> editLab(now = true) { it.copy(showHud = event.enabled) }
            is ControlEvent.TracerToggled -> editLab(now = true) { it.copy(showTracer = event.enabled) }
            is ControlEvent.ForegroundToggled -> editLab(now = true) { it.copy(foreground = event.enabled) }
            is ControlEvent.InstantChangesToggled -> editLab(now = true) { it.copy(instantChanges = event.enabled) }
        }
    }

    private fun edit(now: Boolean = false, transform: (SnowSettings) -> SnowSettings) {
        val base = draft.value ?: saved.value
        if (base == null) {
            // Not loaded yet: nothing on screen to edit from, so write straight through.
            viewModelScope.launch { settingsRepository.update(transform) }
            return
        }
        draft.value = transform(base)
        when {
            now -> saveNow()
            saveJob?.isActive != true -> saveJob = viewModelScope.launch {
                delay(SAVE_THROTTLE_MS)
                saveJob = null
                persistDraft()
            }
        }
    }

    private fun editLab(now: Boolean = false, transform: (LabSettings) -> LabSettings) =
        edit(now) { it.copy(lab = transform(it.lab)) }

    private fun saveNow() {
        saveJob?.cancel()
        saveJob = null
        persistDraft()
    }

    private fun persistDraft() {
        val pending = draft.value ?: return
        viewModelScope.launch { settingsRepository.update { pending } }
    }

    private fun LabSettings.toUiState() = LabUiState(
        layers = layers,
        turbulence = turbulence,
        wind = wind,
        gusts = gusts,
        showHud = showHud,
        showTracer = showTracer,
        foreground = foreground,
        instantChanges = instantChanges,
    )

    companion object {
        /** Longest a slider value waits before it is written; the snow follows every write. */
        const val SAVE_THROTTLE_MS = 150L

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
