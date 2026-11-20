package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.ThemeMode
import com.jecsdev.gestiora.domain.repository.ThemePreferenceRepository

class ManageThemePreference(private val repository: ThemePreferenceRepository) {
    suspend fun load(): ThemeMode = repository.load()
    suspend fun save(mode: ThemeMode) = repository.save(mode)
}
