package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.Consultation

/** Storage failures must throw; an empty list means that no consultations exist. */
interface ConsultationRepository {
    suspend fun list(): List<Consultation>
    suspend fun add(consultation: Consultation)
    suspend fun update(consultation: Consultation)
    suspend fun delete(id: String)
}
