package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.repository.VaccineRecordRepository
import kotlin.random.Random

/** Separates generated local identities from editable record fields. */
class ManageVaccineRecords(private val repository: VaccineRecordRepository) {
    suspend fun list(): List<VaccineRecord> = repository.list()

    suspend fun add(record: VaccineRecord) {
        val id = Random.Default.nextBytes(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        repository.add(record.copy(id = id))
    }

    suspend fun update(record: VaccineRecord) = repository.update(record)
    suspend fun delete(id: String) = repository.delete(id)
}
