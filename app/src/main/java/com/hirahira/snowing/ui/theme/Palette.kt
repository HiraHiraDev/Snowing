package com.hirahira.snowing.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Color values of design 6a (docs/design_handoff_snowfall_v2/Palette.kt). Nothing outside
// ui/theme references them directly: screens read SnowingTheme.colors.

/** Colors of the app's background scene at one time of day. */
@Immutable
data class SkyPalette(
    val skyTop: Color,
    val skyMid: Color,
    val skyLow: Color,
    val skyHorizon: Color,
    val hillFar: Color,
    val hillNear: Color,
    val driftTop: Color,
    val driftBottom: Color,
    /** How brightly the house windows glow, 0..1. */
    val windowGlow: Float,
)

object Sky {
    val Night = SkyPalette(
        Color(0xFF070B1F), Color(0xFF121A44), Color(0xFF26306A), Color(0xFF3A4580),
        Color(0xFF1F2854), Color(0xFF28326A), Color(0xFFAEBBE0), Color(0xFF6D7BB0), windowGlow = 1f,
    )
    val Dawn = SkyPalette(
        Color(0xFF3B3F6E), Color(0xFF8A7AA6), Color(0xFFE4A9A0), Color(0xFFF6D3B8),
        Color(0xFF7A6F96), Color(0xFF6A6088), Color(0xFFF4EEF2), Color(0xFFCFC3D6), windowGlow = 0.35f,
    )
    val Day = SkyPalette(
        Color(0xFF6F9BD4), Color(0xFFA9C6EE), Color(0xFFD6E4F5), Color(0xFFEEF3FA),
        Color(0xFF8FA4C8), Color(0xFF7D93BA), Color(0xFFFFFFFF), Color(0xFFDFE6F2), windowGlow = 0f,
    )
    val Dusk = SkyPalette(
        Color(0xFF171B36), Color(0xFF2E2C55), Color(0xFF6B4F6E), Color(0xFFC98A6A),
        Color(0xFF3A3358), Color(0xFF332D50), Color(0xFFE9DFE6), Color(0xFFB7A5BB), windowGlow = 1f,
    )

    /** Hour of day (0..24) to palette; neighbours blend with smoothstep. Fixed hours, no location (ADR-0010). */
    private val Keyframes = listOf(
        0f to Night, 5.5f to Night, 7f to Dawn, 9.5f to Day,
        16f to Day, 17.5f to Dusk, 19.5f to Night, 24f to Night,
    )

    /** Hours pinned when the user picks a time of day instead of Auto. */
    const val HOUR_DAWN = 7f
    const val HOUR_DAY = 12.5f
    const val HOUR_DUSK = 17.5f
    const val HOUR_NIGHT = 22f

    fun at(hour: Float): SkyPalette {
        val h = hour.coerceIn(0f, 24f)
        val i = Keyframes.indexOfLast { it.first <= h }.coerceIn(0, Keyframes.size - 2)
        val (h0, a) = Keyframes[i]
        val (h1, b) = Keyframes[i + 1]
        val linear = if (h1 == h0) 0f else ((h - h0) / (h1 - h0)).coerceIn(0f, 1f)
        val t = linear * linear * (3 - 2 * linear)
        return SkyPalette(
            mix(a.skyTop, b.skyTop, t), mix(a.skyMid, b.skyMid, t),
            mix(a.skyLow, b.skyLow, t), mix(a.skyHorizon, b.skyHorizon, t),
            mix(a.hillFar, b.hillFar, t), mix(a.hillNear, b.hillNear, t),
            mix(a.driftTop, b.driftTop, t), mix(a.driftBottom, b.driftBottom, t),
            a.windowGlow + (b.windowGlow - a.windowGlow) * t,
        )
    }

    // Straight on the sRGB values, as the design's HTML does; Compose's lerp mixes in Oklab
    // and gives other colors at dawn and dusk.
    private fun mix(a: Color, b: Color, t: Float) = Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
    )
}

/** Colors that do not change with the time of day. */
internal object Fixed {
    val Amber = Color(0xFFE8B45C)
    val AmberText = Color(0xFFF0C580)
    val AmberDeep = Color(0xFFA8661C)
    val AmberDeepText = Color(0xFF8A5310)
    val Milestone = Color(0xFFE0A64E)
    val InkLight = Color(0xFFF6EDE4)
    val InkDark = Color(0xFF1C2233)
    val InkOnAmber = Color(0xFF2A1D14)
    val InkOnDriftSoft = Color(0xFF4A3A48)
    val House = Color(0xFF221C30)
    val MoonDisc = Color(0xFFF3F1E6)
    val SunDisc = Color(0xFFFFF1D6)
    val SunGlow = Color(0x73FFD6A0)
    val MoonGlow = Color(0x2EF3F1E6)
    val ScrimDark = Color(0xFF120E1C)
    val ScrimLight = Color(0xFFEEF3FA)
}
