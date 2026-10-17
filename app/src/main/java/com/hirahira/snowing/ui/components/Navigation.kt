package com.hirahira.snowing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.hirahira.snowing.ui.theme.SnowingTheme

/**
 * Page names spread across the top; the active one in the accent color with a dot under it.
 * [onLongPress] is for hidden tools; leave it null and a long press does nothing.
 */
@Composable
fun PageTabs(
    titles: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: ((Int) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        titles.forEachIndexed { index, title ->
            val selected = index == selectedIndex
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
                Text(
                    text = title,
                    style = SnowingTheme.typography.label,
                    color = if (selected) SnowingTheme.colors.accentText else SnowingTheme.colors.inkSoft,
                )
                if (selected) {
                    Box(
                        Modifier
                            .padding(top = SnowingTheme.sizes.tabDot)
                            .size(SnowingTheme.sizes.tabDot)
                            .clip(SnowingTheme.shapes.circle)
                            .background(SnowingTheme.colors.accentFill),
                    )
                }
            }
        }
    }
}
