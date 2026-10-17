package com.hirahira.snowing.pro

import com.hirahira.snowing.settings.MoodChoice
import com.hirahira.snowing.settings.SnowSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Whether the user owns Pro (SCOPE.md, Monetization). */
interface Entitlements {
    val hasPro: StateFlow<Boolean>
}

/** Everyone has Pro until Google Play Billing lands, which needs its own ADR. */
class UnlockedEntitlements : Entitlements {
    override val hasPro: StateFlow<Boolean> = MutableStateFlow(true)
}

/**
 * Adjusting moods is Pro (ADR-0010): without it the four built-in moods can be
 * picked, but nothing can be changed or saved to the custom slot.
 */
object ProGate {
    fun canAdjustMoods(hasPro: Boolean): Boolean = hasPro

    /** What of [edited] may be written over [saved]; a guard below the UI's own lock. */
    fun persistable(edited: SnowSettings, saved: SnowSettings, hasPro: Boolean): SnowSettings {
        if (hasPro) return edited
        return edited.copy(
            mood = if (edited.mood == MoodChoice.CUSTOM) saved.mood else edited.mood,
            customMood = saved.customMood,
            // Dropping changes is always allowed; making new ones is not.
            moodAdjustment = edited.moodAdjustment?.takeIf { it == saved.moodAdjustment },
        )
    }
}
