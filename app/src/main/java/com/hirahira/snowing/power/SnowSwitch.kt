package com.hirahira.snowing.power

import com.hirahira.snowing.overlay.OverlayController
import com.hirahira.snowing.overlay.StopStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * The one way to turn snow on and off. Records the user's choice first, then
 * moves the overlay to match. Every entry point (panel, notification, tile;
 * widget and scheduler later) goes through here, so they can never disagree.
 * See docs/adr/0005-snow-state.md.
 */
class SnowSwitch(
    private val stateRepository: SnowStateRepository,
    private val overlay: OverlayController,
) {
    val enabled: Flow<Boolean> = stateRepository.enabled

    /**
     * Snow is wanted and the overlay is really up. Most UI shows [enabled] and the app
     * reconciles when opened; an entry point that never reconciles (the tile) shows this,
     * or it would stay lit after the overlay died (force stop, a refused background start).
     */
    val snowing: Flow<Boolean> = combine(stateRepository.enabled, overlay.isRunning) { wanted, running -> wanted && running }

    fun hasPermission(): Boolean = overlay.hasPermission()

    /**
     * @return false when the app has to be opened to finish: the overlay permission
     * is missing (nothing changes then), or Android refused a start from the
     * background (the choice is kept and [reconcile] starts snow once the app is open).
     */
    suspend fun turnOn(): Boolean {
        if (!overlay.hasPermission()) return false
        stateRepository.setEnabled(true)
        return overlay.start()
    }

    /** [style]: gently from the app, at once from the tile and the notification (ADR-0006 §Zmiana). */
    suspend fun turnOff(style: StopStyle = StopStyle.GENTLE) {
        stateRepository.setEnabled(false)
        overlay.stop(style)
    }

    /** @return false when turning on needs the app opened, see [turnOn]. */
    suspend fun toggle(): Boolean =
        if (stateRepository.enabled.first()) {
            turnOff()
            true
        } else {
            turnOn()
        }

    /**
     * Brings the overlay in line with the user's choice. Idempotent; call it
     * whenever the process may have lost the overlay (app opened, boot, update).
     * A missing permission leaves the choice untouched, so snow comes back
     * once the user grants it again.
     */
    suspend fun reconcile() {
        val shouldSnow = stateRepository.enabled.first()
        when {
            shouldSnow && !overlay.isRunning.value && overlay.hasPermission() -> overlay.start()
            !shouldSnow && overlay.isRunning.value -> overlay.stop(StopStyle.GENTLE)
        }
    }
}
