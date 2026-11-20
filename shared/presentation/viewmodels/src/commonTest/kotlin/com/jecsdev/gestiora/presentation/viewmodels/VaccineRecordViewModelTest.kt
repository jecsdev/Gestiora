package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.repository.VaccineRecordRepository
import com.jecsdev.gestiora.domain.usecases.ManageVaccineRecords
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*

class VaccineRecordViewModelTest {
    @Test fun generatedIdentityCrudFailuresAndCancellation() = runBlocking<Unit> {
        var saved: VaccineRecord? = null
        var failure: Exception? = null
        val repository = object : VaccineRecordRepository {
            override suspend fun list(): List<VaccineRecord> { failure?.let { throw it }; return listOfNotNull(saved) }
            override suspend fun add(record: VaccineRecord) { failure?.let { throw it }; saved = record }
            override suspend fun update(record: VaccineRecord) { failure?.let { throw it }; saved = record }
            override suspend fun delete(id: String) { failure?.let { throw it }; saved = null }
        }
        val vm = VaccineRecordViewModel(ManageVaccineRecords(repository))
        assertTrue(vm.add(VaccineRecord("", "Vacuna")).isSuccess)
        val created = vm.load().getOrThrow().single()
        assertEquals(32, created.id.length)
        assertTrue(created.id.all { it in '0'..'9' || it in 'a'..'f' })
        assertTrue(vm.update(created.copy(name = "Editada")).isSuccess)
        failure = IllegalStateException("Unavailable")
        assertTrue(vm.load().isFailure)
        assertTrue(vm.add(created).isFailure)
        assertTrue(vm.update(created).isFailure)
        assertTrue(vm.delete(created.id).isFailure)
        assertEquals("Editada", saved?.name)
        failure = CancellationException("Cancelled")
        assertFailsWith<CancellationException> { vm.load() }
        failure = null
        assertTrue(vm.delete(created.id).isSuccess)
        assertEquals(emptyList(), vm.load().getOrThrow())
    }
}
