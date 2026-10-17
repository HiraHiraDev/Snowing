package com.hirahira.snowing.feature.stats

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hirahira.snowing.R
import com.hirahira.snowing.ui.components.Bar
import com.hirahira.snowing.ui.components.BarChart
import com.hirahira.snowing.ui.components.BodyText
import com.hirahira.snowing.ui.components.Eyebrow
import com.hirahira.snowing.ui.components.FigureTable
import com.hirahira.snowing.ui.components.Page
import com.hirahira.snowing.ui.components.StatHero
import com.hirahira.snowing.ui.theme.Sky
import com.hirahira.snowing.ui.theme.SnowingColors
import com.hirahira.snowing.ui.theme.SnowingTheme
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * The Stats page (design 6a layout, simple content in v1): this week's snow time large,
 * a bar per day, and today and the week to the minute.
 */
@Composable
fun StatsScreen(state: StatsUiState, modifier: Modifier = Modifier) {
    if (!state.loaded) return
    val spacing = SnowingTheme.spacing
    val locale = LocalConfiguration.current.locales[0]
    val spokenDays = state.days.map { "${it.day.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${exactDuration(it.minutes)}" }
    Page(top = spacing.pageTopStats, modifier = modifier) {
        Eyebrow(stringResource(R.string.stats_eyebrow))
        StatHero(roundDuration(state.weekMinutes), Modifier.padding(top = spacing.s))
        BodyText(stringResource(R.string.stats_caption), Modifier.padding(top = spacing.xs), soft = true)
        BarChart(
            bars = state.days.map { Bar(it.fraction, it.day.getDisplayName(TextStyle.NARROW_STANDALONE, locale), it.isToday) },
            contentDescription = stringResource(R.string.stats_chart_description, spokenDays.joinToString()),
            modifier = Modifier.padding(top = spacing.group),
        )
        FigureTable(
            rows = listOf(
                stringResource(R.string.stats_today) to exactDuration(state.todayMinutes),
                stringResource(R.string.stats_week) to exactDuration(state.weekMinutes),
            ),
            modifier = Modifier.padding(top = spacing.xl),
        )
    }
}

/** Short enough for the large figure: whole hours from an hour on, minutes below. */
@Composable
private fun roundDuration(minutes: Int): String =
    if (minutes >= MINUTES_PER_HOUR) {
        stringResource(R.string.duration_h, minutes / MINUTES_PER_HOUR)
    } else {
        stringResource(R.string.duration_m, minutes)
    }

@Composable
private fun exactDuration(minutes: Int): String =
    if (minutes >= MINUTES_PER_HOUR) {
        stringResource(R.string.duration_hm, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    } else {
        stringResource(R.string.duration_m, minutes)
    }

private const val MINUTES_PER_HOUR = 60

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun StatsScreenPreview() {
    val minutes = listOf(40, 62, 30, 78, 55, 100, 0)
    SnowingTheme(colors = SnowingColors.forSky(Sky.at(Sky.HOUR_DUSK))) {
        StatsScreen(
            StatsUiState(
                loaded = true,
                weekMinutes = minutes.sum(),
                todayMinutes = 100,
                days = DayOfWeek.entries.mapIndexed { i, day -> DayBar(day, minutes[i], minutes[i] / 100f, isToday = i == 5) },
            ),
        )
    }
}
