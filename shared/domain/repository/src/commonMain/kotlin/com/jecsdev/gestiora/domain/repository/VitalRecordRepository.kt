package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.VitalRecord

/** Storage failures must throw; an empty list means that no vital records exist. */
interface VitalRecordRepository {
    suspend fun list(): List<VitalRecord>
    suspend fun add(record: VitalRecord)
    suspend fun update(record: VitalRecord)
    suspend fun delete(id: String)
}
