package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.models.VaccineStatus

internal fun VaccineStatus.label() = when (this) {
    VaccineStatus.PENDING -> "Pendiente"
    VaccineStatus.ADMINISTERED -> "Aplicada"
    VaccineStatus.POSTPONED -> "Pospuesta"
    VaccineStatus.NOT_INDICATED -> "No indicada"
}

internal fun vaccineRecordForSave(
    initial: VaccineRecord,
    name: String,
    dose: String,
    scheduled: String,
    administered: String,
    status: VaccineStatus,
    provider: String,
    location: String,
    proof: String,
    notes: String,
): VaccineRecord? {
    if (name.isBlank() || name.length > 512 || listOf(dose, provider, location, proof).any { it.length > 512 } || notes.length > 4096 ||
        (scheduled.isNotEmpty() && isoToUtcMillis(scheduled) == null) ||
        (administered.isNotEmpty() && isoToUtcMillis(administered) == null) ||
        (status == VaccineStatus.ADMINISTERED && administered.isEmpty())) return null
    fun optional(value: String) = value.trim().ifEmpty { null }
    return initial.copy(name = name.trim(), dose = optional(dose), scheduled = scheduled.ifEmpty { null },
        administered = administered.ifEmpty { null }, status = status, provider = optional(provider),
        location = optional(location), proof = optional(proof), notes = optional(notes))
}
