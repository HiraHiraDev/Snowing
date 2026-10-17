package com.hirahira.snowing.feature.moods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hirahira.snowing.R
import com.hirahira.snowing.feature.common.displayName
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.LabQuality
import com.hirahira.snowing.settings.LabSettings
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.TimeOfDay
import com.hirahira.snowing.settings.WindStrength
import com.hirahira.snowing.ui.components.Caption
import com.hirahira.snowing.ui.components.ChoiceList
import com.hirahira.snowing.ui.components.ControlLabel
import com.hirahira.snowing.ui.components.DirectionArrow
import com.hirahira.snowing.ui.components.DotPicker
import com.hirahira.snowing.ui.components.Group
import com.hirahira.snowing.ui.components.LargeValue
import com.hirahira.snowing.ui.components.LevelRuler
import com.hirahira.snowing.ui.components.enabledLook
import com.hirahira.snowing.ui.components.Note
import com.hirahira.snowing.ui.components.OptionPicker
import com.hirahira.snowing.ui.components.ScrollingPage
import com.hirahira.snowing.ui.components.SectionTitle
import com.hirahira.snowing.ui.components.SettingSlider
import com.hirahira.snowing.ui.components.SettingSwitch
import com.hirahira.snowing.ui.components.StepSlider
import com.hirahira.snowing.ui.components.TextAction
import com.hirahira.snowing.ui.components.WindDial
import com.hirahira.snowing.ui.theme.Sky
import com.hirahira.snowing.ui.theme.SnowingColors
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlin.math.roundToInt

/**
 * The Moods page (design 6a): pick a mood, adjust it (Pro), choose the time of day.
 * The prototype lab follows in debug builds until it gets its own screen.
 */
@Composable
fun MoodsScreen(
    state: MoodsUiState,
    onEvent: (MoodsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        // Nothing that depends on stored state shows before it loads (B-008).
        if (!state.loaded) return@Box
        ScrollingPage(top = SnowingTheme.spacing.pageTopMoods) {
            ChoiceList(
                items = state.moods,
                isSelected = { it.selected },
                title = { it.choice.displayName() },
                onSelect = { onEvent(MoodsEvent.MoodSelected(it.choice)) },
            )
            OwnMoodGroup(state.ownMood, onEvent)
            OptionPicker(
                label = stringResource(R.string.time_of_day),
                options = TimeOfDay.entries,
                selected = state.timeOfDay,
                optionLabel = { it.displayName() },
                onSelect = { onEvent(MoodsEvent.TimeOfDayChanged(it)) },
            )
            state.lab?.let { LabGroup(it, onEvent) }
        }
    }
}

/** Built-in moods are only picked; the user's own is created and changed here (Pro). */
@Composable
private fun OwnMoodGroup(own: OwnMoodUiState, onEvent: (MoodsEvent) -> Unit) {
    if (own == OwnMoodUiState.Hidden) return
    Column {
        SectionTitle(
            stringResource(R.string.own_mood),
            Modifier.heightIn(min = SnowingTheme.sizes.touchTarget).wrapContentHeight(),
        )
        when (own) {
            OwnMoodUiState.Locked -> Column(Modifier.enabledLook(false)) {
                TextAction(stringResource(R.string.own_mood_create), onClick = {}, enabled = false)
                Caption(stringResource(R.string.own_mood_pro))
            }
            OwnMoodUiState.CanCreate ->
                TextAction(stringResource(R.string.own_mood_create), onClick = { onEvent(MoodsEvent.CreateOwnMood) }, primary = true)
            is OwnMoodUiState.Editing -> OwnMoodControls(own, onEvent)
            OwnMoodUiState.Hidden -> Unit
        }
    }
}

@Composable
private fun OwnMoodControls(own: OwnMoodUiState.Editing, onEvent: (MoodsEvent) -> Unit) {
    val spacing = SnowingTheme.spacing
    val sizes = SnowingTheme.sizes
    val finished = { onEvent(MoodsEvent.DragFinished) }
    LevelRuler(
        label = stringResource(R.string.control_intensity),
        value = own.intensity,
        levels = Mood.LEVELS,
        valueText = stringResource(R.string.level_of, own.intensity, Mood.LEVELS.last),
        onValueChange = { onEvent(MoodsEvent.IntensityChanged(it)) },
        onValueChangeFinished = finished,
        modifier = Modifier.padding(top = spacing.s),
    )
    Row(
        modifier = Modifier.padding(top = spacing.xl),
        horizontalArrangement = Arrangement.spacedBy(sizes.ctaGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WindDial(
            angle = own.windAngle,
            contentDescription = stringResource(R.string.wind_direction),
            onAngleChange = { onEvent(MoodsEvent.WindAngleChanged(it)) },
            onAngleChangeFinished = finished,
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            ControlLabel(stringResource(R.string.control_wind))
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DirectionArrow(own.windArrow)
                LargeValue(own.windStrength.displayName().lowercase())
            }
            Note(stringResource(R.string.wind_drag))
        }
    }
    OptionPicker(
        options = WindStrength.entries,
        selected = own.windStrength,
        optionLabel = { it.displayName() },
        onSelect = { onEvent(MoodsEvent.WindStrengthChanged(it)) },
        modifier = Modifier.padding(top = spacing.s),
    )
    DotPicker(
        label = stringResource(R.string.flake_size),
        options = FlakeSize.entries,
        selected = own.flakeSize,
        dotSize = { size ->
            when (size) {
                FlakeSize.S -> sizes.dotSmall
                FlakeSize.M -> sizes.dotMedium
                FlakeSize.L -> sizes.dotLarge
            }
        },
        optionLabel = { it.displayName() },
        onSelect = { onEvent(MoodsEvent.FlakeSizeChanged(it)) },
        modifier = Modifier.padding(top = spacing.l),
    )
}

