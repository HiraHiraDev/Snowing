package com.hirahira.snowing.feature.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import com.hirahira.snowing.ui.components.Group
import com.hirahira.snowing.ui.components.LevelIndicator
import com.hirahira.snowing.ui.components.Notice
import com.hirahira.snowing.ui.components.OptionPicker
import com.hirahira.snowing.ui.components.PrimaryButton
import com.hirahira.snowing.ui.components.Scrim
import com.hirahira.snowing.ui.components.ScrollingPage
import com.hirahira.snowing.ui.components.SectionTitle
import com.hirahira.snowing.ui.components.SettingSlider
import com.hirahira.snowing.ui.components.SettingSwitch
import com.hirahira.snowing.ui.components.StepSlider
import com.hirahira.snowing.ui.components.TextAction
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlin.math.roundToInt

@Composable
fun ControlScreen(
    state: ControlUiState,
    onEvent: (ControlEvent) -> Unit,
    onGrantOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        // Prototype controls are long and cross the light drift; the scrim keeps them legible.
        Scrim(Modifier.fillMaxSize())
        // Nothing that depends on stored state shows before it loads (B-008).
        if (!state.loaded) return@Box
        ScrollingPage(top = SnowingTheme.spacing.pageTopSnow) {
            if (!state.hasOverlayPermission) {
                Notice(
                    title = stringResource(R.string.permission_title),
                    body = stringResource(R.string.permission_body),
                    actionLabel = stringResource(R.string.permission_action),
                    onAction = onGrantOverlayPermission,
                )
            }
            PrimaryButton(
                text = stringResource(if (state.snowEnabled) R.string.control_stop else R.string.control_start),
                onClick = { onEvent(ControlEvent.ToggleSnow) },
                enabled = state.canToggle,
            )
            MoodGroup(state.moods, onEvent)
            state.adjust?.let { AdjustGroup(it, onEvent) }
            OptionPicker(
                label = stringResource(R.string.time_of_day),
                options = TimeOfDay.entries,
                selected = state.timeOfDay,
                optionLabel = { it.displayName() },
                onSelect = { onEvent(ControlEvent.TimeOfDayChanged(it)) },
            )
            state.lab?.let { LabGroup(it, onEvent) }
        }
    }
}

@Composable
private fun MoodGroup(moods: List<MoodItem>, onEvent: (ControlEvent) -> Unit) {
    Group {
        SectionTitle(stringResource(R.string.mood_section))
        ChoiceList(
            items = moods,
            isSelected = { it.selected },
            title = { it.choice.displayName() },
            onSelect = { onEvent(ControlEvent.MoodSelected(it.choice)) },
        ) { item ->
            LevelIndicator(
                level = item.intensity,
                max = Mood.LEVELS.last,
                contentDescription = stringResource(R.string.mood_intensity_description, item.intensity, Mood.LEVELS.last),
            )
        }
    }
}

@Composable
private fun AdjustGroup(adjust: AdjustUiState, onEvent: (ControlEvent) -> Unit) {
    val moodName = adjust.mood.displayName()
    val enabled = !adjust.locked
    val released = { onEvent(ControlEvent.SliderReleased) }
    Group {
        SectionTitle(stringResource(R.string.adjust))
        Caption(
            when {
                adjust.locked -> stringResource(R.string.adjust_locked)
                adjust.edited -> stringResource(R.string.adjust_edited, moodName)
                else -> stringResource(R.string.adjust_applies_to, moodName)
            },
        )
        if (adjust.edited) {
            Row(horizontalArrangement = Arrangement.spacedBy(SnowingTheme.spacing.xl)) {
                TextAction(stringResource(R.string.adjust_restore), onClick = { onEvent(ControlEvent.RestoreMood) })
                TextAction(
                    stringResource(R.string.adjust_save_new),
                    onClick = { onEvent(ControlEvent.SaveMoodAsNew) },
                    primary = true,
                )
            }
        }
        StepSlider(
            label = stringResource(R.string.control_intensity),
            value = adjust.intensity,
            range = Mood.LEVELS,
            enabled = enabled,
            onValueChange = { onEvent(ControlEvent.IntensityChanged(it)) },
            onValueChangeFinished = released,
        )
        OptionPicker(
            label = stringResource(R.string.flake_size),
            options = FlakeSize.entries,
            selected = adjust.flakeSize,
            optionLabel = { it.displayName() },
            enabled = enabled,
            onSelect = { onEvent(ControlEvent.FlakeSizeChanged(it)) },
        )
        // A plain slider until the wind wheel of design 6a.
        StepSlider(
            label = stringResource(R.string.control_wind),
            value = adjust.windAngle,
            range = Mood.WIND_ANGLES,
            valueText = stringResource(R.string.degrees, adjust.windAngle),
            enabled = enabled,
            onValueChange = { onEvent(ControlEvent.WindAngleChanged(it)) },
            onValueChangeFinished = released,
        )
        OptionPicker(
            label = stringResource(R.string.wind_strength),
            options = WindStrength.entries,
            selected = adjust.windStrength,
            optionLabel = { it.displayName() },
            enabled = enabled,
            onSelect = { onEvent(ControlEvent.WindStrengthChanged(it)) },
        )
    }
}

