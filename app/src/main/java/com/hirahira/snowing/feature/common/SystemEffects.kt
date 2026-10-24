package com.hirahira.snowing.feature.common

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.net.toUri

// Android side effects shared by the Routes.

/** On API 30+ Android ignores the package and opens the list of all apps (ADR-0011). */
internal fun Context.openOverlaySettings() {
    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
}

// Design 6a: a confirming buzz to start, a light tick to stop. CONFIRM needs API 30.
internal fun View.startFeedback() {
    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.VIRTUAL_KEY
    }
    performHapticFeedback(type)
}

internal fun View.stopFeedback() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}
