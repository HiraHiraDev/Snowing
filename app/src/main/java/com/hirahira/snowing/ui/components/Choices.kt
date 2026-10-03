package com.hirahira.snowing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
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
    ) {
        repeat(max) { index ->
            Box(
                Modifier
                    .size(sizes.levelMarkWidth, sizes.levelMarkHeight)
                    .clip(SnowingTheme.shapes.levelMark)
                    .background(if (index < level) SnowingTheme.colors.primary else SnowingTheme.colors.track),
            )
        }
    }
}

/** One option of a single-choice list; put several in a Column with `Modifier.selectableGroup()`. */
@Composable
fun SelectableRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val shape = SnowingTheme.shapes.control
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (selected) Modifier.background(SnowingTheme.colors.surfaceRaised, shape) else Modifier)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = SnowingTheme.spacing.m, vertical = SnowingTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = if (selected) SnowingTheme.typography.title else SnowingTheme.typography.body,
        )
        trailing()
    }
}

/** A few mutually exclusive options side by side, e.g. S / M / L. */
@Composable
fun SegmentedChoice(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(text = label, style = SnowingTheme.typography.body)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = SnowingTheme.spacing.xs)) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(text = option, style = SnowingTheme.typography.label)
                }
            }
        }
    }
}
