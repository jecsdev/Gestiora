package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.ThemePreferenceStore
import com.jecsdev.gestiora.domain.models.ThemeMode
import com.jecsdev.gestiora.domain.repository.ThemePreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LocalThemePreferenceRepository(private val store: ThemePreferenceStore) : ThemePreferenceRepository {
    private val mutex = Mutex()

    override suspend fun load(): ThemeMode = mutex.withLock {
        withContext(Dispatchers.Default) {
            when (store.read()) {
                null, "system" -> ThemeMode.SYSTEM
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> error("Unsupported theme preference")
            }
        }
    }

    override suspend fun save(mode: ThemeMode): Unit = mutex.withLock {
        withContext(Dispatchers.Default) {
            store.write(when (mode) {
                ThemeMode.SYSTEM -> "system"
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
            })
        }
    }
}
