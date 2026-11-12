package com.jecsdev.gestiora.domain.models

enum class VitalType {
    WEIGHT, BLOOD_PRESSURE, MATERNAL_HEART_RATE, FETAL_HEART_RATE, FUNDAL_HEIGHT, ULTRASOUND, SYMPTOM, CUSTOM;

    val isNumeric: Boolean get() = this != ULTRASOUND && this != SYMPTOM
}

data class VitalRecord(
    val id: String,
    val date: String,
    val type: VitalType,
    val value: Double? = null,
    val value2: Double? = null,
    val label: String? = null,
    val text: String? = null,
    val notes: String? = null,
)

/** Prototype input limits and field consistency only; no clinical interpretation. */
object VitalRecordValidation {
    fun validate(record: VitalRecord) {
        require((record.label?.length ?: 0) <= 512)
        require((record.text?.length ?: 0) <= 4096 && (record.notes?.length ?: 0) <= 4096)
        if (record.type.isNumeric) {
            val value = requireNotNull(record.value)
            require(value.isFinite() && value in 0.0..400.0)
            require(record.text == null)
        } else {
            require(record.value == null && record.value2 == null)
            require(!record.text.isNullOrBlank())
        }
        if (record.type == VitalType.BLOOD_PRESSURE) {
            val diastolic = requireNotNull(record.value2)
            require(diastolic.isFinite() && diastolic in 20.0..200.0)
        } else require(record.value2 == null)
        if (record.type == VitalType.CUSTOM) require(!record.label.isNullOrBlank())
        else require(record.label == null)
    }
}
