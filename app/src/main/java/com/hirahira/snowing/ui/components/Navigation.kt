package com.hirahira.snowing.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlinx.coroutines.launch

/**
 * Page names spread across the top; the active one in the accent color with a dot under it.
 * [position] is where the pages are, fractional while they slide (0 = first page): the dot
 * follows it on a spring, so it travels between the tabs with the pages and settles with a
 * small swing. [onLongPress] is for hidden tools; leave it null and a long press does nothing.
 */
@Composable
fun PageTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    position: () -> Float = { selectedIndex.toFloat() },
    onLongPress: ((Int) -> Unit)? = null,
) {
    val colors = SnowingTheme.colors
    val motion = SnowingTheme.motion
    val dotSize = SnowingTheme.sizes.tabDot
    val glide = motion.glide.spec<Float>()
    val dotAt = remember { Animatable(position()) }
    LaunchedEffect(dotAt) {
        // A new animation per change keeps the dot's speed, so it follows a swipe smoothly.
        snapshotFlow(position).collect { target -> launch { dotAt.animateTo(target, glide) } }
    }
    val slots = remember(titles.size) { DotSlots(titles.size) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .onPlaced { slots.row = it }
            .drawWithContent {
                drawContent()
                val top = slots.measure() ?: return@drawWithContent
                val radius = dotSize.toPx() / 2
                val x = positionBetween(dotAt.value, slots.lefts) + radius
                drawCircle(colors.accentFill, radius = radius, center = Offset(x, top + radius))
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedIndex
            val tint by animateColorAsState(if (selected) colors.accentText else colors.inkSoft, motion.tint(), label = "tab")
            Column(
                modifier = Modifier
                    .heightIn(min = SnowingTheme.sizes.touchTarget)
                    .widthIn(min = SnowingTheme.sizes.touchTarget)
                    .semantics { this.selected = selected }
                    .combinedClickable(
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                        onLongClick = onLongPress?.let { { it(index) } },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = title, style = SnowingTheme.typography.label, color = tint)
                // Every tab keeps a slot for the dot; the dot itself is drawn over the row.
                Spacer(
                    Modifier
                        .padding(top = dotSize)
                        .size(dotSize)
                        .onPlaced { slots.tabs[index] = it },
                )
            }
        }
    }
}

/** Where the row and each tab's dot slot were placed; read when drawing, which follows layout. */
private class DotSlots(count: Int) {
    var row: LayoutCoordinates? = null
    val tabs = arrayOfNulls<LayoutCoordinates>(count)
    val lefts = FloatArray(count)

    /** Fills [lefts] in the row's coordinates and returns the slots' top, or null before layout. */
    fun measure(): Float? {
        val parent = row?.takeIf { it.isAttached } ?: return null
        var top = 0f
        for (i in tabs.indices) {
            val slot = tabs[i]?.takeIf { it.isAttached } ?: return null
            val at = parent.localPositionOf(slot, Offset.Zero)
            lefts[i] = at.x
            top = at.y
        }
        return top
    }
}
