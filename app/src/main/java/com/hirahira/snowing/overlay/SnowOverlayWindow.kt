package com.hirahira.snowing.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.hirahira.snowing.engine.QualityLevel
import com.hirahira.snowing.engine.QualityPolicy
import com.hirahira.snowing.engine.SnowField
import com.hirahira.snowing.engine.ThermalState
import com.hirahira.snowing.settings.SnowSettings

/**
 * A full-screen, non-interactive window above other apps.
 * Window flags and alpha are what keep touches passing through: read
 * docs/adr/0001-overlay-window.md before changing them.
 *
 * Also owns the quality level (ADR-0008): device signals and frame timing
 * pick it, and a change reaches new flakes only, like any other change (ADR-0006).
 */
internal class SnowOverlayWindow(
    context: Context,
    initial: SnowSettings,
    private val signals: DeviceSignals,
) {

    private val windowContext: Context = overlayWindowContext(context)
    private val windowManager = windowContext.getSystemService(WindowManager::class.java)
    private val pxPerDp = windowContext.resources.displayMetrics.density
    private var settings = initial
    private val quality = QualityPolicy(
        startLevel = if (signals.isLowRamDevice) QualityLevel.LOW else QualityLevel.HIGH,
        onChange = ::onQualityChanged,
    )
    private val initialScene = SnowConfigMapper.toScene(initial, quality.level)
    private val snow = SnowField(initialScene.snow, pxPerDp)
    private val foreground = SnowField(initialScene.foreground, pxPerDp)
    private val view = SnowView(windowContext, snow, foreground, quality).apply {
        fpsCap = quality.level.fpsCap
        renderOptions = SnowConfigMapper.toRenderOptions(initial)
    }
    private var attached = false
    private var screenOn = true

    /** True while nothing is drawn: screen off or too hot. */
    val isPaused: Boolean get() = view.isPaused

    /** Told whenever the snow starts or stops being drawn (attached, screen on, not too hot). */
    var onVisibleChanged: ((Boolean) -> Unit)? = null
    private var visible = false

    private val signalListener = object : DeviceSignals.Listener {
        override fun onPowerSaveChanged(enabled: Boolean) {
            quality.powerSave = enabled
        }

        override fun onThermalChanged(state: ThermalState) {
            quality.thermal = state
        }

        override fun onRefreshRateChanged(hz: Float) {
            view.refreshRate = hz
        }
    }

    init {
        // Last, because a forced level calls back into everything above.
        quality.forced = SnowConfigMapper.forcedQuality(initial)
    }

    fun attach() {
        if (attached) return
        windowManager.addView(view, layoutParams())
        attached = true
        signals.start(signalListener)
        updateVisible()
    }

    fun detach() {
        if (!attached) return
        signals.stop()
        windowManager.removeViewImmediate(view)
        attached = false
        updateVisible()
    }

    /** New flakes take [newSettings]; the lab's instant mode also re-derives falling ones. */
    fun apply(newSettings: SnowSettings) {
        settings = newSettings
        quality.forced = SnowConfigMapper.forcedQuality(newSettings)
        applyScene(immediate = newSettings.lab.instantChanges)
        view.renderOptions = SnowConfigMapper.toRenderOptions(newSettings)
    }

    fun setScreenOn(on: Boolean) {
        screenOn = on
        if (on) quality.onScreenOn()
        updatePaused()
    }

    /** Called once when the snow has stopped falling and every flake is gone. */
    var onDrained: (() -> Unit)?
        get() = view.onDrained
        set(value) {
            view.onDrained = value
        }

    fun stopFalling() {
        snow.stopFalling()
        foreground.stopFalling()
    }

    fun fadeOut() {
        snow.fadeOut()
        foreground.fadeOut()
    }

    fun resumeFalling() {
        snow.resumeFalling()
        foreground.resumeFalling()
        view.resetDrained()
    }

    private fun onQualityChanged() {
        Log.i(
            TAG,
            "quality ${quality.level} paused=${quality.isPaused} missed=${quality.missedFraction} " +
                "refresh=${view.refreshRate} powerSave=${quality.powerSave} thermal=${quality.thermal}",
        )
        view.fpsCap = quality.level.fpsCap
        // Always as a front from the top, even in the lab's instant mode.
        applyScene(immediate = false)
        updatePaused()
    }

    private fun applyScene(immediate: Boolean) {
        val scene = SnowConfigMapper.toScene(settings, quality.level)
        snow.updateConfig(scene.snow, immediate)
        foreground.updateConfig(scene.foreground, immediate)
    }

    private fun updatePaused() {
        view.isPaused = !screenOn || quality.isPaused
        updateVisible()
    }

    private fun updateVisible() {
        val now = attached && !view.isPaused
        if (now == visible) return
        visible = now
        onVisibleChanged?.invoke(now)
    }

    private fun layoutParams() = LayoutParams(
        LayoutParams.MATCH_PARENT,
        LayoutParams.MATCH_PARENT,
        LayoutParams.TYPE_APPLICATION_OVERLAY,
        LayoutParams.FLAG_NOT_TOUCHABLE or
            LayoutParams.FLAG_NOT_FOCUSABLE or
            LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        title = "Snowing"
        alpha = touchPassThroughAlpha()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            fitInsetsTypes = 0
            layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    /**
     * Android 12+ blocks touches under another app's window unless the window
     * is at most this opaque (0.8 by default). Above it, the phone stops
     * responding wherever snow is drawn.
     */
    private fun touchPassThroughAlpha(): Float {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return MAX_WINDOW_ALPHA
        val limit = windowContext.getSystemService(InputManager::class.java).maximumObscuringOpacityForTouch
        return minOf(MAX_WINDOW_ALPHA, limit)
    }

    private companion object {
        const val TAG = "SnowOverlayWindow"
        const val MAX_WINDOW_ALPHA = 0.8f

        fun overlayWindowContext(context: Context): Context {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return context
            val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
            return context.createDisplayContext(display)
                .createWindowContext(LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        }
    }
}
