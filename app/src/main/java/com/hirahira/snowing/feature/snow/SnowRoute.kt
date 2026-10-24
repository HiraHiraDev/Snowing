package com.hirahira.snowing.feature.snow

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hirahira.snowing.feature.common.openOverlaySettings
import com.hirahira.snowing.feature.common.startFeedback
import com.hirahira.snowing.feature.common.stopFeedback

/**
 * Wires [SnowViewModel] to [SnowScreen] and owns the Android side effects: system
 * settings, the notification permission and haptics, so the screen stays pure.
 */
@Composable
fun SnowRoute(viewModel: SnowViewModel = viewModel(factory = SnowViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    // Permission is granted outside the app, and the overlay may have died
    // while we were away (force stop, task manager), so re-sync on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(SnowEvent.Resumed)
    }

    // The ongoing notification is optional, so snow starts whatever the answer.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onEvent(SnowEvent.ToggleSnow) }

    SnowScreen(
        state = state,
        onEvent = { event ->
            when {
                event != SnowEvent.ToggleSnow -> viewModel.onEvent(event)
                state.startNeedsPermission -> context.openOverlaySettings()
                state.snowEnabled -> {
                    view.stopFeedback()
                    viewModel.onEvent(event)
                }
                else -> {
                    view.startFeedback()
                    if (needsNotificationPermission(context)) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onEvent(event)
                    }
                }
            }
        },
    )
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
