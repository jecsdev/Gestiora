package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.repository.VitalRecordRepository
import kotlin.random.Random

/** Separates generated local identities from editable record fields. */
class ManageVitalRecords(private val repository: VitalRecordRepository) {
    suspend fun list(): List<VitalRecord> = repository.list()

    suspend fun add(record: VitalRecord) {
        val id = Random.Default.nextBytes(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        repository.add(record.copy(id = id))
    }

    suspend fun update(record: VitalRecord) = repository.update(record)
    suspend fun delete(id: String) = repository.delete(id)
}
