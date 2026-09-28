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
 * Flake shapes in one bitmap, so the GPU can batch a whole field. [extent] maps
 * a radius to the half-size of the drawn square: soft falloff reaches past the
 * size the eye reads.
 */
internal class SpriteAtlas(
    val bitmap: Bitmap,
    private val cells: Array<Rect>,
    val extent: Float,
) {
    fun cell(shape: Float): Rect = cells[min((shape * cells.size).toInt(), cells.size - 1)]
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
        val width = cellPx * variants
        val pixels = IntArray(width * cellPx)
        val center = cellPx / 2f
        val cells = Array(variants) { v ->
            val phases = FloatArray(wobble.size) { random.nextFloat() * TWO_PI }
            val left = v * cellPx
            for (py in 0 until cellPx) {
                for (px in 0 until cellPx) {
                    val dx = px + 0.5f - center
                    val dy = py + 0.5f - center
                    val angle = atan2(dy, dx)
                    var edge = 1f
                    for (k in wobble.indices) {
                        if (wobble[k] != 0f) edge += wobble[k] * sin(k * angle + phases[k])
                    }
                    val d = sqrt(dx * dx + dy * dy) / (edgeRadius * cellPx * edge)
                    val alpha = (profile(d).coerceIn(0f, 1f) * 255).toInt()
                    pixels[py * width + left + px] = (alpha shl 24) or 0xFFFFFF
                }
            }
            Rect(left, 0, left + cellPx, cellPx)
        }
        val bitmap = Bitmap.createBitmap(pixels, width, cellPx, Bitmap.Config.ARGB_8888)
        // Flakes are drawn far smaller than the cell; mipmaps keep the downscale smooth.
        bitmap.setHasMipMap(true)
        return SpriteAtlas(bitmap, cells, extent)
    }

    private fun smoothstep(from: Float, to: Float, x: Float): Float {
        val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private const val FLAKE_VARIANTS = 8
    private const val FLAKE_CELL_PX = 64
    private const val BOKEH_VARIANTS = 1
    private const val BOKEH_CELL_PX = 128
    private const val TWO_PI = (2 * PI).toFloat()
}
