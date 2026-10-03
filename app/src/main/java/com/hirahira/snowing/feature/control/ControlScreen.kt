package com.hirahira.snowing.feature.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hirahira.snowing.R
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.LabQuality
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.ui.components.LevelIndicator
import com.hirahira.snowing.ui.components.NoticeCard
import com.hirahira.snowing.ui.components.PrimaryButton
import com.hirahira.snowing.ui.components.SectionTitle
import com.hirahira.snowing.ui.components.SegmentedChoice
import com.hirahira.snowing.ui.components.SelectableRow
import com.hirahira.snowing.ui.components.SettingSlider
import com.hirahira.snowing.ui.components.SettingSwitch
import com.hirahira.snowing.ui.components.SnowingCard
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlin.math.roundToInt

@Composable
fun ControlScreen(
    state: ControlUiState,
    onEvent: (ControlEvent) -> Unit,
    onGrantOverlayPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = SnowingTheme.spacing
    Surface(
        modifier = modifier.fillMaxSize(),
        color = SnowingTheme.colors.background,
        contentColor = SnowingTheme.colors.content,
    ) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.m, vertical = spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.m),
        ) {
            Text(text = stringResource(R.string.app_name), style = SnowingTheme.typography.display)

            if (!state.hasOverlayPermission) {
                NoticeCard(
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

            MoodCard(state, onEvent)

            state.customMood?.let { CustomMoodCard(it, onEvent) }

            state.lab?.let { LabCard(it, onEvent) }
        }
    }
}

