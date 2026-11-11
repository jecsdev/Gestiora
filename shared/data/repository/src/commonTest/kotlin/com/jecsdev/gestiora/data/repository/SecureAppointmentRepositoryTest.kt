package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureAppointmentsStore
import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.models.AppointmentStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SecureAppointmentRepositoryTest {
    private fun appointment(id: String) = Appointment(id, "2028-02-29", "09:30", "Dra. Lucía", "Obstetricia", "Clínica", "Control", AppointmentStatus.SCHEDULED, "Nota 😊\nseguimiento")

    @Test fun roundTripUpdateAndDelete() = runBlocking {
        val store = FakeStore()
        val repo = SecureAppointmentRepository(store)
        assertEquals(emptyList(), repo.list())
        val original = appointment("one")
        repo.add(original)
        assertEquals(listOf(original), repo.list())
        val updated = original.copy(status = AppointmentStatus.COMPLETED, notes = null)
        repo.update(updated)
        assertEquals(listOf(updated), repo.list())
        repo.delete(original.id)
        assertEquals(emptyList(), repo.list())
    }

    @Test fun invalidAndMissingOperationsNeverOverwrite() = runBlocking {
        val store = FakeStore()
        val repo = SecureAppointmentRepository(store)
        repo.add(appointment("one"))
        val original = store.payload
        assertFailsWith<IllegalArgumentException> { repo.add(appointment("one")) }
        assertFailsWith<IllegalArgumentException> { repo.update(appointment("missing")) }
        assertFailsWith<IllegalArgumentException> { repo.delete("missing") }
        assertFailsWith<IllegalArgumentException> { repo.add(appointment("two").copy(date = "2027-02-29")) }
        assertFailsWith<IllegalArgumentException> { repo.add(appointment("two").copy(time = "24:00")) }
        assertFailsWith<IllegalArgumentException> { repo.add(appointment("two").copy(reason = "  ")) }
        assertEquals(original, store.payload)
    }

    @Test fun corruptOrUnknownPayloadAndReadFailuresCannotBeOverwritten() = runBlocking {
        val store = FakeStore()
        val repo = SecureAppointmentRepository(store)
        for (payload in listOf("broken", "GESTIORA_APPOINTMENTS:1\n1\n5:bad", "GESTIORA_APPOINTMENTS:2\n0\n")) {
            store.payload = payload
            assertFailsWith<IllegalStateException> { repo.list() }
            assertFailsWith<IllegalStateException> { repo.add(appointment("one")) }
            assertEquals(payload, store.payload)
        }
        store.payload = null
        store.failRead = true
        assertFailsWith<IllegalStateException> { repo.add(appointment("one")) }
        assertNull(store.payload)
        store.failRead = false
        store.failWrite = true
        assertFailsWith<IllegalStateException> { repo.add(appointment("one")) }
        assertNull(store.payload)
    }

    @Test fun concurrentAddsAreSerialized() = runBlocking {
        val repo = SecureAppointmentRepository(FakeStore())
        (0 until 100).map { id -> async { repo.add(appointment(id.toString())) } }.awaitAll()
        assertEquals((0 until 100).map(Int::toString).toSet(), repo.list().map { it.id }.toSet())
    }

    private class FakeStore(var payload: String? = null) : SecureAppointmentsStore {
        var failRead = false
        var failWrite = false
        override fun read(): String? {
            if (failRead) error("read failed")
            return payload
        }
        override fun write(payload: String) {
            if (failWrite) error("write failed")
            this.payload = payload
        }
    }
}
