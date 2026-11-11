package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.usecases.ManageAppointments
import kotlin.coroutines.cancellation.CancellationException

class AppointmentViewModel(private val appointments: ManageAppointments) {
    suspend fun load(): Result<List<Appointment>> = capture { appointments.list() }
    suspend fun add(appointment: Appointment): Result<Unit> = capture { appointments.add(appointment) }
    suspend fun update(appointment: Appointment): Result<Unit> = capture { appointments.update(appointment) }
    suspend fun delete(id: String): Result<Unit> = capture { appointments.delete(id) }

    private suspend inline fun <T> capture(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }
}
