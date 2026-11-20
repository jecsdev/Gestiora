package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureVaccineRecordsStore
import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.repository.VaccineRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Each read-modify-write is one serialized operation; storage failures are never treated as empty. */
class SecureVaccineRecordRepository(private val store: SecureVaccineRecordsStore) : VaccineRecordRepository {
    private val mutex = Mutex()

    override suspend fun list(): List<VaccineRecord> = mutex.withLock {
        withContext(Dispatchers.Default) { readAll() }
    }

    override suspend fun add(record: VaccineRecord) = mutate { existing ->
        validateVaccineRecord(record)
        require(existing.none { it.id == record.id }) { "VaccineRecord id already exists" }
        existing + record
    }

    override suspend fun update(record: VaccineRecord) = mutate { existing ->
        validateVaccineRecord(record)
        require(existing.any { it.id == record.id }) { "VaccineRecord not found" }
        existing.map { if (it.id == record.id) record else it }
    }

    override suspend fun delete(id: String) = mutate { existing ->
        require(existing.any { it.id == id }) { "VaccineRecord not found" }
        existing.filterNot { it.id == id }
    }

    private suspend fun mutate(change: (List<VaccineRecord>) -> List<VaccineRecord>) = mutex.withLock {
        withContext(Dispatchers.Default) {
            val current = readAll()
            store.write(VaccineRecordCodec.encode(change(current)))
        }
    }

    private fun readAll(): List<VaccineRecord> = store.read()?.let(VaccineRecordCodec::decode) ?: emptyList()
}
