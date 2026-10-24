package com.hirahira.snowing.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.hirahira.snowing.ui.theme.SnowingColors
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The shared background of the pages (design 6a, scene update 6.1): sky with the sun or moon;
 * hills with a forest and pines; the cabin with smoke from its chimney; a snowdrift with a
 * cat's paw prints. Each layer is wider than the screen and shifted at its own speed.
 *
 * Scrolling only moves the layers ([pageOffset] is read during placement), so it never redraws;
 * a new [hour] or palette does. The smoke has a layer of its own and is the only thing drawn
 * every frame, unless [smokeMoving] is off (reduced motion, battery saver): then it stands still.
 *
 * @param pageOffset current page plus the offset fraction, 0 for the first page.
 * @param smokeLean -1..1, which way and how far the wind bends the smoke, 0 straight up.
 */
@Composable
fun Landscape(
    hour: Float,
    pageOffset: () -> Float,
    modifier: Modifier = Modifier,
    smokeLean: Float = 0f,
    smokeMoving: Boolean = true,
) {
    val colors = SnowingTheme.colors
    val sky = colors.sky
    val smokeTime = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(smokeMoving) {
        if (!smokeMoving) return@LaunchedEffect
        // The clock pauses with the app; a long gap counts as one short step, so the smoke never jumps.
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                smokeTime.floatValue += ((now - last) / NANOS_PER_SECOND).coerceAtMost(MAX_SMOKE_STEP)
                last = now
            }
        }
    }
    Box(modifier.fillMaxSize()) {
        Spacer(
            Modifier
                .parallaxLayer(widthFactor = SKY_WIDTH / DESIGN_WIDTH, speed = SKY_SPEED, pageOffset)
                .drawWithCache {
                    val u = size.width / SKY_WIDTH
                    val gradient = Brush.verticalGradient(
                        0f to sky.skyTop, 0.4f to sky.skyMid, 0.72f to sky.skyLow, 1f to sky.skyHorizon,
                    )
                    val sunUp = hour in SUNRISE..SUNSET
                    val disc = if (sunUp) SUN else MOON
                    val radius = disc.size / 2 * u
                    // The disc's top in the 780 dp tall design, kept at the same distance from the bottom.
                    val top = DESIGN_HEIGHT - DISC_LOWEST + elevation(hour) * DISC_TRAVEL
                    val center = Offset((DISC_X + disc.size / 2) * u, size.height - (top - disc.size / 2) * u)
                    val glowRadius = radius + (disc.glowSpread + disc.glowBlur / 2) * u
                    val discColor = if (sunUp) colors.sunDisc else colors.moonDisc
                    val glow = glowBrush(if (sunUp) colors.sunGlow else colors.moonGlow, center, radius, glowRadius)
                    onDrawBehind {
                        drawRect(gradient)
                        drawCircle(glow, glowRadius, center)
                        drawCircle(discColor, radius, center)
                    }
                },
        )
        Spacer(
            Modifier
                .parallaxLayer(widthFactor = HILLS_WIDTH / DESIGN_WIDTH, speed = HILLS_SPEED, pageOffset)
                .drawBehind {
                    val ridges = LandscapeScene.ridges
                    inScene {
                        drawPath(ridges.hazeGround, colors.haze)
                        drawPath(ridges.hazeTrees, colors.haze)
                        drawPath(ridges.farHill, sky.hillFar)
                        drawPath(ridges.farPines, sky.hillFar)
                        drawPath(ridges.nearHill, sky.hillNear)
                        drawPath(ridges.nearPines, sky.hillNear)
                    }
                },
        )
        Spacer(
            Modifier
                .parallaxLayer(widthFactor = HILLS_WIDTH / DESIGN_WIDTH, speed = HILLS_SPEED, pageOffset)
                .drawBehind { inScene { smoke(sky.driftTop, smokeLean, smokeTime.floatValue, smokeMoving) } },
        )
        Spacer(
            Modifier
                .parallaxLayer(widthFactor = HILLS_WIDTH / DESIGN_WIDTH, speed = HILLS_SPEED, pageOffset)
                .drawBehind { inScene { cabin(colors, sky.driftTop, sky.windowGlow) } },
        )
        Spacer(
            Modifier
                .parallaxLayer(widthFactor = HILLS_WIDTH / DESIGN_WIDTH, speed = HILLS_SPEED, pageOffset)
                .drawBehind {
                    inScene {
                        drawPath(
                            LandscapeScene.drift,
                            Brush.verticalGradient(
                                listOf(sky.driftTop, sky.driftBottom),
                                startY = LandscapeScene.DRIFT_SHADE_TOP,
                                endY = LandscapeScene.DRIFT_SHADE_BOTTOM,
                            ),
                        )
                        LandscapeScene.paws.forEach { drawCircle(colors.paw, it.radius, it.center) }
                    }
                },
        )
    }
}

