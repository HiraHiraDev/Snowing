package com.hirahira.snowing.feature.onboarding

import com.hirahira.snowing.power.SnowSwitch
import com.hirahira.snowing.testing.FakeOnboardingPlatform
import com.hirahira.snowing.testing.FakeOnboardingRepository
import com.hirahira.snowing.testing.FakeOverlayController
import com.hirahira.snowing.testing.FakeSnowStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val onboarding = FakeOnboardingRepository()
    private val snowState = FakeSnowStateRepository()
    private val overlay = FakeOverlayController(permission = false)
    private val platform = FakeOnboardingPlatform()
    private val main = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(main)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(platform: FakeOnboardingPlatform = this.platform) =
        OnboardingViewModel(onboarding, SnowSwitch(snowState, overlay), platform)

    @Test
    fun `the step follows the overlay permission granted in settings`() {
        val vm = viewModel()
        assertEquals(OnboardingStep.OVERLAY, vm.uiState.value.step)

        overlay.permission = true
        vm.onEvent(OnboardingEvent.Resumed)
        assertEquals(OnboardingStep.STOP, vm.uiState.value.step)
        assertEquals(2, vm.uiState.value.stepCount)
    }

    @Test
    fun `with the overlay already allowed there is only the stop step`() {
        overlay.permission = true
        val vm = viewModel()
        assertEquals(OnboardingStep.STOP, vm.uiState.value.step)
        assertEquals(1, vm.uiState.value.stepCount)
    }

    @Test
    fun `not now closes the panel, then ends onboarding without snow`() = runTest {
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.NotNow)
        assertTrue(vm.uiState.value.closing)
        assertFalse(onboarding.completed.value)

        vm.onEvent(OnboardingEvent.Closed)
        assertTrue(onboarding.completed.value)
        assertFalse(snowState.state.value)
        assertEquals(0, overlay.startCalls)
    }

    @Test
    fun `let it snow closes the panel, then ends onboarding and starts the snow`() = runTest {
        overlay.permission = true
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.LetItSnow)
        assertTrue(vm.uiState.value.closing)

        vm.onEvent(OnboardingEvent.Closed)
        assertTrue(onboarding.completed.value)
        assertTrue(snowState.state.value)
        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `swiping the panel away is not now on the first step`() = runTest {
        viewModel().onEvent(OnboardingEvent.Dismissed)
        assertTrue(onboarding.completed.value)
        assertFalse(snowState.state.value)
    }

    @Test
    fun `swiping the panel away is let it snow on the second step`() = runTest {
        overlay.permission = true
        viewModel().onEvent(OnboardingEvent.Dismissed)
        assertTrue(onboarding.completed.value)
        assertTrue(overlay.isRunning.value)
    }

    @Test
    fun `notifications are asked for once, then settings take over`() {
        overlay.permission = true
        val vm = viewModel()
        assertEquals(NotificationsAction.ASK, vm.uiState.value.notificationsAction)

        vm.onEvent(OnboardingEvent.NotificationsAnswered)
        assertFalse(vm.uiState.value.notificationsAllowed)
        assertEquals(NotificationsAction.OPEN_SETTINGS, vm.uiState.value.notificationsAction)

        platform.notifications = true
        vm.onEvent(OnboardingEvent.Resumed)
        assertTrue(vm.uiState.value.notificationsAllowed)
    }

    @Test
    fun `older Androids get the one-line guide, settings for notifications and no tile button`() {
        val vm = viewModel(
            FakeOnboardingPlatform(overlaySettingsListApps = false, canAskForNotifications = false, canOfferTile = false),
        )
        assertEquals(OverlayGuide.APP_PAGE, vm.uiState.value.overlayGuide)
        assertEquals(NotificationsAction.OPEN_SETTINGS, vm.uiState.value.notificationsAction)
        assertFalse(vm.uiState.value.canOfferTile)
    }

    @Test
    fun `a tile once added stays added`() {
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.TileAnswered(added = true))
        vm.onEvent(OnboardingEvent.TileAnswered(added = false))
        assertTrue(vm.uiState.value.tileAdded)
    }
}
