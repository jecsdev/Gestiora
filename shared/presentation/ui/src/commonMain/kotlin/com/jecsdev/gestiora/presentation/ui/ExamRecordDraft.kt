package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import com.jecsdev.gestiora.domain.models.ExamKind
import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.models.ExamStatus

internal fun ExamKind.label(ui: UiCopy) = when (this) {
    ExamKind.ULTRASOUND -> ui.s(Res.string.ultrasound)
    ExamKind.BLOOD_TEST -> ui.s(Res.string.blood_test)
    ExamKind.URINE_TEST -> ui.s(Res.string.urine_test)
    ExamKind.GLUCOSE -> ui.s(Res.string.glucose)
    ExamKind.OTHER -> ui.s(Res.string.other)
}

internal fun ExamStatus.label(ui: UiCopy) = when (this) {
    ExamStatus.PENDING -> ui.s(Res.string.pending)
    ExamStatus.SCHEDULED -> ui.s(Res.string.scheduled_2)
    ExamStatus.COMPLETED -> ui.s(Res.string.completed_2)
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
