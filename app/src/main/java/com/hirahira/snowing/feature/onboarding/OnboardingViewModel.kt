package com.hirahira.snowing.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hirahira.snowing.appContainer
import com.hirahira.snowing.onboarding.OnboardingPlatform
import com.hirahira.snowing.onboarding.OnboardingRepository
import com.hirahira.snowing.power.SnowSwitch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** First-run onboarding (ADR-0011): the overlay permission, then how to stop the snow from anywhere. */
class OnboardingViewModel(
    private val onboarding: OnboardingRepository,
    private val snowSwitch: SnowSwitch,
    private val platform: OnboardingPlatform,
) : ViewModel() {

    private var notificationsAsked = false

    /** Whether the panel closing on the user's choice ends with snow. */
    private var snowAfterClose = false

    private val _uiState = MutableStateFlow(
        read(
            OnboardingUiState(
                stepCount = if (snowSwitch.hasPermission()) 1 else 2,
                overlayGuide = if (platform.overlaySettingsListApps) OverlayGuide.APP_LIST else OverlayGuide.APP_PAGE,
                canOfferTile = platform.canOfferTile,
            ),
        ),
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Resumed -> _uiState.update(::read)
            OnboardingEvent.NotificationsAnswered -> {
                notificationsAsked = true
                _uiState.update(::read)
            }
            is OnboardingEvent.TileAnswered -> _uiState.update { it.copy(tileAdded = it.tileAdded || event.added) }
            OnboardingEvent.NotNow -> close(snow = false)
            OnboardingEvent.LetItSnow -> close(snow = true)
            OnboardingEvent.Closed -> finish(snowAfterClose)
            OnboardingEvent.Dismissed -> finish(snow = _uiState.value.step == OnboardingStep.STOP)
            // Android side effects, done by the Route.
            OnboardingEvent.AllowOverlay, OnboardingEvent.AllowNotifications, OnboardingEvent.AddTile -> Unit
        }
    }

    private fun close(snow: Boolean) {
        snowAfterClose = snow
        _uiState.update { it.copy(closing = true) }
    }

    private fun finish(snow: Boolean) {
        viewModelScope.launch {
            // Without the overlay (taken back meanwhile) nothing starts; the Snow page then asks.
            if (snow) snowSwitch.turnOn()
            onboarding.complete()
        }
    }

    private fun read(state: OnboardingUiState) = state.copy(
        step = if (snowSwitch.hasPermission()) OnboardingStep.STOP else OnboardingStep.OVERLAY,
        notificationsAllowed = platform.notificationsAllowed(),
        notificationsAction = if (platform.canAskForNotifications && !notificationsAsked) {
            NotificationsAction.ASK
        } else {
            NotificationsAction.OPEN_SETTINGS
        },
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = this[APPLICATION_KEY]!!.appContainer
                OnboardingViewModel(container.onboardingRepository, container.snowSwitch, container.onboardingPlatform)
            }
        }
    }
}
