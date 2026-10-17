package com.hirahira.snowing.feature.snow

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hirahira.snowing.R
import com.hirahira.snowing.feature.common.displayName
import com.hirahira.snowing.settings.Mood
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.ui.components.HeroTitle
import com.hirahira.snowing.ui.components.Hint
import com.hirahira.snowing.ui.components.LevelIndicator
import com.hirahira.snowing.ui.components.Page
import com.hirahira.snowing.ui.components.StartStopButton
import com.hirahira.snowing.ui.theme.Sky
import com.hirahira.snowing.ui.theme.SnowingColors
import com.hirahira.snowing.ui.theme.SnowingTheme

/**
 * The Snow page (design 6a): the active mood, large, and the start / stop button on the
 * drift. No "snowing / not snowing" text: the snow itself and the button show the state.
 * The mood only shows here; it is picked on the Moods page.
 */
@Composable
fun SnowScreen(
    state: SnowUiState,
    onEvent: (SnowEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.loaded) return
    val spacing = SnowingTheme.spacing
    Page(top = SnowingTheme.spacing.pageTopSnow, modifier = modifier) {
        HeroTitle(state.mood.displayName())
        LevelIndicator(
            level = state.intensity,
            max = Mood.LEVELS.last,
            contentDescription = stringResource(R.string.mood_intensity_description, state.intensity, Mood.LEVELS.last),
            modifier = Modifier.padding(top = spacing.l),
        )
        // Says why the button opens system settings instead of starting the snow.
        if (!state.hasOverlayPermission) {
            Hint(stringResource(R.string.hint_permission), Modifier.padding(top = spacing.xl))
        }
        Spacer(Modifier.weight(1f))
        StartStopButton(
            running = state.snowEnabled,
            label = stringResource(if (state.snowEnabled) R.string.control_stop else R.string.control_start),
            onClick = { onEvent(SnowEvent.ToggleSnow) },
        )
    }
}

private val previewState = SnowUiState(
    loaded = true,
    hasOverlayPermission = true,
    mood = MoodChoice.HEAVY,
    intensity = 6,
)

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun SnowScreenIdlePreview() {
    SnowingTheme(colors = SnowingColors.forSky(Sky.at(Sky.HOUR_DUSK))) {
        SnowScreen(previewState, onEvent = {})
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun SnowScreenSnowingPreview() {
    SnowingTheme(colors = SnowingColors.forSky(Sky.at(Sky.HOUR_DAY))) {
        SnowScreen(previewState.copy(snowEnabled = true), onEvent = {})
    }
}
