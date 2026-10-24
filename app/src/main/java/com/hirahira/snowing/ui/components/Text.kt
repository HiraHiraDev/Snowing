package com.hirahira.snowing.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.hirahira.snowing.ui.theme.SnowingTheme

// Text by role. Screens and components say what a text is, never which style and color it
// takes, so a role looks the same everywhere and changes in one place.

/** The one big name a screen is about, e.g. the active mood. */
@Composable
fun HeroTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.moodHero)
}

/** A sentence-long title of a screen that has no hero, e.g. an onboarding step. */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.pageTitle)
}

/** Small accent capitals above a title, e.g. "Step 1 of 2". */
@Composable
fun StepLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = SnowingTheme.typography.stepLabel,
        color = SnowingTheme.colors.accentText,
    )
}

/** Running text of a panel, set back from the titles. */
@Composable
fun Paragraph(text: String, modifier: Modifier = Modifier) {
    Paragraph(AnnotatedString(text), modifier)
}

@Composable
fun Paragraph(text: AnnotatedString, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.paragraph, color = SnowingTheme.colors.inkSoft)
}

/**
 * [template] with its `%1$s` replaced by [emphasis], set bold in full ink, e.g. the name of a
 * permission inside a sentence.
 */
@Composable
fun emphasized(template: String, emphasis: String): AnnotatedString {
    val style = SpanStyle(fontWeight = FontWeight.Bold, color = SnowingTheme.colors.ink)
    val (before, after) = template.split(PLACEHOLDER, limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    return buildAnnotatedString {
        append(before)
        withStyle(style) { append(emphasis) }
        append(after)
    }
}

private const val PLACEHOLDER = "%1${'$'}s"

/** The title of a row in a list, e.g. "Notification". */
@Composable
fun ItemTitle(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.itemTitle)
}

/** What a row in a list is about, set back from its title. */
@Composable
fun ItemBody(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.itemBody, color = SnowingTheme.colors.inkSoft)
}

/** A short instruction in a row, in full ink, where there is no button for it. */
@Composable
fun ItemNote(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.itemNote)
}

/** The quietest text, e.g. a remark at the end of a panel. */
@Composable
fun Footnote(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.footnote, color = SnowingTheme.colors.inkFaint)
}

/** An action shown as accent text under a row, e.g. "Add the tile". At least a touch target tall. */
@Composable
fun InlineAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = SnowingTheme.sizes.touchTarget)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = text, style = SnowingTheme.typography.inlineAction, color = SnowingTheme.colors.accentText)
    }
}

/** A quiet secondary action, centered under the main one, e.g. "Not now". */
@Composable
fun QuietAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SnowingTheme.sizes.touchTarget)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = SnowingTheme.typography.quietAction,
            color = SnowingTheme.colors.inkSoft,
            textAlign = TextAlign.Center,
        )
    }
}

/** A big figure a screen is about, e.g. "18 h" of snow. */
@Composable
fun StatHero(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.statsHero)
}

/** A small spaced-out heading in capitals above a figure, e.g. "THIS WEEK". */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = SnowingTheme.typography.eyebrow,
        color = SnowingTheme.colors.inkSoft,
    )
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

/** A value that stands for itself, set large, e.g. the wind's "gentle". */
@Composable
fun LargeValue(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.valueLarge)
}

/** The smallest explanation under a control, e.g. "drag the needle". */
@Composable
fun Note(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.note, color = SnowingTheme.colors.inkSoft)
}

/** Secondary explanation under a title or control, e.g. "changes apply to Heavy". */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, style = SnowingTheme.typography.meta, color = SnowingTheme.colors.inkSoft)
}

/** A sentence or paragraph; [soft] sets it back, e.g. a caption under a figure. */
@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, soft: Boolean = false) {
    Text(
        text = text,
        modifier = modifier,
        style = SnowingTheme.typography.body,
        color = if (soft) SnowingTheme.colors.inkSoft else Color.Unspecified,
    )
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
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .heightIn(min = SnowingTheme.sizes.touchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = SnowingTheme.typography.action,
            color = if (primary) SnowingTheme.colors.accentText else SnowingTheme.colors.ink,
        )
    }
}
