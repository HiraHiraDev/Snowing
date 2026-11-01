package com.hirahira.snowing.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlin.math.roundToInt

/** The filled main action of a screen or panel: a tall accent pill with a soft glow (design 6.2). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SnowingTheme.colors
    val shape = SnowingTheme.shapes.pill
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SnowingTheme.sizes.button)
            .dropShadow(shape, Shadow(radius = SnowingTheme.sizes.buttonGlow, color = colors.ctaGlow)),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = colors.ctaFill, contentColor = colors.ctaDot),
    ) {
        Text(text = text, style = SnowingTheme.typography.button)
    }
}

/**
 * The big round start / stop button with its label beside it (design 6a). Idle: a filled,
 * glowing circle with a dot. [running]: an outlined circle with a square. It sits on the
 * light snowdrift, so its ink is the drift's whatever the sky. Between the two the fill and
 * glow fade while one mark shrinks away and the other pops in, a little past its size.
 */
@Composable
fun StartStopButton(
    running: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val colors = SnowingTheme.colors
    val sizes = SnowingTheme.sizes
    // 1 idle, 0 running; the spring runs past both ends, which is the marks' pop.
    val idle = animateFloatAsState(if (running) 0f else 1f, SnowingTheme.motion.pop.spec(), label = "start stop")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(sizes.ctaGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(sizes.ctaButton)
                .glow(colors.ctaGlow, sizes.ctaGlow) { idle.value.coerceIn(0f, 1f) }
                .drawBehind {
                    val filled = idle.value.coerceIn(0f, 1f)
                    drawCircle(colors.ctaFill, alpha = filled)
                    val outline = sizes.ctaOutline.toPx()
                    drawCircle(colors.onDrift, radius = (size.minDimension - outline) / 2, style = Stroke(outline), alpha = 1f - filled)
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(sizes.ctaStop)
                    .graphicsLayer { scale(1f - idle.value) }
                    .background(colors.onDrift, SnowingTheme.shapes.tick),
            )
            Box(
                Modifier
                    .size(sizes.ctaDot)
                    .graphicsLayer { scale(idle.value) }
                    .background(colors.ctaDot, SnowingTheme.shapes.circle),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(SnowingTheme.spacing.xs)) {
            Text(text = label, style = SnowingTheme.typography.ctaLabel, color = colors.onDrift)
            if (caption != null) {
                Text(text = caption, style = SnowingTheme.typography.metaStrong, color = colors.onDriftSoft)
            }
        }
    }
}

// A CSS-like glow: [color] at the circle's edge, fading out over [radius] beyond it.
private fun Modifier.glow(color: Color, radius: Dp, strength: () -> Float) = drawBehind {
    val inner = size.minDimension / 2
    val outer = inner + radius.toPx()
    drawCircle(
        alpha = strength(),
        brush = Brush.radialGradient(
            0f to color,
            inner / outer to color,
            1f to color.copy(alpha = 0f),
            center = center,
            radius = outer,
        ),
        radius = outer,
    )
}

/** A named slider over a continuous range, with its value shown as [valueText]. */
@Composable
fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SnowingTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ControlLabel(label, Modifier.weight(1f))
            if (valueText != null) ControlValue(valueText)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

/**
 * A named slider over whole numbers, e.g. a level 1-10. Short ranges show a mark per step;
 * long ones (degrees) slide freely and round.
 */
@Composable
fun StepSlider(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    valueText: String = value.toString(),
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val count = range.count()
    SettingSlider(
        label = label,
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        modifier = modifier,
        valueText = valueText,
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = if (count <= MAX_MARKED_STEPS) count - 2 else 0,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
    )
}

private const val MAX_MARKED_STEPS = 12

@Composable
fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SnowingTheme.sizes.touchTarget)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        horizontalArrangement = Arrangement.spacedBy(SnowingTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

// A spring may run below zero; a mark that has shrunk away stays gone.
private fun GraphicsLayerScope.scale(by: Float) {
    val scale = by.coerceAtLeast(0f)
    scaleX = scale
    scaleY = scale
    alpha = if (scale > 0f) 1f else 0f
}
