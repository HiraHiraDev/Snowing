package com.hirahira.snowing.overlay

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Flake shapes in one bitmap, so a whole field is one draw. Each shape comes in
 * sizes halving down to [SnowSprites.MIN_CELL_PX], our own mip levels: a batched draw samples
 * through a shader, which may not use the bitmap's mipmaps. [extent] maps a radius
 * to the half-size of the drawn square: soft falloff reaches past the size the eye reads.
 */
internal class SpriteAtlas(
    val bitmap: Bitmap,
    /** Per level, largest first: the cell of each shape variant. */
    private val levels: List<Array<Rect>>,
    val extent: Float,
) {
    /** The cell for [shape] that is drawn at [sizePx] with at most a 2x downscale. */
    fun cell(shape: Float, sizePx: Float): Rect {
        var level = 0
        while (level + 1 < levels.size && levels[level + 1][0].width() >= sizePx) level++
        val cells = levels[level]
        return cells[min((shape * cells.size).toInt(), cells.size - 1)]
    }
}

internal object SnowSprites {

    /** Soft dots with a slightly ragged edge, so flakes don't look like drawn circles. */
    fun flakes(random: Random = Random.Default): SpriteAtlas = atlas(
        variants = FLAKE_VARIANTS,
        cellPx = FLAKE_CELL_PX,
        random = random,
        // Weak low harmonics keep it round; the fine ones fray the edge.
        wobble = floatArrayOf(0f, 0f, 0.03f, 0.035f, 0f, 0.03f, 0f, 0.025f, 0f, 0.02f, 0f, 0.018f),
        profile = { d -> 1f - smoothstep(0.25f, 1f, d) },
        edgeRadius = 0.44f,
        extent = 1.6f,
    )

    /** Out-of-focus flakes right in front of the lens: a heavily blurred dot. */
    fun bokeh(random: Random = Random.Default): SpriteAtlas = atlas(
        variants = BOKEH_VARIANTS,
        cellPx = BOKEH_CELL_PX,
        random = random,
        wobble = floatArrayOf(),
        profile = { d -> exp(-4.5f * d * d) },
        edgeRadius = 0.46f,
        // The eye reads the size where the glow is half bright, far inside the cell.
        extent = 2.8f,
    )

    private fun atlas(
        variants: Int,
        cellPx: Int,
        random: Random,
        wobble: FloatArray,
        profile: (Float) -> Float,
        edgeRadius: Float,
        extent: Float,
    ): SpriteAtlas {
        val center = cellPx / 2f
        // Alpha of each variant at full size, then box-filtered down level by level like mipmaps.
        var alphas = List(variants) {
            val phases = FloatArray(wobble.size) { random.nextFloat() * TWO_PI }
            FloatArray(cellPx * cellPx) { index ->
                val dx = index % cellPx + 0.5f - center
                val dy = index / cellPx + 0.5f - center
                val angle = atan2(dy, dx)
                var edge = 1f
                for (k in wobble.indices) {
                    if (wobble[k] != 0f) edge += wobble[k] * sin(k * angle + phases[k])
                }
                profile(sqrt(dx * dx + dy * dy) / (edgeRadius * cellPx * edge)).coerceIn(0f, 1f)
            }
        }
        val levelSizes = generateSequence(cellPx) { it / 2 }.takeWhile { it >= MIN_CELL_PX }.toList()

        val width = cellPx * variants
        val height = levelSizes.sum()
        val pixels = IntArray(width * height)
        var top = 0
        val levels = levelSizes.mapIndexed { level, size ->
            if (level > 0) alphas = alphas.map { halve(it, size * 2) }
            val cells = Array(variants) { v ->
                val left = v * size
                for (y in 0 until size) {
                    for (x in 0 until size) {
                        val alpha = (alphas[v][y * size + x] * 255).toInt()
                        pixels[(top + y) * width + left + x] = (alpha shl 24) or 0xFFFFFF
                    }
                }
                Rect(left, top, left + size, top + size)
            }
            top += size
            cells
        }
        return SpriteAtlas(Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888), levels, extent)
    }

    /** Averages each 2x2 block of a [size] x [size] alpha cell. */
    private fun halve(alpha: FloatArray, size: Int): FloatArray {
        val half = size / 2
        return FloatArray(half * half) { index ->
            val x = index % half * 2
            val y = index / half * 2
            (alpha[y * size + x] + alpha[y * size + x + 1] + alpha[(y + 1) * size + x] + alpha[(y + 1) * size + x + 1]) / 4f
        }
    }

    private fun smoothstep(from: Float, to: Float, x: Float): Float {
        val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private const val FLAKE_VARIANTS = 8
    private const val FLAKE_CELL_PX = 64
    private const val BOKEH_VARIANTS = 1
    private const val BOKEH_CELL_PX = 128

    /** Smallest mip level; flakes drawn smaller than this are downscaled from it. */
    private const val MIN_CELL_PX = 8
    private const val TWO_PI = (2 * PI).toFloat()
}
