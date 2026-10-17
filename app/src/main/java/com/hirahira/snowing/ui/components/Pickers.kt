package com.hirahira.snowing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.hirahira.snowing.ui.theme.SnowingTheme

// Pickers take the options themselves (an enum's entries, a list of moods) rather than
// indexes, so a screen never maps positions back to values.

/** A few mutually exclusive options side by side under a name, e.g. flake size S / M / L. */
@Composable
fun <T> OptionPicker(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SnowingTheme.spacing.s)) {
        ControlLabel(label)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    ControlLabel(optionLabel(option))
                }
            }
        }
    }
}

/**
 * A list to pick one item from, one row per item separated by lines, e.g. the moods.
 * [trailing] adds something read-only at the end of a row.
 */
@Composable
fun <T> ChoiceList(
    items: List<T>,
    isSelected: (T) -> Boolean,
    title: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (T) -> Unit = {},
) {
    Column(modifier.fillMaxWidth().selectableGroup()) {
        for (item in items) {
            ChoiceRow(title = title(item), selected = isSelected(item), onClick = { onSelect(item) }) {
                trailing(item)
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SnowingTheme.sizes.touchTarget)
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .padding(vertical = SnowingTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = SnowingTheme.typography.listItem,
                color = if (selected) SnowingTheme.colors.accentText else SnowingTheme.colors.ink,
            )
            trailing()
        }
        HorizontalDivider(thickness = SnowingTheme.sizes.line, color = SnowingTheme.colors.line)
    }
}

/** A read-only row of marks showing [level] out of [max], e.g. a mood's intensity. */
@Composable
fun LevelIndicator(
    level: Int,
    max: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val sizes = SnowingTheme.sizes
    Row(
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        horizontalArrangement = Arrangement.spacedBy(sizes.levelMarkGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(max) { index ->
            val lit = index < level
            Box(
                Modifier
                    .size(sizes.levelMarkWidth, if (lit) sizes.levelMarkHeight else sizes.levelMarkDimHeight)
                    .clip(SnowingTheme.shapes.tick)
                    .background(if (lit) SnowingTheme.colors.accentFill else SnowingTheme.colors.tickDim),
            )
        }
    }
}
