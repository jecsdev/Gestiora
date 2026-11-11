package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureAppointmentsStore
import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.repository.AppointmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Each read-modify-write is one serialized operation; storage failures are never treated as empty. */
class SecureAppointmentRepository(private val store: SecureAppointmentsStore) : AppointmentRepository {
    private val mutex = Mutex()

    override suspend fun list(): List<Appointment> = mutex.withLock {
        withContext(Dispatchers.Default) { readAll() }
    }

    override suspend fun add(appointment: Appointment) = mutate { existing ->
        validateAppointment(appointment)
        require(existing.none { it.id == appointment.id }) { "Appointment id already exists" }
        existing + appointment
    }

    override suspend fun update(appointment: Appointment) = mutate { existing ->
        validateAppointment(appointment)
        require(existing.any { it.id == appointment.id }) { "Appointment not found" }
        existing.map { if (it.id == appointment.id) appointment else it }
    }

    override suspend fun delete(id: String) = mutate { existing ->
        require(existing.any { it.id == id }) { "Appointment not found" }
        existing.filterNot { it.id == id }
    }

    private suspend fun mutate(change: (List<Appointment>) -> List<Appointment>) = mutex.withLock {
        withContext(Dispatchers.Default) {
            val current = readAll()
            store.write(AppointmentCodec.encode(change(current)))
        }
    }

    private fun readAll(): List<Appointment> = store.read()?.let(AppointmentCodec::decode) ?: emptyList()
}
