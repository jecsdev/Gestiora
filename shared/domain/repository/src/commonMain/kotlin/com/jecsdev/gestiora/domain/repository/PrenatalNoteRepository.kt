package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.PrenatalNote

/** Storage failures must throw; an empty list means that no notes exist. */
interface PrenatalNoteRepository {
    suspend fun list(): List<PrenatalNote>
    suspend fun add(note: PrenatalNote)
    suspend fun update(note: PrenatalNote)
    suspend fun delete(id: String)
}
