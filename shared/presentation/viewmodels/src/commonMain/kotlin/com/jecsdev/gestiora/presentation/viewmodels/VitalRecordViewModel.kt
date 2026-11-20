package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.usecases.ManageVitalRecords
import kotlin.coroutines.cancellation.CancellationException

class VitalRecordViewModel(private val vitalRecords: ManageVitalRecords) {
    suspend fun load(): Result<List<VitalRecord>> = capture { vitalRecords.list() }
    suspend fun add(record: VitalRecord): Result<Unit> = capture { vitalRecords.add(record) }
    suspend fun update(record: VitalRecord): Result<Unit> = capture { vitalRecords.update(record) }
    suspend fun delete(id: String): Result<Unit> = capture { vitalRecords.delete(id) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
