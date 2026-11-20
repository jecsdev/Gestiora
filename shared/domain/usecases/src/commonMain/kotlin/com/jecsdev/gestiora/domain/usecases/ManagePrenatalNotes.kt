package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.PrenatalNote
import com.jecsdev.gestiora.domain.repository.PrenatalNoteRepository
import kotlin.random.Random

/** Separates generated local identities from editable note fields. */
class ManagePrenatalNotes(private val repository: PrenatalNoteRepository) {
    suspend fun list(): List<PrenatalNote> = repository.list()

    suspend fun add(note: PrenatalNote) {
        val id = Random.Default.nextBytes(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        repository.add(note.copy(id = id))
    }

    suspend fun update(note: PrenatalNote) = repository.update(note)
    suspend fun delete(id: String) = repository.delete(id)
}
