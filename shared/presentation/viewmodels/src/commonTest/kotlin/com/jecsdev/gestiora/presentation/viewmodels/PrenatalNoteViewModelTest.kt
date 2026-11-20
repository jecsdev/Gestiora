package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.PrenatalNote
import com.jecsdev.gestiora.domain.repository.PrenatalNoteRepository
import com.jecsdev.gestiora.domain.usecases.ManagePrenatalNotes
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class PrenatalNoteViewModelTest {
    @Test fun generatedIdentityAndErrorsArePropagated() = runBlocking {
        var saved: PrenatalNote? = null
        var fail = false
        val repository = object : PrenatalNoteRepository {
            override suspend fun list(): List<PrenatalNote> { check(!fail); return listOfNotNull(saved) }
            override suspend fun add(note: PrenatalNote) { check(!fail); saved = note }
            override suspend fun update(note: PrenatalNote) { check(!fail); saved = note }
            override suspend fun delete(id: String) { check(!fail); saved = null }
        }
        val vm = PrenatalNoteViewModel(ManagePrenatalNotes(repository))
        assertTrue(vm.add(PrenatalNote("", "Pregunta")).isSuccess)
        val created = vm.load().getOrThrow().single()
        assertEquals(32, created.id.length)
        assertTrue(vm.update(created.copy(title = "Editada")).isSuccess)
        fail = true
        assertTrue(vm.load().isFailure)
        assertTrue(vm.update(created).isFailure)
        assertTrue(vm.delete(created.id).isFailure)
        assertEquals("Editada", saved?.title)
        fail = false
        assertTrue(vm.delete(created.id).isSuccess)
        assertEquals(emptyList(), vm.load().getOrThrow())
    }
}
