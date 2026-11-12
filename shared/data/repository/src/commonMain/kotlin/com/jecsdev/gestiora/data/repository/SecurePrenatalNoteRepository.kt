package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecurePrenatalNotesStore
import com.jecsdev.gestiora.domain.models.PrenatalNote
import com.jecsdev.gestiora.domain.repository.PrenatalNoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Each read-modify-write is one serialized operation; storage failures are never treated as empty. */
class SecurePrenatalNoteRepository(private val store: SecurePrenatalNotesStore) : PrenatalNoteRepository {
    private val mutex = Mutex()

    override suspend fun list(): List<PrenatalNote> = mutex.withLock {
        withContext(Dispatchers.Default) { readAll() }
    }

    override suspend fun add(note: PrenatalNote) = mutate { existing ->
        validatePrenatalNote(note)
        require(existing.none { it.id == note.id }) { "PrenatalNote id already exists" }
        existing + note
    }

    override suspend fun update(note: PrenatalNote) = mutate { existing ->
        validatePrenatalNote(note)
        require(existing.any { it.id == note.id }) { "PrenatalNote not found" }
        existing.map { if (it.id == note.id) note else it }
    }

    override suspend fun delete(id: String) = mutate { existing ->
        require(existing.any { it.id == id }) { "PrenatalNote not found" }
        existing.filterNot { it.id == id }
    }

    private suspend fun mutate(change: (List<PrenatalNote>) -> List<PrenatalNote>) = mutex.withLock {
        withContext(Dispatchers.Default) {
            val current = readAll()
            store.write(PrenatalNoteCodec.encode(change(current)))
        }
    }

    private fun readAll(): List<PrenatalNote> = store.read()?.let(PrenatalNoteCodec::decode) ?: emptyList()
}
