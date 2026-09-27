package com.hirahira.snowing.overlay

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
        appContext.stopService(serviceIntent)
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
