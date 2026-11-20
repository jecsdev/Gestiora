package com.jecsdev.gestiora.domain.repository

import com.jecsdev.gestiora.domain.models.MedicalCondition

/** Storage failures must throw; an empty list means that no medicalConditions exist. */
interface MedicalConditionRepository {
    suspend fun list(): List<MedicalCondition>
    suspend fun add(condition: MedicalCondition)
    suspend fun update(condition: MedicalCondition)
    suspend fun delete(id: String)
}
