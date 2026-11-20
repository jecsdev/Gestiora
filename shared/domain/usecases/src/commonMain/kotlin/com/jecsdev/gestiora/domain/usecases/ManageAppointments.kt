package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.repository.AppointmentRepository
import kotlin.random.Random

/** Separates generated local identities from editable appointment fields. */
class ManageAppointments(private val repository: AppointmentRepository) {
    suspend fun list(): List<Appointment> = repository.list()

    suspend fun add(appointment: Appointment) {
        val id = Random.Default.nextBytes(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        repository.add(appointment.copy(id = id))
    }

    suspend fun update(appointment: Appointment) = repository.update(appointment)
    suspend fun delete(id: String) = repository.delete(id)
}
