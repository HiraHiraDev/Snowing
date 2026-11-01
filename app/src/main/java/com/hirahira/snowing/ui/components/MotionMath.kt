package com.hirahira.snowing.ui.components

/**
 * Turns a wrapped angle (0-359°) into a running one, so an animation from 350° to 10° turns
 * 20° forward instead of 340° back. Same input twice gives the same result, so it is safe to
 * call while composing.
 */
internal class AngleUnwrapper(start: Int) {
    private var last = start.mod(FULL_TURN)
    private var running = last.toFloat()

    fun follow(angle: Int): Float {
        val wrapped = angle.mod(FULL_TURN)
        if (wrapped != last) {
            running += shortestTurn(last, wrapped)
            last = wrapped
        }
        return running
    }
}

/** Degrees from [from] to [to] the short way round, -180 (exclusive) to 180. */
internal fun shortestTurn(from: Int, to: Int): Int {
    val turn = (to - from).mod(FULL_TURN)
    return if (turn > HALF_TURN) turn - FULL_TURN else turn
}

/**
 * Where an indicator between items sits for a fractional [position] (0 = first item):
 * linear between neighbouring [centers], and continuing past both ends, so an overshooting
 * spring carries it a little beyond the first or last item.
 */
internal fun positionBetween(position: Float, centers: FloatArray): Float {
    if (centers.size == 1) return centers[0]
    val segment = position.toInt().coerceIn(0, centers.size - 2)
    val along = position - segment
    return centers[segment] + (centers[segment + 1] - centers[segment]) * along
}

private const val FULL_TURN = 360
private const val HALF_TURN = 180
