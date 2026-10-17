package com.hirahira.snowing.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role

/**
 * Makes anything tappable the way design 6a does: no ripple, the whole thing dims while
 * pressed. For large blocks (a title, a row) where a ripple would look like a card.
 */
@Composable
fun Modifier.pressable(onClick: () -> Unit, onClickLabel: String? = null, role: Role = Role.Button): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    return this
        .alpha(if (pressed) PRESSED_ALPHA else 1f)
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick,
        )
}

private const val PRESSED_ALPHA = 0.75f
