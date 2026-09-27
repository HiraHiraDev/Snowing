package com.hirahira.snowing.overlay

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Process

/**
 * Watches the "Display over other apps" permission while the overlay runs.
 *
 * Revoking it does not kill the app: the system only hides our window, so
 * the service would keep animating an invisible overlay and report snow that
 * isn't there. `Settings.canDrawOverlays()` can return the stale value inside
 * the change callback, so the op mode is read directly.
 */
internal class OverlayPermissionWatcher(
    private val context: Context,
    private val onRevoked: () -> Unit,
) {
    private val appOps = context.getSystemService(AppOpsManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val listener = AppOpsManager.OnOpChangedListener { op, packageName ->
        if (op == AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW && packageName == context.packageName) {
            // Delivered on a binder thread.
            mainHandler.post { if (!isGranted()) onRevoked() }
        }
    }

    fun start() {
        appOps.startWatchingMode(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, context.packageName, listener)
    }

    fun stop() {
        appOps.stopWatchingMode(listener)
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun isGranted(): Boolean {
        // Undeprecated again in API 36 (replacing unsafeCheckOpNoThrow); same code path on every API level.
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW, Process.myUid(), context.packageName)
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(Manifest.permission.SYSTEM_ALERT_WINDOW) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
