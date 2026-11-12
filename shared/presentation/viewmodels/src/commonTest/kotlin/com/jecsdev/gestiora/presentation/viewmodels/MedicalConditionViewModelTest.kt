package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.MedicalCondition
import com.jecsdev.gestiora.domain.repository.MedicalConditionRepository
import com.jecsdev.gestiora.domain.usecases.ManageMedicalConditions
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*

class MedicalConditionViewModelTest {
    @Test fun generatedIdentityCrudFailuresAndCancellation() = runBlocking<Unit> {
        var saved: MedicalCondition? = null
        var failure: Exception? = null
        val repository = object : MedicalConditionRepository {
            override suspend fun list(): List<MedicalCondition> { failure?.let { throw it }; return listOfNotNull(saved) }
            override suspend fun add(condition: MedicalCondition) { failure?.let { throw it }; saved = condition }
            override suspend fun update(condition: MedicalCondition) { failure?.let { throw it }; saved = condition }
            override suspend fun delete(id: String) { failure?.let { throw it }; saved = null }
        }
        val vm = MedicalConditionViewModel(ManageMedicalConditions(repository))
        assertTrue(vm.add(MedicalCondition("", "Condición", "2028-02-29", "Profesional")).isSuccess)
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
