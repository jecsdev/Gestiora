package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureExamRecordsStore
import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.repository.ExamRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Each read-modify-write is one serialized operation; storage failures are never treated as empty. */
class SecureExamRecordRepository(private val store: SecureExamRecordsStore) : ExamRecordRepository {
    private val mutex = Mutex()

    override suspend fun list(): List<ExamRecord> = mutex.withLock {
        withContext(Dispatchers.Default) { readAll() }
    }

    override suspend fun add(record: ExamRecord) = mutate { existing ->
        validateExamRecord(record)
        require(existing.none { it.id == record.id }) { "ExamRecord id already exists" }
        existing + record
    }

    override suspend fun update(record: ExamRecord) = mutate { existing ->
        validateExamRecord(record)
        require(existing.any { it.id == record.id }) { "ExamRecord not found" }
        existing.map { if (it.id == record.id) record else it }
    }

    override suspend fun delete(id: String) = mutate { existing ->
        require(existing.any { it.id == id }) { "ExamRecord not found" }
        existing.filterNot { it.id == id }
    }

    private suspend fun mutate(change: (List<ExamRecord>) -> List<ExamRecord>) = mutex.withLock {
        withContext(Dispatchers.Default) {
            val current = readAll()
            store.write(ExamRecordCodec.encode(change(current)))
        }
    }

    private fun readAll(): List<ExamRecord> = store.read()?.let(ExamRecordCodec::decode) ?: emptyList()
}
