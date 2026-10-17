package com.hirahira.snowing.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.hirahira.snowing.ui.theme.SnowingTheme

// Text by role. Screens and components say what a text is, never which style and color it
// takes, so a role looks the same everywhere and changes in one place.

/** The one big name a screen is about, e.g. the active mood. */
@Composable
fun HeroTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.moodHero)
}

/** Heading of a group of controls, e.g. "Adjust". */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.sectionHeader)
}

/**
 * Name of a control or of one of its options, e.g. "Flake size" or "Calm". Takes the
 * surrounding content color, so it also reads right inside buttons and disabled controls.
 */
@Composable
fun ControlLabel(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.label)
}

/** The current value next to a control's name, e.g. "6" or "200°". */
@Composable
fun ControlValue(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = SnowingTheme.typography.label,
        color = SnowingTheme.colors.inkSoft,
        maxLines = 1,
    )
}

/** A quiet line that explains the screen, e.g. why a permission is needed. */
@Composable
fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.hint, color = SnowingTheme.colors.inkSoft)
}

/** Secondary explanation under a title or control, e.g. "changes apply to Heavy". */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.meta, color = SnowingTheme.colors.inkSoft)
}

/** A sentence or paragraph. */
@Composable
fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.body)
}

/** Title that asks for attention, e.g. a missing permission. */
@Composable
fun AttentionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.rowTitle, color = SnowingTheme.colors.accentText)
}

/**
 * An action shown as underlined text, e.g. "Restore". [primary] takes the accent color,
 * for the action the user most likely wants ("Save as new").
 */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    Box(
        modifier = modifier
            .heightIn(min = SnowingTheme.sizes.touchTarget)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = SnowingTheme.typography.action,
            color = if (primary) SnowingTheme.colors.accentText else SnowingTheme.colors.ink,
        )
    }
}
