package com.jecsdev.gestiora.domain.models

enum class VaccineStatus { PENDING, ADMINISTERED, POSTPONED, NOT_INDICATED }

/** A recorded vaccination; this does not recommend or generate an immunization schedule. */
data class VaccineRecord(
    val id: String,
    val name: String,
    val dose: String? = null,
    val scheduled: String? = null,
    val administered: String? = null,
    val status: VaccineStatus = VaccineStatus.PENDING,
    val provider: String? = null,
    val location: String? = null,
    val proof: String? = null,
    val notes: String? = null,
)
