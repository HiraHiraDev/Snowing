package com.hirahira.snowing.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Choreographer
import android.view.View
import com.hirahira.snowing.engine.SnowField
import java.util.Locale
import kotlin.math.roundToInt

/** Debug aids drawn on top of the snow. */
data class RenderOptions(
    val showHud: Boolean = false,
    val showTracer: Boolean = false,
)

/**
 * Draws a [SnowField] and drives it from vsync. dt comes from Choreographer
 * frame timestamps, so the simulation runs on real time at any refresh rate.
 */
@SuppressLint("ViewConstructor")
internal class SnowView(context: Context, private val snow: SnowField) :
    View(context), Choreographer.FrameCallback {

    var renderOptions: RenderOptions = RenderOptions()
        set(value) {
            field = value
            invalidate()
        }

    var isPaused: Boolean = false
        set(value) {
            field = value
            updateLoop()
        }

    private val choreographer = Choreographer.getInstance()
    private val density = resources.displayMetrics.density
    private val stats = FrameStats()
    private var attached = false
    private var looping = false
    private var lastFrameNanos = 0L

    private val flakePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val tracerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = TRACER_STROKE_DP * density
    }
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = HUD_TEXT_DP * density
        setShadowLayer(3f * density, 0f, 0f, Color.BLACK)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
        updateLoop()
    }

    override fun onDetachedFromWindow() {
        attached = false
        updateLoop()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        snow.resize(w, h)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!looping) return
        if (lastFrameNanos != 0L) {
            val dt = (frameTimeNanos - lastFrameNanos) / NANOS_PER_SECOND
            snow.step(dt)
            stats.record(dt)
        }
        lastFrameNanos = frameTimeNanos
        invalidate()
        choreographer.postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        for (i in 0 until snow.count) {
            flakePaint.alpha = (snow.alpha(i) * 255).roundToInt()
            canvas.drawCircle(snow.x(i), snow.y(i), snow.radius(i), flakePaint)
        }
        if (renderOptions.showTracer && snow.count > 0) {
            canvas.drawCircle(snow.x(0), snow.y(0), snow.radius(0) + TRACER_GAP_DP * density, tracerPaint)
        }
        if (renderOptions.showHud) drawHud(canvas)
    }

    private fun drawHud(canvas: Canvas) {
        val x = HUD_LEFT_DP * density
        val lineHeight = hudPaint.textSize * 1.3f
        var y = HUD_TOP_DP * density
        val lines = listOf(
            String.format(Locale.US, "fps %.0f   dt %.1f ms   worst %.1f ms", stats.fps, stats.averageMs, stats.worstMs),
            String.format(
                Locale.US,
                "flakes %d   tracer %.0f px/s",
                snow.count,
                if (snow.count > 0) snow.fallSpeed(0) else 0f,
            ),
        )
        for (line in lines) {
            canvas.drawText(line, x, y, hudPaint)
            y += lineHeight
        }
    }

    private fun updateLoop() {
        val shouldLoop = attached && !isPaused
        if (shouldLoop && !looping) {
            looping = true
            lastFrameNanos = 0L
            choreographer.postFrameCallback(this)
        } else if (!shouldLoop && looping) {
            looping = false
            choreographer.removeFrameCallback(this)
        }
    }

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000f
        const val TRACER_STROKE_DP = 2f
        const val TRACER_GAP_DP = 6f
        const val HUD_TEXT_DP = 13f
        const val HUD_LEFT_DP = 16f
        const val HUD_TOP_DP = 120f
    }
}

/** Rolling frame timing for the HUD: smoothed average plus worst frame of the last second. */
private class FrameStats {
    var averageMs = 0f
        private set
    var worstMs = 0f
        private set
    val fps: Float
        get() = if (averageMs > 0f) 1000f / averageMs else 0f

    private var windowWorstMs = 0f
    private var windowElapsedMs = 0f

    fun record(dtSeconds: Float) {
        val ms = dtSeconds * 1000f
        averageMs = if (averageMs == 0f) ms else averageMs * 0.95f + ms * 0.05f
        windowWorstMs = maxOf(windowWorstMs, ms)
        windowElapsedMs += ms
        if (windowElapsedMs >= 1000f) {
            worstMs = windowWorstMs
            windowWorstMs = 0f
            windowElapsedMs = 0f
        }
    }
}
