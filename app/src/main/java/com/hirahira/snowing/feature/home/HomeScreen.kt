package com.hirahira.snowing.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hirahira.snowing.R
import com.hirahira.snowing.ui.components.Landscape
import com.hirahira.snowing.ui.components.PageTabs
import com.hirahira.snowing.ui.components.Scrim
import com.hirahira.snowing.ui.theme.Sky
import com.hirahira.snowing.ui.theme.SnowingColors
import com.hirahira.snowing.ui.theme.SnowingTheme
import kotlinx.coroutines.launch

/** Pages left to right; the app opens on [SNOW]. */
enum class HomePage { STATS, SNOW, MOODS }

/**
 * Three pages over one parallax landscape (design 6a). Each page draws itself through [page].
 * Before them, once, [onboarding]: a panel over the same landscape, given a copy of it to blur
 * (ADR-0011, design 6.2). Without [sceneMotion]
 * (reduced motion, battery saver) the scene stands still.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    onPageLongPress: ((HomePage) -> Unit)? = null,
    onboarding: @Composable (backdrop: @Composable () -> Unit) -> Unit = {},
    sceneMotion: Boolean = true,
    page: @Composable (HomePage) -> Unit,
) {
    val pages = HomePage.entries
    val pagerState = rememberPagerState(initialPage = HomePage.SNOW.ordinal) { pages.size }
    val scope = rememberCoroutineScope()
    val pageOffset = { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    val sceneColors = SnowingTheme.colors
    val scene: @Composable () -> Unit = {
        Landscape(
            hour = state.skyHour,
            pageOffset = pageOffset,
            smokeLean = state.smokeLean,
            smokeMoving = sceneMotion,
        )
    }
    // The onboarding panel blurs a copy of the scene behind it, in the sky's colors, not its own.
    val backdrop: @Composable () -> Unit = {
        SnowingTheme(colors = sceneColors) { Box(Modifier.fillMaxSize().background(sceneColors.sky.skyTop)) { scene() } }
    }
    CompositionLocalProvider(LocalContentColor provides SnowingTheme.colors.ink) {
        Box(modifier.fillMaxSize().background(SnowingTheme.colors.sky.skyTop)) {
            scene()
            AnimatedContent(
                targetState = state.content,
                // The onboarding panel slides in and out by itself; the pages fade in once it is gone.
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "home content",
            ) { content ->
                when (content) {
                    HomeContent.LOADING -> Unit
                    HomeContent.ONBOARDING -> onboarding(backdrop)
                    HomeContent.PAGES -> Box(Modifier.fillMaxSize()) {
                        // The Moods page's scrim belongs to the scene, not the page: it fades in as the page
                        // comes in, instead of sliding in with a hard edge.
                        Scrim(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(SnowingTheme.sizes.moodsScrim)
                                .graphicsLayer { alpha = (pageOffset() - HomePage.SNOW.ordinal).coerceIn(0f, 1f) },
                        )
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                            page(pages[index])
                        }
                        PageTabs(
                            titles = pages.map { stringResource(it.titleRes()) },
                            selectedIndex = pagerState.currentPage,
                            position = pageOffset,
                            onSelect = { scope.launch { pagerState.animateScrollToPage(it) } },
                            onLongPress = onPageLongPress?.let { { index -> it(pages[index]) } },
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = SnowingTheme.spacing.tabsTop)
                                .padding(horizontal = SnowingTheme.spacing.screen),
                        )
                    }
                }
            }
        }
    }
}

private fun HomePage.titleRes(): Int = when (this) {
    HomePage.STATS -> R.string.tab_stats
    HomePage.SNOW -> R.string.tab_snow
    HomePage.MOODS -> R.string.tab_moods
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun HomeScreenDuskPreview() {
    SnowingTheme(colors = SnowingColors.forSky(Sky.at(Sky.HOUR_DUSK))) {
        HomeScreen(HomeUiState(skyHour = Sky.HOUR_DUSK, content = HomeContent.PAGES)) {}
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun HomeScreenDayPreview() {
    SnowingTheme(colors = SnowingColors.forSky(Sky.at(Sky.HOUR_DAY))) {
        HomeScreen(HomeUiState(skyHour = Sky.HOUR_DAY, content = HomeContent.PAGES)) {}
    }
}
