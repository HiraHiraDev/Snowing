package com.hirahira.snowing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.hirahira.snowing.ui.theme.SnowingTheme

// Numbers shown as numbers: a row of thin bars and a table of figures (design 6a, Stats).

/** One bar: [fraction] of the chart's height, 0..1. */
data class Bar(
    val fraction: Float,
    val label: String,
    val highlighted: Boolean = false,
)

/**
 * Thin bars side by side with a label under each, e.g. snow per day of the week. The
 * highlighted bar takes the accent color; a zero bar keeps a stub so the row keeps its shape.
 * TalkBack reads [contentDescription] instead of the bars one by one.
 */
@Composable
fun BarChart(bars: List<Bar>, contentDescription: String, modifier: Modifier = Modifier) {
    val sizes = SnowingTheme.sizes
    val colors = SnowingTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalArrangement = Arrangement.spacedBy(sizes.chartLabelGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(sizes.chartHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            for (bar in bars) {
                Box(
                    Modifier.width(sizes.labelColumn).fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .width(sizes.barWidth)
                            .height(maxOf(sizes.chartHeight * bar.fraction.coerceIn(0f, 1f), sizes.barStub))
                            .background(if (bar.highlighted) colors.accentFill else colors.tickDim, SnowingTheme.shapes.tick),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (bar in bars) {
                Text(
                    text = bar.label,
                    modifier = Modifier.width(sizes.labelColumn),
                    style = SnowingTheme.typography.axis,
                    color = colors.inkSoft,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A short table of named figures, one per row between lines, e.g. "snow today  3 h 12 min". */
@Composable
fun FigureTable(rows: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val colors = SnowingTheme.colors
    Column(modifier.fillMaxWidth()) {
        for ((name, value) in rows) {
            HorizontalDivider(thickness = SnowingTheme.sizes.line, color = colors.line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SnowingTheme.spacing.row)
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BodyText(name, soft = true)
                BodyText(value)
            }
        }
        HorizontalDivider(thickness = SnowingTheme.sizes.line, color = colors.line)
    }
}
