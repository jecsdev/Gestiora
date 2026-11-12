package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.ConditionStatus
import com.jecsdev.gestiora.domain.models.MedicalCondition

internal fun ConditionStatus.label() = when (this) {
    ConditionStatus.ACTIVE -> "Activa"
    ConditionStatus.FOLLOW_UP -> "En seguimiento"
    ConditionStatus.RESOLVED -> "Resuelta"
}

internal fun medicalConditionForSave(
    initial: MedicalCondition,
    name: String,
    date: String,
    professional: String,
    instructions: String,
    treatment: String,
    monitoring: String,
    followUp: String,
    status: ConditionStatus,
    notes: String,
): MedicalCondition? {
    if (name.isBlank() || name.length > 512 || professional.isBlank() || professional.length > 512 ||
        isoToUtcMillis(date) == null || (followUp.isNotEmpty() && isoToUtcMillis(followUp) == null) ||
        listOf(instructions, treatment, monitoring, notes).any { it.length > 4096 }) return null
    fun optional(value: String) = value.trim().ifEmpty { null }
    return initial.copy(name = name.trim(), date = date, professional = professional.trim(),
        instructions = optional(instructions), treatment = optional(treatment), monitoring = optional(monitoring),
        followUp = followUp.ifEmpty { null }, status = status, notes = optional(notes))
}
