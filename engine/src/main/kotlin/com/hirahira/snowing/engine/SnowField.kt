package com.hirahira.snowing.engine

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Time-based snowfall: moves by `velocity * dt`, so it looks the same at any
 * refresh rate, and keeps state in flat arrays, so a frame allocates nothing.
 *
 * A flake takes all its parameters when born above the top edge and keeps them
 * until it falls out (ADR-0006); [config] only describes flakes yet to be born.
 * Flakes drift in shared air: swirls, gusts and a mean wind (ADR-0009).
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

    val isEmpty: Boolean get() = count == 0

    // What a flake born right now gets: eases toward [config] so the front has a soft edge.
    private var bornFallSpeed = config.fallSpeed
    private var bornWind = config.wind
    private var bornGusts = config.gusts
    private var bornTurbulence = config.turbulence
    private var bornFlutter = config.flutter
    private var bornMinRadius = config.minRadius
    private var bornMaxRadius = config.maxRadius
    private var bornOpacity = config.opacity

    // The air. Clocks wrap at the noise period, which is seamless.
    private var eddyClock = 0f
    private var gustClock = 0f

    // Each flake samples the air only every `airStride` frames (~20 Hz), staggered by index.
    private var frame = 0
    private var airStride = 1

    private var fade = 1f
    private var fadeTarget = 1f

    // Seeds, rolled at birth.
    private var depthSeed = FloatArray(0)
    private var sizeSeed = FloatArray(0)
    private var shapeSeed = FloatArray(0)
    private var flutterRow = FloatArray(0)

    // Parameters, fixed at birth.
    private var fallPx = FloatArray(0)
    private var windPx = FloatArray(0)
    private var gustPx = FloatArray(0)
    private var eddyPx = FloatArray(0)
    private var flutterPx = FloatArray(0)
    private var flutterRate = FloatArray(0)
    private var inertia = FloatArray(0)
    private var layerZ = FloatArray(0)
    private var radiusPx = FloatArray(0)
    private var baseAlpha = FloatArray(0)

    private var posX = FloatArray(0)
    private var posY = FloatArray(0)
    private var velX = FloatArray(0)
    private var airX = FloatArray(0)
    private var airY = FloatArray(0)
    private var flutterPhase = FloatArray(0)
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
                posX[i] *= scaleX
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
        eddyClock = (eddyClock + dt * EDDY_RATE) % Noise.PERIOD
        gustClock = (gustClock + dt * GUST_RATE) % Noise.PERIOD
        frame++
        airStride = if (dt > 0f) (AIR_SAMPLE_SECONDS / dt).toInt().coerceIn(1, MAX_AIR_STRIDE) else 1
        spawnMissing()

        val target = targetCount()
        if (count > target) retireWaiting(target)
        var i = 0
        while (i < count) {
            advance(i, dt)
            wrapSideways(i)
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

    /** Fall speed of flake [i] in still air, px/s. The air adds to it and takes from it. */
    fun fallSpeed(i: Int): Float = fallPx[i]

    private fun advance(i: Int, dt: Float) {
        // The air changes over seconds and inertia smooths it further, so ~20
        // samples a second are indistinguishable from one per frame.
        if ((i + frame) % airStride == 0) sampleAir(i)
        velX[i] += (airX[i] - velX[i]) * (1f - exp(-dt / inertia[i]))
        posX[i] += velX[i] * dt

        val fall = fallPx[i]
        posY[i] += max(fall + airY[i], fall * MIN_FALL) * dt
        flutterPhase[i] = (flutterPhase[i] + flutterRate[i] * dt) % Noise.PERIOD
    }

    private fun sampleAir(i: Int) {
        val xDp = posX[i] / pxPerDp
        val yDp = posY[i] / pxPerDp

        // Swirls: the curl of a noise field has no sources or sinks, so flakes
        // flow around each other instead of bunching up or spreading apart.
        val sx = xDp / EDDY_SIZE_DP
        val sy = yDp / EDDY_SIZE_DP
        val sz = eddyClock + layerZ[i]
        val dPsiDy = (Noise.at(sx, sy + CURL_STEP, sz) - Noise.at(sx, sy - CURL_STEP, sz)) / (2 * CURL_STEP)
        val dPsiDx = (Noise.at(sx + CURL_STEP, sy, sz) - Noise.at(sx - CURL_STEP, sy, sz)) / (2 * CURL_STEP)

        // Gusts: one wide wave of wind travelling sideways, so they reach one edge first.
        val gust = Noise.at(gustClock - xDp / GUST_WIDTH_DP, GUST_ROW, 0f)

        airX[i] = windPx[i] + gust * gustPx[i] + dPsiDy * eddyPx[i]
        airY[i] = -dPsiDx * eddyPx[i] * VERTICAL_EDDY
    }

    private fun wrapSideways(i: Int) {
        // Per flake, so flakes born later with other sizes never change where this one wraps.
        val margin = radiusPx[i] + flutterPx[i]
        val x = posX[i]
        if (x > width + margin) {
            posX[i] = x - width - 2 * margin
        } else if (x < -margin) {
            posX[i] = x + width + 2 * margin
        }
    }

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
        shapeSeed[i] = random.nextFloat()
        flutterRow[i] = random.nextFloat() * Noise.PERIOD
        flutterPhase[i] = random.nextFloat() * Noise.PERIOD
        posX[i] = random.nextFloat() * width
        assignParameters(i)
        sampleAir(i)
        velX[i] = airX[i]
    }

    private fun assignParameters(i: Int) {
        val depth = depth(depthSeed[i], config.layers)
        // Parallax: the same air moves a distant flake fewer pixels.
        val motion = lerp(FAR_MOTION, 1f, depth) * pxPerDp
        fallPx[i] = bornFallSpeed * motion
        windPx[i] = bornWind * motion
        gustPx[i] = bornGusts * motion
        eddyPx[i] = bornTurbulence * motion
        flutterPx[i] = bornFlutter * pxPerDp * lerp(FAR_FLUTTER, 1f, depth)
        flutterRate[i] = lerp(FLUTTER_RATE_MIN, FLUTTER_RATE_MAX, sizeSeed[i])
        // Bigger flakes are heavier and turn more lazily.
        inertia[i] = lerp(INERTIA_MIN_S, INERTIA_MAX_S, sizeSeed[i])
        // Each depth layer drifts in its own slice of the air.
        layerZ[i] = depth * LAYER_Z_STEP
        radiusPx[i] = lerp(bornMinRadius, bornMaxRadius, depth) *
            lerp(1f - SIZE_JITTER, 1f + SIZE_JITTER, sizeSeed[i]) *
            pxPerDp
        baseAlpha[i] = bornOpacity * lerp(FAR_ALPHA, 1f, depth)
    }

    private fun removeAt(i: Int) {
        val last = count - 1
        if (i != last) {
            depthSeed[i] = depthSeed[last]
            sizeSeed[i] = sizeSeed[last]
            shapeSeed[i] = shapeSeed[last]
            flutterRow[i] = flutterRow[last]
            fallPx[i] = fallPx[last]
            windPx[i] = windPx[last]
            gustPx[i] = gustPx[last]
            eddyPx[i] = eddyPx[last]
            flutterPx[i] = flutterPx[last]
            flutterRate[i] = flutterRate[last]
            inertia[i] = inertia[last]
            layerZ[i] = layerZ[last]
            radiusPx[i] = radiusPx[last]
            baseAlpha[i] = baseAlpha[last]
            posX[i] = posX[last]
            posY[i] = posY[last]
            velX[i] = velX[last]
            airX[i] = airX[last]
            airY[i] = airY[last]
            flutterPhase[i] = flutterPhase[last]
            renderX[i] = renderX[last]
        }
        count = last
    }

    private fun snapBornToTarget() {
        bornFallSpeed = config.fallSpeed
        bornWind = config.wind
        bornGusts = config.gusts
        bornTurbulence = config.turbulence
        bornFlutter = config.flutter
        bornMinRadius = config.minRadius
        bornMaxRadius = config.maxRadius
        bornOpacity = config.opacity
    }

    // Exponential smoothing: frame-rate independent and handles a target changing mid-way.
    private fun easeBornTowardTarget(dt: Float) {
        val k = 1f - exp(-dt / FRONT_TAU_SECONDS)
        bornFallSpeed += (config.fallSpeed - bornFallSpeed) * k
        bornWind += (config.wind - bornWind) * k
        bornGusts += (config.gusts - bornGusts) * k
        bornTurbulence += (config.turbulence - bornTurbulence) * k
        bornFlutter += (config.flutter - bornFlutter) * k
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
            renderX[i] = posX[i] + Noise.at(flutterPhase[i], flutterRow[i], FLUTTER_SLICE) * flutterPx[i]
        }
    }

    private fun ensureCapacity(size: Int) {
        if (size <= posX.size) return
        depthSeed = depthSeed.copyOf(size)
        sizeSeed = sizeSeed.copyOf(size)
        shapeSeed = shapeSeed.copyOf(size)
        flutterRow = flutterRow.copyOf(size)
        fallPx = fallPx.copyOf(size)
        windPx = windPx.copyOf(size)
        gustPx = gustPx.copyOf(size)
        eddyPx = eddyPx.copyOf(size)
        flutterPx = flutterPx.copyOf(size)
        flutterRate = flutterRate.copyOf(size)
        inertia = inertia.copyOf(size)
        layerZ = layerZ.copyOf(size)
        radiusPx = radiusPx.copyOf(size)
        baseAlpha = baseAlpha.copyOf(size)
        posX = posX.copyOf(size)
        posY = posY.copyOf(size)
        velX = velX.copyOf(size)
        airX = airX.copyOf(size)
        airY = airY.copyOf(size)
        flutterPhase = flutterPhase.copyOf(size)
        renderX = renderX.copyOf(size)
    }

    companion object {
        /** Longest step simulated in one go; after a hitch flakes resume instead of teleporting. */
        const val MAX_STEP_SECONDS = 0.1f

        const val MAX_FLAKES = 1500

        /** Time constant of the soft front edge: ~95% of a change after 3τ ≈ 2 s. */
        const val FRONT_TAU_SECONDS = 0.6f

        const val FADE_SECONDS = 2f

        /** Height of the respawn band above the screen, as a fraction of screen height. */
        const val SPAWN_BAND = 0.3f

        /** A flake never falls slower than this share of its still-air speed, however the air swirls. */
        const val MIN_FALL = 0.25f

        private const val AREA_UNIT_DP2 = 10_000f

        // Farthest layer relative to the closest one.
        private const val FAR_MOTION = 0.45f
        private const val FAR_ALPHA = 0.35f
        private const val FAR_FLUTTER = 0.5f

        // Per-flake variation so a layer doesn't look cloned.
        private const val SIZE_JITTER = 0.2f

        // The air (ADR-0009). Swirls about a third of a phone screen wide, changing over ~15 s.
        private const val EDDY_SIZE_DP = 180f
        private const val EDDY_RATE = 0.07f
        private const val CURL_STEP = 0.05f
        private const val VERTICAL_EDDY = 0.5f
        private const val LAYER_Z_STEP = 37.3f

        // Gusts rise and fall over ~10 s and cross a phone screen in ~1.5 s.
        private const val GUST_RATE = 0.1f
        private const val GUST_WIDTH_DP = 2500f
        private const val GUST_ROW = 91.7f

        // Flutter: an irregular wobble, not a pendulum.
        private const val FLUTTER_RATE_MIN = 0.25f
        private const val FLUTTER_RATE_MAX = 0.6f
        private const val FLUTTER_SLICE = 13.1f

        // How often a flake looks at the air, and the most frames it may skip.
        private const val AIR_SAMPLE_SECONDS = 0.05f
        private const val MAX_AIR_STRIDE = 8

        // Seconds for a flake to catch up with the air.
        private const val INERTIA_MIN_S = 0.15f
        private const val INERTIA_MAX_S = 0.35f

        /** 0 = farthest layer, 1 = closest. */
        private fun depth(seed: Float, layers: Int): Float {
            if (layers == 1) return 1f
            val level = min((seed * layers).toInt(), layers - 1)
            return level / (layers - 1f)
        }
    }
}

private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
