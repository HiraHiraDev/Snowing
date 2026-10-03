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
 * What may be stored without Pro (ADR-0007). The custom mood is Pro: without it
 * the user can pick and shape it, but only [saved]'s choice and custom mood are
 * written, so the edit stays a preview.
 */
object ProGate {
    fun persistable(edited: SnowSettings, saved: SnowSettings, hasPro: Boolean): SnowSettings {
        if (hasPro) return edited
        return edited.copy(
            mood = if (edited.mood == MoodChoice.CUSTOM) saved.mood else edited.mood,
            customMood = saved.customMood,
        )
    }
}
