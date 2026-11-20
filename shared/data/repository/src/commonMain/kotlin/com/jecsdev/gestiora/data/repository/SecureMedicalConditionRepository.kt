package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureMedicalConditionsStore
import com.jecsdev.gestiora.domain.models.MedicalCondition
import com.jecsdev.gestiora.domain.repository.MedicalConditionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Each read-modify-write is one serialized operation; storage failures are never treated as empty. */
class SecureMedicalConditionRepository(private val store: SecureMedicalConditionsStore) : MedicalConditionRepository {
    private val mutex = Mutex()

    override suspend fun list(): List<MedicalCondition> = mutex.withLock {
        withContext(Dispatchers.Default) { readAll() }
    }

    override suspend fun add(condition: MedicalCondition) = mutate { existing ->
        validateMedicalCondition(condition)
        require(existing.none { it.id == condition.id }) { "MedicalCondition id already exists" }
        existing + condition
    }

    override suspend fun update(condition: MedicalCondition) = mutate { existing ->
        validateMedicalCondition(condition)
        require(existing.any { it.id == condition.id }) { "MedicalCondition not found" }
        existing.map { if (it.id == condition.id) condition else it }
    }

    override suspend fun delete(id: String) = mutate { existing ->
        require(existing.any { it.id == id }) { "MedicalCondition not found" }
        existing.filterNot { it.id == id }
    }

    private suspend fun mutate(change: (List<MedicalCondition>) -> List<MedicalCondition>) = mutex.withLock {
        withContext(Dispatchers.Default) {
            val current = readAll()
            store.write(MedicalConditionCodec.encode(change(current)))
        }
    }

    private fun readAll(): List<MedicalCondition> = store.read()?.let(MedicalConditionCodec::decode) ?: emptyList()
}
