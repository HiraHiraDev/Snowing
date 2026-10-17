package com.hirahira.snowing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.hirahira.snowing.ui.theme.SnowingTheme

/**
 * A page's scrolling content: screen margins, groups set apart by space (design 6a has no
 * cards), and starting [top] below the status bar. Content scrolled up under the page tabs
 * fades out instead of running into them.
 */
@Composable
fun ScrollingPage(
    top: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SnowingTheme.spacing
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = modifier
            .fillMaxSize()
            .fadeOutAbove(start = statusBar + spacing.underTabsFadeStart, length = SnowingTheme.sizes.underTabsFade)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = spacing.screen)
            .padding(top = top, bottom = spacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(spacing.group),
        content = content,
    )
}

/** Related controls kept together by space alone. */
@Composable
fun Group(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SnowingTheme.spacing.m),
        content = content,
    )
}

/** Asks the user to do something, with one action. */
@Composable
fun Notice(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Group(modifier) {
        AttentionTitle(title)
        BodyText(body)
        PrimaryButton(text = actionLabel, onClick = onAction)
    }
}

/** Darkens (or, on a light sky, lightens) the scene behind long content, fading in from the top. */
@Composable
fun Scrim(modifier: Modifier = Modifier) {
    val colors = SnowingTheme.colors
    Box(
        modifier.background(
            Brush.verticalGradient(
                0f to colors.scrimMid.copy(alpha = 0f),
                SCRIM_MID_AT to colors.scrimMid,
                1f to colors.scrimEnd,
            ),
        ),
    )
}

private const val SCRIM_MID_AT = 0.3f

/**
 * Makes the content invisible above [start] and fades it in over [length] below, on an eased
 * curve so the edge has no visible line. A mask, not a painted band: whatever lies behind
 * (sky, moon) shows through unchanged.
 */
private fun Modifier.fadeOutAbove(start: Dp, length: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val startPx = start.toPx()
        val mask = Brush.verticalGradient(
            colorStops = EasedMask,
            startY = startPx,
            endY = startPx + length.toPx(),
        )
        onDrawWithContent {
            drawContent()
            drawRect(mask, blendMode = BlendMode.DstIn)
        }
    }

// Mask opacity along the fade, smoothstep-shaped: no hard start or end to catch the eye.
// Only the alpha matters; the color is ignored by DstIn.
private const val MASK_STEPS = 8
private val EasedMask: Array<Pair<Float, Color>> = Array(MASK_STEPS + 1) { i ->
    val t = i.toFloat() / MASK_STEPS
    t to Color.Black.copy(alpha = t * t * (3 - 2 * t))
}
