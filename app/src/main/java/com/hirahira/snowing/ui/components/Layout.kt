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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import com.hirahira.snowing.ui.theme.SnowingTheme

/**
 * A page's scrolling content: screen margins, groups set apart by space (design 6a has no
 * cards), and starting [top] below the status bar. On a page under the tabs ([underTabs]),
 * content scrolled up fades out instead of running into them.
 */
@Composable
fun ScrollingPage(
    top: Dp,
    modifier: Modifier = Modifier,
    underTabs: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SnowingTheme.spacing
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (underTabs) {
                    Modifier.fadeOutAbove(start = statusBar + spacing.underTabsFadeStart, length = SnowingTheme.sizes.underTabsFade)
                } else {
                    Modifier
                },
            )
            .verticalScroll(rememberScrollState())
            .pagePadding(top),
        verticalArrangement = Arrangement.spacedBy(spacing.group),
        content = content,
    )
}

/**
 * A page that fits the screen, with the same margins as [ScrollingPage]. Lays out like a
 * Column, so `Modifier.weight` can push content to the bottom.
 */
@Composable
fun Page(
    top: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().pagePadding(top), content = content)
}

@Composable
private fun Modifier.pagePadding(top: Dp): Modifier {
    val spacing = SnowingTheme.spacing
    return safeDrawingPadding()
        .padding(horizontal = spacing.screen)
        .padding(top = top, bottom = spacing.screenBottom)
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

/**
 * Darkens (or, on a light sky, lightens) the scene behind long content: nothing at the top,
 * full strength at the bottom, on an eased curve so it has no visible edge.
 */
@Composable
fun Scrim(modifier: Modifier = Modifier) {
    val scrim = SnowingTheme.colors.scrim
    Box(modifier.background(Brush.verticalGradient(colorStops = eased(scrim.copy(alpha = 0f), scrim))))
}

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
            colorStops = MaskStops,
            startY = startPx,
            endY = startPx + length.toPx(),
        )
        onDrawWithContent {
            drawContent()
            drawRect(mask, blendMode = BlendMode.DstIn)
        }
    }

// Only the mask's alpha matters; the color is ignored by DstIn.
private val MaskStops = eased(Color.Black.copy(alpha = 0f), Color.Black)

/**
 * Color stops from [from] to [to] along a smoothstep curve: it starts and ends gently, so a
 * gradient shows no line where it begins or ends.
 */
private fun eased(from: Color, to: Color): Array<Pair<Float, Color>> = Array(EASE_STEPS + 1) { i ->
    val t = i.toFloat() / EASE_STEPS
    t to lerp(from, to, t * t * (3 - 2 * t))
}

private const val EASE_STEPS = 8
