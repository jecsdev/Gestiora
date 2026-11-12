package com.jecsdev.gestiora.presentation.ui

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

internal fun VitalType.label() = when (this) {
    VitalType.WEIGHT -> "Peso materno (kg)"
    VitalType.BLOOD_PRESSURE -> "Presión arterial (mmHg)"
    VitalType.MATERNAL_HEART_RATE -> "Frecuencia cardíaca materna (lpm)"
    VitalType.FETAL_HEART_RATE -> "Frecuencia cardíaca fetal (lpm)"
    VitalType.FUNDAL_HEIGHT -> "Altura uterina (cm)"
    VitalType.ULTRASOUND -> "Hallazgo ecográfico / medidas fetales"
    VitalType.SYMPTOM -> "Síntoma"
    VitalType.CUSTOM -> "Medida personalizada"
}

internal fun vitalRecordValue(record: VitalRecord): String = when (record.type) {
    VitalType.ULTRASOUND, VitalType.SYMPTOM -> record.text.orEmpty()
    VitalType.BLOOD_PRESSURE -> "${record.value?.let(::formatVitalNumber) ?: "—"}/${record.value2?.let(::formatVitalNumber) ?: "—"} mmHg"
    else -> {
        val value = record.value?.let(::formatVitalNumber) ?: "—"
        when (record.type) {
            VitalType.WEIGHT -> "$value kg"
            VitalType.MATERNAL_HEART_RATE, VitalType.FETAL_HEART_RATE -> "$value lpm"
            VitalType.FUNDAL_HEIGHT -> "$value cm"
            else -> value
        }
    }
}
