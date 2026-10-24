package com.hirahira.snowing.feature.onboarding

/** First-run steps (ADR-0011). Which one shows follows from the permissions, not from navigation. */
enum class OnboardingStep { OVERLAY, STOP }

/** How the user finds the overlay setting (design 6.2). */
enum class OverlayGuide {
    /** API 30+: a list of all apps; numbered steps and a look-alike of the app's row. */
    APP_LIST,

    /** API 26-29: the app's own page; one line. */
    APP_PAGE,
}

/** What "turn on notifications" does next. */
enum class NotificationsAction {
    /** The system's permission question (API 33+), once. */
    ASK,

    /** The app's notification settings: after a refusal, or where there is nothing to ask. */
    OPEN_SETTINGS,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.OVERLAY,
    /** Steps in this onboarding; one when the overlay was allowed before it began. */
    val stepCount: Int = 2,
    val overlayGuide: OverlayGuide = OverlayGuide.APP_LIST,
    val notificationsAllowed: Boolean = false,
    val notificationsAction: NotificationsAction = NotificationsAction.ASK,
    /** The system can add the tile on request; elsewhere the screen says how to add it. */
    val canOfferTile: Boolean = false,
    val tileAdded: Boolean = false,
    /** The user chose to finish; the panel slides away, then [OnboardingEvent.Closed] ends onboarding. */
    val closing: Boolean = false,
)

sealed interface OnboardingEvent {
    /** Back from system settings or a system question: permissions may have changed. */
    data object Resumed : OnboardingEvent

    /** The Route opens the overlay settings. */
    data object AllowOverlay : OnboardingEvent

    /** Leaves onboarding without the overlay; the Snow page asks again. */
    data object NotNow : OnboardingEvent

    /** The Route asks or opens settings, per [OnboardingUiState.notificationsAction]. */
    data object AllowNotifications : OnboardingEvent

    data object NotificationsAnswered : OnboardingEvent

    /** The Route asks the system to add the tile. */
    data object AddTile : OnboardingEvent

    data class TileAnswered(val added: Boolean) : OnboardingEvent

    /** Ends onboarding and starts the snow. */
    data object LetItSnow : OnboardingEvent

    /** The panel finished sliding away after [NotNow] or [LetItSnow]. */
    data object Closed : OnboardingEvent

    /**
     * Swiped away, scrim tapped or Back: like [NotNow] on the first step, like [LetItSnow] on
     * the second (the overlay is allowed there).
     */
    data object Dismissed : OnboardingEvent
}
