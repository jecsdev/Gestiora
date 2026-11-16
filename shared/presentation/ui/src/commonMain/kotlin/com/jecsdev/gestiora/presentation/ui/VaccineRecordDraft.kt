package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.models.VaccineStatus

internal fun VaccineStatus.label(ui: UiCopy) = when (this) {
    VaccineStatus.PENDING -> ui.s(Res.string.pending)
    VaccineStatus.ADMINISTERED -> ui.s(Res.string.administered)
    VaccineStatus.POSTPONED -> ui.s(Res.string.postponed)
    VaccineStatus.NOT_INDICATED -> ui.s(Res.string.not_indicated)
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
