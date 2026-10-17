package com.hirahira.snowing.debug.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.settings.LabSettings
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.SettingsEditor
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.SnowSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The Prototype lab (debug builds only): knobs for tuning moods and checking performance. */
class LabViewModel(settingsRepository: SnowSettingsRepository) : ViewModel() {

    private val editor = SettingsEditor(settingsRepository, viewModelScope)

    val uiState: StateFlow<LabUiState> = editor.settings
        .map { it?.toUiState() ?: LabUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LabUiState())

    fun onEvent(event: LabEvent) {
        when (event) {
            is LabEvent.MoodOverrideToggled -> editor.edit(now = true) { it.withMoodOverride(event.enabled) }
            is LabEvent.SpeedChanged -> editLab { it.copy(speed = event.value.coerceIn(Mood.LEVELS)) }
            is LabEvent.LayersChanged -> editLab { it.copy(layers = event.value.coerceIn(LabSettings.LAYERS)) }
            is LabEvent.TurbulenceChanged -> editLab { it.copy(turbulence = event.value) }
            is LabEvent.GustsChanged -> editLab { it.copy(gusts = event.value) }
            is LabEvent.QualityChanged -> editLab(now = true) { it.copy(quality = event.value) }
            LabEvent.DragFinished -> editor.saveNow()
            is LabEvent.HudToggled -> editLab(now = true) { it.copy(showHud = event.enabled) }
            is LabEvent.TracerToggled -> editLab(now = true) { it.copy(showTracer = event.enabled) }
            is LabEvent.ForegroundToggled -> editLab(now = true) { it.copy(foreground = event.enabled) }
            is LabEvent.InstantChangesToggled -> editLab(now = true) { it.copy(instantChanges = event.enabled) }
        }
    }

    private fun editLab(now: Boolean = false, transform: (LabSettings) -> LabSettings) =
        editor.edit(now) { it.copy(lab = transform(it.lab)) }

    // Tuning starts from what is falling now, so turning the override on changes nothing until a slider moves.
    private fun SnowSettings.withMoodOverride(enabled: Boolean): SnowSettings {
        if (!enabled) return copy(lab = lab.copy(overrideMood = false))
        val mood = activeMood
        return copy(
            lab = lab.copy(
                overrideMood = true,
                speed = mood.speed,
                layers = mood.layers,
                turbulence = mood.turbulence,
                gusts = mood.gusts,
            ),
        )
    }

    // Without the override the disabled sliders show the active mood's values, the ones actually in use.
    private fun SnowSettings.toUiState() = LabUiState(
        loaded = true,
        overrideMood = lab.overrideMood,
        speed = if (lab.overrideMood) lab.speed else activeMood.speed,
        layers = if (lab.overrideMood) lab.layers else activeMood.layers,
        turbulence = if (lab.overrideMood) lab.turbulence else activeMood.turbulence,
        gusts = if (lab.overrideMood) lab.gusts else activeMood.gusts,
        showHud = lab.showHud,
        showTracer = lab.showTracer,
        foreground = lab.foreground,
        instantChanges = lab.instantChanges,
        quality = lab.quality,
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                LabViewModel(this[APPLICATION_KEY]!!.appContainer.settingsRepository)
            }
        }
    }
}
