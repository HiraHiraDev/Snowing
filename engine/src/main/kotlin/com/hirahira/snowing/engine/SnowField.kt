package com.hirahira.snowing.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.random.Random

/**
 * Time-based snowfall: moves by `velocity * dt`, so it looks the same at any
 * refresh rate, and keeps state in flat arrays, so a frame allocates nothing.
 *
 * A flake takes all its parameters when born above the top edge and keeps them
 * until it falls out (ADR-0006); [config] only describes flakes yet to be born.
 * Flakes drift in shared air: swirls, gusts and a mean wind (ADR-0009). Each
 * flake falls at its own speed and answers the air with its own lightness, so
 * neighbours caught in the same air still part ways (ADR-0009 §Zmiana 2026-10-24).
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
    private var sideClock = 0f

    // Which way gusts blow when the mood has no wind of its own; turns only while the air is quiet.
    private var weatherSide = 1f

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
    private var fallSeed = FloatArray(0)
    private var lightSeed = FloatArray(0)

    // Parameters, fixed at birth.
    private var fallPx = FloatArray(0)
    private var windPx = FloatArray(0)
    private var gustPx = FloatArray(0)
    private var eddyPx = FloatArray(0)
    private var flutterPx = FloatArray(0)
    private var gustSide = FloatArray(0)
    private var lightness = FloatArray(0)
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
    private var liftY = FloatArray(0)
    private var flutterPhase = FloatArray(0)
    private var renderX = FloatArray(0)
    private var renderY = FloatArray(0)

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
        updateRender()
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
        updateRender()
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
        sideClock = (sideClock + dt * SIDE_RATE) % Noise.PERIOD
        turnWeatherSide()
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
        updateRender()
    }

    fun x(i: Int): Float = renderX[i]

    fun y(i: Int): Float = renderY[i]

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
        posY[i] += (max(fall + airY[i], fall * MIN_FALL) - liftY[i]) * dt
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

        // Gusts: bursts that blow one way and die down, never a swing back. Each one travels
        // with the wind, so it reaches the upwind edge first, and lifts flakes as it passes.
        val side = if (gustSide[i] != 0f) gustSide[i] else weatherSide
        val gust = gustAt(gustClock - side * xDp / GUST_WIDTH_DP) * gustPx[i] * lightness[i]

        val light = lightness[i]
        airX[i] = windPx[i] + side * gust + dPsiDy * eddyPx[i] * light
        airY[i] = -dPsiDx * eddyPx[i] * light * VERTICAL_EDDY
        liftY[i] = gust * GUST_LIFT
    }

    /** 0 between gusts, rising smoothly to 1 at the height of one. */
    private fun gustAt(at: Float): Float {
        val t = ((Noise.at(at, GUST_ROW, 0f) - GUST_ON) / (GUST_FULL - GUST_ON)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** Calm moods get gusts from either side, but a gust never turns round halfway. */
    private fun turnWeatherSide() {
        if (gustAt(gustClock - width / pxPerDp / 2f / GUST_WIDTH_DP) > 0f) return
        weatherSide = if (Noise.at(sideClock, SIDE_ROW, 0f) >= 0f) 1f else -1f
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
            posY[i] = -hiddenAbove(i) - random.nextFloat() * band
            render(i)
        }
        count = target
    }

    /** Drops flakes still waiting above the screen, down to [target]: nobody has seen them yet. */
    private fun retireWaiting(target: Int) {
        var i = 0
        while (i < count && count > target) {
            if (renderY[i] + radiusPx[i] < 0f) {
                removeAt(i)
                continue
            }
            i++
        }
    }

    private fun respawn(i: Int) {
        rollSeeds(i)
        posY[i] = -hiddenAbove(i) - random.nextFloat() * height * respawnBand()
        render(i)
    }

    /** How far above the top edge a flake is still out of sight, flutter included. */
    private fun hiddenAbove(i: Int): Float = radiusPx[i] + flutterPx[i] * FLUTTER_Y

    private fun respawnBand(): Float = SPAWN_BAND + config.respawnSpread

    private fun rollSeeds(i: Int) {
        depthSeed[i] = random.nextFloat()
        sizeSeed[i] = random.nextFloat()
        shapeSeed[i] = random.nextFloat()
        flutterRow[i] = random.nextFloat() * Noise.PERIOD
        flutterPhase[i] = random.nextFloat() * Noise.PERIOD
        fallSeed[i] = random.nextFloat()
        lightSeed[i] = random.nextFloat()
        posX[i] = random.nextFloat() * width
        assignParameters(i)
        sampleAir(i)
        velX[i] = airX[i]
    }

    private fun assignParameters(i: Int) {
        val depth = depth(depthSeed[i], config.layers)
        // Parallax: the same air moves a distant flake fewer pixels.
        val motion = lerp(FAR_MOTION, 1f, depth) * pxPerDp
        // Speeds spread within a layer too, so no two neighbours fall in step.
        fallPx[i] = bornFallSpeed * motion * lerp(1f - FALL_SPREAD, 1f + FALL_SPREAD, fallSeed[i])
        windPx[i] = bornWind * motion
        gustPx[i] = bornGusts * motion
        eddyPx[i] = bornTurbulence * motion
        lightness[i] = lerp(LIGHTNESS_MIN, LIGHTNESS_MAX, lightSeed[i])
        // Gusts follow the mood's wind; without one they come from the weather.
        gustSide[i] = if (abs(bornWind) < STILL_WIND_DP) 0f else sign(bornWind)
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
            fallSeed[i] = fallSeed[last]
            lightSeed[i] = lightSeed[last]
            fallPx[i] = fallPx[last]
            windPx[i] = windPx[last]
            gustPx[i] = gustPx[last]
            eddyPx[i] = eddyPx[last]
            flutterPx[i] = flutterPx[last]
            gustSide[i] = gustSide[last]
            lightness[i] = lightness[last]
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
            liftY[i] = liftY[last]
            flutterPhase[i] = flutterPhase[last]
            renderX[i] = renderX[last]
            renderY[i] = renderY[last]
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

    // Flutter in both axes, less up and down: a flake tumbles in small irregular loops instead of swinging.
    private fun updateRender() {
        for (i in 0 until count) render(i)
    }

    private fun render(i: Int) {
        renderX[i] = posX[i] + Noise.at(flutterPhase[i], flutterRow[i], FLUTTER_SLICE) * flutterPx[i]
        renderY[i] = posY[i] + Noise.at(flutterPhase[i], flutterRow[i], FLUTTER_SLICE_Y) * flutterPx[i] * FLUTTER_Y
    }

    private fun ensureCapacity(size: Int) {
        if (size <= posX.size) return
        depthSeed = depthSeed.copyOf(size)
        sizeSeed = sizeSeed.copyOf(size)
        shapeSeed = shapeSeed.copyOf(size)
        flutterRow = flutterRow.copyOf(size)
        fallSeed = fallSeed.copyOf(size)
        lightSeed = lightSeed.copyOf(size)
        fallPx = fallPx.copyOf(size)
        windPx = windPx.copyOf(size)
        gustPx = gustPx.copyOf(size)
        eddyPx = eddyPx.copyOf(size)
        flutterPx = flutterPx.copyOf(size)
        gustSide = gustSide.copyOf(size)
        lightness = lightness.copyOf(size)
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
        liftY = liftY.copyOf(size)
        flutterPhase = flutterPhase.copyOf(size)
        renderX = renderX.copyOf(size)
        renderY = renderY.copyOf(size)
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
        private const val FAR_MOTION = 0.4f
        private const val FAR_ALPHA = 0.35f
        private const val FAR_FLUTTER = 0.5f

        // Per-flake variation so a layer doesn't look cloned.
        private const val SIZE_JITTER = 0.2f
        private const val FALL_SPREAD = 0.25f

        // How strongly a flake answers swirls and gusts: light ones get blown about more.
        private const val LIGHTNESS_MIN = 0.6f
        private const val LIGHTNESS_MAX = 1.4f

        // The air (ADR-0009). Swirls about a third of a phone screen wide, changing over ~15 s.
        private const val EDDY_SIZE_DP = 180f
        private const val EDDY_RATE = 0.07f
        private const val CURL_STEP = 0.05f
        private const val VERTICAL_EDDY = 0.5f
        private const val LAYER_Z_STEP = 37.3f

        // Gusts: a burst every ~13 s lasting ~4 s, crossing a phone screen in under a second.
        // The noise sits in about -0.45..0.45; above GUST_ON a gust starts, at GUST_FULL it peaks.
        private const val GUST_RATE = 0.2f
        private const val GUST_WIDTH_DP = 2500f
        private const val GUST_ROW = 91.7f
        private const val GUST_ON = 0.1f
        private const val GUST_FULL = 0.45f

        // A gust also lifts flakes by this share of its sideways speed: they hover or rise briefly.
        private const val GUST_LIFT = 0.5f

        // Without a mood wind (below this, dp/s) the gust side comes from the weather and turns every ~half minute.
        private const val STILL_WIND_DP = 1f
        private const val SIDE_RATE = 0.02f
        private const val SIDE_ROW = 57.3f

        // Flutter: an irregular wobble, not a pendulum.
        private const val FLUTTER_RATE_MIN = 0.25f
        private const val FLUTTER_RATE_MAX = 0.6f
        private const val FLUTTER_SLICE = 13.1f
        private const val FLUTTER_SLICE_Y = 29.7f
        private const val FLUTTER_Y = 0.5f

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
