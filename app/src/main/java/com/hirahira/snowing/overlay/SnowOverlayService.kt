package com.hirahira.snowing.overlay

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.hirahira.snowing.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Draws the snow while it runs; knows nothing about whether it *should* run.
 * That choice lives in [com.hirahira.snowing.power.SnowSwitch] — this service
 * only reports reality through [OverlayRuntime].
 * Foreground, because Android kills background services that hold a window.
 * See docs/adr/0001-overlay-window.md and docs/adr/0005-snow-state.md.
 */
class SnowOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settingsJob: Job? = null
    private var window: SnowOverlayWindow? = null
    // Created in onCreate: the service has no Context before that.
    private lateinit var permissionWatcher: OverlayPermissionWatcher

    // Nothing to animate while the display is off.
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            window?.setPaused(intent.action == Intent.ACTION_SCREEN_OFF)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        OverlayNotifications.ensureChannel(this)
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        permissionWatcher = OverlayPermissionWatcher(this, onRevoked = { stopSelf() })
        permissionWatcher.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground must come first: a service started with
        // startForegroundService() crashes if it stops without calling it.
        ServiceCompat.startForeground(
            this,
            OverlayNotifications.NOTIFICATION_ID,
            OverlayNotifications.build(this),
            foregroundServiceType(),
        )
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        OverlayRuntime.setRunning(true)
        if (settingsJob == null) settingsJob = scope.launch { followSettings() }
        return START_STICKY
    }

    private suspend fun followSettings() {
        appContainer.settingsRepository.settings.collect { settings ->
            val config = SnowConfigMapper.toConfig(settings)
            val options = SnowConfigMapper.toRenderOptions(settings)
            val current = window ?: attachWindow(SnowOverlayWindow(this, config)) ?: return@collect
            current.apply(config, options)
        }
    }

    /** Permission can disappear between the check and the attach; the window manager then throws. */
    private fun attachWindow(candidate: SnowOverlayWindow): SnowOverlayWindow? =
        try {
            candidate.attach()
            window = candidate
            candidate
        } catch (e: WindowManager.BadTokenException) {
            Log.w(TAG, "Overlay window rejected", e)
            stopSelf()
            null
        } catch (e: SecurityException) {
            Log.w(TAG, "Overlay permission missing", e)
            stopSelf()
            null
        }

    override fun onDestroy() {
        scope.cancel()
        permissionWatcher.stop()
        unregisterReceiver(screenReceiver)
        window?.detach()
        window = null
        OverlayRuntime.setRunning(false)
        super.onDestroy()
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    private companion object {
        const val TAG = "SnowOverlayService"
    }
}
