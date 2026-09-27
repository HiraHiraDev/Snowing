package com.hirahira.snowing.ui.theme

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
private val LocalTypography = staticCompositionLocalOf<SnowingTypography> { error("SnowingTheme not provided") }

/** Access point for tokens: `SnowingTheme.colors.surface`, `SnowingTheme.spacing.m`, … */
object SnowingTheme {
    val colors: SnowingColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val spacing: SnowingSpacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val sizes: SnowingSizes
        @Composable @ReadOnlyComposable get() = LocalSizes.current
    val shapes: SnowingShapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current
    val typography: SnowingTypography
        @Composable @ReadOnlyComposable get() = LocalTypography.current
}

/**
 * Provides Snowing tokens and bridges them into MaterialTheme, so Material
 * components (Slider, Switch, Button) follow the same palette.
 */
@Composable
fun SnowingTheme(
    colors: SnowingColors = PlaceholderNightColors,
    typography: SnowingTypography = PlaceholderTypography,
    spacing: SnowingSpacing = SnowingSpacing(),
    sizes: SnowingSizes = SnowingSizes(),
    shapes: SnowingShapes = SnowingShapes(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalSpacing provides spacing,
        LocalSizes provides sizes,
        LocalShapes provides shapes,
        LocalTypography provides typography,
    ) {
        MaterialTheme(colorScheme = colors.toMaterialColorScheme(), content = content)
    }
}

private fun SnowingColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        background = background,
        onBackground = content,
        surface = surface,
        onSurface = content,
        surfaceVariant = surfaceRaised,
        onSurfaceVariant = contentMuted,
        surfaceContainerHighest = surfaceRaised,
        outline = contentMuted,
    )
}
