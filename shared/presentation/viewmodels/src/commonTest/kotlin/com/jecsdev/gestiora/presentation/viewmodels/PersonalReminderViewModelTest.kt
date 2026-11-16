package com.jecsdev.gestiora.presentation.viewmodels

import com.jecsdev.gestiora.domain.models.PersonalReminder
import com.jecsdev.gestiora.domain.repository.PersonalReminderRepository
import com.jecsdev.gestiora.domain.usecases.ManagePersonalReminders
import kotlinx.coroutines.*
import kotlin.test.*

class PersonalReminderViewModelTest {
    @Test fun generatedIdsCrudErrorsAndCancellation() = runBlocking<Unit> {
        val rows = mutableListOf<PersonalReminder>()
        var failure: Exception? = null
        val repository = object : PersonalReminderRepository {
            override suspend fun list(): List<PersonalReminder> { failure?.let { throw it }; return rows.toList() }
            override suspend fun add(note: PersonalReminder) { rows += note }
            override suspend fun update(note: PersonalReminder) { rows[rows.indexOfFirst { it.id == note.id }] = note }
            override suspend fun delete(id: String) { rows.removeAll { it.id == id } }
        }
        val vm = PersonalReminderViewModel(ManagePersonalReminders(repository))
        assertTrue(vm.add(PersonalReminder("", "Título", "2026-11-16")).isSuccess)
        val saved = vm.load().getOrThrow().single()
        assertEquals(32, saved.id.length)
        assertTrue(vm.update(saved.copy(done = true)).isSuccess)
        assertTrue(vm.load().getOrThrow().single().done)
        assertTrue(vm.delete(saved.id).isSuccess)
        assertEquals(emptyList(), vm.load().getOrThrow())
        failure = IllegalStateException("read failed")
        assertTrue(vm.load().isFailure)
        failure = CancellationException()
        assertFailsWith<CancellationException> { vm.load() }
    }
}
