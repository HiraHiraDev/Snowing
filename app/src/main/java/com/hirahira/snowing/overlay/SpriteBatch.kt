package com.hirahira.snowing.overlay

import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import com.hirahira.snowing.engine.SnowField
import kotlin.math.roundToInt

/**
 * Draws a whole [SnowField] from [atlas] as one mesh of textured quads. One
 * drawBitmap per flake cost a JNI call, a recorded op and a GPU op each, which
 * was most of the frame. Hardware canvases draw vertices only from API 29; below
 * that it falls back to a drawBitmap per flake.
 */
internal class SpriteBatch(private val atlas: SpriteAtlas) {

    private val meshPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        shader = BitmapShader(atlas.bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val bounds = RectF()

    private var verts = FloatArray(0)
    private var texs = FloatArray(0)
    private var colors = IntArray(0)
    private var indices = ShortArray(0)

    fun draw(canvas: Canvas, field: SnowField, bottom: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !canvas.isHardwareAccelerated) {
            drawMesh(canvas, field, bottom)
        } else {
            drawEach(canvas, field, bottom)
        }
    }

    private fun drawMesh(canvas: Canvas, field: SnowField, bottom: Float) {
        ensureCapacity(field.count)
        var quads = 0
        for (i in 0 until field.count) {
            val half = field.radius(i) * atlas.extent
            val y = field.y(i)
            // Most of the waiting band sits above the screen; skip what cannot be seen.
            if (y + half < 0f || y - half > bottom) continue
            val x = field.x(i)
            val cell = atlas.cell(field.shape(i), 2 * half)
            val v = quads * 8
            putQuad(verts, v, x - half, y - half, x + half, y + half)
            putQuad(texs, v, cell.left.toFloat(), cell.top.toFloat(), cell.right.toFloat(), cell.bottom.toFloat())
            // The mesh multiplies the vertex colour with the sprite: white keeps it, alpha fades it.
            val color = ((field.alpha(i) * 255).roundToInt() shl 24) or 0xFFFFFF
            colors.fill(color, quads * 4, quads * 4 + 4)
            quads++
        }
        if (quads == 0) return
        canvas.drawVertices(
            Canvas.VertexMode.TRIANGLES, quads * 8, verts, 0, texs, 0, colors, 0, indices, 0, quads * 6, meshPaint,
        )
    }

    private fun drawEach(canvas: Canvas, field: SnowField, bottom: Float) {
        for (i in 0 until field.count) {
            val half = field.radius(i) * atlas.extent
            val y = field.y(i)
            if (y + half < 0f || y - half > bottom) continue
            val x = field.x(i)
            bounds.set(x - half, y - half, x + half, y + half)
            bitmapPaint.alpha = (field.alpha(i) * 255).roundToInt()
            canvas.drawBitmap(atlas.bitmap, atlas.cell(field.shape(i), 2 * half), bounds, bitmapPaint)
        }
    }

    // Corners clockwise from top-left, matching the two triangles in [indices].
    private fun putQuad(into: FloatArray, at: Int, left: Float, top: Float, right: Float, bottom: Float) {
        into[at] = left
        into[at + 1] = top
        into[at + 2] = right
        into[at + 3] = top
        into[at + 4] = right
        into[at + 5] = bottom
        into[at + 6] = left
        into[at + 7] = bottom
    }

    private fun ensureCapacity(flakes: Int) {
        if (flakes * 4 <= colors.size) return
        verts = FloatArray(flakes * 8)
        texs = FloatArray(flakes * 8)
        colors = IntArray(flakes * 4)
        indices = ShortArray(flakes * 6) { index ->
            val corner = (index / 6) * 4
            (corner + QUAD_CORNERS[index % 6]).toShort()
        }
    }

    private companion object {
        val QUAD_CORNERS = intArrayOf(0, 1, 2, 0, 2, 3)
    }
}
