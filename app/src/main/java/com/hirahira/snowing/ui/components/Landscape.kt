package com.hirahira.snowing.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The shared background of the pages (design 6a): sky with the sun or moon, hills with a
 * house, and a snowdrift, each layer wider than the screen and shifted at its own speed.
 * Shapes are the design's placeholders; layers and palettes are final.
 *
 * Scrolling only moves the layers ([pageOffset] is read during placement), so it never redraws;
 * a new [hour] or palette does.
 *
 * @param pageOffset current page plus the offset fraction, 0 for the first page.
 * @param driftDrop 0..1, how far the drift sinks to make room for content above it.
 */
@Composable
fun Landscape(
    hour: Float,
    pageOffset: () -> Float,
    modifier: Modifier = Modifier,
    driftDrop: () -> Float = { 0f },
) {
    val colors = SnowingTheme.colors
    val sky = colors.sky
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
                .drawWithCache {
                    val u = size.width / HILLS_WIDTH
                    val house = shape(HOUSE_X, HOUSE_BOTTOM, HOUSE_W, HOUSE_H, u)
                    val windowGlow = colors.windowLight.copy(alpha = WINDOW_GLOW_ALPHA * sky.windowGlow)
                    val window = colors.windowLight.copy(alpha = sky.windowGlow)
                    val windowSize = Size(WINDOW * u, WINDOW * u)
                    val windows = WINDOW_XS.map { x -> house.topLeft + Offset(x * u, WINDOW_Y * u) }
                    val windowGlowRadius = (WINDOW / 2 + WINDOW_GLOW_SPREAD + WINDOW_GLOW_BLUR / 2) * u
                    val glows = windows.map { topLeft ->
                        val center = topLeft + Offset(windowSize.width / 2, windowSize.height / 2)
                        center to glowBrush(windowGlow, center, windowSize.width / 2, windowGlowRadius)
                    }
                    val far = shape(FAR_HILL_X, FAR_HILL_BOTTOM, FAR_HILL_W, FAR_HILL_H, u)
                    val near = shape(NEAR_HILL_X, NEAR_HILL_BOTTOM, NEAR_HILL_W, NEAR_HILL_H, u)
                    onDrawBehind {
                        drawOval(sky.hillFar, far.topLeft, far.size)
                        drawOval(sky.hillNear, near.topLeft, near.size)
                        drawRect(colors.house, house.topLeft, house.size)
                        if (sky.windowGlow > 0f) {
                            glows.forEach { (center, brush) -> drawCircle(brush, windowGlowRadius, center) }
                            windows.forEach { drawRect(window, it, windowSize) }
                        }
                    }
                },
        )
        Spacer(
            Modifier
                .parallaxLayer(widthFactor = HILLS_WIDTH / DESIGN_WIDTH, speed = HILLS_SPEED, pageOffset) {
                    driftDrop().coerceIn(0f, 1f) * DRIFT_DROP
                }
                .drawWithCache {
                    val u = size.width / HILLS_WIDTH
                    val drift = shape(DRIFT_X, DRIFT_BOTTOM, DRIFT_W, DRIFT_H, u)
                    val gradient = Brush.verticalGradient(
                        listOf(sky.driftTop, sky.driftBottom),
                        startY = drift.topLeft.y,
                        endY = drift.topLeft.y + drift.size.height,
                    )
                    onDrawBehind { drawOval(gradient, drift.topLeft, drift.size) }
                },
        )
    }
}

/** How high the sun (by day) or the moon (by night) stands, 0 at the horizon to 1. */
internal fun elevation(hour: Float): Float {
    val sinceRise = if (hour in SUNRISE..SUNSET) hour - SUNRISE else (hour + HOURS_TO_MOONRISE) % HOURS_PER_DAY
    return sin(PI * sinceRise / (SUNSET - SUNRISE)).toFloat().coerceAtLeast(0f)
}

/**
 * A layer [widthFactor] times as wide as the screen, shifted left by [speed] screen widths
 * per page and down by [drop] design dp. Both only move the layer, so its drawing is kept.
 */
private fun Modifier.parallaxLayer(
    widthFactor: Float,
    speed: Float,
    pageOffset: () -> Float,
    drop: () -> Float = { 0f },
) = layout { measurable, constraints ->
    val screenWidth = constraints.maxWidth
    val placeable = measurable.measure(
        Constraints.fixed((screenWidth * widthFactor).roundToInt(), constraints.maxHeight),
    )
    layout(screenWidth, constraints.maxHeight) {
        placeable.placeWithLayer(0, 0) {
            translationX = -pageOffset() * speed * screenWidth
            translationY = drop() * screenWidth / DESIGN_WIDTH
        }
    }
}

private class Shape(val topLeft: Offset, val size: Size)

// Design coordinates are dp in a 360 x 780 frame, measured from the bottom; [u] scales them to the screen width.
private fun CacheDrawScope.shape(x: Float, bottom: Float, width: Float, height: Float, u: Float) =
    Shape(Offset(x * u, size.height - (bottom + height) * u), Size(width * u, height * u))

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

private const val FAR_HILL_X = -100f
private const val FAR_HILL_BOTTOM = 120f
private const val FAR_HILL_W = 520f
private const val FAR_HILL_H = 220f
private const val NEAR_HILL_X = 340f
private const val NEAR_HILL_BOTTOM = 110f
private const val NEAR_HILL_W = 560f
private const val NEAR_HILL_H = 240f
private const val HOUSE_X = 470f
private const val HOUSE_BOTTOM = 196f
private const val HOUSE_W = 64f
private const val HOUSE_H = 44f
private const val WINDOW = 14f
private const val WINDOW_Y = 14f
private val WINDOW_XS = listOf(10f, 38f)
private const val WINDOW_GLOW_BLUR = 18f
private const val WINDOW_GLOW_SPREAD = 4f
private const val WINDOW_GLOW_ALPHA = 0.6f
private const val DRIFT_X = -60f
private const val DRIFT_BOTTOM = -60f
private const val DRIFT_W = 860f
private const val DRIFT_H = 300f

/** How far the drift sinks on the Stats page: from bottom -60 to -130 in the design. */
private const val DRIFT_DROP = 70f
