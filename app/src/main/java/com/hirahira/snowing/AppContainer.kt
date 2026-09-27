package com.hirahira.snowing

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.hirahira.snowing.overlay.OverlayController
import com.hirahira.snowing.overlay.ServiceOverlayController
import com.hirahira.snowing.power.DataStoreSnowStateRepository
import com.hirahira.snowing.power.SnowSwitch
import com.hirahira.snowing.settings.DataStoreSnowSettingsRepository
import com.hirahira.snowing.settings.SnowSettingsRepository

private val Context.snowSettingsDataStore by preferencesDataStore(name = "snow_settings")
private val Context.snowStateDataStore by preferencesDataStore(name = "snow_state")

/**
 * Manual dependency graph. Everything outside this file depends on the
 * interfaces, so implementations can be swapped (or faked in tests) here.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository: SnowSettingsRepository =
        DataStoreSnowSettingsRepository(appContext.snowSettingsDataStore)

    val overlayController: OverlayController = ServiceOverlayController(appContext)

    val snowSwitch = SnowSwitch(
        stateRepository = DataStoreSnowStateRepository(appContext.snowStateDataStore),
        overlay = overlayController,
    )
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SnowingApplication).container
