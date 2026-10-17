package com.hirahira.snowing.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hirahira.snowing.R
import com.hirahira.snowing.settings.FlakeSize
import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.TimeOfDay
import com.hirahira.snowing.settings.WindStrength

// What the user sees for each setting value; shared by every screen that shows one.

@Composable
fun MoodChoice.displayName(): String = stringResource(
    when (this) {
        MoodChoice.FIRST_SNOW -> R.string.mood_first_snow
        MoodChoice.FLUFF -> R.string.mood_fluff
        MoodChoice.HEAVY -> R.string.mood_heavy
        MoodChoice.BLIZZARD -> R.string.mood_blizzard
        MoodChoice.CUSTOM -> R.string.mood_custom
    },
)

@Composable
fun FlakeSize.displayName(): String = stringResource(
    when (this) {
        FlakeSize.S -> R.string.flake_size_s
        FlakeSize.M -> R.string.flake_size_m
        FlakeSize.L -> R.string.flake_size_l
    },
)

@Composable
fun WindStrength.displayName(): String = stringResource(
    when (this) {
        WindStrength.CALM -> R.string.wind_calm
        WindStrength.GENTLE -> R.string.wind_gentle
        WindStrength.GUSTY -> R.string.wind_gusty
    },
)

@Composable
fun TimeOfDay.displayName(): String = stringResource(
    when (this) {
        TimeOfDay.AUTO -> R.string.tod_auto
        TimeOfDay.DAWN -> R.string.tod_dawn
        TimeOfDay.DAY -> R.string.tod_day
        TimeOfDay.DUSK -> R.string.tod_dusk
        TimeOfDay.NIGHT -> R.string.tod_night
    },
)
