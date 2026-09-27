package com.hirahira.snowing.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Placeholder values for the prototype. Replaced wholesale by the design
// handoff; nothing outside ui/theme references them directly.

internal val PlaceholderNightColors = SnowingColors(
    background = Color(0xFF0B1020),
    surface = Color(0xFF151B2E),
    surfaceRaised = Color(0xFF1E2640),
    primary = Color(0xFFDCE8FF),
    onPrimary = Color(0xFF0B1020),
    content = Color(0xFFF2F5FF),
    contentMuted = Color(0xFF9AA4C0),
    attention = Color(0xFFFFC56B),
    isDark = true,
)

internal val PlaceholderTypography = SnowingTypography(
    display = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
    title = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    body = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    label = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
)
