package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.VaccineRecord

/** Storage failures must throw; an empty list means that no vaccineRecords exist. */
interface VaccineRecordRepository {
    suspend fun list(): List<VaccineRecord>
    suspend fun add(record: VaccineRecord)
    suspend fun update(record: VaccineRecord)
    suspend fun delete(id: String)
}
