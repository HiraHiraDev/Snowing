package com.hirahira.snowing.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import com.hirahira.snowing.engine.SnowConfig
import com.hirahira.snowing.engine.SnowField

/**
 * A full-screen, non-interactive window above other apps.
 * Window flags and alpha are what keep touches passing through —
 * see docs/adr/0001-overlay-window.md before changing them.
 */
internal class SnowOverlayWindow(context: Context, initialConfig: SnowConfig) {

    private val windowContext: Context = overlayWindowContext(context)
    private val windowManager = windowContext.getSystemService(WindowManager::class.java)
    private val snow = SnowField(initialConfig, pxPerDp = windowContext.resources.displayMetrics.density)
    private val view = SnowView(windowContext, snow)
    private var attached = false

    fun attach() {
        if (attached) return
        windowManager.addView(view, layoutParams())
        attached = true
    }

    fun detach() {
        if (!attached) return
        windowManager.removeViewImmediate(view)
        attached = false
    }

    fun apply(config: SnowConfig, options: RenderOptions) {
        snow.updateConfig(config)
        view.renderOptions = options
    }

    fun setPaused(paused: Boolean) {
        view.isPaused = paused
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
        const val MAX_WINDOW_ALPHA = 0.8f

        fun overlayWindowContext(context: Context): Context {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return context
            val display = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
            return context.createDisplayContext(display)
                .createWindowContext(LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        }
    }
}
