package com.hirahira.snowing.feature.home

/** What the shell shows over the landscape. */
enum class HomeContent {
    /** Until the store says whether onboarding is done: the landscape alone, nothing that could flash. */
    LOADING,
    ONBOARDING,
    PAGES,
}

data class HomeUiState(
    /** 0..24; picks the sky palette and where the sun or moon stands. */
    val skyHour: Float,
    val content: HomeContent = HomeContent.LOADING,
    /** -1..1, see [SmokeLean]. */
    val smokeLean: Float = 0f,
)
