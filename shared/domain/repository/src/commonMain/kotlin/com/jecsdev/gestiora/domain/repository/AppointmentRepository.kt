package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.Appointment

/** Storage failures must throw; an empty list means that no appointments exist. */
interface AppointmentRepository {
    suspend fun list(): List<Appointment>
    suspend fun add(appointment: Appointment)
    suspend fun update(appointment: Appointment)
    suspend fun delete(id: String)
}
