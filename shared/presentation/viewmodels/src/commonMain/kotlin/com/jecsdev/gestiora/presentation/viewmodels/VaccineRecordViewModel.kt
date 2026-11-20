package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.usecases.ManageVaccineRecords
import kotlin.coroutines.cancellation.CancellationException

class VaccineRecordViewModel(private val vaccineRecords: ManageVaccineRecords) {
    suspend fun load(): Result<List<VaccineRecord>> = capture { vaccineRecords.list() }
    suspend fun add(record: VaccineRecord): Result<Unit> = capture { vaccineRecords.add(record) }
    suspend fun update(record: VaccineRecord): Result<Unit> = capture { vaccineRecords.update(record) }
    suspend fun delete(id: String): Result<Unit> = capture { vaccineRecords.delete(id) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
