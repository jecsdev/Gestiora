package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.ThemeMode
import com.jecsdev.gestiora.domain.repository.ThemePreferenceRepository
import com.jecsdev.gestiora.domain.usecases.ManageThemePreference
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*

class ThemePreferenceViewModelTest {
    @Test fun exposesFailuresAndKeepsCancellationCooperative() = runBlocking<Unit> {
        val repository = FakeRepository()
        val viewModel = ThemePreferenceViewModel(ManageThemePreference(repository))
        assertEquals(ThemeMode.SYSTEM, viewModel.load().getOrThrow())
        viewModel.save(ThemeMode.DARK).getOrThrow()
        assertEquals(ThemeMode.DARK, viewModel.load().getOrThrow())
        val failure = IllegalStateException("Unavailable")
        repository.failure = failure
        assertSame(failure, viewModel.load().exceptionOrNull())
        assertSame(failure, viewModel.save(ThemeMode.LIGHT).exceptionOrNull())
        repository.failure = CancellationException()
        assertFailsWith<CancellationException> { viewModel.load() }
        assertFailsWith<CancellationException> { viewModel.save(ThemeMode.LIGHT) }
        repository.failure = null
        assertEquals(ThemeMode.DARK, viewModel.load().getOrThrow())
    }

    private class FakeRepository : ThemePreferenceRepository {
        var mode = ThemeMode.SYSTEM
        var failure: Exception? = null
        override suspend fun load(): ThemeMode { failure?.let { throw it }; return mode }
        override suspend fun save(mode: ThemeMode) { failure?.let { throw it }; this.mode = mode }
    }
}
