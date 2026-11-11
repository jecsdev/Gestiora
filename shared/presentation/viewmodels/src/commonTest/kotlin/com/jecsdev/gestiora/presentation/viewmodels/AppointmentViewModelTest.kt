package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.Appointment
import com.jecsdev.gestiora.domain.models.AppointmentStatus
import com.jecsdev.gestiora.domain.repository.AppointmentRepository
import com.jecsdev.gestiora.domain.usecases.ManageAppointments
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AppointmentViewModelTest {
    private val sample = Appointment("", "2026-11-12", "10:00", "", "", "", "Consulta", AppointmentStatus.SCHEDULED)

    @Test fun generatedIdsAreDistinctAndUpdatesRetainIdentity() = runBlocking {
        val repository = FakeRepository()
        val vm = AppointmentViewModel(ManageAppointments(repository))
        assertTrue(vm.add(sample).isSuccess)
        assertTrue(vm.add(sample).isSuccess)
        val appointments = vm.load().getOrThrow()
        assertEquals(2, appointments.size)
        assertEquals(32, appointments.first().id.length)
        assertNotEquals(appointments[0].id, appointments[1].id)
        assertTrue(vm.update(appointments[0].copy(reason = "Control")).isSuccess)
        assertEquals("Control", vm.load().getOrThrow().first().reason)
        assertTrue(vm.delete(appointments[1].id).isSuccess)
        assertEquals(listOf(appointments[0].id), vm.load().getOrThrow().map { it.id })
    }

    @Test fun failuresAreExplicitAndCancellationPropagates() = runBlocking {
        val repository = FakeRepository()
        val vm = AppointmentViewModel(ManageAppointments(repository))
        assertTrue(vm.add(sample).isSuccess)
        val before = repository.items.toList()
        repository.failure = IllegalStateException("unavailable")
        assertTrue(vm.load().isFailure)
        assertTrue(vm.add(sample).isFailure)
        assertTrue(vm.update(before[0]).isFailure)
        assertTrue(vm.delete(before[0].id).isFailure)
        assertEquals(before, repository.items)
        repository.failure = CancellationException("cancelled")
        assertFailsWith<CancellationException> { vm.load() }
        Unit
    }

    private class FakeRepository : AppointmentRepository {
        val items = mutableListOf<Appointment>()
        var failure: Exception? = null
        private fun checkFailure() { failure?.let { throw it } }
        override suspend fun list(): List<Appointment> { checkFailure(); return items.toList() }
        override suspend fun add(appointment: Appointment) { checkFailure(); items.add(appointment) }
        override suspend fun update(appointment: Appointment) {
            checkFailure()
            val index = items.indexOfFirst { it.id == appointment.id }
            check(index >= 0)
            items[index] = appointment
        }
        override suspend fun delete(id: String) { checkFailure(); check(items.removeAll { it.id == id }) }
    }
}
