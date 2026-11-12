package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.Consultation
import com.jecsdev.gestiora.domain.repository.ConsultationRepository
import kotlin.random.Random

/** Separates generated local identities from editable consultation fields. */
class ManageConsultations(private val repository: ConsultationRepository) {
    suspend fun list(): List<Consultation> = repository.list()

    suspend fun add(consultation: Consultation) {
        val id = Random.Default.nextBytes(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        repository.add(consultation.copy(id = id))
    }

    suspend fun update(consultation: Consultation) = repository.update(consultation)
    suspend fun delete(id: String) = repository.delete(id)
}
