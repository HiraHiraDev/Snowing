package com.hirahira.snowing.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

// How a touch looks is the theme's job (PressFade in ui/theme), so components only use clickable.

/** How a control looks when it does not respond, e.g. locked without Pro. */
fun Modifier.enabledLook(enabled: Boolean): Modifier = if (enabled) this else alpha(DISABLED_ALPHA)

private const val DISABLED_ALPHA = 0.38f
