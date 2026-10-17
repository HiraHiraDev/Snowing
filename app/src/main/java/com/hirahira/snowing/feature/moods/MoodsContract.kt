package com.hirahira.snowing.feature.moods

import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.TimeOfDay
import com.hirahira.snowing.settings.WindStrength

data class MoodsUiState(
    /** False until the stored settings arrive; until then nothing below is real (B-008). */
    val loaded: Boolean = false,
    /** Built-in moods, then the user's own once it exists. */
    val moods: List<MoodItem> = emptyList(),
    val ownMood: OwnMoodUiState = OwnMoodUiState.Hidden,
    val timeOfDay: TimeOfDay = TimeOfDay.AUTO,
)

data class MoodItem(
    val choice: MoodChoice,
    val selected: Boolean,
)

/** The "Your own mood" section (Pro, ADR-0010). Built-in moods are only picked, never changed. */
sealed interface OwnMoodUiState {
    /** Without Pro: creating one is shown, but locked. */
    data object Locked : OwnMoodUiState

    /** Pro, and no own mood yet. */
    data object CanCreate : OwnMoodUiState

    /** The own mood is the active one: its controls, changing the snow live. */
    data class Editing(
        val intensity: Int,
        val flakeSize: FlakeSize,
        val windAngle: Int,
        /** [windAngle] to the nearest eighth of a turn, for the arrow beside the dial (design 6a). */
        val windArrow: Int,
        val windStrength: WindStrength,
    ) : OwnMoodUiState

    /** The own mood exists but another is active: it is picked from the list like any other. */
    data object Hidden : OwnMoodUiState
}

sealed interface MoodsEvent {
    data class MoodSelected(val choice: MoodChoice) : MoodsEvent
    /** Copies the active mood into the user's own and makes it active. */
    data object CreateOwnMood : MoodsEvent
    data class IntensityChanged(val value: Int) : MoodsEvent
    data class FlakeSizeChanged(val value: FlakeSize) : MoodsEvent
    /** Degrees clockwise from up, as the wheel's needle points. */
    data class WindAngleChanged(val value: Int) : MoodsEvent
    data class WindStrengthChanged(val value: WindStrength) : MoodsEvent
    data class TimeOfDayChanged(val value: TimeOfDay) : MoodsEvent
    /** The finger left the ruler or the dial: write the final value now. */
    data object DragFinished : MoodsEvent
}
