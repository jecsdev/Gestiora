package com.jecsdev.gestiora.domain.models

/** Historical clinical text stays unchanged when pregnancy dating is edited. */
data class Consultation(
    val id: String,
    val date: String,
    val gestationalAge: String? = null,
    val doctor: String? = null,
    val observations: String? = null,
    val instructions: String? = null,
    val medications: String? = null,
    val exams: String? = null,
    val followUp: String? = null,
    val nextDate: String? = null,
)
