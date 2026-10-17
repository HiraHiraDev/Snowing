package com.hirahira.snowing.feature.moods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hirahira.snowing.BuildConfig
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.pro.Entitlements
import com.hirahira.snowing.pro.ProGate
import com.hirahira.snowing.settings.LabSettings
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.MoodLook
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
import kotlin.math.roundToInt

/** The prototype panel: moods, "Adjust", time of day and the lab, until the 6a Moods screen. */
class MoodsViewModel(
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

    val uiState: StateFlow<MoodsUiState> = combine(
        saved,
        draft,
        entitlements.hasPro,
    ) { saved, draft, hasPro ->
        val settings = draft ?: saved ?: return@combine MoodsUiState()
        MoodsUiState(
            loaded = true,
            moods = settings.moodChoices.map { MoodItem(choice = it, selected = it == settings.mood) },
            ownMood = settings.toOwnMoodUiState(ProGate.canOwnMood(hasPro)),
            timeOfDay = settings.timeOfDay,
            lab = if (labEnabled) settings.toLabUiState() else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoodsUiState())

    fun onEvent(event: MoodsEvent) {
        when (event) {
            is MoodsEvent.MoodSelected -> edit(now = true) { it.choosing(event.choice) }
            is MoodsEvent.IntensityChanged -> adjust { it.copy(intensity = event.value) }
            is MoodsEvent.FlakeSizeChanged -> adjust(now = true) { it.copy(flakeSize = event.value) }
            is MoodsEvent.WindAngleChanged -> adjust { it.copy(windAngle = event.value) }
            is MoodsEvent.WindStrengthChanged -> adjust(now = true) { it.copy(windStrength = event.value) }
            MoodsEvent.CreateOwnMood -> if (canOwnMood()) edit(now = true) { it.creatingOwnMood() }
            is MoodsEvent.TimeOfDayChanged -> edit(now = true) { it.copy(timeOfDay = event.value) }
            is MoodsEvent.MoodOverrideToggled -> edit(now = true) { it.withMoodOverride(event.enabled) }
            is MoodsEvent.SpeedChanged -> editLab { it.copy(speed = event.value.coerceIn(Mood.LEVELS)) }
            is MoodsEvent.LayersChanged -> editLab { it.copy(layers = event.value) }
            is MoodsEvent.TurbulenceChanged -> editLab { it.copy(turbulence = event.value) }
            is MoodsEvent.GustsChanged -> editLab { it.copy(gusts = event.value) }
            is MoodsEvent.QualityChanged -> editLab { it.copy(quality = event.value) }
            MoodsEvent.DragFinished -> saveNow()
            is MoodsEvent.HudToggled -> editLab(now = true) { it.copy(showHud = event.enabled) }
            is MoodsEvent.TracerToggled -> editLab(now = true) { it.copy(showTracer = event.enabled) }
            is MoodsEvent.ForegroundToggled -> editLab(now = true) { it.copy(foreground = event.enabled) }
            is MoodsEvent.InstantChangesToggled -> editLab(now = true) { it.copy(instantChanges = event.enabled) }
        }
    }

    private fun canOwnMood(): Boolean = ProGate.canOwnMood(entitlements.hasPro.value)

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
    private fun adjust(now: Boolean = false, transform: (MoodLook) -> MoodLook) {
        if (!canOwnMood()) return
        edit(now) { it.editingOwnMood(transform) }
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

    private fun SnowSettings.toOwnMoodUiState(canOwnMood: Boolean): OwnMoodUiState {
        val own = customMood
        return when {
            own == null -> if (canOwnMood) OwnMoodUiState.CanCreate else OwnMoodUiState.Locked
            isOwnMoodActive && canOwnMood -> OwnMoodUiState.Editing(
                intensity = own.intensity,
                flakeSize = own.flakeSize,
                windAngle = own.windAngle,
                windArrow = (own.windAngle / ARROW_STEP.toFloat()).roundToInt() * ARROW_STEP % Mood.WIND_ANGLES.count(),
                windStrength = own.windStrength,
            )
            else -> OwnMoodUiState.Hidden
        }
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

        private const val ARROW_STEP = 45

        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                MoodsViewModel(
                    settingsRepository = container.settingsRepository,
                    entitlements = container.entitlements,
                    labEnabled = BuildConfig.DEBUG,
                )
            }
        }
    }
}
