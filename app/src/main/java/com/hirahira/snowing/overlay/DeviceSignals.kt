package com.hirahira.snowing.overlay

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Display
import androidx.core.content.ContextCompat
import com.hirahira.snowing.engine.ThermalState

/** What the device tells the snow about its limits (ADR-0008 §4). Callbacks arrive on the main thread. */
interface DeviceSignals {
    val isLowRamDevice: Boolean

    /** Delivers the current values at once, then every change until [stop]. */
    fun start(listener: Listener)

    fun stop()

    interface Listener {
        fun onPowerSaveChanged(enabled: Boolean)
        fun onThermalChanged(state: ThermalState)
        fun onRefreshRateChanged(hz: Float)
    }
}

class AndroidDeviceSignals(context: Context) : DeviceSignals {

    private val appContext = context.applicationContext
    private val powerManager = appContext.getSystemService(PowerManager::class.java)
    private val displayManager = appContext.getSystemService(DisplayManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: DeviceSignals.Listener? = null

    override val isLowRamDevice: Boolean =
        appContext.getSystemService(ActivityManager::class.java).isLowRamDevice

    private val powerSaveReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            listener?.onPowerSaveChanged(powerManager.isPowerSaveMode)
        }
    }

    // Typed as Any so the class still loads below API 29, where the listener type does not exist.
    private var thermalListener: Any? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) reportRefreshRate()
        }

        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    override fun start(listener: DeviceSignals.Listener) {
        if (this.listener != null) stop()
        this.listener = listener

        ContextCompat.registerReceiver(
            appContext,
            powerSaveReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        listener.onPowerSaveChanged(powerManager.isPowerSaveMode)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val thermal = PowerManager.OnThermalStatusChangedListener { status ->
                this.listener?.onThermalChanged(thermalState(status))
            }
            powerManager.addThermalStatusListener(ContextCompat.getMainExecutor(appContext), thermal)
            thermalListener = thermal
            listener.onThermalChanged(thermalState(powerManager.currentThermalStatus))
        } else {
            listener.onThermalChanged(ThermalState.NONE)
        }

        displayManager.registerDisplayListener(displayListener, mainHandler)
        reportRefreshRate()
    }

    override fun stop() {
        if (listener == null) return
        listener = null
        appContext.unregisterReceiver(powerSaveReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (thermalListener as? PowerManager.OnThermalStatusChangedListener)
                ?.let(powerManager::removeThermalStatusListener)
        }
        thermalListener = null
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun reportRefreshRate() {
        val hz = displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate ?: return
        if (hz > 0f) listener?.onRefreshRateChanged(hz)
    }

    private fun thermalState(status: Int): ThermalState = when {
        status >= PowerManager.THERMAL_STATUS_CRITICAL -> ThermalState.CRITICAL
        status >= PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
        status >= PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
        else -> ThermalState.NONE
    }
}
