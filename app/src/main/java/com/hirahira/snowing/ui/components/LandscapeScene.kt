package com.hirahira.snowing.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// Scene update 6.1: forest, pines, the cabin, the snow mound and paw prints. Coordinates are
// dp of the hills layer as the design draws it, 720 x 780 with the origin top left; the
// Landscape scales them to the screen. Built once (a fixed seed keeps the forest the same
// on every launch) and only recolored when drawn.
//
// Ours on top of 6.1 (owner, 2026-10-18): rolling hills and a rolling drift instead of the
// design's ellipses, running the layer's full width, so none ends in a cut-off slope at a
// screen edge; and the cabin group moved left, clear of the Snow page's right edge.

/** Everything drawn in the hills layer behind the cabin, one path per color. */
internal class Ridges(
    val hazeGround: Path,
    val hazeTrees: Path,
    val farHill: Path,
    val farPines: Path,
    val nearHill: Path,
    val nearPines: Path,
)

internal class Cabin(
    val walls: Path,
    val logs: List<Rect>,
    val gable: Path,
    val roofSnow: Path,
    val windows: List<Rect>,
    val mullions: List<Rect>,
    val sidePines: Path,
    val sidePineSnow: Path,
    val mound: Path,
)

internal class PawPrint(val center: Offset, val radius: Float)

internal object LandscapeScene {
    const val WIDTH = 720f
    const val HEIGHT = 780f

    /** The snowdrift in front, and the band its light-to-dark gradient spans. */
    val drift: Path by lazy { hill(::driftRidge) }
    const val DRIFT_SHADE_TOP = 540f
    const val DRIFT_SHADE_BOTTOM = 840f

    /** Where the smoke leaves the chimney. */
    val ChimneyTop = Offset(521f + CABIN_SHIFT, 482f)

    val ridges: Ridges by lazy { buildRidges() }
    val cabin: Cabin by lazy { buildCabin() }
    val paws: List<PawPrint> by lazy { buildPaws() }
}

// The ridges: a few soft humps and a little unevenness. The far one peaks on the Stats page
// and dips in the middle, where the haze forest shows; the near one stays low on the left
// and rises behind the cabin into the Moods page.
private fun farRidge(x: Float) = 548f - 98f * hump(x, 135f, 120f) - 40f * hump(x, 300f, 62f) -
    62f * hump(x, 575f, 105f) - 22f * hump(x, 700f, 45f) + 3f * sin(x * 0.05f) + 2f * sin(x * 0.13f)

private fun nearRidge(x: Float) = 602f - 18f * hump(x, 250f, 90f) - 34f * hump(x, 470f, 55f) -
    158f * hump(x, 655f, 150f) + 3f * sin(x * 0.04f + 1f) + 1.5f * sin(x * 0.12f)

// The drift's crest is highest under the cabin, which stands on it, and stays low and calm
// on the Stats and Moods pages.
private fun driftRidge(x: Float) = 576f - 30f * hump(x, 470f, 170f) - 8f * hump(x, 120f, 90f) +
    4f * sin(x * 0.021f + 0.6f) + 2f * sin(x * 0.07f)

private fun hump(x: Float, center: Float, width: Float): Float {
    val d = (x - center) / width
    return exp(-d * d)
}

/** The ground below [ridge], across the whole layer and a little past its edges. */
private fun hill(ridge: (Float) -> Float) = Path().apply {
    moveTo(LEFT_EDGE, BOTTOM_EDGE)
    var x = LEFT_EDGE
    while (x <= RIGHT_EDGE) {
        lineTo(x, ridge(x))
        x += RIDGE_STEP
    }
    lineTo(RIGHT_EDGE, BOTTOM_EDGE)
    close()
}

private fun buildRidges(): Ridges {
    val ground = Path().apply {
        moveTo(LEFT_EDGE, hazeY(LEFT_EDGE))
        var x = 0f
        while (x <= RIGHT_EDGE) {
            lineTo(x, hazeY(x))
            x += 10f
        }
        lineTo(RIGHT_EDGE, BOTTOM_EDGE)
        lineTo(LEFT_EDGE, BOTTOM_EDGE)
        close()
    }
    val hazeTrees = Path()
    Random(HAZE_SEED).let { rnd ->
        var x = -8f
        while (x <= RIGHT_EDGE) {
            val h = 16f + rnd.nextFloat() * 22f
            hazeTrees.pine(x, 531f + 6f * sin(x * 0.02f), h, h * PINE_WIDTH)
            x += 4f + rnd.nextFloat() * 7f
        }
    }
    val farPines = Path()
    Random(FAR_SEED).let { rnd ->
        var x = LEFT_EDGE
        while (x <= RIGHT_EDGE) {
            val h = 18f + rnd.nextFloat() * 18f
            if (rnd.nextFloat() < 0.8f) farPines.pine(x, farRidge(x) + 4f, h, h * PINE_WIDTH)
            x += 6f + rnd.nextFloat() * 9f
        }
    }
    val nearPines = Path()
    Random(NEAR_SEED).let { rnd ->
        var x = 330f
        while (x <= RIGHT_EDGE) {
            val h = 20f + rnd.nextFloat() * 20f
            if (rnd.nextFloat() < 0.7f) nearPines.pine(x, nearRidge(x) + 5f, h, h * PINE_WIDTH)
            x += 8f + rnd.nextFloat() * 14f
        }
    }
    return Ridges(ground, hazeTrees, hill(::farRidge), farPines, hill(::nearRidge), nearPines)
}

private fun hazeY(x: Float) = 528f + 6f * sin(x * 0.02f) + 3f * sin(x * 0.07f)

