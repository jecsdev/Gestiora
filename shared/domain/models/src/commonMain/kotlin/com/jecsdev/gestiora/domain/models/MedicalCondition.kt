package com.jecsdev.gestiora.domain.models

enum class ConditionStatus { ACTIVE, FOLLOW_UP, RESOLVED }

/** A historical condition supplied by a health professional; no diagnosis is inferred. */
data class MedicalCondition(
    val id: String,
    val name: String,
    val date: String,
    val professional: String,
    val instructions: String? = null,
    val treatment: String? = null,
    val monitoring: String? = null,
    val followUp: String? = null,
    val status: ConditionStatus = ConditionStatus.FOLLOW_UP,
    val notes: String? = null,
)
