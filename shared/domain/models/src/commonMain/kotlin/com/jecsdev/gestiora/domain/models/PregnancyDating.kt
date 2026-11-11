package com.jecsdev.gestiora.domain.models

enum class DatingMethod { FUM, ULTRASOUND, MEDICAL }

/** All dates use an ISO calendar date (yyyy-MM-dd), without a time zone. */
data class PregnancyDating(
    val method: DatingMethod,
    val lmp: String? = null,
    val dueDate: String? = null,
    val referenceDate: String? = null,
    val referenceWeeks: Int? = null,
    val referenceDays: Int? = null,
)

data class PregnancyAge(
    val totalDays: Int,
    val weeks: Int,
    val days: Int,
    val trimester: Int,
    val dueDate: String,
    val daysLeft: Int,
    val progress: Double,
)
