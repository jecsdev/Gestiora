package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureVitalRecordsStore
import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.repository.VitalRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Each read-modify-write is one serialized operation; storage failures are never treated as empty. */
class SecureVitalRecordRepository(private val store: SecureVitalRecordsStore) : VitalRecordRepository {
    private val mutex = Mutex()

    override suspend fun list(): List<VitalRecord> = mutex.withLock {
        withContext(Dispatchers.Default) { readAll() }
    }

    override suspend fun add(record: VitalRecord) = mutate { existing ->
        validateVitalRecord(record)
        require(existing.none { it.id == record.id }) { "VitalRecord id already exists" }
        existing + record
    }

    override suspend fun update(record: VitalRecord) = mutate { existing ->
        validateVitalRecord(record)
        require(existing.any { it.id == record.id }) { "VitalRecord not found" }
        existing.map { if (it.id == record.id) record else it }
    }

    override suspend fun delete(id: String) = mutate { existing ->
        require(existing.any { it.id == id }) { "VitalRecord not found" }
        existing.filterNot { it.id == id }
    }

    private suspend fun mutate(change: (List<VitalRecord>) -> List<VitalRecord>) = mutex.withLock {
        withContext(Dispatchers.Default) {
            val current = readAll()
            store.write(VitalRecordCodec.encode(change(current)))
        }
    }

    private fun readAll(): List<VitalRecord> = store.read()?.let(VitalRecordCodec::decode) ?: emptyList()
}
