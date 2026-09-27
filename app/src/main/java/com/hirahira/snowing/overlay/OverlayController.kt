package com.hirahira.snowing.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Starts and stops the snow overlay. The UI talks only to this interface. */
interface OverlayController {
    val isRunning: StateFlow<Boolean>

    /** "Display over other apps" — granted by the user in system settings. */
    fun hasPermission(): Boolean

    fun start()

    /** Snow stops falling; the service ends by itself once the last flakes are out (ADR-0006 §5). */
    fun stop()
}

class ServiceOverlayController(context: Context) : OverlayController {
    private val appContext = context.applicationContext
    private val serviceIntent = Intent(appContext, SnowOverlayService::class.java)

    override val isRunning: StateFlow<Boolean> = OverlayRuntime.running

    override fun hasPermission(): Boolean = Settings.canDrawOverlays(appContext)

    override fun start() {
        if (!hasPermission()) return
        ContextCompat.startForegroundService(appContext, serviceIntent)
    }

    override fun stop() {
        if (!OverlayRuntime.running.value) return
        try {
            // Allowed from the background: the app is running a foreground service.
            appContext.startService(Intent(serviceIntent).setAction(SnowOverlayService.ACTION_STOP))
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Graceful stop refused, stopping at once", e)
            appContext.stopService(serviceIntent)
        }
    }

    private companion object {
        const val TAG = "OverlayController"
    }
}

/** Process-wide running flag, written only by [SnowOverlayService]. */
internal object OverlayRuntime {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun setRunning(running: Boolean) {
        _running.value = running
    }
}
