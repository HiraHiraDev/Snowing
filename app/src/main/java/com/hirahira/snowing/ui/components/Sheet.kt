package com.hirahira.snowing.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import com.hirahira.snowing.R
import com.hirahira.snowing.ui.theme.SnowingTheme

// A panel from the bottom over the scene (design 6.2) and the pieces it is made of.

/**
 * A modal panel from the bottom over the scene: dragged or tapped away like any bottom sheet.
 * Translucent over a blurred copy of the scene ([backdrop], drawn as the screen shows it) on
 * API 31+, nearly opaque below. [lightStatusBar] keeps the status bar icons readable on the
 * sky above it; the navigation bar lies on the dark panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    backdrop: @Composable () -> Unit,
    lightStatusBar: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SnowingTheme.colors
    val shape = SnowingTheme.shapes.sheet
    val spacing = SnowingTheme.spacing
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = colors.ink,
        scrimColor = colors.sheetScrim,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
        properties = ModalBottomSheetProperties(
            isAppearanceLightStatusBars = lightStatusBar,
            isAppearanceLightNavigationBars = false,
        ),
    ) {
        var top by remember { mutableFloatStateOf(0f) }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .onGloballyPositioned { top = it.positionOnScreen().y },
        ) {
            if (canBlur) BlurredBackdrop(top = { top }, backdrop = backdrop, modifier = Modifier.matchParentSize())
            Column(
                Modifier
                    .background(if (canBlur) colors.sheet else colors.sheetSolid)
                    .border(topHighlight(), shape),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = spacing.sheetHandleTop, bottom = spacing.sheetHandleGap)
                        .size(SnowingTheme.sizes.handleWidth, SnowingTheme.sizes.handleHeight)
                        .background(colors.sheetHandle, SnowingTheme.shapes.pill),
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.union(WindowInsets.displayCutout)
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                        )
                        .padding(horizontal = spacing.sheetSide)
                        .padding(bottom = spacing.sheetBottom),
                    content = content,
                )
            }
        }
    }
}

// A 1 dp light along the top edge that fades out down the rounded corners.
@Composable
private fun topHighlight(): BorderStroke {
    val fade = with(LocalDensity.current) { SnowingTheme.sizes.sheetEdgeFade.toPx() }
    return BorderStroke(
        SnowingTheme.sizes.line,
        Brush.verticalGradient(0f to SnowingTheme.colors.sheetHighlight, 1f to Color.Transparent, endY = fade),
    )
}

/** The scene as the screen shows it, shifted to sit exactly behind the panel, and blurred. */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun BlurredBackdrop(top: () -> Float, backdrop: @Composable () -> Unit, modifier: Modifier) {
    val screen = LocalWindowInfo.current.containerSize
    val blur = with(LocalDensity.current) { SnowingTheme.sizes.sheetBlur.toPx() }
    val effect = remember(blur) { sceneBlur(blur) }
    Box(modifier.clipToBounds()) {
        Box(
            Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(Constraints.fixed(screen.width, screen.height))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeWithLayer(0, 0) { translationY = -top() }
                    }
                }
                .graphicsLayer { renderEffect = effect },
        ) { backdrop() }
    }
}

// Blur, then a little more color, like the design's backdrop-filter: blur() saturate(1.4).
@RequiresApi(Build.VERSION_CODES.S)
private fun sceneBlur(radius: Float): RenderEffect {
    val saturate = ColorMatrix().apply { setSaturation(BACKDROP_SATURATION) }
    return android.graphics.RenderEffect.createChainEffect(
        android.graphics.RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(saturate)),
        android.graphics.RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
    ).asComposeRenderEffect()
}

private const val BACKDROP_SATURATION = 1.4f

/** A small accent label and a title read together as the panel's heading. */
@Composable
fun SheetHeading(title: String, modifier: Modifier = Modifier, label: String? = null) {
    Column(modifier.semantics(mergeDescendants = true) { heading() }) {
        if (label != null) StepLabel(label, Modifier.padding(bottom = SnowingTheme.spacing.headingGap))
        PageTitle(title)
    }
}

