package com.hirahira.snowing.feature.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hirahira.snowing.R
import com.hirahira.snowing.ui.components.AppIcon
import com.hirahira.snowing.ui.components.BellGlyph
import com.hirahira.snowing.ui.components.DoneNote
import com.hirahira.snowing.ui.components.InfoNote
import com.hirahira.snowing.ui.components.InlineAction
import com.hirahira.snowing.ui.components.ItemNote
import com.hirahira.snowing.ui.components.ItemRow
import com.hirahira.snowing.ui.components.NumberedStep
import com.hirahira.snowing.ui.components.Paragraph
import com.hirahira.snowing.ui.components.PrimaryButton
import com.hirahira.snowing.ui.components.QuietAction
import com.hirahira.snowing.ui.components.Rule
import com.hirahira.snowing.ui.components.SceneSheet
import com.hirahira.snowing.ui.components.SettingPreview
import com.hirahira.snowing.ui.components.SheetHeading
import com.hirahira.snowing.ui.components.TileGlyph
import com.hirahira.snowing.ui.components.emphasized
import com.hirahira.snowing.ui.theme.SnowingColors
import com.hirahira.snowing.ui.theme.SnowingTheme

/**
 * First-run onboarding (ADR-0011, design 6.2) on a translucent panel over the scene: why the
 * overlay permission and how to find it, then how to stop the snow from anywhere.
 * [backdrop] is the scene as the screen shows it, blurred behind the panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
    backdrop: @Composable () -> Unit,
) {
    // Read before the panel's own dark theme: the status bar lies over the sky.
    val lightStatusBar = SnowingTheme.colors.isLightSky
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(state.closing) {
        if (state.closing) {
            sheetState.hide()
            onEvent(OnboardingEvent.Closed)
        }
    }
    SnowingTheme(colors = remember { SnowingColors.forSheet() }) {
        SceneSheet(
            onDismissRequest = { onEvent(OnboardingEvent.Dismissed) },
            sheetState = sheetState,
            backdrop = backdrop,
            lightStatusBar = lightStatusBar,
        ) {
            OnboardingContent(state, onEvent)
        }
    }
}

@Composable
private fun OnboardingContent(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    Crossfade(targetState = state.step, animationSpec = tween(STEP_FADE_MS), label = "onboarding step") { step ->
        Column {
            SheetHeading(
                title = stringResource(if (step == OnboardingStep.OVERLAY) R.string.ob_overlay_title else R.string.ob_stop_title),
                label = if (state.stepCount > 1) {
                    stringResource(R.string.ob_step_of, if (step == OnboardingStep.OVERLAY) 1 else state.stepCount, state.stepCount)
                } else {
                    null
                },
            )
            when (step) {
                OnboardingStep.OVERLAY -> OverlayStep(state, onEvent)
                OnboardingStep.STOP -> StopStep(state, onEvent)
            }
        }
    }
}

@Composable
private fun OverlayStep(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    val spacing = SnowingTheme.spacing
    Paragraph(stringResource(R.string.ob_overlay_body), Modifier.padding(top = spacing.m))
    Paragraph(
        emphasized(stringResource(R.string.ob_overlay_why), stringResource(R.string.ob_overlay_perm_name)),
        Modifier.padding(top = spacing.paragraph),
    )
    Rule(Modifier.padding(top = spacing.dividerAbove, bottom = spacing.dividerBelow))
    when (state.overlayGuide) {
        OverlayGuide.APP_LIST -> Column(verticalArrangement = Arrangement.spacedBy(spacing.stepGap)) {
            GuideStep(1, R.string.ob_overlay_guide_1)
            GuideStep(2, R.string.ob_overlay_guide_2)
            SettingPreview(stringResource(R.string.app_name), Modifier.padding(start = spacing.previewInset)) { AppIcon() }
            GuideStep(3, R.string.ob_overlay_guide_3)
        }
        OverlayGuide.APP_PAGE -> ItemNote(stringResource(R.string.ob_overlay_guide_legacy))
    }
    PrimaryButton(
        text = stringResource(R.string.ob_overlay_open),
        onClick = { onEvent(OnboardingEvent.AllowOverlay) },
        modifier = Modifier.padding(top = spacing.sheetAction),
    )
    QuietAction(stringResource(R.string.ob_not_now), onClick = { onEvent(OnboardingEvent.NotNow) })
}

@Composable
private fun GuideStep(number: Int, text: Int) {
    NumberedStep(number, stringResource(text), numberDescription = stringResource(R.string.ob_guide_step, number))
}

@Composable
private fun StopStep(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit) {
    val spacing = SnowingTheme.spacing
    ItemRow(
        title = stringResource(R.string.ob_notif_title),
        body = stringResource(R.string.ob_notif_body, stringResource(R.string.notification_stop)),
        icon = { BellGlyph() },
        modifier = Modifier.padding(top = spacing.l),
    ) {
        if (state.notificationsAllowed) {
            DoneNote(stringResource(R.string.ob_notif_done))
        } else {
            InlineAction(stringResource(R.string.ob_notif_action), onClick = { onEvent(OnboardingEvent.AllowNotifications) })
        }
    }
    ItemRow(
        title = stringResource(R.string.ob_tile_title, stringResource(R.string.tile_label)),
        body = stringResource(R.string.ob_tile_body),
        icon = { TileGlyph() },
    ) {
        when {
            !state.canOfferTile -> ItemNote(stringResource(R.string.ob_tile_manual), Modifier.padding(vertical = spacing.s))
            state.tileAdded -> DoneNote(stringResource(R.string.ob_tile_done))
            else -> InlineAction(stringResource(R.string.ob_tile_action), onClick = { onEvent(OnboardingEvent.AddTile) })
        }
    }
    InfoNote(stringResource(R.string.ob_secure_note))
    PrimaryButton(
        text = stringResource(R.string.ob_go),
        onClick = { onEvent(OnboardingEvent.LetItSnow) },
        modifier = Modifier.padding(top = spacing.sheetAction),
    )
}

private const val STEP_FADE_MS = 250

// The modal panel needs a window, so the previews show its content on the panel's color.
@Composable
private fun ContentPreview(state: OnboardingUiState) {
    SnowingTheme(colors = SnowingColors.forSheet()) {
        Column(
            Modifier
                .background(SnowingTheme.colors.sheetSolid)
                .padding(horizontal = SnowingTheme.spacing.sheetSide, vertical = SnowingTheme.spacing.sheetBottom),
        ) {
            OnboardingContent(state, onEvent = {})
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun OnboardingOverlayPreview() {
    ContentPreview(OnboardingUiState())
}

@Preview(widthDp = 360)
@Composable
private fun OnboardingStopPreview() {
    ContentPreview(OnboardingUiState(step = OnboardingStep.STOP, canOfferTile = true, notificationsAllowed = true))
}
