package com.hirahira.snowing.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColors = staticCompositionLocalOf<SnowingColors> { error("SnowingTheme not provided") }
private val LocalSpacing = staticCompositionLocalOf { SnowingSpacing() }
private val LocalSizes = staticCompositionLocalOf { SnowingSizes() }
private val LocalShapes = staticCompositionLocalOf { SnowingShapes() }
internal val LocalMotion = staticCompositionLocalOf { SnowingMotion() }
private val LocalTypography = staticCompositionLocalOf<SnowingTypography> { error("SnowingTheme not provided") }

/** Access point for tokens: `SnowingTheme.colors.ink`, `SnowingTheme.spacing.m`, … */
object SnowingTheme {
    val colors: SnowingColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val spacing: SnowingSpacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val sizes: SnowingSizes
        @Composable @ReadOnlyComposable get() = LocalSizes.current
    val shapes: SnowingShapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current
    val motion: SnowingMotion
        @Composable @ReadOnlyComposable get() = LocalMotion.current
    val typography: SnowingTypography
        @Composable @ReadOnlyComposable get() = LocalTypography.current
}

/**
 * Provides Snowing tokens and bridges them into MaterialTheme, so Material
 * components (Slider, Switch, Button) follow the same palette. [colors] follow
 * the sky: `SnowingColors.forSky(Sky.at(hour))`.
 */
@Composable
fun SnowingTheme(
    colors: SnowingColors = SnowingColors.forSky(Sky.Night),
    typography: SnowingTypography = DefaultTypography,
    spacing: SnowingSpacing = SnowingSpacing(),
    sizes: SnowingSizes = SnowingSizes(),
    shapes: SnowingShapes = SnowingShapes(),
    motion: SnowingMotion = SnowingMotion(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalSpacing provides spacing,
        LocalSizes provides sizes,
        LocalShapes provides shapes,
        LocalMotion provides motion,
        LocalTypography provides typography,
    ) {
        MaterialTheme(colorScheme = colors.toMaterialColorScheme()) {
            // Inside MaterialTheme, which would otherwise provide its ripple.
            CompositionLocalProvider(LocalIndication provides PressFade, content = content)
        }
    }
}

private fun SnowingColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isLightSky) lightColorScheme() else darkColorScheme()
    return base.copy(
        primary = accentFill,
        onPrimary = ctaDot,
        secondaryContainer = accentFill.copy(alpha = SELECTED_ALPHA),
        onSecondaryContainer = ink,
        background = sky.skyTop,
        onBackground = ink,
        surface = sky.skyTop,
        onSurface = ink,
        surfaceVariant = line,
        onSurfaceVariant = inkSoft,
        surfaceContainerHighest = tickDim,
        outline = inkSoft,
        outlineVariant = line,
    )
}

private const val SELECTED_ALPHA = 0.22f
