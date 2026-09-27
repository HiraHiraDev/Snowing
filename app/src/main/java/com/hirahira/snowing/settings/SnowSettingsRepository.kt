package com.hirahira.snowing.settings

import kotlinx.coroutines.flow.Flow

interface SnowSettingsRepository {
    val settings: Flow<SnowSettings>

    suspend fun update(transform: (SnowSettings) -> SnowSettings)
}
