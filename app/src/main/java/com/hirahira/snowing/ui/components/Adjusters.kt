package com.hirahira.snowing.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
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
 * Levels up to [value] stand tall in the accent color. Ticks the phone once per level.
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
    val colors = SnowingTheme.colors
    val sizes = SnowingTheme.sizes
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val finish by rememberUpdatedState(onValueChangeFinished)
    val count = levels.count()

    fun pick(x: Float, width: Int) {
        val level = (levels.first + (x / width * count).toInt()).coerceIn(levels)
        if (level != current) {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            change(level)
        }
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
                                detectTapGestures { pick(it.x, size.width); finish() }
                            }
                            .pointerInput(levels) {
                                detectHorizontalDragGestures(
                                    onDragStart = { pick(it.x, size.width) },
                                    onDragEnd = { finish() },
                                    onDragCancel = { finish() },
                                ) { change, _ ->
                                    change.consume()
                                    pick(change.position.x, size.width)
                                }
                            }
                    },
                ),
        ) {
            for (level in levels) {
                val lit = level <= value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(sizes.touchTarget)
                        .padding(bottom = sizes.rulerBottomPadding),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .size(
                                if (lit) sizes.rulerLitWidth else sizes.rulerDimWidth,
                                if (lit) sizes.rulerLitHeight else sizes.rulerDimHeight,
                            )
                            .background(if (lit) colors.accentFill else colors.tickDim, SnowingTheme.shapes.tick),
                    )
                }
            }
        }
    }
}

/**
 * A direction picked on a round dial: drag anywhere on it and the needle follows. [angle] is
 * in degrees clockwise from up. Ticks the phone each time the needle passes an eighth of a turn.
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
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(angle)
    val change by rememberUpdatedState(onAngleChange)
    val finish by rememberUpdatedState(onAngleChangeFinished)

    fun point(at: Offset, size: Size) {
        val dx = at.x - size.width / 2
        val dy = at.y - size.height / 2
        val degrees = Math.toDegrees(atan2(dx, -dy).toDouble()).roundToInt().mod(FULL_TURN)
        if (degrees / SNAP != current / SNAP) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
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
        rotate(angle.toFloat()) {
            val tip = Offset(center.x, center.y - sizes.needleLength.toPx())
            drawLine(colors.accentFill, center, tip, strokeWidth = sizes.needleWidth.toPx(), cap = StrokeCap.Round)
            drawCircle(colors.accentFill, radius = sizes.needleHead.toPx() / 2, center = tip)
        }
        drawCircle(colors.ink, radius = sizes.dialHub.toPx() / 2)
    }
}

/** An arrow pointing [angle] degrees clockwise from up, e.g. where the wind blows. */
@Composable
fun DirectionArrow(angle: Int, modifier: Modifier = Modifier, size: Dp = SnowingTheme.sizes.directionArrow) {
    val color = SnowingTheme.colors.ink
    val stroke = SnowingTheme.sizes.needleWidth
    Canvas(modifier.size(size)) {
        val width = stroke.toPx()
        val head = this.size.minDimension * ARROW_HEAD
        rotate(angle.toFloat()) {
            val top = Offset(center.x, width)
            drawLine(color, Offset(center.x, this.size.height - width), top, strokeWidth = width, cap = StrokeCap.Round)
            drawLine(color, top, Offset(center.x - head, top.y + head), strokeWidth = width, cap = StrokeCap.Round)
            drawLine(color, top, Offset(center.x + head, top.y + head), strokeWidth = width, cap = StrokeCap.Round)
        }
    }
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
