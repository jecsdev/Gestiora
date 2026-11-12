package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.MedicalCondition
import com.jecsdev.gestiora.domain.usecases.ManageMedicalConditions
import kotlin.coroutines.cancellation.CancellationException

class MedicalConditionViewModel(private val medicalConditions: ManageMedicalConditions) {
    suspend fun load(): Result<List<MedicalCondition>> = capture { medicalConditions.list() }
    suspend fun add(condition: MedicalCondition): Result<Unit> = capture { medicalConditions.add(condition) }
    suspend fun update(condition: MedicalCondition): Result<Unit> = capture { medicalConditions.update(condition) }
    suspend fun delete(id: String): Result<Unit> = capture { medicalConditions.delete(id) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
