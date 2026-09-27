package com.hirahira.snowing.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens. Screens and components read these through [SnowingTheme]
 * and never use literal colors, sizes or text styles, so a new design
 * (or several themes) is a change of values, not of screens.
 */
@Immutable
data class SnowingColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val primary: Color,
    val onPrimary: Color,
    val content: Color,
    val contentMuted: Color,
    val attention: Color,
    val isDark: Boolean,
)

@Immutable
data class SnowingSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 40.dp,
)

@Immutable
data class SnowingSizes(
    val primaryButtonHeight: Dp = 56.dp,
)

@Immutable
data class SnowingShapes(
    val card: Shape = RoundedCornerShape(20.dp),
    val control: Shape = RoundedCornerShape(14.dp),
)

@Immutable
data class SnowingTypography(
    val display: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
)
