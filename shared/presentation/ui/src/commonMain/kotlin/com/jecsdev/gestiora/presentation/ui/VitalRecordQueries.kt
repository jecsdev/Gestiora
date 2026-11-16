package com.jecsdev.gestiora.presentation.ui

import gestiora.shared.presentation.ui.generated.resources.*

import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.models.VitalType
import com.jecsdev.gestiora.domain.models.VitalRecordValidation

/** A decimal separator is accepted in either language; grouping separators are not. */
internal fun parseVitalNumber(input: String): Double? {
    val value = input.trim()
    if (!Regex("[+-]?(?:[0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)(?:[eE][+-]?[0-9]+)?").matches(value)) return null
    return value.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
}

internal fun orderedVitalRecords(records: List<VitalRecord>): List<VitalRecord> =
    records.sortedWith(compareByDescending<VitalRecord> { it.date }.thenBy { it.id })

/** Date-only records have no time order; ID breaks ties consistently across reloads. */
internal fun latestVitalRecord(records: List<VitalRecord>, type: VitalType): VitalRecord? =
    orderedVitalRecords(records).firstOrNull { it.type == type }

internal fun formatVitalNumber(value: Double): String = value.toString().removeSuffix(".0")

/** Saving a changed type never carries hidden values from the previous type. */
internal fun vitalRecordForSave(
    initial: VitalRecord,
    date: String,
    type: VitalType,
    value: String,
    value2: String,
    label: String,
    text: String,
    notes: String,
): VitalRecord? {
    if (isoToUtcMillis(date) == null || notes.length > 4096 ||
        (type == VitalType.CUSTOM && label.length > 512) || (!type.isNumeric && text.length > 4096)) return null
    val record = initial.copy(
        date = date,
        type = type,
        value = if (type.isNumeric) parseVitalNumber(value) else null,
        value2 = if (type == VitalType.BLOOD_PRESSURE) parseVitalNumber(value2) else null,
        label = if (type == VitalType.CUSTOM) label.trim() else null,
        text = if (!type.isNumeric) text.trim() else null,
        notes = notes.trim().ifEmpty { null },
    )
    return try {
        VitalRecordValidation.validate(record)
        record
    } catch (_: IllegalArgumentException) { null }
}

internal fun VitalType.label(ui: UiCopy) = when (this) {
    VitalType.WEIGHT -> ui.s(Res.string.maternal_weight_kg)
    VitalType.BLOOD_PRESSURE -> ui.s(Res.string.blood_pressure_mmhg)
    VitalType.MATERNAL_HEART_RATE -> ui.s(Res.string.maternal_heart_rate_bpm)
    VitalType.FETAL_HEART_RATE -> ui.s(Res.string.fetal_heart_rate_bpm)
    VitalType.FUNDAL_HEIGHT -> ui.s(Res.string.fundal_height_cm)
    VitalType.ULTRASOUND -> ui.s(Res.string.ultrasound_findings_fetal_measurements)
    VitalType.SYMPTOM -> ui.s(Res.string.symptom)
    VitalType.CUSTOM -> ui.s(Res.string.custom_measurement)
}

internal fun vitalRecordValue(record: VitalRecord, ui: UiCopy? = null): String = when (record.type) {
    VitalType.ULTRASOUND, VitalType.SYMPTOM -> record.text.orEmpty()
    VitalType.BLOOD_PRESSURE -> "${record.value?.let(::formatVitalNumber) ?: "—"}/${record.value2?.let(::formatVitalNumber) ?: "—"} mmHg"
    else -> {
        val value = record.value?.let(::formatVitalNumber) ?: "—"
        val unit = when (record.type) {
            VitalType.WEIGHT -> Res.string.kg
            VitalType.MATERNAL_HEART_RATE, VitalType.FETAL_HEART_RATE -> Res.string.bpm
            VitalType.FUNDAL_HEIGHT -> Res.string.cm
            else -> null
        }
        val suffix = when (record.type) {
            VitalType.WEIGHT -> "kg"
            VitalType.FUNDAL_HEIGHT -> "cm"
            else -> "bpm"
        }
        if (unit == null) value else ui?.s(unit, value) ?: "$value $suffix"
    }
}
