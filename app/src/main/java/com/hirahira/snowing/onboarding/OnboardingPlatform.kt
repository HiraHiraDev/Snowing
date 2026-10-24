package com.hirahira.snowing.onboarding

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * What the Android version lets onboarding say and offer (ADR-0011): how the overlay setting
 * is found, and the ways to stop snow without the app (ADR-0003): the notification's stop
 * action and the Quick Settings tile.
 */
interface OnboardingPlatform {
    /** The overlay setting opens on a list of all apps (API 30+), not on the app's own page. */
    val overlaySettingsListApps: Boolean

    /** The service's notification, and with it the stop action, can be seen. */
    fun notificationsAllowed(): Boolean

    /** Notifications are a permission the app can ask for (API 33+); otherwise only settings turn them on. */
    val canAskForNotifications: Boolean

    /** The app can ask the system to add its tile (API 33+). */
    val canOfferTile: Boolean
}

class AndroidOnboardingPlatform(context: Context) : OnboardingPlatform {
    private val appContext = context.applicationContext

    override val overlaySettingsListApps: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    override fun notificationsAllowed(): Boolean =
        NotificationManagerCompat.from(appContext).areNotificationsEnabled()

    override val canAskForNotifications: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override val canOfferTile: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
