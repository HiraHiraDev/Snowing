package com.hirahira.snowing.feature.home

data class HomeUiState(
    /** 0..24; picks the sky palette and where the sun or moon stands. */
    val skyHour: Float,
)
