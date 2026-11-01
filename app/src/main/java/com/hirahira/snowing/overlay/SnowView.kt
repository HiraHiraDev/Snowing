package com.hirahira.snowing.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.Choreographer
import android.view.View
import android.view.WindowInsets
import androidx.core.view.WindowInsetsCompat
import com.hirahira.snowing.engine.FramePacer
import com.hirahira.snowing.engine.QualityPolicy
import com.hirahira.snowing.engine.SnowField
import java.util.Locale

/** Debug aids drawn on top of the snow. */
data class RenderOptions(
    val showHud: Boolean = false,
    val showTracer: Boolean = false,
)

/**
 * Draws the snow and the bokeh in front of it; dt comes from Choreographer frame
 * timestamps. Draws only on the vsyncs [FramePacer] picks and reports each drawn
 * frame to [quality] (ADR-0008).
 */
@SuppressLint("ViewConstructor")
internal class SnowView(
    context: Context,
    private val snow: SnowField,
    private val foreground: SnowField,
    private val quality: QualityPolicy,
) : View(context), Choreographer.FrameCallback {

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

    var refreshRate: Float
        get() = pacer.refreshRate
        set(value) {
            pacer.refreshRate = value
        }

    var fpsCap: Int
        get() = pacer.fpsCap
        set(value) {
            pacer.fpsCap = value
            // Lets the display drop to a lower refresh rate when nothing else needs it.
            // Below API 35 a plain View has no public way to vote; the pacer still skips frames.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) setRequestedFrameRate(value.toFloat())
        }

    /** Called once when both fields have stopped falling and are empty. */
    var onDrained: (() -> Unit)? = null
    private var drainedReported = false

    private val choreographer = Choreographer.getInstance()
    private val density = resources.displayMetrics.density
    private val stats = FrameStats()
    private var hudInsetLeft = 0
    private var hudInsetTop = 0
    private val pacer = FramePacer()
    private var attached = false
    private var looping = false
    private var lastFrameNanos = 0L

    private val flakeBatch = SpriteBatch(SnowSprites.flakes())
    private val bokehBatch = SpriteBatch(SnowSprites.bokeh())
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

    /** The window spans the whole display (ADR-0001); the HUD keeps clear of system bars and the cutout. */
    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val safe = WindowInsetsCompat.toWindowInsetsCompat(insets, this)
            .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        hudInsetLeft = safe.left
        hudInsetTop = safe.top
        return super.onApplyWindowInsets(insets)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        snow.resize(w, h)
        foreground.resize(w, h)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!looping) return
        choreographer.postFrameCallback(this)
        if (!pacer.shouldDraw(frameTimeNanos)) return
        if (lastFrameNanos != 0L) {
            val dt = (frameTimeNanos - lastFrameNanos) / NANOS_PER_SECOND
            snow.step(dt)
            foreground.step(dt)
            stats.record(dt)
            // May change the level or pause the loop; the step above is already done.
            quality.onFrame(dt, pacer.targetIntervalSeconds)
            reportDrained()
        }
        lastFrameNanos = frameTimeNanos
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        flakeBatch.draw(canvas, snow, height.toFloat())
        bokehBatch.draw(canvas, foreground, height.toFloat())
        if (renderOptions.showTracer && snow.count > 0) {
            canvas.drawCircle(snow.x(0), snow.y(0), snow.radius(0) + TRACER_GAP_DP * density, tracerPaint)
        }
        if (renderOptions.showHud) drawHud(canvas)
    }

    fun resetDrained() {
        drainedReported = false
    }

    private fun reportDrained() {
        if (drainedReported || snow.isFalling || !snow.isEmpty || !foreground.isEmpty) return
        drainedReported = true
        onDrained?.invoke()
    }

    private fun drawHud(canvas: Canvas) {
        val x = hudInsetLeft + HUD_MARGIN_DP * density
        val lineHeight = hudPaint.textSize * 1.3f
        // drawText takes the baseline, so the first line starts one ascent below the margin.
        var y = hudInsetTop + HUD_MARGIN_DP * density - hudPaint.ascent()
        val lines = listOf(
            String.format(Locale.US, "fps %.0f   dt %.1f ms   worst %.1f ms", stats.fps, stats.averageMs, stats.worstMs),
            String.format(
                Locale.US,
                "flakes %d + %d bokeh   tracer %.0f px/s",
                snow.count,
                foreground.count,
                if (snow.count > 0) snow.fallSpeed(0) else 0f,
            ),
            String.format(
                Locale.US,
                "quality %s%s   %.0f fps of %.0f Hz   missed %.1f%%",
                quality.level,
                if (quality.forced != null) " (forced)" else "",
                pacer.targetFps,
                pacer.refreshRate,
                quality.missedFraction * 100f,
            ),
            "battery saver ${if (quality.powerSave) "on" else "off"}   thermal ${quality.thermal}",
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
            pacer.reset()
            quality.onLoopStarted()
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
        const val HUD_MARGIN_DP = 16f
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
