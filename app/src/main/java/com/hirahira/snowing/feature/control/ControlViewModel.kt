package com.hirahira.snowing.feature.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hirahira.snowing.BuildConfig
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.pro.Entitlements
import com.hirahira.snowing.pro.ProGate
import com.hirahira.snowing.settings.BuiltInMoods
import com.hirahira.snowing.settings.LabSettings
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.MoodAdjustment
import com.hirahira.snowing.settings.MoodChoice
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

/** The prototype panel: moods, "Adjust", time of day and the lab, until the 6a Moods screen. */
class ControlViewModel(
    private val settingsRepository: SnowSettingsRepository,
    private val entitlements: Entitlements,
    private val labEnabled: Boolean,
) : ViewModel() {

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
        entitlements.hasPro,
    ) { saved, draft, hasPro ->
        val settings = draft ?: saved ?: return@combine ControlUiState()
        ControlUiState(
            loaded = true,
            moods = settings.moodChoices.map { choice ->
                val mood = BuiltInMoods.of(choice) ?: settings.customMood!!
                MoodItem(choice = choice, intensity = mood.intensity, selected = choice == settings.mood)
            },
            adjust = settings.toAdjustUiState(locked = !ProGate.canAdjustMoods(hasPro)),
            timeOfDay = settings.timeOfDay,
            lab = if (labEnabled) settings.toLabUiState() else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ControlUiState())

    fun onEvent(event: ControlEvent) {
        when (event) {
            is ControlEvent.MoodSelected -> edit(now = true) { it.choosing(event.choice) }
            is ControlEvent.IntensityChanged -> adjust { it.copy(intensity = event.value) }
            is ControlEvent.FlakeSizeChanged -> adjust(now = true) { it.copy(flakeSize = event.value) }
            is ControlEvent.WindAngleChanged -> adjust { it.copy(windAngle = event.value) }
            is ControlEvent.WindStrengthChanged -> adjust(now = true) { it.copy(windStrength = event.value) }
            ControlEvent.RestoreMood -> edit(now = true) { it.copy(moodAdjustment = null) }
            ControlEvent.SaveMoodAsNew -> if (canAdjust()) edit(now = true) { it.savedAsNewMood() }
            is ControlEvent.TimeOfDayChanged -> edit(now = true) { it.copy(timeOfDay = event.value) }
            is ControlEvent.MoodOverrideToggled -> edit(now = true) { it.withMoodOverride(event.enabled) }
            is ControlEvent.SpeedChanged -> editLab { it.copy(speed = event.value.coerceIn(Mood.LEVELS)) }
            is ControlEvent.LayersChanged -> editLab { it.copy(layers = event.value) }
            is ControlEvent.TurbulenceChanged -> editLab { it.copy(turbulence = event.value) }
            is ControlEvent.GustsChanged -> editLab { it.copy(gusts = event.value) }
            is ControlEvent.QualityChanged -> editLab { it.copy(quality = event.value) }
            ControlEvent.SliderReleased -> saveNow()
            is ControlEvent.HudToggled -> editLab(now = true) { it.copy(showHud = event.enabled) }
            is ControlEvent.TracerToggled -> editLab(now = true) { it.copy(showTracer = event.enabled) }
            is ControlEvent.ForegroundToggled -> editLab(now = true) { it.copy(foreground = event.enabled) }
            is ControlEvent.InstantChangesToggled -> editLab(now = true) { it.copy(instantChanges = event.enabled) }
        }
    }

    private fun canAdjust(): Boolean = ProGate.canAdjustMoods(entitlements.hasPro.value)

    private fun edit(now: Boolean = false, transform: (SnowSettings) -> SnowSettings) {
        val base = draft.value ?: saved.value
        if (base == null) {
            // Not loaded yet: nothing on screen to edit from, so write straight through.
            viewModelScope.launch {
                settingsRepository.update { ProGate.persistable(transform(it), it, entitlements.hasPro.value) }
            }
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

    // Locked without Pro, so the controls stay put instead of showing a change that is never written.
    private fun adjust(now: Boolean = false, transform: (MoodAdjustment) -> MoodAdjustment) {
        if (!canAdjust()) return
        edit(now) { it.withAdjustment(transform(it.activeMood.adjustment)) }
    }

    private fun saveNow() {
        saveJob?.cancel()
        saveJob = null
        persistDraft()
    }

    private fun persistDraft() {
        val pending = draft.value ?: return
        viewModelScope.launch {
            settingsRepository.update { saved -> ProGate.persistable(pending, saved, entitlements.hasPro.value) }
        }
    }

    // An adjustment that lands back on the mood as designed is no change at all.
    private fun SnowSettings.withAdjustment(adjustment: MoodAdjustment): SnowSettings {
        val normalized = adjustment.normalized()
        return copy(moodAdjustment = normalized.takeIf { it != baseMood.adjustment })
    }

    // The custom mood keeps the hidden parameters of the mood it grew from (ADR-0010).
    private fun SnowSettings.savedAsNewMood(): SnowSettings =
        copy(mood = MoodChoice.CUSTOM, customMood = activeMood, moodAdjustment = null)

    private fun SnowSettings.toAdjustUiState(locked: Boolean): AdjustUiState {
        val mood = activeMood
        return AdjustUiState(
            mood = this.mood,
            edited = isMoodAdjusted,
            locked = locked,
            intensity = mood.intensity,
            flakeSize = mood.flakeSize,
            windAngle = mood.windAngle,
            windStrength = mood.windStrength,
        )
    }

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
    private fun SnowSettings.toLabUiState() = LabUiState(
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
        /** Longest a slider value waits before it is written; the snow follows every write. */
        const val SAVE_THROTTLE_MS = 150L

        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                ControlViewModel(
                    settingsRepository = container.settingsRepository,
                    entitlements = container.entitlements,
                    labEnabled = BuildConfig.DEBUG,
                )
            }
        }
    }
}
