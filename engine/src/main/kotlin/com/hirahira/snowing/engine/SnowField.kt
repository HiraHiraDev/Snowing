package com.hirahira.snowing.engine

import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Time-based snowfall simulation with no Android dependencies.
 *
 * Positions advance by `velocity * dt`, where dt is the real time between
 * frames, so a flake covers the same px/s at 60, 90 or 120 Hz. Sway is a
 * function of accumulated time, not of frame count. State lives in flat arrays
 * so a frame allocates nothing.
 *
 * Per frame: call [step] with the elapsed time, then read flakes
 * `0 until count` through [x], [y], [radius] and [alpha].
 */
class SnowField(
    config: SnowConfig,
    private val pxPerDp: Float = 1f,
    private val random: Random = Random.Default,
) {
    var config: SnowConfig = config
        private set
    var width: Float = 0f
        private set
    var height: Float = 0f
        private set
    var count: Int = 0
        private set

    // Double: a Float clock loses sub-frame precision after a few hours of snow.
    private var time = 0.0

    private var anchorX = FloatArray(0)
    private var posY = FloatArray(0)
    private var depthSeed = FloatArray(0)
    private var sizeSeed = FloatArray(0)
    private var tempoSeed = FloatArray(0)
    private var phase = FloatArray(0)
    private var renderX = FloatArray(0)

    /** Sets the drawing area. The first call fills the whole screen with snow. */
    fun resize(widthPx: Int, heightPx: Int) {
        val newWidth = widthPx.toFloat()
        val newHeight = heightPx.toFloat()
        if (newWidth == width && newHeight == height) return
        val firstLayout = width <= 0f || height <= 0f
        if (!firstLayout) {
            val scaleX = newWidth / width
            val scaleY = newHeight / height
            for (i in 0 until count) {
                anchorX[i] *= scaleX
                posY[i] *= scaleY
            }
        }
        width = newWidth
        height = newHeight
        syncCount(fillScreen = firstLayout)
        updateRenderX()
    }

    /** Applies a new config immediately. Added flakes enter from above the screen. */
    fun updateConfig(newConfig: SnowConfig) {
        config = newConfig
        syncCount(fillScreen = false)
        updateRenderX()
    }

    fun step(dtSeconds: Float) {
        if (count == 0) return
        val dt = dtSeconds.coerceIn(0f, MAX_STEP_SECONDS)
        time += dt
        val speedPx = config.fallSpeed * pxPerDp
        val windPx = config.wind * pxPerDp
        val margin = horizontalMargin()
        val span = width + 2 * margin
        for (i in 0 until count) {
            val motion = lerp(FAR_MOTION, 1f, depth(i))
            posY[i] += speedPx * motion * dt
            var ax = anchorX[i] + windPx * motion * dt
            if (ax > width + margin) ax -= span else if (ax < -margin) ax += span
            anchorX[i] = ax
            if (posY[i] - radius(i) > height) respawnAtTop(i)
        }
        updateRenderX()
    }

    fun x(i: Int): Float = renderX[i]

    fun y(i: Int): Float = posY[i]

    fun radius(i: Int): Float =
        lerp(config.minRadius, config.maxRadius, depth(i)) *
            lerp(1f - SIZE_JITTER, 1f + SIZE_JITTER, sizeSeed[i]) *
            pxPerDp

    fun alpha(i: Int): Float = config.opacity * lerp(FAR_ALPHA, 1f, depth(i))

    /** Vertical speed of flake [i] in px/s — what it should measure on screen. */
    fun fallSpeed(i: Int): Float = config.fallSpeed * pxPerDp * lerp(FAR_MOTION, 1f, depth(i))

    /** 0 = farthest layer, 1 = closest. */
    private fun depth(i: Int): Float {
        val layers = config.layers
        if (layers == 1) return 1f
        val level = min((depthSeed[i] * layers).toInt(), layers - 1)
        return level / (layers - 1f)
    }

    private fun updateRenderX() {
        val swayPx = config.swayAmplitude * pxPerDp
        val omega = 2.0 * PI * config.swayFrequency
        for (i in 0 until count) {
            val tempo = lerp(1f - TEMPO_JITTER, 1f + TEMPO_JITTER, tempoSeed[i])
            val offset = sin(phase[i] + time * omega * tempo).toFloat()
            renderX[i] = anchorX[i] + offset * swayPx * lerp(FAR_SWAY, 1f, depth(i))
        }
    }

    private fun syncCount(fillScreen: Boolean) {
        val target = targetCount()
        ensureCapacity(target)
        for (i in count until target) spawn(i, fillScreen)
        count = target
    }

    private fun targetCount(): Int {
        if (width <= 0f || height <= 0f) return 0
        val areaDp2 = (width / pxPerDp) * (height / pxPerDp)
        return (areaDp2 / AREA_UNIT_DP2 * config.density).roundToInt().coerceAtMost(MAX_FLAKES)
    }

    private fun spawn(i: Int, anywhereOnScreen: Boolean) {
        depthSeed[i] = random.nextFloat()
        sizeSeed[i] = random.nextFloat()
        tempoSeed[i] = random.nextFloat()
        phase[i] = random.nextFloat() * TWO_PI
        anchorX[i] = random.nextFloat() * width
        posY[i] = if (anywhereOnScreen) random.nextFloat() * height else aboveTop(i)
    }

    private fun respawnAtTop(i: Int) {
        anchorX[i] = random.nextFloat() * width
        posY[i] = aboveTop(i)
    }

    /** Spread respawns over a band above the screen so flakes don't arrive in rows. */
    private fun aboveTop(i: Int): Float = -radius(i) - random.nextFloat() * height * SPAWN_BAND

    private fun horizontalMargin(): Float =
        (config.maxRadius * (1f + SIZE_JITTER) + config.swayAmplitude) * pxPerDp

    private fun ensureCapacity(size: Int) {
        if (size <= anchorX.size) return
        anchorX = anchorX.copyOf(size)
        posY = posY.copyOf(size)
        depthSeed = depthSeed.copyOf(size)
        sizeSeed = sizeSeed.copyOf(size)
        tempoSeed = tempoSeed.copyOf(size)
        phase = phase.copyOf(size)
        renderX = renderX.copyOf(size)
    }

    companion object {
        /** Longest step simulated in one go; after a hitch flakes resume instead of teleporting. */
        const val MAX_STEP_SECONDS = 0.1f

        /** Hard cap, regardless of density and screen size. */
        const val MAX_FLAKES = 1500

        private const val AREA_UNIT_DP2 = 10_000f

        // Farthest layer relative to the closest one.
        private const val FAR_MOTION = 0.45f
        private const val FAR_ALPHA = 0.35f
        private const val FAR_SWAY = 0.5f

        // Per-flake variation so a layer doesn't look cloned.
        private const val SIZE_JITTER = 0.2f
        private const val TEMPO_JITTER = 0.3f

        /** Height of the respawn band above the screen, as a fraction of screen height. */
        private const val SPAWN_BAND = 0.3f

        private const val TWO_PI = (2 * PI).toFloat()
    }
}

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
