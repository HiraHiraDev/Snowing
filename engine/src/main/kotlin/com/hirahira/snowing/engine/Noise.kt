package com.hirahira.snowing.engine

import kotlin.math.floor

/**
 * Improved Perlin noise (Ken Perlin, 2002): smooth, deterministic, allocation
 * free. Output is roughly in -1..1 and the pattern repeats every 256 units on
 * each axis, so callers can wrap their inputs modulo [PERIOD] without a seam.
 */
internal object Noise {

    const val PERIOD = 256f

    fun at(x: Float, y: Float, z: Float): Float {
        val fx = floor(x)
        val fy = floor(y)
        val fz = floor(z)
        val xi = fx.toInt() and 255
        val yi = fy.toInt() and 255
        val zi = fz.toInt() and 255
        val xf = x - fx
        val yf = y - fy
        val zf = z - fz
        val u = fade(xf)
        val v = fade(yf)
        val w = fade(zf)

        val a = p[xi] + yi
        val aa = p[a] + zi
        val ab = p[a + 1] + zi
        val b = p[xi + 1] + yi
        val ba = p[b] + zi
        val bb = p[b + 1] + zi

        return lerp(
            lerp(
                lerp(grad(p[aa], xf, yf, zf), grad(p[ba], xf - 1, yf, zf), u),
                lerp(grad(p[ab], xf, yf - 1, zf), grad(p[bb], xf - 1, yf - 1, zf), u),
                v,
            ),
            lerp(
                lerp(grad(p[aa + 1], xf, yf, zf - 1), grad(p[ba + 1], xf - 1, yf, zf - 1), u),
                lerp(grad(p[ab + 1], xf, yf - 1, zf - 1), grad(p[bb + 1], xf - 1, yf - 1, zf - 1), u),
                v,
            ),
            w,
        )
    }

    private fun fade(t: Float): Float = t * t * t * (t * (t * 6 - 15) + 10)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)

    private fun grad(hash: Int, x: Float, y: Float, z: Float): Float {
        val h = hash and 15
        val u = if (h < 8) x else y
        val v = if (h < 4) y else if (h == 12 || h == 14) x else z
        return (if (h and 1 == 0) u else -u) + (if (h and 2 == 0) v else -v)
    }

    // Perlin's reference permutation, repeated so p[i + 1] never needs a wrap.
    private val p: IntArray = run {
        val permutation = intArrayOf(
            151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142,
            8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117,
            35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71,
            134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41,
            55, 46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89,
            18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226,
            250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182,
            189, 28, 42, 223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43,
            172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97,
            228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239,
            107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254,
            138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180,
        )
        IntArray(512) { permutation[it and 255] }
    }
}
