package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.ThemePreferenceStore
import com.jecsdev.gestiora.domain.models.ThemeMode
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class LocalThemePreferenceRepositoryTest {
    @Test fun preferenceSurvivesRepositoryReloadAndMissingPreferenceFollowsSystem() = runBlocking<Unit> {
        val store = MemoryStore()
        val repository = LocalThemePreferenceRepository(store)
        assertEquals(ThemeMode.SYSTEM, repository.load())
        assertEquals(0, store.writes)
        for (mode in listOf(ThemeMode.DARK, ThemeMode.LIGHT, ThemeMode.SYSTEM)) {
            repository.save(mode)
            assertEquals(mode, LocalThemePreferenceRepository(store).load())
        }
        assertEquals(3, store.writes)
    }

    @Test fun unknownValuesAndReadFailuresDoNotRewritePreferencesAndWriteFailureIsExplicit() = runBlocking<Unit> {
        val store = MemoryStore("future-theme")
        val repository = LocalThemePreferenceRepository(store)
        assertFailsWith<IllegalStateException> { repository.load() }
        assertEquals("future-theme", store.value)
        assertEquals(0, store.writes)
        store.fail = true
        assertFailsWith<IllegalStateException> { repository.load() }
        assertFailsWith<IllegalStateException> { repository.save(ThemeMode.DARK) }
        assertEquals("future-theme", store.value)
        assertEquals(0, store.writes)
        store.fail = false
        repository.save(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.load())
    }

    private class MemoryStore(var value: String? = null) : ThemePreferenceStore {
        var writes = 0
        var fail = false
        override fun read(): String? { check(!fail); return value }
        override fun write(value: String) { check(!fail); this.value = value; writes++ }
    }
}
