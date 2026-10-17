package com.hirahira.snowing.debug.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hirahira.snowing.R
import com.hirahira.snowing.settings.LabQuality
import com.hirahira.snowing.settings.LabSettings
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.ui.components.Caption
import com.hirahira.snowing.ui.components.OptionPicker
import com.hirahira.snowing.ui.components.ScrollingPage
import com.hirahira.snowing.ui.components.SectionTitle
import com.hirahira.snowing.ui.components.SettingSlider
import com.hirahira.snowing.ui.components.SettingSwitch
import com.hirahira.snowing.ui.components.StepSlider
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlin.math.roundToInt

@Composable
fun LabRoute(viewModel: LabViewModel = viewModel(factory = LabViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LabScreen(state, viewModel::onEvent)
}

@Composable
fun LabScreen(state: LabUiState, onEvent: (LabEvent) -> Unit, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalContentColor provides SnowingTheme.colors.ink) {
        Box(modifier.fillMaxSize().background(SnowingTheme.colors.sky.skyTop)) {
            if (!state.loaded) return@Box
            ScrollingPage(top = SnowingTheme.spacing.l, underTabs = false) {
                LabControls(state, onEvent)
            }
        }
    }
}

@Composable
private fun LabControls(lab: LabUiState, onEvent: (LabEvent) -> Unit) {
    val released = { onEvent(LabEvent.DragFinished) }
    SectionTitle(stringResource(R.string.lab_title))
    Caption(stringResource(R.string.lab_hint))
    SettingSwitch(
        label = stringResource(R.string.lab_override_mood),
        checked = lab.overrideMood,
        onCheckedChange = { onEvent(LabEvent.MoodOverrideToggled(it)) },
    )
    StepSlider(
        label = stringResource(R.string.lab_speed),
        value = lab.speed,
        range = Mood.LEVELS,
        enabled = lab.overrideMood,
        onValueChange = { onEvent(LabEvent.SpeedChanged(it)) },
        onValueChangeFinished = released,
    )
    StepSlider(
        label = stringResource(R.string.lab_layers),
        value = lab.layers,
        range = LabSettings.LAYERS,
        enabled = lab.overrideMood,
        onValueChange = { onEvent(LabEvent.LayersChanged(it)) },
        onValueChangeFinished = released,
    )
    SettingSlider(
        label = stringResource(R.string.lab_turbulence),
        value = lab.turbulence,
        valueText = lab.turbulence.asPercent(),
        enabled = lab.overrideMood,
        onValueChange = { onEvent(LabEvent.TurbulenceChanged(it)) },
        onValueChangeFinished = released,
    )
    SettingSlider(
        label = stringResource(R.string.lab_gusts),
        value = lab.gusts,
        valueText = lab.gusts.asPercent(),
        enabled = lab.overrideMood,
        onValueChange = { onEvent(LabEvent.GustsChanged(it)) },
        onValueChangeFinished = released,
    )
    OptionPicker(
        label = stringResource(R.string.lab_quality),
        options = LabQuality.entries,
        selected = lab.quality,
        optionLabel = { it.name.lowercase() },
        onSelect = { onEvent(LabEvent.QualityChanged(it)) },
    )
    SettingSwitch(
        label = stringResource(R.string.lab_hud),
        checked = lab.showHud,
        onCheckedChange = { onEvent(LabEvent.HudToggled(it)) },
    )
    SettingSwitch(
        label = stringResource(R.string.lab_tracer),
        checked = lab.showTracer,
        onCheckedChange = { onEvent(LabEvent.TracerToggled(it)) },
    )
    SettingSwitch(
        label = stringResource(R.string.lab_foreground),
        checked = lab.foreground,
        onCheckedChange = { onEvent(LabEvent.ForegroundToggled(it)) },
    )
    SettingSwitch(
        label = stringResource(R.string.lab_instant),
        checked = lab.instantChanges,
        onCheckedChange = { onEvent(LabEvent.InstantChangesToggled(it)) },
    )
}

private fun Float.asPercent(): String = "${(this * 100).roundToInt()}%"

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun LabScreenPreview() {
    SnowingTheme {
        LabScreen(
            LabUiState(loaded = true, speed = 5, layers = 3, turbulence = 0.5f, gusts = 0.4f, showHud = true, foreground = true),
            onEvent = {},
        )
    }
}
