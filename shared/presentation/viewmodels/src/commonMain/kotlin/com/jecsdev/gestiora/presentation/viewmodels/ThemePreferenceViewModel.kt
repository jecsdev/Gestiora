package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.ThemeMode
import com.jecsdev.gestiora.domain.usecases.ManageThemePreference
import kotlin.coroutines.cancellation.CancellationException

class ThemePreferenceViewModel(private val preference: ManageThemePreference) {
    suspend fun load(): Result<ThemeMode> = capture { preference.load() }
    suspend fun save(mode: ThemeMode): Result<Unit> = capture { preference.save(mode) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
