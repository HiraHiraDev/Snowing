package com.hirahira.snowing.feature.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.hirahira.snowing.R
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.feature.common.nameRes
import com.hirahira.snowing.overlay.StopStyle
import com.hirahira.snowing.settings.MoodChoice
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings tile: snow on and off without opening the app, also over screens that
 * refuse taps under our window (ADR-0003). Unlike the app it never reconciles, so it shows
 * whether snow really falls ([com.hirahira.snowing.power.SnowSwitch.snowing], ADR-0005
 * §Kafelek) and a tap does the opposite of what it shows. Keeps no state of its own.
 */
class SnowTileService : TileService() {

    private val scope = MainScope()
    private var listening: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        val container = appContainer
        listening = scope.launch {
            combine(container.snowSwitch.snowing, container.settingsRepository.settings) { snowing, settings ->
                snowing to settings.mood
            }.collect { (snowing, mood) -> render(snowing, mood) }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val snowSwitch = appContainer.snowSwitch
        if (!snowSwitch.hasPermission()) {
            unlockAndRun(::openApp)
            return
        }
        // appScope: the write must finish even if the shade closes and unbinds the tile.
        appContainer.appScope.launch {
            if (snowSwitch.snowing.first()) {
                snowSwitch.turnOff(StopStyle.AT_ONCE)
            } else if (!snowSwitch.turnOn()) {
                unlockAndRun(::openApp)
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Small tiles show only the icon, lit or not; a large one adds the subtitle: the mood
     * that falls, or why the tile cannot start snow. TalkBack reads the state description.
     */
    private fun render(snowing: Boolean, mood: MoodChoice) {
        val tile = qsTile ?: return
        val permission = appContainer.snowSwitch.hasPermission()
        val active = snowing && permission
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        val moodName = getString(mood.nameRes())
        val subtitle = when {
            !permission -> getString(R.string.tile_needs_permission)
            active -> moodName
            else -> null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = when {
                !permission -> getString(R.string.tile_needs_permission)
                active -> getString(R.string.tile_state_snowing, moodName)
                else -> getString(R.string.tile_state_off)
            }
        }
        tile.updateTile()
    }

    /** The app asks for the permission, or starts snow when Android refused it here. */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(launch)
        }
    }
}
