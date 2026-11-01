package com.hirahira.snowing.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlin.math.atan2
import kotlin.math.roundToInt

// Controls of design 6a that set a value by touch. Each one swallows its own drags, so the
// page pager behind it never takes them over.

/**
 * A level picked on a ruler of marks, e.g. intensity 1-10: tap a mark or drag along it.
 * Levels up to [value] stand tall in the accent color; marks spring up and down as the level
 * changes. Ticks the phone once per level, firmer at either end.
 */
@Composable
fun LevelRuler(
    label: String,
    value: Int,
    levels: IntRange,
    valueText: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
) {
    val sizes = SnowingTheme.sizes
    val haptics = rememberControlHaptics()
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val finish by rememberUpdatedState(onValueChangeFinished)
    val count = levels.count()

    fun pick(x: Float, width: Int, dragging: Boolean) {
        val level = (levels.first + (x / width * count).toInt()).coerceIn(levels)
        if (level == current) return
        when {
            level == levels.first || level == levels.last -> haptics.limit()
            dragging -> haptics.step()
            else -> haptics.pick()
        }
        change(level)
    }

    Column(modifier.fillMaxWidth().enabledLook(enabled)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ControlLabel(label)
            ControlValue(valueText)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .overhang(sizes.rulerOverhang)
                .height(sizes.touchTarget)
                .semantics { stateDescription = valueText }
                .progressSemantics(value.toFloat(), levels.first.toFloat()..levels.last.toFloat(), steps = count - 2)
                .semantics {
                    if (enabled) {
                        setProgress { target ->
                            change(target.roundToInt().coerceIn(levels))
                            finish()
                            true
                        }
                    }
                }
                .then(
                    if (!enabled) {
                        Modifier
                    } else {
                        Modifier
                            .pointerInput(levels) {
                                detectTapGestures { pick(it.x, size.width, dragging = false); finish() }
                            }
                            .pointerInput(levels) {
                                detectHorizontalDragGestures(
                                    onDragStart = { pick(it.x, size.width, dragging = true) },
                                    onDragEnd = { finish() },
                                    onDragCancel = { finish() },
                                ) { change, _ ->
                                    change.consume()
                                    pick(change.position.x, size.width, dragging = true)
                                }
                            }
                    },
                ),
        ) {
            for (level in levels) {
                RulerMark(lit = level <= value, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * A direction picked on a round dial: drag anywhere on it and the needle follows with a little
 * weight, swinging past and back when the finger stops. [angle] is in degrees clockwise from
 * up. Ticks the phone each time the needle passes an eighth of a turn, firmer at the four
 * marked directions.
 */
@Composable
fun WindDial(
    angle: Int,
    onAngleChange: (Int) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onAngleChangeFinished: () -> Unit = {},
) {
    val colors = SnowingTheme.colors
    val sizes = SnowingTheme.sizes
    val haptics = rememberControlHaptics()
    val current by rememberUpdatedState(angle)
    val change by rememberUpdatedState(onAngleChange)
    val finish by rememberUpdatedState(onAngleChangeFinished)
    val unwrapper = remember { AngleUnwrapper(angle) }
    val needle = animateFloatAsState(unwrapper.follow(angle), SnowingTheme.motion.needle.spec(), label = "needle")

    fun point(at: Offset, size: Size) {
        val dx = at.x - size.width / 2
        val dy = at.y - size.height / 2
        val degrees = Math.toDegrees(atan2(dx, -dy).toDouble()).roundToInt().mod(FULL_TURN)
        if (degrees / SNAP != current / SNAP) {
            // The step just passed is the one nearest the finger.
            val passed = (degrees.toFloat() / SNAP).roundToInt() * SNAP
            if (passed % QUARTER_TURN.toInt() == 0) haptics.notch() else haptics.step()
        }
        change(degrees)
    }

    Canvas(
        modifier = modifier
            .size(sizes.dial)
            .enabledLook(enabled)
            .semantics { this.contentDescription = contentDescription }
            .progressSemantics(angle.toFloat(), 0f..(FULL_TURN - SNAP).toFloat(), steps = FULL_TURN / SNAP - 2)
            .semantics {
                // TalkBack moves the needle an eighth of a turn at a time.
                if (enabled) {
                    setProgress { target ->
                        change((target / SNAP).roundToInt() * SNAP % FULL_TURN)
                        finish()
                        true
                    }
                }
            }
            .then(
                if (!enabled) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {
                        val area = Size(size.width.toFloat(), size.height.toFloat())
                        detectDragGestures(
                            onDragStart = { point(it, area) },
                            onDragEnd = { finish() },
                            onDragCancel = { finish() },
                        ) { change, _ ->
                            change.consume()
                            point(change.position, area)
                        }
                    }
                },
            ),
    ) {
        val ring = sizes.dialRing.toPx()
        val radius = size.minDimension / 2
        drawCircle(colors.tickDim, radius = radius - ring / 2, style = Stroke(ring))
        val tickStart = sizes.dialTickInset.toPx()
        val tickEnd = tickStart + sizes.dialTick.toPx()
        for (quarter in 0 until 4) {
            rotate(quarter * QUARTER_TURN) {
                drawLine(colors.tickDim, Offset(center.x, tickStart), Offset(center.x, tickEnd), strokeWidth = ring)
            }
        }
        rotate(needle.value) {
            val tip = Offset(center.x, center.y - sizes.needleLength.toPx())
            drawLine(colors.accentFill, center, tip, strokeWidth = sizes.needleWidth.toPx(), cap = StrokeCap.Round)
            drawCircle(colors.accentFill, radius = sizes.needleHead.toPx() / 2, center = tip)
        }
        drawCircle(colors.ink, radius = sizes.dialHub.toPx() / 2)
    }
}

/** An arrow pointing [angle] degrees clockwise from up, e.g. where the wind blows. It turns the short way. */
@Composable
fun DirectionArrow(angle: Int, modifier: Modifier = Modifier, size: Dp = SnowingTheme.sizes.directionArrow) {
    val color = SnowingTheme.colors.ink
    val stroke = SnowingTheme.sizes.needleWidth
    val unwrapper = remember { AngleUnwrapper(angle) }
    val turned = animateFloatAsState(unwrapper.follow(angle), SnowingTheme.motion.glide.spec(), label = "arrow")
    Canvas(modifier.size(size)) {
        val width = stroke.toPx()
        val head = this.size.minDimension * ARROW_HEAD
        rotate(turned.value) {
            val top = Offset(center.x, width)
            drawLine(color, Offset(center.x, this.size.height - width), top, strokeWidth = width, cap = StrokeCap.Round)
            drawLine(color, top, Offset(center.x - head, top.y + head), strokeWidth = width, cap = StrokeCap.Round)
            drawLine(color, top, Offset(center.x + head, top.y + head), strokeWidth = width, cap = StrokeCap.Round)
        }
    }
}

/** One level's mark: it springs to its lit or dim size and fades between the two colors. */
@Composable
private fun RulerMark(lit: Boolean, modifier: Modifier) {
    val colors = SnowingTheme.colors
    val sizes = SnowingTheme.sizes
    val motion = SnowingTheme.motion
    val shape = SnowingTheme.shapes.tick
    val width = animateDpAsState(if (lit) sizes.rulerLitWidth else sizes.rulerDimWidth, motion.pop.spec(), label = "mark width")
    val height = animateDpAsState(if (lit) sizes.rulerLitHeight else sizes.rulerDimHeight, motion.pop.spec(), label = "mark height")
    val color = animateColorAsState(if (lit) colors.accentFill else colors.tickDim, motion.tint(), label = "mark color")
    Box(
        modifier
            .fillMaxHeight()
            .padding(bottom = sizes.rulerBottomPadding)
            .drawBehind {
                val mark = Size(width.value.toPx(), height.value.toPx())
                translate(left = (size.width - mark.width) / 2, top = size.height - mark.height) {
                    drawOutline(shape.createOutline(mark, layoutDirection, this), color.value)
                }
            },
    )
}

/** Lets a full-width row reach [by] past both sides of its parent. */
private fun Modifier.overhang(by: Dp) = layout { measurable, constraints ->
    val extra = by.roundToPx() * 2
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.minWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-by.roundToPx(), 0) }
}

private const val FULL_TURN = 360
private const val SNAP = 45
private const val QUARTER_TURN = 90f
private const val ARROW_HEAD = 0.3f