/** Draws [block] in the scene's design dp, scaled to the layer's width and anchored to its bottom. */
private inline fun DrawScope.inScene(crossinline block: DrawScope.() -> Unit) {
    val u = size.width / LandscapeScene.WIDTH
    withTransform({
        translate(top = size.height - LandscapeScene.HEIGHT * u)
        scale(u, u, pivot = Offset.Zero)
    }) { block() }
}

private fun DrawScope.cabin(colors: SnowingColors, snow: Color, windowLight: Float) {
    val cabin = LandscapeScene.cabin
    drawPath(cabin.walls, colors.cabin)
    cabin.logs.forEach { drawRect(colors.logs, it.topLeft, it.size) }
    drawPath(cabin.gable, colors.gable)
    drawPath(cabin.roofSnow, snow)
    cabin.windows.forEach { drawRect(colors.windowBase, it.topLeft, it.size) }
    if (windowLight > 0f) {
        cabin.windows.forEach { drawRect(colors.windowLight.copy(alpha = windowLight), it.topLeft, it.size) }
    }
    cabin.mullions.forEach { drawRect(colors.cabin, it.topLeft, it.size) }
    drawPath(cabin.sidePines, colors.cabin)
    drawPath(cabin.sidePineSnow, snow)
    drawPath(cabin.mound, snow)
}

/**
 * Puffs rise from the chimney, fast at first, grow and fade, leaning with the wind and
 * wobbling gently. [time] runs on, so the wobble never restarts with a puff's cycle.
 */
private fun DrawScope.smoke(color: Color, lean: Float, time: Float, moving: Boolean) {
    val chimney = LandscapeScene.ChimneyTop
    val wind = lean.coerceIn(-1f, 1f)
    val phases = if (moving) {
        List(SMOKE_PUFFS) { i -> (time / SMOKE_CYCLE + i.toFloat() / SMOKE_PUFFS) % 1f }
    } else {
        STILL_PUFFS
    }
    phases.forEachIndexed { i, p ->
        val rise = 1f - (1f - p).pow(1.6f)
        val x = chimney.x + rise * SMOKE_DRIFT * wind + sin(time * 0.9f + i * 1.7f) * (1f + p * 4f)
        val y = chimney.y - rise * SMOKE_RISE
        val alpha = SMOKE_ALPHA * sin(PI.toFloat() * p).pow(1.3f)
        drawCircle(color.copy(alpha = alpha), 2.6f + p * 9f, Offset(x, y))
    }
}

/** How high the sun (by day) or the moon (by night) stands, 0 at the horizon to 1. */
internal fun elevation(hour: Float): Float {
    val sinceRise = if (hour in SUNRISE..SUNSET) hour - SUNRISE else (hour + HOURS_TO_MOONRISE) % HOURS_PER_DAY
    return sin(PI * sinceRise / (SUNSET - SUNRISE)).toFloat().coerceAtLeast(0f)
}

/**
 * A layer [widthFactor] times as wide as the screen, shifted left by [speed] screen widths
 * per page. That only moves the layer, so its drawing is kept.
 */
private fun Modifier.parallaxLayer(
    widthFactor: Float,
    speed: Float,
    pageOffset: () -> Float,
) = layout { measurable, constraints ->
    val screenWidth = constraints.maxWidth
    val placeable = measurable.measure(
        Constraints.fixed((screenWidth * widthFactor).roundToInt(), constraints.maxHeight),
    )
    layout(screenWidth, constraints.maxHeight) {
        placeable.placeWithLayer(0, 0) {
            translationX = -pageOffset() * speed * screenWidth
        }
    }
}

// A CSS box-shadow glow, approximated: solid out to the shape's edge, then fading to nothing.
private fun glowBrush(color: Color, center: Offset, solidRadius: Float, radius: Float) = Brush.radialGradient(
    0f to color,
    solidRadius / radius to color,
    1f to color.copy(alpha = 0f),
    center = center,
    radius = radius,
)

private class Disc(val size: Float, val glowBlur: Float, val glowSpread: Float)

private val SUN = Disc(size = 64f, glowBlur = 90f, glowSpread = 40f)
private val MOON = Disc(size = 52f, glowBlur = 70f, glowSpread = 26f)

private const val DESIGN_WIDTH = 360f
private const val DESIGN_HEIGHT = 780f
private const val SKY_WIDTH = 504f
private const val HILLS_WIDTH = 720f
private const val SKY_SPEED = 0.2f
private const val HILLS_SPEED = 0.5f

private const val SUNRISE = 6f
private const val SUNSET = 18f
private const val HOURS_PER_DAY = 24f
private const val HOURS_TO_MOONRISE = 6f
private const val DISC_X = 330f
private const val DISC_LOWEST = 560f
private const val DISC_TRAVEL = 440f

private const val SMOKE_PUFFS = 5
private const val SMOKE_CYCLE = 7f
private const val SMOKE_DRIFT = 22f
private const val SMOKE_RISE = 70f
private const val SMOKE_ALPHA = 0.34f
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val MAX_SMOKE_STEP = 0.1f

/** With motion off: three puffs standing along the way up. */
private val STILL_PUFFS = listOf(0.2f, 0.5f, 0.8f)
