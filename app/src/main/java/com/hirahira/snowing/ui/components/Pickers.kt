package com.hirahira.snowing.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hirahira.snowing.ui.theme.SnowingTheme

// Pickers take the options themselves (an enum's entries, a list of moods) rather than
// indexes, so a screen never maps positions back to values.

/**
 * A few mutually exclusive options side by side, optionally under a name (design 6a): equal
 * cells over a line, the picked one in the accent color and underlined. The underline glides
 * to a new pick, a little past it and back.
 */
@Composable
fun <T> OptionPicker(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
) {
    val colors = SnowingTheme.colors
    val motion = SnowingTheme.motion
    val haptics = rememberControlHaptics()
    // Five cells are narrow, so their names step down a size.
    val style = if (options.size > ROOMY_OPTIONS) SnowingTheme.typography.metaStrong else SnowingTheme.typography.label
    val underlineHeight = SnowingTheme.sizes.optionUnderline
    val pickedIndex = options.indexOf(selected)
    val underlineAt = animateFloatAsState(pickedIndex.toFloat(), motion.glide.spec(), label = "underline")
    Column(modifier.fillMaxWidth().enabledLook(enabled)) {
        if (label != null) ControlLabel(label)
        Box(Modifier.fillMaxWidth()) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                thickness = SnowingTheme.sizes.line,
                color = colors.line,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .drawBehind {
                        if (pickedIndex < 0) return@drawBehind
                        val cell = size.width / options.size
                        val width = cell * UNDERLINE_WIDTH
                        val height = underlineHeight.toPx()
                        drawRect(
                            color = colors.accentFill,
                            topLeft = Offset(cell * (underlineAt.value + 0.5f) - width / 2, size.height - height),
                            size = Size(width, height),
                        )
                    },
            ) {
                for (option in options) {
                    val picked = option == selected
                    val tint by animateColorAsState(if (picked) colors.accentText else colors.inkSoft, motion.tint(), label = "option")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(SnowingTheme.sizes.touchTarget)
                            .selectable(
                                selected = picked,
                                enabled = enabled,
                                role = Role.RadioButton,
                                onClick = {
                                    if (!picked) haptics.pick()
                                    onSelect(option)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = optionLabel(option),
                            style = style,
                            color = tint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private const val ROOMY_OPTIONS = 3

/** How much of a cell the picked option's underline spans. */
private const val UNDERLINE_WIDTH = 0.64f

/**
 * Options shown as dots of different sizes beside a name, e.g. flake size. The picked dot
 * takes the accent color and a soft ring that springs out around it.
 */
@Composable
fun <T> DotPicker(
    label: String,
    options: List<T>,
    selected: T,
    dotSize: (T) -> Dp,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SnowingTheme.colors
    val sizes = SnowingTheme.sizes
    val motion = SnowingTheme.motion
    val haptics = rememberControlHaptics()
    Row(
        modifier = modifier.fillMaxWidth().enabledLook(enabled),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(label, Modifier.weight(1f))
        Row(Modifier.selectableGroup()) {
            for (option in options) {
                val picked = option == selected
                val description = optionLabel(option)
                val halo = animateDpAsState(if (picked) sizes.dotHalo else 0.dp, motion.pop.spec(), label = "halo")
                val fill by animateColorAsState(if (picked) colors.accentFill else colors.tickDim, motion.tint(), label = "dot")
                Box(
                    modifier = Modifier
                        .size(sizes.touchTarget)
                        .selectable(
                            selected = picked,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = {
                                if (!picked) haptics.pick()
                                onSelect(option)
                            },
                        )
                        .semantics { contentDescription = description },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(dotSize(option))
                            .drawBehind {
                                // The spring undershoots below zero as the ring leaves.
                                val ring = halo.value.toPx()
                                if (ring > 0f) drawCircle(colors.accentHalo, radius = size.minDimension / 2 + ring)
                            }
                            .background(fill, SnowingTheme.shapes.circle),
                    )
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
    val haptics = rememberControlHaptics()
    Column(modifier.fillMaxWidth().selectableGroup()) {
        for (item in items) {
            val selected = isSelected(item)
            val pick = {
                if (!selected) haptics.pick()
                onSelect(item)
            }
            ChoiceRow(title = title(item), selected = selected, onClick = pick) {
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
    val colors = SnowingTheme.colors
    val tint by animateColorAsState(if (selected) colors.accentText else colors.ink, SnowingTheme.motion.tint(), label = "choice")
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
                color = tint,
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
