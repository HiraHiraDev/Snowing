package com.hirahira.snowing.engine

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Time-based snowfall simulation with no Android dependencies.
 *
 * Positions advance by `velocity * dt`, where dt is the real time between
 * frames, so a flake covers the same px/s at 60, 90 or 120 Hz. State lives in
 * flat arrays so a frame allocates nothing.
 *
 * Continuity (docs/adr/0006-snow-continuity.md): a flake takes all of its
 * parameters when it is born above the top edge and keeps them until it falls
 * out at the bottom. [config] only describes flakes yet to be born, so every
 * change — start, stop, a new mood, a slider — sweeps down the screen like a
 * weather front instead of changing the whole screen at once.
 *
 * Per frame: call [step] with the elapsed time, then read flakes
 * `0 until count` through [x], [y], [radius], [alpha] and [shape].
 */
class SnowField(
    config: SnowConfig,
    private val pxPerDp: Float = 1f,
    private val random: Random = Random.Default,
) {
    /** Target for flakes born from now on. */
    var config: SnowConfig = config
        private set
    var width: Float = 0f
        private set
    var height: Float = 0f
        private set
    var count: Int = 0
        private set

    /** False after [stopFalling]: no flakes are born, the rest fall out. */
    var isFalling: Boolean = true
        private set

    /** True once stopped and every flake has left the screen. */
    val isEmpty: Boolean get() = count == 0

    // What a flake born right now gets: eases toward [config] so the front has a soft edge.
    private var bornFallSpeed = config.fallSpeed
    private var bornWind = config.wind
    private var bornSwayAmplitude = config.swayAmplitude
    private var bornSwayFrequency = config.swayFrequency
    private var bornMinRadius = config.minRadius
    private var bornMaxRadius = config.maxRadius
    private var bornOpacity = config.opacity

    // Visibility of the whole field: 1 normally, eases to 0 in [fadeOut].
    private var fade = 1f
    private var fadeTarget = 1f

    // Widest flake plus sway ever born; flakes wrap sideways only beyond it.
    private var marginPx = 0f

    // Seeds, rolled at birth.
    private var depthSeed = FloatArray(0)
    private var sizeSeed = FloatArray(0)
    private var tempoSeed = FloatArray(0)
    private var shapeSeed = FloatArray(0)

    // Parameters, fixed at birth.
    private var fallPx = FloatArray(0)
    private var windPx = FloatArray(0)
    private var swayPx = FloatArray(0)
    private var swayOmega = FloatArray(0)
    private var radiusPx = FloatArray(0)
    private var baseAlpha = FloatArray(0)

    // Motion.
    private var anchorX = FloatArray(0)
    private var posY = FloatArray(0)
    private var phase = FloatArray(0)
    private var renderX = FloatArray(0)

    /**
     * Sets the drawing area. On the first call snow starts falling from the top
     * edge; later calls (rotation) keep every flake and rescale positions.
     */
    fun resize(widthPx: Int, heightPx: Int) {
        val newWidth = widthPx.toFloat()
        val newHeight = heightPx.toFloat()
        if (newWidth == width && newHeight == height) return
        if (width > 0f && height > 0f) {
            val scaleX = newWidth / width
            val scaleY = newHeight / height
            for (i in 0 until count) {
                anchorX[i] *= scaleX
                posY[i] *= scaleY
            }
        }
        width = newWidth
        height = newHeight
        spawnMissing()
        updateRenderX()
    }

    /**
     * Sets the target for flakes born from now on. Flakes already falling keep
     * their parameters; [immediate] (debug tuning only) re-derives them at once.
     */
    fun updateConfig(newConfig: SnowConfig, immediate: Boolean = false) {
        config = newConfig
        if (immediate || count == 0) snapBornToTarget()
        if (immediate) {
            for (i in 0 until count) assignParameters(i)
            if (count > targetCount()) count = targetCount()
        }
        retireWaiting(targetCount())
        spawnMissing()
        updateRenderX()
    }

    /** Stops new flakes from being born; the ones on screen fall out. */
    fun stopFalling() {
        isFalling = false
        retireWaiting(0)
    }

    /** Undoes [stopFalling] and [fadeOut]; new flakes enter from the top again. */
    fun resumeFalling() {
        isFalling = true
        fadeTarget = 1f
        spawnMissing()
    }

    /** Fades out everything still on screen (the stop fuse, ADR-0006 §5). */
    fun fadeOut() {
        stopFalling()
        fadeTarget = 0f
    }

    fun step(dtSeconds: Float) {
        if (width <= 0f || height <= 0f) return
        val dt = dtSeconds.coerceIn(0f, MAX_STEP_SECONDS)
        easeBornTowardTarget(dt)
        easeFade(dt)
        spawnMissing()

        val span = width + 2 * marginPx
        val target = targetCount()
        if (count > target) retireWaiting(target)
        var i = 0
        while (i < count) {
            posY[i] += fallPx[i] * dt
            var ax = anchorX[i] + windPx[i] * dt
            if (ax > width + marginPx) ax -= span else if (ax < -marginPx) ax += span
            anchorX[i] = ax
            phase[i] = (phase[i] + swayOmega[i] * dt) % TWO_PI
            if (posY[i] - radiusPx[i] > height) {
                if (count > target) {
                    removeAt(i)
                    continue // the flake moved into slot i has not been stepped yet
                }
                respawn(i)
            }
            i++
        }
        updateRenderX()
    }

    fun x(i: Int): Float = renderX[i]

    fun y(i: Int): Float = posY[i]

    fun radius(i: Int): Float = radiusPx[i]

    fun alpha(i: Int): Float = baseAlpha[i] * fade

    /** Stable per-flake value in 0..1 for picking a sprite variant. */
    fun shape(i: Int): Float = shapeSeed[i]

    /** Vertical speed of flake [i] in px/s — what it should measure on screen. */
    fun fallSpeed(i: Int): Float = fallPx[i]

    private fun targetCount(): Int {
        if (!isFalling || width <= 0f || height <= 0f) return 0
        val areaDp2 = (width / pxPerDp) * (height / pxPerDp)
        return (areaDp2 / AREA_UNIT_DP2 * config.density).roundToInt().coerceAtMost(MAX_FLAKES)
    }

    /** New flakes spread over the band above the screen, so the front enters at once and density matches steady state. */
    private fun spawnMissing() {
        val target = targetCount()
        if (count >= target) return
        ensureCapacity(target)
        val band = height * (1f + respawnBand())
        for (i in count until target) {
            rollSeeds(i)
            posY[i] = -radiusPx[i] - random.nextFloat() * band
        }
        count = target
    }

    /** Drops flakes still waiting above the screen, down to [target]: nobody has seen them yet. */
    private fun retireWaiting(target: Int) {
        var i = 0
        while (i < count && count > target) {
            if (posY[i] + radiusPx[i] < 0f) {
                removeAt(i)
                continue
            }
            i++
        }
    }

    private fun respawn(i: Int) {
        rollSeeds(i)
        posY[i] = -radiusPx[i] - random.nextFloat() * height * respawnBand()
    }

    private fun respawnBand(): Float = SPAWN_BAND + config.respawnSpread

    private fun rollSeeds(i: Int) {
        depthSeed[i] = random.nextFloat()
        sizeSeed[i] = random.nextFloat()
        tempoSeed[i] = random.nextFloat()
        shapeSeed[i] = random.nextFloat()
        phase[i] = random.nextFloat() * TWO_PI
        anchorX[i] = random.nextFloat() * width
        assignParameters(i)
    }

    private fun assignParameters(i: Int) {
        val depth = depth(depthSeed[i], config.layers)
        val motion = lerp(FAR_MOTION, 1f, depth)
        fallPx[i] = bornFallSpeed * pxPerDp * motion
        windPx[i] = bornWind * pxPerDp * motion
        swayPx[i] = bornSwayAmplitude * pxPerDp * lerp(FAR_SWAY, 1f, depth)
        swayOmega[i] = TWO_PI * bornSwayFrequency * lerp(1f - TEMPO_JITTER, 1f + TEMPO_JITTER, tempoSeed[i])
        radiusPx[i] = lerp(bornMinRadius, bornMaxRadius, depth) *
            lerp(1f - SIZE_JITTER, 1f + SIZE_JITTER, sizeSeed[i]) *
            pxPerDp
        baseAlpha[i] = bornOpacity * lerp(FAR_ALPHA, 1f, depth)
        marginPx = max(marginPx, radiusPx[i] + swayPx[i])
    }

    private fun removeAt(i: Int) {
        val last = count - 1
        if (i != last) {
            depthSeed[i] = depthSeed[last]
            sizeSeed[i] = sizeSeed[last]
            tempoSeed[i] = tempoSeed[last]
            shapeSeed[i] = shapeSeed[last]
            fallPx[i] = fallPx[last]
            windPx[i] = windPx[last]
            swayPx[i] = swayPx[last]
            swayOmega[i] = swayOmega[last]
            radiusPx[i] = radiusPx[last]
            baseAlpha[i] = baseAlpha[last]
            anchorX[i] = anchorX[last]
            posY[i] = posY[last]
            phase[i] = phase[last]
            renderX[i] = renderX[last]
        }
        count = last
    }

    private fun snapBornToTarget() {
        bornFallSpeed = config.fallSpeed
        bornWind = config.wind
        bornSwayAmplitude = config.swayAmplitude
        bornSwayFrequency = config.swayFrequency
        bornMinRadius = config.minRadius
        bornMaxRadius = config.maxRadius
        bornOpacity = config.opacity
    }

    // Exponential smoothing: frame-rate independent and handles a target changing mid-way.
    private fun easeBornTowardTarget(dt: Float) {
        val k = 1f - exp(-dt / FRONT_TAU_SECONDS)
        bornFallSpeed += (config.fallSpeed - bornFallSpeed) * k
        bornWind += (config.wind - bornWind) * k
        bornSwayAmplitude += (config.swayAmplitude - bornSwayAmplitude) * k
        bornSwayFrequency += (config.swayFrequency - bornSwayFrequency) * k
        bornMinRadius += (config.minRadius - bornMinRadius) * k
        bornMaxRadius += (config.maxRadius - bornMaxRadius) * k
        bornOpacity += (config.opacity - bornOpacity) * k
    }

    private fun easeFade(dt: Float) {
        if (fade == fadeTarget) return
        val delta = dt / FADE_SECONDS
        fade = if (fadeTarget > fade) min(fadeTarget, fade + delta) else max(fadeTarget, fade - delta)
        if (fade == 0f) count = 0
    }

    private fun updateRenderX() {
        for (i in 0 until count) {
            renderX[i] = anchorX[i] + sin(phase[i]) * swayPx[i]
        }
    }

    private fun ensureCapacity(size: Int) {
        if (size <= anchorX.size) return
        depthSeed = depthSeed.copyOf(size)
        sizeSeed = sizeSeed.copyOf(size)
        tempoSeed = tempoSeed.copyOf(size)
        shapeSeed = shapeSeed.copyOf(size)
        fallPx = fallPx.copyOf(size)
        windPx = windPx.copyOf(size)
        swayPx = swayPx.copyOf(size)
        swayOmega = swayOmega.copyOf(size)
        radiusPx = radiusPx.copyOf(size)
        baseAlpha = baseAlpha.copyOf(size)
        anchorX = anchorX.copyOf(size)
        posY = posY.copyOf(size)
        phase = phase.copyOf(size)
        renderX = renderX.copyOf(size)
    }

    companion object {
        /** Longest step simulated in one go; after a hitch flakes resume instead of teleporting. */
        const val MAX_STEP_SECONDS = 0.1f

        /** Hard cap, regardless of density and screen size. */
        const val MAX_FLAKES = 1500

        /** Time constant of the soft front edge: ~95% of a change after 3τ ≈ 2 s. */
        const val FRONT_TAU_SECONDS = 0.6f

        /** Duration of [fadeOut]. */
        const val FADE_SECONDS = 2f

        /** Height of the respawn band above the screen, as a fraction of screen height. */
        const val SPAWN_BAND = 0.3f

        private const val AREA_UNIT_DP2 = 10_000f

        // Farthest layer relative to the closest one.
        private const val FAR_MOTION = 0.45f
        private const val FAR_ALPHA = 0.35f
        private const val FAR_SWAY = 0.5f

        // Per-flake variation so a layer doesn't look cloned.
        private const val SIZE_JITTER = 0.2f
        private const val TEMPO_JITTER = 0.3f

        private const val TWO_PI = (2 * PI).toFloat()

        /** 0 = farthest layer, 1 = closest. */
        private fun depth(seed: Float, layers: Int): Float {
            if (layers == 1) return 1f
            val level = min((seed * layers).toInt(), layers - 1)
            return level / (layers - 1f)
        }
    }
}

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
