package com.hirahira.snowing.feature.home

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hirahira.snowing.feature.control.ControlRoute
import com.hirahira.snowing.feature.snow.SnowRoute
import com.hirahira.snowing.ui.theme.Sky
import com.hirahira.snowing.ui.theme.SnowingColors
import com.hirahira.snowing.ui.theme.SnowingTheme

/** The app's root: picks the theme for the current sky and fills the pages. */
@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = remember(state.skyHour) { SnowingColors.forSky(Sky.at(state.skyHour)) }
    SystemBarIcons(dark = colors.isLightSky)
    SnowingTheme(colors = colors) {
        HomeScreen(state) { page ->
            when (page) {
                HomePage.SNOW -> SnowRoute()
                // The prototype panel until the Moods screen of design 6a is built.
                HomePage.MOODS -> ControlRoute()
                HomePage.STATS -> Unit
            }
        }
    }
}

/** Status and navigation bar icons dark on a light sky, light otherwise. */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val window = LocalActivity.current?.window ?: return
    val view = LocalView.current
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }
}
