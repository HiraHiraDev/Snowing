package com.hirahira.snowing.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import com.hirahira.snowing.ui.theme.SnowingTheme

// Small line icons drawn in code, on a 24 x 24 grid scaled to their size. A handful of shapes
// does not justify an icon library (lightness). They take the content color unless told.

/** A bell: notifications. */
@Composable
fun BellGlyph(modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Glyph(modifier, SnowingTheme.sizes.glyph) { drawPath(Bell, color) }
}

/** A 2 x 2 grid with one square lit: a Quick Settings tile. */
@Composable
fun TileGlyph(modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Glyph(modifier, SnowingTheme.sizes.glyph) {
        val corner = CornerRadius(1.5f)
        TILE_CELLS.forEachIndexed { i, cell ->
            drawRoundRect(color.copy(alpha = if (i == LIT_CELL) 1f else DIM), cell, Size(CELL, CELL), corner)
        }
    }
}

/** A check: something is done. */
@Composable
fun CheckGlyph(modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Glyph(modifier, SnowingTheme.sizes.checkGlyph) {
        drawPath(Check, color, style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** An "i" in a ring: a remark. */
@Composable
fun InfoGlyph(modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Glyph(modifier, SnowingTheme.sizes.infoRing) {
        drawCircle(color, radius = 10.5f, center = Offset(12f, 12f), style = Stroke(width = 1.6f))
        drawCircle(color, radius = 1.4f, center = Offset(12f, 7.6f))
        drawLine(color, Offset(12f, 10.8f), Offset(12f, 17f), strokeWidth = 2f, cap = StrokeCap.Round)
    }
}

@Composable
private fun Glyph(modifier: Modifier, size: Dp, draw: DrawScope.() -> Unit) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / GRID, pivot = Offset.Zero) { draw() }
    }
}

private const val GRID = 24f

private val Bell: Path = PathParser().parsePathString(
    "M12,22c1.1,0 2,-0.9 2,-2h-4c0,1.1 0.9,2 2,2zM18,16v-5c0,-3.07 -1.64,-5.64 -4.5,-6.32V4" +
        "c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2z",
).toPath()

private val Check: Path = Path().apply {
    moveTo(4.5f, 12.5f)
    lineTo(9.5f, 17.5f)
    lineTo(19.5f, 6.5f)
}

private const val CELL = 8f
private const val DIM = 0.45f
private const val LIT_CELL = 1
private val TILE_CELLS = listOf(Offset(3f, 3f), Offset(13f, 3f), Offset(3f, 13f), Offset(13f, 13f))
