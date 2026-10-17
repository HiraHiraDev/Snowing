package com.hirahira.snowing.feature.snow

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

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

private fun Context.openOverlaySettings() {
    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
}

// Design 6a: a confirming buzz to start, a light tick to stop. CONFIRM needs API 30.
private fun View.startFeedback() {
    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }
    performHapticFeedback(type)
}

private fun View.stopFeedback() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
