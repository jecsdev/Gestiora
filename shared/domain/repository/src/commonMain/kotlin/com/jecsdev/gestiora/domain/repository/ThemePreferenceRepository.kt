package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.ThemeMode

interface ThemePreferenceRepository {
    suspend fun load(): ThemeMode
    suspend fun save(mode: ThemeMode)
}
