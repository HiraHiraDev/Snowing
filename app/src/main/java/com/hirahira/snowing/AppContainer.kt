package com.hirahira.snowing

import android.app.PendingIntent
import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.preferencesDataStore
import com.hirahira.snowing.clock.SystemWallClock
import com.hirahira.snowing.clock.WallClock
import com.hirahira.snowing.overlay.AndroidDeviceSignals
import com.hirahira.snowing.overlay.DeviceSignals
import com.hirahira.snowing.overlay.OverlayController
import com.hirahira.snowing.overlay.ServiceOverlayController
import com.hirahira.snowing.power.DataStoreSnowStateRepository
import com.hirahira.snowing.power.SnowOffReceiver
import com.hirahira.snowing.power.SnowSwitch
import com.hirahira.snowing.pro.Entitlements
import com.hirahira.snowing.pro.UnlockedEntitlements
import com.hirahira.snowing.settings.DataStoreSnowSettingsRepository
import com.hirahira.snowing.settings.SnowSettingsRepository
import com.hirahira.snowing.stats.DataStoreSnowStatsRepository
import com.hirahira.snowing.stats.SnowStatsRepository
import com.hirahira.snowing.stats.SnowTimeRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val Context.snowSettingsDataStore by preferencesDataStore(name = "snow_settings")
private val Context.snowStateDataStore by preferencesDataStore(name = "snow_state")
private val Context.snowStatsDataStore by preferencesDataStore(name = "snow_stats")

/**
 * Manual dependency graph. Everything outside this file depends on the
 * interfaces, so implementations can be swapped (or faked in tests) here.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository: SnowSettingsRepository =
        DataStoreSnowSettingsRepository(appContext.snowSettingsDataStore)

    val overlayController: OverlayController = ServiceOverlayController(appContext)

    val deviceSignals: DeviceSignals = AndroidDeviceSignals(appContext)

    val entitlements: Entitlements = UnlockedEntitlements()

    val wallClock: WallClock = SystemWallClock()

    /** Outlives screens and the overlay service, for writes that must finish after they end. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val statsRepository: SnowStatsRepository = DataStoreSnowStatsRepository(appContext.snowStatsDataStore)

    val snowTimeRecorder = SnowTimeRecorder(
        stats = statsRepository,
        clock = wallClock,
        elapsedMillis = SystemClock::elapsedRealtime,
        scope = appScope,
    )

    val snowSwitch = SnowSwitch(
        stateRepository = DataStoreSnowStateRepository(appContext.snowStateDataStore),
        overlay = overlayController,
    )

    /** The notification's stop action; overlay must not know [SnowSwitch], so it gets the intent from here. */
    val snowOffAction: PendingIntent
        get() = SnowOffReceiver.pendingIntent(appContext)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SnowingApplication).container
