package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.usecases.ManageExamRecords
import kotlin.coroutines.cancellation.CancellationException

class ExamRecordViewModel(private val examRecords: ManageExamRecords) {
    suspend fun load(): Result<List<ExamRecord>> = capture { examRecords.list() }
    suspend fun add(record: ExamRecord): Result<Unit> = capture { examRecords.add(record) }
    suspend fun update(record: ExamRecord): Result<Unit> = capture { examRecords.update(record) }
    suspend fun delete(id: String): Result<Unit> = capture { examRecords.delete(id) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
