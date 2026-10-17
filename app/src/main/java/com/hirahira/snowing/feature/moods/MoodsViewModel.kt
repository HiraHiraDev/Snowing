package com.hirahira.snowing.feature.moods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.pro.Entitlements
import com.hirahira.snowing.pro.ProGate
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.MoodLook
import com.hirahira.snowing.settings.SettingsEditor
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.SnowSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

/** The Moods page: pick a mood, shape the user's own (Pro), choose the time of day. */
class MoodsViewModel(
    settingsRepository: SnowSettingsRepository,
    private val entitlements: Entitlements,
) : ViewModel() {

    private val editor = SettingsEditor(settingsRepository, viewModelScope) { edited, saved ->
        ProGate.persistable(edited, saved, entitlements.hasPro.value)
    }

    val uiState: StateFlow<MoodsUiState> = combine(editor.settings, entitlements.hasPro) { settings, hasPro ->
        if (settings == null) return@combine MoodsUiState()
        MoodsUiState(
            loaded = true,
            moods = settings.moodChoices.map { MoodItem(choice = it, selected = it == settings.mood) },
            ownMood = settings.toOwnMoodUiState(ProGate.canOwnMood(hasPro)),
            timeOfDay = settings.timeOfDay,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoodsUiState())

    fun onEvent(event: MoodsEvent) {
        when (event) {
            is MoodsEvent.MoodSelected -> editor.edit(now = true) { it.choosing(event.choice) }
            MoodsEvent.CreateOwnMood -> if (canOwnMood()) editor.edit(now = true) { it.creatingOwnMood() }
            is MoodsEvent.IntensityChanged -> adjust { it.copy(intensity = event.value) }
            is MoodsEvent.FlakeSizeChanged -> adjust(now = true) { it.copy(flakeSize = event.value) }
            is MoodsEvent.WindAngleChanged -> adjust { it.copy(windAngle = event.value) }
            is MoodsEvent.WindStrengthChanged -> adjust(now = true) { it.copy(windStrength = event.value) }
            is MoodsEvent.TimeOfDayChanged -> editor.edit(now = true) { it.copy(timeOfDay = event.value) }
            MoodsEvent.DragFinished -> editor.saveNow()
        }
    }

    private fun canOwnMood(): Boolean = ProGate.canOwnMood(entitlements.hasPro.value)

    // Locked without Pro, so the controls stay put instead of showing a change that is never written.
    private fun adjust(now: Boolean = false, transform: (MoodLook) -> MoodLook) {
        if (!canOwnMood()) return
        editor.edit(now) { it.editingOwnMood(transform) }
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

    companion object {
        private const val ARROW_STEP = 45

        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                MoodsViewModel(settingsRepository = container.settingsRepository, entitlements = container.entitlements)
            }
        }
    }
}
