package com.hirahira.snowing.testing

import com.hirahira.snowing.clock.WallClock
import com.hirahira.snowing.onboarding.OnboardingRepository
import com.hirahira.snowing.onboarding.OnboardingPlatform
import com.hirahira.snowing.overlay.OverlayController
import com.hirahira.snowing.overlay.StopStyle
import com.hirahira.snowing.power.SnowStateRepository
import com.hirahira.snowing.pro.Entitlements
import com.hirahira.snowing.settings.SnowSettings
import com.hirahira.snowing.settings.SnowSettingsRepository
import com.hirahira.snowing.stats.SnowStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.LocalTime

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

    /** Android refusing a foreground-service start from the background. */
    var refuseStart = false

    override fun hasPermission() = permission

    override fun start(): Boolean {
        startCalls++
        if (refuseStart) return false
        isRunning.value = true
        return true
    }

    var lastStop: StopStyle? = null
        private set

    override fun stop(style: StopStyle) {
        lastStop = style
        isRunning.value = false
    }
}

class FakeEntitlements(hasPro: Boolean = true) : Entitlements {
    override val hasPro = MutableStateFlow(hasPro)
}

class FakeStatsRepository : SnowStatsRepository {
    val state = MutableStateFlow<Map<LocalDate, Long>>(emptyMap())
    override val dailyMillis = state

    override suspend fun add(date: LocalDate, millis: Long) {
        state.value = state.value + (date to (state.value[date] ?: 0L) + millis)
    }
}

/** A clock standing still at [time] on [date]; tests move it by hand. */
class FakeWallClock(
    var date: LocalDate = LocalDate.of(2026, 10, 17),
    var time: LocalTime = LocalTime.NOON,
) : WallClock {
    override fun now() = time

    override fun today() = date
}

class FakeOnboardingRepository(completed: Boolean = false) : OnboardingRepository {
    override val completed = MutableStateFlow(completed)

    override suspend fun complete() {
        completed.value = true
    }
}

/** API 33+ unless told otherwise: the overlay setting lists apps, notifications can be asked for, the tile offered. */
class FakeOnboardingPlatform(
    var notifications: Boolean = false,
    override val overlaySettingsListApps: Boolean = true,
    override val canAskForNotifications: Boolean = true,
    override val canOfferTile: Boolean = true,
) : OnboardingPlatform {
    override fun notificationsAllowed() = notifications
}
