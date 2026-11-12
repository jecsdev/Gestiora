package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureConsultationsStore
import com.jecsdev.gestiora.domain.models.Consultation
import com.jecsdev.gestiora.domain.repository.ConsultationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Each read-modify-write is one serialized operation; storage failures are never treated as empty. */
class SecureConsultationRepository(private val store: SecureConsultationsStore) : ConsultationRepository {
    private val mutex = Mutex()

    override suspend fun list(): List<Consultation> = mutex.withLock {
        withContext(Dispatchers.Default) { readAll() }
    }

    override suspend fun add(consultation: Consultation) = mutate { existing ->
        validateConsultation(consultation)
        require(existing.none { it.id == consultation.id }) { "Consultation id already exists" }
        existing + consultation
    }

    override suspend fun update(consultation: Consultation) = mutate { existing ->
        validateConsultation(consultation)
        require(existing.any { it.id == consultation.id }) { "Consultation not found" }
        existing.map { if (it.id == consultation.id) consultation else it }
    }

    override suspend fun delete(id: String) = mutate { existing ->
        require(existing.any { it.id == id }) { "Consultation not found" }
        existing.filterNot { it.id == id }
    }

    private suspend fun mutate(change: (List<Consultation>) -> List<Consultation>) = mutex.withLock {
        withContext(Dispatchers.Default) {
            val current = readAll()
            store.write(ConsultationCodec.encode(change(current)))
        }
    }

    private fun readAll(): List<Consultation> = store.read()?.let(ConsultationCodec::decode) ?: emptyList()
}
