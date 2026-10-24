package com.hirahira.snowing.feature.home

import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hirahira.snowing.BuildConfig
import com.hirahira.snowing.feature.moods.MoodsRoute
import com.hirahira.snowing.feature.onboarding.OnboardingRoute
import com.hirahira.snowing.feature.snow.SnowRoute
import com.hirahira.snowing.feature.stats.StatsRoute
import com.hirahira.snowing.ui.theme.Sky
import com.hirahira.snowing.ui.theme.SnowingColors
import com.hirahira.snowing.ui.theme.SnowingTheme

/** The app's root: picks the theme for the current sky and fills the pages. */
@Composable
fun HomeRoute(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = remember(state.skyHour) { SnowingColors.forSky(Sky.at(state.skyHour)) }
    SystemBarIcons(dark = colors.isLightSky)
    val context = LocalContext.current
    SnowingTheme(colors = colors) {
        HomeScreen(
            state = state,
            // Debug builds open the Prototype lab from a long press on the Moods tab.
            onPageLongPress = if (BuildConfig.DEBUG) {
                { page -> if (page == HomePage.MOODS) context.openLab() }
            } else {
                null
            },
            onboarding = { backdrop -> OnboardingRoute(backdrop) },
            sceneMotion = rememberSceneMotion(),
        ) { page ->
            when (page) {
                HomePage.SNOW -> SnowRoute()
                HomePage.MOODS -> MoodsRoute()
                HomePage.STATS -> StatsRoute()
            }
        }
    }
}

/** Only debug builds declare an activity for this action (src/debug: LabActivity). */
internal const val ACTION_OPEN_LAB = "com.hirahira.snowing.action.OPEN_LAB"

private fun Context.openLab() {
    try {
        startActivity(Intent(ACTION_OPEN_LAB).setPackage(packageName))
    } catch (e: ActivityNotFoundException) {
        Log.w("HomeRoute", "No lab in this build", e)
    }
}

/**
 * Whether the background may move: not with animations removed in accessibility settings,
 * nor in battery saver. Battery saver is often switched in Quick Settings, which does not
 * pause the app, so it is watched; the animation setting is read when the app comes back.
 */
@Composable
private fun rememberSceneMotion(): Boolean {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(context.sceneMotionAllowed()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = context.sceneMotionAllowed() }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                allowed = context.sceneMotionAllowed()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return allowed
}

private fun Context.sceneMotionAllowed(): Boolean =
    ValueAnimator.areAnimatorsEnabled() && !getSystemService(PowerManager::class.java).isPowerSaveMode

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
