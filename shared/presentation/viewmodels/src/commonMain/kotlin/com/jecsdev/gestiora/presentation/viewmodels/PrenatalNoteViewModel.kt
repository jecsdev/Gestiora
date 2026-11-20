package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.PrenatalNote
import com.jecsdev.gestiora.domain.usecases.ManagePrenatalNotes
import kotlin.coroutines.cancellation.CancellationException

class PrenatalNoteViewModel(private val notes: ManagePrenatalNotes) {
    suspend fun load(): Result<List<PrenatalNote>> = capture { notes.list() }
    suspend fun add(note: PrenatalNote): Result<Unit> = capture { notes.add(note) }
    suspend fun update(note: PrenatalNote): Result<Unit> = capture { notes.update(note) }
    suspend fun delete(id: String): Result<Unit> = capture { notes.delete(id) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
