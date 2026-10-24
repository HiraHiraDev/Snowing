package com.hirahira.snowing.feature.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hirahira.snowing.R
import com.hirahira.snowing.feature.common.openNotificationSettings
import com.hirahira.snowing.feature.common.openOverlaySettings
import com.hirahira.snowing.feature.common.startFeedback
import com.hirahira.snowing.feature.tile.SnowTileService

/** Wires [OnboardingViewModel] to [OnboardingScreen] and does what only Android can: settings, questions, the tile. */
@Composable
fun OnboardingRoute(
    backdrop: @Composable () -> Unit,
    viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    // Permissions are granted outside the app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(OnboardingEvent.Resumed)
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onEvent(OnboardingEvent.NotificationsAnswered) }

    OnboardingScreen(
        state = state,
        backdrop = backdrop,
        onEvent = { event ->
            when (event) {
                OnboardingEvent.AllowOverlay -> context.openOverlaySettings()
                OnboardingEvent.AllowNotifications -> when (state.notificationsAction) {
                    NotificationsAction.ASK -> notificationPermission.launch(NOTIFICATIONS_PERMISSION)
                    NotificationsAction.OPEN_SETTINGS -> context.openNotificationSettings()
                }
                OnboardingEvent.AddTile -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.requestTile { added -> viewModel.onEvent(OnboardingEvent.TileAnswered(added)) }
                }
                OnboardingEvent.LetItSnow -> {
                    view.startFeedback()
                    viewModel.onEvent(event)
                }
                else -> viewModel.onEvent(event)
            }
        },
    )
}

// The view model offers ASK only on API 33+ (StopShortcuts.canAskForNotifications).
@SuppressLint("InlinedApi")
private const val NOTIFICATIONS_PERMISSION = Manifest.permission.POST_NOTIFICATIONS

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Context.requestTile(onAnswer: (added: Boolean) -> Unit) {
    getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(this, SnowTileService::class.java),
        getString(R.string.tile_label),
        Icon.createWithResource(this, R.drawable.ic_stat_snow),
        mainExecutor,
    ) { result ->
        onAnswer(
            result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED,
        )
    }
}