@Composable
private fun MoodCard(state: ControlUiState, onEvent: (ControlEvent) -> Unit) {
    SnowingCard {
        SectionTitle(stringResource(R.string.mood_section))
        Column(Modifier.selectableGroup()) {
            for (item in state.moods) {
                SelectableRow(
                    title = stringResource(item.choice.nameRes()),
                    selected = item.selected,
                    onClick = { onEvent(ControlEvent.MoodSelected(item.choice)) },
                ) {
                    LevelIndicator(
                        level = item.intensity,
                        max = Mood.LEVELS.last,
                        contentDescription = stringResource(R.string.mood_intensity_description, item.intensity, Mood.LEVELS.last),
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomMoodCard(custom: CustomMoodUiState, onEvent: (ControlEvent) -> Unit) {
    SnowingCard {
        SectionTitle(stringResource(R.string.mood_custom))
        SegmentedChoice(
            label = stringResource(R.string.custom_flake_size),
            options = FlakeSize.entries.map { stringResource(it.labelRes()) },
            selectedIndex = custom.flakeSize.ordinal,
            onSelect = { onEvent(ControlEvent.CustomFlakeSizeChanged(FlakeSize.entries[it])) },
        )
        LevelSlider(
            label = stringResource(R.string.control_intensity),
            value = custom.intensity,
            levels = Mood.LEVELS,
            onValueChange = { onEvent(ControlEvent.CustomIntensityChanged(it)) },
            onEvent = onEvent,
        )
        LevelSlider(
            label = stringResource(R.string.control_speed),
            value = custom.speed,
            levels = Mood.LEVELS,
            onValueChange = { onEvent(ControlEvent.CustomSpeedChanged(it)) },
            onEvent = onEvent,
        )
        LevelSlider(
            label = stringResource(R.string.control_wind),
            value = custom.wind,
            levels = Mood.WIND_LEVELS,
            onValueChange = { onEvent(ControlEvent.CustomWindChanged(it)) },
            onEvent = onEvent,
        )
    }
}

@Composable
private fun LevelSlider(
    label: String,
    value: Int,
    levels: IntRange,
    onValueChange: (Int) -> Unit,
    onEvent: (ControlEvent) -> Unit,
) {
    SettingSlider(
        label = label,
        value = value.toFloat(),
        valueText = value.toString(),
        valueRange = levels.first.toFloat()..levels.last.toFloat(),
        steps = levels.last - levels.first - 1,
        onValueChange = { onValueChange(it.roundToInt()) },
        onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
    )
}

private fun MoodChoice.nameRes(): Int = when (this) {
    MoodChoice.FIRST_SNOW -> R.string.mood_first_snow
    MoodChoice.FLUFF -> R.string.mood_fluff
    MoodChoice.HEAVY -> R.string.mood_heavy
    MoodChoice.BLIZZARD -> R.string.mood_blizzard
    MoodChoice.CUSTOM -> R.string.mood_custom
}

private fun FlakeSize.labelRes(): Int = when (this) {
    FlakeSize.S -> R.string.flake_size_s
    FlakeSize.M -> R.string.flake_size_m
    FlakeSize.L -> R.string.flake_size_l
}

@Composable
private fun LabCard(lab: LabUiState, onEvent: (ControlEvent) -> Unit) {
    SnowingCard {
        SectionTitle(stringResource(R.string.lab_title))
        Text(
            text = stringResource(R.string.lab_hint),
            style = SnowingTheme.typography.label,
            color = SnowingTheme.colors.contentMuted,
        )
        SettingSlider(
            label = stringResource(R.string.lab_wind_direction),
            value = lab.windDirection,
            valueText = "${lab.windDirection.roundToInt()}°",
            valueRange = -SnowSettings.MAX_WIND_DIRECTION..SnowSettings.MAX_WIND_DIRECTION,
            onValueChange = { onEvent(ControlEvent.WindDirectionChanged(it)) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
        )
        SettingSwitch(
            label = stringResource(R.string.lab_override_mood),
            checked = lab.overrideMood,
            onCheckedChange = { onEvent(ControlEvent.MoodOverrideToggled(it)) },
        )
        SettingSlider(
            label = stringResource(R.string.lab_layers),
            value = lab.layers.toFloat(),
            valueText = lab.layers.toString(),
            valueRange = LAB_LAYERS_MIN.toFloat()..LAB_LAYERS_MAX.toFloat(),
            steps = LAB_LAYERS_MAX - LAB_LAYERS_MIN - 1,
            enabled = lab.overrideMood,
            onValueChange = { onEvent(ControlEvent.LayersChanged(it.roundToInt())) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
        )
        SettingSlider(
            label = stringResource(R.string.lab_turbulence),
            value = lab.turbulence,
            valueText = lab.turbulence.asPercent(),
            enabled = lab.overrideMood,
            onValueChange = { onEvent(ControlEvent.TurbulenceChanged(it)) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
        )
        SettingSlider(
            label = stringResource(R.string.lab_gusts),
            value = lab.gusts,
            valueText = lab.gusts.asPercent(),
            enabled = lab.overrideMood,
            onValueChange = { onEvent(ControlEvent.GustsChanged(it)) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
        )
        SettingSlider(
            label = stringResource(R.string.lab_quality),
            value = lab.quality.ordinal.toFloat(),
            valueText = lab.quality.name,
            valueRange = 0f..LabQuality.entries.lastIndex.toFloat(),
            steps = LabQuality.entries.size - 2,
            onValueChange = { onEvent(ControlEvent.QualityChanged(LabQuality.entries[it.roundToInt()])) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
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

private const val LAB_LAYERS_MIN = 1
private const val LAB_LAYERS_MAX = 5

private fun Float.asPercent(): String = "${(this * 100).roundToInt()}%"

private val previewMoods = MoodChoice.entries.map { choice ->
    MoodItem(choice, intensity = (choice.ordinal + 1) * 2, selected = choice == MoodChoice.CUSTOM)
}

@Preview
@Composable
private fun ControlScreenPreview() {
    SnowingTheme {
        ControlScreen(
            state = ControlUiState(
                snowEnabled = true,
                hasOverlayPermission = true,
                moods = previewMoods,
                customMood = CustomMoodUiState(FlakeSize.L, intensity = 5, speed = 2, wind = 2),
                lab = LabUiState(
                    windDirection = SnowSettings.DEFAULT_WIND_DIRECTION,
                    overrideMood = false,
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
            state = ControlUiState(moods = previewMoods.map { it.copy(selected = it.choice == MoodChoice.FIRST_SNOW) }),
            onEvent = {},
            onGrantOverlayPermission = {},
        )
    }
}
