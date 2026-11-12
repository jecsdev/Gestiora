package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.repository.ExamRecordRepository
import com.jecsdev.gestiora.domain.usecases.ManageExamRecords
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*

class ExamRecordViewModelTest {
    @Test fun generatedIdentityCrudFailuresAndCancellation() = runBlocking<Unit> {
        var saved: ExamRecord? = null
        var failure: Exception? = null
        val repository = object : ExamRecordRepository {
            override suspend fun list(): List<ExamRecord> { failure?.let { throw it }; return listOfNotNull(saved) }
            override suspend fun add(record: ExamRecord) { failure?.let { throw it }; saved = record }
            override suspend fun update(record: ExamRecord) { failure?.let { throw it }; saved = record }
            override suspend fun delete(id: String) { failure?.let { throw it }; saved = null }
        }
        val vm = ExamRecordViewModel(ManageExamRecords(repository))
        assertTrue(vm.add(ExamRecord("", "Estudio")).isSuccess)
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
