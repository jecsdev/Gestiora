package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.repository.ExamRecordRepository
import kotlin.random.Random

/** Separates generated local identities from editable record fields. */
class ManageExamRecords(private val repository: ExamRecordRepository) {
    suspend fun list(): List<ExamRecord> = repository.list()

    suspend fun add(record: ExamRecord) {
        val id = Random.Default.nextBytes(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        repository.add(record.copy(id = id))
    }

    suspend fun update(record: ExamRecord) = repository.update(record)
    suspend fun delete(id: String) = repository.delete(id)
}
