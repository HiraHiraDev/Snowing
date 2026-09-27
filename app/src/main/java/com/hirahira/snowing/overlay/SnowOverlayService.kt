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
import kotlinx.coroutines.delay
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
    private var stopFuseJob: Job? = null
    private val isStopping: Boolean get() = stopFuseJob != null
    private var window: SnowOverlayWindow? = null
    // Created in onCreate: the service has no Context before that.
    private lateinit var permissionWatcher: OverlayPermissionWatcher

    // Nothing to animate while the display is off.
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val screenOff = intent.action == Intent.ACTION_SCREEN_OFF
            // Nobody watches the last flakes fall out on a dark screen.
            if (screenOff && isStopping) stopSelf() else window?.setPaused(screenOff)
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
        if (intent?.action == ACTION_STOP) {
            beginStop()
            return START_NOT_STICKY
        }
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
        cancelStop()
        if (settingsJob == null) settingsJob = scope.launch { followSettings() }
        return START_STICKY
    }

    /** Snow stops falling; the service ends when the last flakes are out, or after the fuse (ADR-0006 §5). */
    private fun beginStop() {
        val current = window
        if (current == null) {
            stopSelf()
            return
        }
        if (isStopping) return
        current.stopFalling()
        stopFuseJob = scope.launch {
            delay(STOP_FUSE_MS)
            current.fadeOut()
        }
    }

    /** Switched back on while stopping: new flakes enter from the top, the falling ones carry on. */
    private fun cancelStop() {
        val fuse = stopFuseJob ?: return
        fuse.cancel()
        stopFuseJob = null
        window?.resumeFalling()
    }

    private suspend fun followSettings() {
        appContainer.settingsRepository.settings.collect { settings ->
            val scene = SnowConfigMapper.toScene(settings)
            val options = SnowConfigMapper.toRenderOptions(settings)
            val current = window ?: attachWindow(SnowOverlayWindow(this, scene).apply { onDrained = { stopSelf() } })
                ?: return@collect
            current.apply(scene, options, immediate = settings.lab.instantChanges)
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

    companion object {
        /** Stop falling and end once the snow is gone, instead of vanishing at once. */
        const val ACTION_STOP = "com.hirahira.snowing.action.STOP_SNOW"

        private const val TAG = "SnowOverlayService"

        /** After this, whatever is still falling fades out (ADR-0006 §5: at most ~22 s). */
        private const val STOP_FUSE_MS = 20_000L
    }
}
