package com.hirahira.snowing.power

import com.hirahira.snowing.overlay.OverlayController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * The one way to turn snow on and off. Records the user's choice first, then
 * moves the overlay to match. Every entry point (panel today; tile, widget
 * and scheduler later) goes through here, so they can never disagree.
 * See docs/adr/0005-snow-state.md.
 */
class SnowSwitch(
    private val stateRepository: SnowStateRepository,
    private val overlay: OverlayController,
) {
    val enabled: Flow<Boolean> = stateRepository.enabled

    fun hasPermission(): Boolean = overlay.hasPermission()

    /** @return false when the overlay permission is missing; nothing changes then. */
    suspend fun turnOn(): Boolean {
        if (!overlay.hasPermission()) return false
        stateRepository.setEnabled(true)
        overlay.start()
        return true
    }

    suspend fun turnOff() {
        stateRepository.setEnabled(false)
        overlay.stop()
    }

    /** @return false when turning on failed for lack of permission. */
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
            !shouldSnow && overlay.isRunning.value -> overlay.stop()
        }
    }
}