@Composable
private fun LabGroup(lab: LabUiState, onEvent: (MoodsEvent) -> Unit) {
    val released = { onEvent(MoodsEvent.DragFinished) }
    Group {
        SectionTitle(stringResource(R.string.lab_title))
        Caption(stringResource(R.string.lab_hint))
        SettingSwitch(
            label = stringResource(R.string.lab_override_mood),
            checked = lab.overrideMood,
            onCheckedChange = { onEvent(MoodsEvent.MoodOverrideToggled(it)) },
        )
        StepSlider(
            label = stringResource(R.string.lab_speed),
            value = lab.speed,
            range = Mood.LEVELS,
            enabled = lab.overrideMood,
            onValueChange = { onEvent(MoodsEvent.SpeedChanged(it)) },
            onValueChangeFinished = released,
        )
        StepSlider(
            label = stringResource(R.string.lab_layers),
            value = lab.layers,
            range = LabSettings.LAYERS,
            enabled = lab.overrideMood,
            onValueChange = { onEvent(MoodsEvent.LayersChanged(it)) },
            onValueChangeFinished = released,
        )
        SettingSlider(
            label = stringResource(R.string.lab_turbulence),
            value = lab.turbulence,
            valueText = lab.turbulence.asPercent(),
            enabled = lab.overrideMood,
            onValueChange = { onEvent(MoodsEvent.TurbulenceChanged(it)) },
            onValueChangeFinished = released,
        )
        SettingSlider(
            label = stringResource(R.string.lab_gusts),
            value = lab.gusts,
            valueText = lab.gusts.asPercent(),
            enabled = lab.overrideMood,
            onValueChange = { onEvent(MoodsEvent.GustsChanged(it)) },
            onValueChangeFinished = released,
        )
        OptionPicker(
            label = stringResource(R.string.lab_quality),
            options = LabQuality.entries,
            selected = lab.quality,
            optionLabel = { it.name.lowercase() },
            onSelect = { onEvent(MoodsEvent.QualityChanged(it)) },
        )
        SettingSwitch(
            label = stringResource(R.string.lab_hud),
            checked = lab.showHud,
            onCheckedChange = { onEvent(MoodsEvent.HudToggled(it)) },
        )
        SettingSwitch(
            label = stringResource(R.string.lab_tracer),
            checked = lab.showTracer,
            onCheckedChange = { onEvent(MoodsEvent.TracerToggled(it)) },
        )
        SettingSwitch(
            label = stringResource(R.string.lab_foreground),
            checked = lab.foreground,
            onCheckedChange = { onEvent(MoodsEvent.ForegroundToggled(it)) },
        )
        SettingSwitch(
            label = stringResource(R.string.lab_instant),
            checked = lab.instantChanges,
            onCheckedChange = { onEvent(MoodsEvent.InstantChangesToggled(it)) },
        )
    }
}

private fun Float.asPercent(): String = "${(this * 100).roundToInt()}%"

private val previewMoods = MoodChoice.entries.map { MoodItem(it, selected = it == MoodChoice.CUSTOM) }

private val previewOwnMood = OwnMoodUiState.Editing(
    intensity = 7,
    flakeSize = FlakeSize.M,
    windAngle = 200,
    windArrow = 180,
    windStrength = WindStrength.GENTLE,
)

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun MoodsScreenOwnMoodPreview() {
    SnowingTheme(colors = SnowingColors.forSky(Sky.at(Sky.HOUR_DUSK))) {
        MoodsScreen(
            state = MoodsUiState(loaded = true, moods = previewMoods, ownMood = previewOwnMood),
            onEvent = {},
        )
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun MoodsScreenLockedPreview() {
    SnowingTheme(colors = SnowingColors.forSky(Sky.at(Sky.HOUR_DAY))) {
        MoodsScreen(
            state = MoodsUiState(
                loaded = true,
                moods = MoodChoice.BUILT_IN.map { MoodItem(it, selected = it == MoodChoice.FIRST_SNOW) },
                ownMood = OwnMoodUiState.Locked,
            ),
            onEvent = {},
        )
    }
}
