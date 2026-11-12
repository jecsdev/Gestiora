package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.Consultation
import com.jecsdev.gestiora.domain.usecases.ManageConsultations
import kotlin.coroutines.cancellation.CancellationException

class ConsultationViewModel(private val consultations: ManageConsultations) {
    suspend fun load(): Result<List<Consultation>> = capture { consultations.list() }
    suspend fun add(consultation: Consultation): Result<Unit> = capture { consultations.add(consultation) }
    suspend fun update(consultation: Consultation): Result<Unit> = capture { consultations.update(consultation) }
    suspend fun delete(id: String): Result<Unit> = capture { consultations.delete(id) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
