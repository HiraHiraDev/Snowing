package com.hirahira.snowing.feature.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hirahira.snowing.R
import com.hirahira.snowing.ui.components.NoticeCard
import com.hirahira.snowing.ui.components.PrimaryButton
import com.hirahira.snowing.ui.components.SectionTitle
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

            SnowfallCard(state, onEvent)

            state.lab?.let { LabCard(it, onEvent) }
        }
    }
}

@Composable
private fun SnowfallCard(state: ControlUiState, onEvent: (ControlEvent) -> Unit) {
    SnowingCard {
        SectionTitle(stringResource(R.string.control_section_snowfall))
        SettingSlider(
            label = stringResource(R.string.control_intensity),
            value = state.intensity,
            valueText = state.intensity.asPercent(),
            onValueChange = { onEvent(ControlEvent.IntensityChanged(it)) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
        )
        SettingSlider(
            label = stringResource(R.string.control_speed),
            value = state.speed,
            valueText = state.speed.asPercent(),
            onValueChange = { onEvent(ControlEvent.SpeedChanged(it)) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
        )
    }
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
            label = stringResource(R.string.lab_layers),
            value = lab.layers.toFloat(),
            valueText = lab.layers.toString(),
            valueRange = LAB_LAYERS_MIN.toFloat()..LAB_LAYERS_MAX.toFloat(),
            steps = LAB_LAYERS_MAX - LAB_LAYERS_MIN - 1,
            onValueChange = { onEvent(ControlEvent.LayersChanged(it.roundToInt())) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
        )
        SettingSlider(
            label = stringResource(R.string.lab_turbulence),
            value = lab.turbulence,
            valueText = lab.turbulence.asPercent(),
            onValueChange = { onEvent(ControlEvent.TurbulenceChanged(it)) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
        )
        SettingSlider(
            label = stringResource(R.string.lab_wind),
            value = lab.wind,
            valueText = lab.wind.asPercent(),
            valueRange = -1f..1f,
            onValueChange = { onEvent(ControlEvent.WindChanged(it)) },
            onValueChangeFinished = { onEvent(ControlEvent.SliderReleased) },
        )
        SettingSlider(
            label = stringResource(R.string.lab_gusts),
            value = lab.gusts,
            valueText = lab.gusts.asPercent(),
            onValueChange = { onEvent(ControlEvent.GustsChanged(it)) },
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

@Preview
@Composable
private fun ControlScreenPreview() {
    SnowingTheme {
        ControlScreen(
            state = ControlUiState(
                snowEnabled = true,
                hasOverlayPermission = true,
                intensity = 0.45f,
                speed = 0.4f,
                lab = LabUiState(
                    layers = 3,
                    turbulence = 0.5f,
                    wind = 0f,
                    gusts = 0.4f,
                    showHud = true,
                    showTracer = false,
                    foreground = true,
                    instantChanges = false,
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
            state = ControlUiState(intensity = 0.45f, speed = 0.4f),
            onEvent = {},
            onGrantOverlayPermission = {},
        )
    }
}
