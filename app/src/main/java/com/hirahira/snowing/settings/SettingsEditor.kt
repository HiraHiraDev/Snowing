package com.hirahira.snowing.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Edits the settings for one screen. A change shows at once, on screen and in the snow, as a
 * draft; the store gets it at most once per [THROTTLE_MS] while a finger drags, and at once on
 * [saveNow] (B-002). Once the store catches up the draft goes away and the store leads again,
 * so changes made elsewhere (a tile, the schedule) show too.
 *
 * [guard] decides what of an edit may be written over what is stored, e.g. what Pro allows.
 */
class SettingsEditor(
    private val repository: SnowSettingsRepository,
    private val scope: CoroutineScope,
    private val guard: (edited: SnowSettings, saved: SnowSettings) -> SnowSettings = { edited, _ -> edited },
) {
    private val saved: StateFlow<SnowSettings?> = repository.settings.stateIn(scope, SharingStarted.Eagerly, null)
    private val draft = MutableStateFlow<SnowSettings?>(null)
    private var saveJob: Job? = null

    /** What the screen shows; null until the store has answered. */
    val settings: Flow<SnowSettings?> = combine(saved, draft) { saved, draft -> draft ?: saved }

    init {
        scope.launch {
            saved.collect { if (it != null && it == draft.value) draft.value = null }
        }
    }

    fun edit(now: Boolean = false, transform: (SnowSettings) -> SnowSettings) {
        val base = draft.value ?: saved.value
        if (base == null) {
            // Not loaded yet: nothing on screen to edit from, so write straight through.
            scope.launch { repository.update { guard(transform(it), it) } }
            return
        }
        draft.value = transform(base)
        when {
            now -> saveNow()
            saveJob?.isActive != true -> saveJob = scope.launch {
                delay(THROTTLE_MS)
                saveJob = null
                persistDraft()
            }
        }
    }

    /** Writes what is pending now, e.g. when the finger leaves a slider. */
    fun saveNow() {
        saveJob?.cancel()
        saveJob = null
        persistDraft()
    }

    private fun persistDraft() {
        val pending = draft.value ?: return
        scope.launch { repository.update { saved -> guard(pending, saved) } }
    }

    companion object {
        /** Longest a dragged value waits before it is written; the snow follows every write. */
        const val THROTTLE_MS = 150L
    }
}
