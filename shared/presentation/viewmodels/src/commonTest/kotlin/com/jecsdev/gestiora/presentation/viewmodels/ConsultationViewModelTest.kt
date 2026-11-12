package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.Consultation
import com.jecsdev.gestiora.domain.repository.ConsultationRepository
import com.jecsdev.gestiora.domain.usecases.ManageConsultations
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*

class ConsultationViewModelTest {
    @Test fun generatedIdentityCrudFailuresAndCancellation() = runBlocking {
        var saved: Consultation? = null
        var failure: Exception? = null
        val repository = object : ConsultationRepository {
            override suspend fun list(): List<Consultation> { failure?.let { throw it }; return listOfNotNull(saved) }
            override suspend fun add(consultation: Consultation) { failure?.let { throw it }; saved = consultation }
            override suspend fun update(consultation: Consultation) { failure?.let { throw it }; saved = consultation }
            override suspend fun delete(id: String) { failure?.let { throw it }; saved = null }
        }
        val vm = ConsultationViewModel(ManageConsultations(repository))
        assertTrue(vm.add(Consultation("", "2028-02-29", "24 sem 2 d")).isSuccess)
        val created = vm.load().getOrThrow().single()
        assertEquals(32, created.id.length)
        assertTrue(created.id.all { it in '0'..'9' || it in 'a'..'f' })
        assertTrue(vm.update(created.copy(doctor = "Editada")).isSuccess)
        failure = IllegalStateException("Unavailable")
        assertTrue(vm.load().isFailure)
        assertTrue(vm.add(created).isFailure)
        assertTrue(vm.update(created).isFailure)
        assertTrue(vm.delete(created.id).isFailure)
        assertEquals("Editada", saved?.doctor)
        failure = CancellationException("Cancelled")
        assertFailsWith<CancellationException> { vm.load() }
        failure = null
        assertTrue(vm.delete(created.id).isSuccess)
        assertEquals(emptyList(), vm.load().getOrThrow())
    }
}
