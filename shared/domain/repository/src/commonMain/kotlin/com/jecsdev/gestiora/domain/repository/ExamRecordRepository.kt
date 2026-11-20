package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.ExamRecord

/** Storage failures must throw; an empty list means that no examRecords exist. */
interface ExamRecordRepository {
    suspend fun list(): List<ExamRecord>
    suspend fun add(record: ExamRecord)
    suspend fun update(record: ExamRecord)
    suspend fun delete(id: String)
}