@Composable
private fun LabGroup(lab: LabUiState, onEvent: (ControlEvent) -> Unit) {
    val released = { onEvent(ControlEvent.SliderReleased) }
    Group {
        SectionTitle(stringResource(R.string.lab_title))
        Caption(stringResource(R.string.lab_hint))
        SettingSwitch(
            label = stringResource(R.string.lab_override_mood),
            checked = lab.overrideMood,
            onCheckedChange = { onEvent(ControlEvent.MoodOverrideToggled(it)) },
        )
        StepSlider(
            label = stringResource(R.string.lab_speed),
            value = lab.speed,
            range = Mood.LEVELS,
            enabled = lab.overrideMood,
            onValueChange = { onEvent(ControlEvent.SpeedChanged(it)) },
            onValueChangeFinished = released,
        )
        StepSlider(
            label = stringResource(R.string.lab_layers),
            value = lab.layers,
            range = LabSettings.LAYERS,
            enabled = lab.overrideMood,
            onValueChange = { onEvent(ControlEvent.LayersChanged(it)) },
            onValueChangeFinished = released,
        )
        SettingSlider(
            label = stringResource(R.string.lab_turbulence),
            value = lab.turbulence,
            valueText = lab.turbulence.asPercent(),
            enabled = lab.overrideMood,
            onValueChange = { onEvent(ControlEvent.TurbulenceChanged(it)) },
            onValueChangeFinished = released,
        )
        SettingSlider(
            label = stringResource(R.string.lab_gusts),
            value = lab.gusts,
            valueText = lab.gusts.asPercent(),
            enabled = lab.overrideMood,
            onValueChange = { onEvent(ControlEvent.GustsChanged(it)) },
            onValueChangeFinished = released,
        )
        OptionPicker(
            label = stringResource(R.string.lab_quality),
            options = LabQuality.entries,
            selected = lab.quality,
            optionLabel = { it.name.lowercase() },
            onSelect = { onEvent(ControlEvent.QualityChanged(it)) },
        )
        SettingSwitch(
            label = stringResource(R.string.lab_hud),
            checked = lab.showHud,
            onCheckedChange = { onEvent(ControlEvent.HudToggled(it)) },
        )
        SettingSwitch(
            label = stringResource(R.string.lab_tracer),
            checked = lab.showTracer,
            onCheckedChange = { onEvent(ControlEvent.TracerToggled(it)) },
        )
        SettingSwitch(
            label = stringResource(R.string.lab_foreground),
            checked = lab.foreground,
            onCheckedChange = { onEvent(ControlEvent.ForegroundToggled(it)) },
        )
        SettingSwitch(
            label = stringResource(R.string.lab_instant),
            checked = lab.instantChanges,
            onCheckedChange = { onEvent(ControlEvent.InstantChangesToggled(it)) },
        )
    }
}

private fun Float.asPercent(): String = "${(this * 100).roundToInt()}%"

private val previewMoods = MoodChoice.entries.map { choice ->
    MoodItem(choice, intensity = (choice.ordinal + 1) * 2, selected = choice == MoodChoice.HEAVY)
}

private val previewAdjust = AdjustUiState(
    mood = MoodChoice.HEAVY,
    edited = true,
    locked = false,
    intensity = 7,
    flakeSize = FlakeSize.M,
    windAngle = 200,
    windStrength = WindStrength.GENTLE,
)

@Preview
@Composable
private fun ControlScreenPreview() {
    SnowingTheme {
        ControlScreen(
            state = ControlUiState(
                loaded = true,
                snowEnabled = true,
                hasOverlayPermission = true,
                moods = previewMoods,
                adjust = previewAdjust,
                lab = LabUiState(
                    overrideMood = false,
                    speed = 5,
                    layers = 3,
                    turbulence = 0.5f,
                    gusts = 0.4f,
                    showHud = true,
                    showTracer = false,
                    foreground = true,
                    instantChanges = false,
                    quality = LabQuality.AUTO,
                ),
            ),
            onEvent = {},
            onGrantOverlayPermission = {},
        )
    }
}

@Preview
@Composable
private fun ControlScreenNoPermissionPreview() {
    SnowingTheme {
        ControlScreen(
            state = ControlUiState(
                loaded = true,
                moods = previewMoods.map { it.copy(selected = it.choice == MoodChoice.FIRST_SNOW) },
                adjust = previewAdjust.copy(mood = MoodChoice.FIRST_SNOW, edited = false, locked = true),
            ),
            onEvent = {},
            onGrantOverlayPermission = {},
        )
    }
}