private fun buildCabin(): Cabin {
    val walls = Path().apply {
        addRect(Rect(470f, 512f, 530f, 548f))
        addRect(Rect(517f, 484f, 525f, 506f))
    }
    val gable = polygon(listOf(Offset(466f, 516f), Offset(500f, 488f), Offset(534f, 516f)))
    val roofSnow = polygon(
        listOf(
            Offset(459f, 517f), Offset(500f, 482f), Offset(541f, 517f),
            Offset(537f, 521f), Offset(500f, 490f), Offset(463f, 521f),
        ),
    ).apply { addRect(Rect(515.5f, 481f, 526.5f, 485f)) }
    val sidePines = Path()
    val sidePineSnow = Path()
    sidePines.pine(446f, 554f, 62f, 17f, sidePineSnow)
    sidePines.pine(560f, 556f, 84f, 22f, sidePineSnow)
    sidePines.pine(584f, 558f, 52f, 14f, sidePineSnow)
    val mound = Path().apply {
        moveTo(440f, 562f)
        quadraticTo(470f, 552f, 500f, 552f)
        quadraticTo(540f, 549f, 596f, 562f)
        lineTo(596f, 610f)
        lineTo(440f, 610f)
        close()
    }
    val shift = Offset(CABIN_SHIFT, 0f)
    listOf(walls, gable, roofSnow, sidePines, sidePineSnow, mound).forEach { it.translate(shift) }
    return Cabin(
        walls = walls,
        logs = listOf(521f, 530f, 539f).map { Rect(470f, it, 530f, it + 1.2f).translate(shift) },
        gable = gable,
        roofSnow = roofSnow,
        windows = listOf(Rect(478f, 522f, 492f, 535f), Rect(507f, 522f, 521f, 535f)).map { it.translate(shift) },
        mullions = listOf(
            Rect(484.4f, 522f, 485.8f, 535f), Rect(513.4f, 522f, 514.8f, 535f),
            Rect(478f, 527.8f, 492f, 529.2f), Rect(507f, 527.8f, 521f, 529.2f),
        ).map { it.translate(shift) },
        sidePines = sidePines,
        sidePineSnow = sidePineSnow,
        mound = mound,
    )
}

/** A cat's trail across the drift to the cabin, prints smaller farther away and squashed by the ground's perspective. */
private fun buildPaws(): List<PawPrint> {
    // The trail starts in the foreground and ends at the (moved) cabin.
    val p0 = Offset(392f, 730f)
    val p1 = Offset(580f + CABIN_SHIFT, 640f)
    val p2 = Offset(498f + CABIN_SHIFT, 556f)
    return (0 until PAW_PRINTS).flatMap { i ->
        val t = i / (PAW_PRINTS - 1f)
        val a = 1 - t
        val at = p0 * (a * a) + p1 * (2 * a * t) + p2 * (t * t)
        val tangent = (p1 - p0) * (2 * a) + (p2 - p1) * (2 * t)
        val ux = tangent.x / hypot(tangent.x, tangent.y)
        val uy = tangent.y / hypot(tangent.x, tangent.y)
        val s = 1.25f - 0.7f * t
        val side = if (i % 2 == 0) 1f else -1f
        val px = at.x - uy * side * 3.2f * s
        val py = at.y + ux * side * 3.2f * s * PERSPECTIVE
        val toes = (-1..1).map { k ->
            PawPrint(
                Offset(
                    px + ux * 2.4f * s - uy * k * 1.3f * s,
                    py + (uy * 2.4f * s + ux * k * 1.3f * s) * PERSPECTIVE,
                ),
                0.62f * s,
            )
        }
        listOf(PawPrint(Offset(px, py), 1.5f * s)) + toes
    }
}

/**
 * A pine of three stacked tiers and a trunk, its base at ([x], [baseY]). With [snow], each
 * tier also gets a snow tip there. Every part winds clockwise, so overlaps in one path add
 * up instead of cancelling out.
 */
private fun Path.pine(x: Float, baseY: Float, h: Float, w: Float, snow: Path? = null) {
    for (k in 0..2) {
        val top = baseY - h * (1 - k * 0.27f)
        val bottom = top + h * 0.42f
        val half = w * (0.45f + 0.275f * k)
        moveTo(x, top)
        lineTo(x + half, bottom)
        lineTo(x - half, bottom)
        close()
        snow?.apply {
            fun at(v: Float) = top + (bottom - top) * v
            moveTo(x, top)
            lineTo(x + half * 0.62f, at(0.62f))
            lineTo(x + half * 0.2f, at(0.5f))
            lineTo(x, at(0.64f))
            lineTo(x - half * 0.24f, at(0.48f))
            lineTo(x - half * 0.62f, at(0.62f))
            close()
        }
    }
    val left = x - w * 0.07f
    val right = x + w * 0.07f
    val top = baseY - h * 0.06f
    val bottom = top + h * 0.1f
    moveTo(left, top)
    lineTo(right, top)
    lineTo(right, bottom)
    lineTo(left, bottom)
    close()
}

private fun polygon(points: List<Offset>) = Path().apply {
    moveTo(points[0].x, points[0].y)
    points.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

/** The cabin group moved left from the design's x, clear of the Snow page's right edge. */
private const val CABIN_SHIFT = -16f

// The layer drawn a little past its edges, so nothing ends at a screen border.
private const val LEFT_EDGE = -10f
private const val RIGHT_EDGE = 730f
private const val BOTTOM_EDGE = 790f
private const val RIDGE_STEP = 4f

private const val PINE_WIDTH = 0.28f
private const val PAW_PRINTS = 16

/** Squashes the prints vertically: they lie on the ground. */
private const val PERSPECTIVE = 0.6f

private const val HAZE_SEED = 61
private const val FAR_SEED = 62
private const val NEAR_SEED = 63
