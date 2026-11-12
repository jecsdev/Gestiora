package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.models.VitalType
import com.jecsdev.gestiora.domain.repository.VitalRecordRepository
import com.jecsdev.gestiora.domain.usecases.ManageVitalRecords
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*

class VitalRecordViewModelTest {
    @Test fun generatedIdentityCrudFailuresAndCancellation() = runBlocking<Unit> {
        var saved: VitalRecord? = null
        var failure: Exception? = null
        val repository = object : VitalRecordRepository {
            override suspend fun list(): List<VitalRecord> { failure?.let { throw it }; return listOfNotNull(saved) }
            override suspend fun add(record: VitalRecord) { failure?.let { throw it }; saved = record }
            override suspend fun update(record: VitalRecord) { failure?.let { throw it }; saved = record }
            override suspend fun delete(id: String) { failure?.let { throw it }; saved = null }
        }
        val vm = VitalRecordViewModel(ManageVitalRecords(repository))
        assertTrue(vm.add(VitalRecord("", "2028-02-29", VitalType.WEIGHT, 62.75)).isSuccess)
        val created = vm.load().getOrThrow().single()
        assertEquals(32, created.id.length)
        assertTrue(created.id.all { it in '0'..'9' || it in 'a'..'f' })
        assertTrue(vm.update(created.copy(value = 63.0)).isSuccess)
        failure = IllegalStateException("Unavailable")
        assertTrue(vm.load().isFailure)
        assertTrue(vm.add(created).isFailure)
        assertTrue(vm.update(created).isFailure)
        assertTrue(vm.delete(created.id).isFailure)
        assertEquals(63.0, saved?.value)
        failure = CancellationException("Cancelled")
        assertFailsWith<CancellationException> { vm.load() }
        failure = null
        assertTrue(vm.delete(created.id).isSuccess)
        assertEquals(emptyList(), vm.load().getOrThrow())
    }
}