/** A 1 dp line between parts of a panel. */
@Composable
fun Rule(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(SnowingTheme.sizes.line)
            .background(SnowingTheme.colors.line),
    )
}

/** One step of a numbered list; screen readers hear [numberDescription] (e.g. "Step 1") for the ring. */
@Composable
fun NumberedStep(number: Int, text: String, numberDescription: String, modifier: Modifier = Modifier) {
    val colors = SnowingTheme.colors
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SnowingTheme.sizes.stepRing)
                .border(SnowingTheme.sizes.ringStroke, colors.accentText, SnowingTheme.shapes.circle)
                .clearAndSetSemantics { contentDescription = numberDescription },
            contentAlignment = Alignment.Center,
        ) {
            Text(number.toString(), style = SnowingTheme.typography.stepNumber, color = colors.accentText)
        }
        Spacer(Modifier.width(SnowingTheme.spacing.inline))
        Text(text, style = SnowingTheme.typography.stepText)
    }
}

/**
 * A look-alike of a system settings row with its switch on, showing the user what to look
 * for. Decorative: screen readers skip it.
 */
@Composable
fun SettingPreview(label: String, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    val colors = SnowingTheme.colors
    val spacing = SnowingTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.well, SnowingTheme.shapes.well)
            .padding(horizontal = spacing.previewPaddingH, vertical = spacing.previewPaddingV)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(spacing.inline))
        Text(label, Modifier.weight(1f), style = SnowingTheme.typography.stepText)
        Switch(
            checked = true,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.ctaFill,
                checkedThumbColor = colors.ctaDot,
                checkedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** The app's launcher icon, small, as a system list shows it. */
@Composable
fun AppIcon(modifier: Modifier = Modifier) {
    val size = SnowingTheme.sizes.appIcon
    Box(
        modifier = modifier
            .size(size)
            .clip(SnowingTheme.shapes.appIcon)
            .background(colorResource(R.color.ic_launcher_background)),
        contentAlignment = Alignment.Center,
    ) {
        // An adaptive icon's foreground is 108 dp with the art in the middle 72 dp.
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(size * ADAPTIVE_SCALE))
    }
}

private const val ADAPTIVE_SCALE = 108f / 72f

/**
 * A row of a panel's list, under a line: an icon in a ring, a title, what it is about, and
 * [below] for its action or state.
 */
@Composable
fun ItemRow(
    title: String,
    body: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    below: @Composable ColumnScope.() -> Unit = {},
) {
    val spacing = SnowingTheme.spacing
    Column(modifier.fillMaxWidth()) {
        Rule()
        Row(Modifier.padding(top = spacing.itemRow, bottom = spacing.itemRow - spacing.s)) {
            Box(
                modifier = Modifier
                    .size(SnowingTheme.sizes.iconRing)
                    .border(SnowingTheme.sizes.ringStroke, SnowingTheme.colors.outline, SnowingTheme.shapes.circle),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Spacer(Modifier.width(spacing.m))
            Column(Modifier.weight(1f)) {
                ItemTitle(title)
                ItemBody(body, Modifier.padding(top = spacing.xs))
                below()
            }
        }
    }
}

/** A done state in place of an action: a check and a quiet word, e.g. "Added". Not clickable. */
@Composable
fun DoneNote(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.heightIn(min = SnowingTheme.sizes.touchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckGlyph(color = SnowingTheme.colors.accentText)
        Spacer(Modifier.width(SnowingTheme.spacing.s))
        ItemBody(text)
    }
}

/** A remark at the end of a panel, under a line, with an "i". */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Rule()
        Row(Modifier.padding(top = SnowingTheme.spacing.m)) {
            InfoGlyph(Modifier.padding(top = SnowingTheme.spacing.xxs), color = SnowingTheme.colors.inkFaint)
            Spacer(Modifier.width(SnowingTheme.spacing.paragraph))
            Footnote(text)
        }
    }
}
