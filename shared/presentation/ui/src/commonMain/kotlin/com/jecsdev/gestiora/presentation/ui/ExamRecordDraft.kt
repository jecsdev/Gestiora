package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.ExamKind
import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.models.ExamStatus

internal fun ExamKind.label() = when (this) {
    ExamKind.ULTRASOUND -> "Ecografía"
    ExamKind.BLOOD_TEST -> "Análisis de sangre"
    ExamKind.URINE_TEST -> "Análisis de orina"
    ExamKind.GLUCOSE -> "Glucosa"
    ExamKind.OTHER -> "Otro"
}

internal fun ExamStatus.label() = when (this) {
    ExamStatus.PENDING -> "Pendiente"
    ExamStatus.SCHEDULED -> "Programado"
    ExamStatus.COMPLETED -> "Completado"
}

internal fun examRecordForSave(
    initial: ExamRecord,
    name: String,
    kind: ExamKind,
    requested: String,
    scheduled: String,
    completed: String,
    status: ExamStatus,
    physician: String,
    results: String,
    notes: String,
    attachment: String,
): ExamRecord? {
    if (name.isBlank() || name.length > 512 || physician.length > 512 || attachment.length > 512 ||
        results.length > 4096 || notes.length > 4096 ||
        listOf(requested, scheduled, completed).any { it.isNotEmpty() && isoToUtcMillis(it) == null } ||
        (status == ExamStatus.COMPLETED && completed.isEmpty())) return null
    fun optional(value: String) = value.trim().ifEmpty { null }
    return initial.copy(name = name.trim(), kind = kind, requested = requested.ifEmpty { null }, scheduled = scheduled.ifEmpty { null },
        completed = completed.ifEmpty { null }, status = status, physician = optional(physician), results = optional(results),
        notes = optional(notes), attachment = optional(attachment))
}
