package com.hirahira.snowing.feature.control

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Wires [ControlViewModel] to [ControlScreen] and owns the Android side
 * effects (system settings, runtime permissions) so the screen stays pure.
 */
@Composable
fun ControlRoute(viewModel: ControlViewModel = viewModel(factory = ControlViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permission is granted outside the app, and the overlay may have died
    // while we were away (force stop, task manager) — re-sync on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onEvent(ControlEvent.Resumed)
    }

    // The ongoing notification is optional, so snow starts whatever the answer.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.onEvent(ControlEvent.ToggleSnow) }

    ControlScreen(
        state = state,
        onEvent = { event ->
            if (event == ControlEvent.ToggleSnow && !state.snowEnabled && needsNotificationPermission(context)) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onEvent(event)
            }
        },
        onGrantOverlayPermission = {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
            )
        },
    )
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
