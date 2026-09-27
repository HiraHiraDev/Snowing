package com.hirahira.snowing.testing

import com.hirahira.snowing.overlay.OverlayController
import com.hirahira.snowing.power.SnowStateRepository
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.SnowSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SnowSettingsRepository {
    val state = MutableStateFlow(SnowSettings())
    override val settings = state
    var writes = 0
        private set

    override suspend fun update(transform: (SnowSettings) -> SnowSettings) {
        writes++
        state.value = transform(state.value)
    }
}

class FakeSnowStateRepository(enabled: Boolean = false) : SnowStateRepository {
    val state = MutableStateFlow(enabled)
    override val enabled = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = enabled
    }
}

/** Starts and stops synchronously; the real service reports running asynchronously. */
class FakeOverlayController(var permission: Boolean = true) : OverlayController {
    override val isRunning = MutableStateFlow(false)
    var startCalls = 0
        private set

    override fun hasPermission() = permission

    override fun start() {
        startCalls++
        isRunning.value = true
    }

    override fun stop() {
        isRunning.value = false
    }
}
