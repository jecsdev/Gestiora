package com.jecsdev.gestiora.domain.usecases

import com.jecsdev.gestiora.domain.models.MedicalCondition
import com.jecsdev.gestiora.domain.repository.MedicalConditionRepository
import kotlin.random.Random

/** Separates generated local identities from editable condition fields. */
class ManageMedicalConditions(private val repository: MedicalConditionRepository) {
    suspend fun list(): List<MedicalCondition> = repository.list()

    suspend fun add(condition: MedicalCondition) {
        val id = Random.Default.nextBytes(16).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        repository.add(condition.copy(id = id))
    }

    suspend fun update(condition: MedicalCondition) = repository.update(condition)
    suspend fun delete(id: String) = repository.delete(id)
}
