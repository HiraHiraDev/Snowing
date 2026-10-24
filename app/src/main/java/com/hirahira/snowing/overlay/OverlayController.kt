package com.hirahira.snowing.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How snow goes away when it is switched off (ADR-0006 §Zmiana 2026-10-18). */
enum class StopStyle {
    /** From the app: no new flakes, the low ones fall out, the rest fades; a few seconds. */
    GENTLE,

    /** From the tile or the notification: gone at once, e.g. over a screen that refuses taps under it. */
    AT_ONCE,
}

/** Starts and stops the snow overlay. The UI talks only to this interface. */
interface OverlayController {
    val isRunning: StateFlow<Boolean>

    /** "Display over other apps", granted in system settings. */
    fun hasPermission(): Boolean

    /** @return false when Android refused to start the service from the background. */
    fun start(): Boolean

    fun stop(style: StopStyle)
}

class ServiceOverlayController(context: Context) : OverlayController {
    private val appContext = context.applicationContext
    private val serviceIntent = Intent(appContext, SnowOverlayService::class.java)

    override val isRunning: StateFlow<Boolean> = OverlayRuntime.running

    override fun hasPermission(): Boolean = Settings.canDrawOverlays(appContext)

    override fun start(): Boolean {
        if (!hasPermission()) return false
        return try {
            ContextCompat.startForegroundService(appContext, serviceIntent)
            true
        } catch (e: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException on API 31+: no exemption for this caller.
            Log.w(TAG, "Start refused from the background", e)
            false
        }
    }

    override fun stop(style: StopStyle) {
        if (!OverlayRuntime.running.value) return
        if (style == StopStyle.AT_ONCE) {
            appContext.stopService(serviceIntent)
            return
        }
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
