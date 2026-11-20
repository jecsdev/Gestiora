package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.LegacyClinicalPayloadValidator

class ClinicalPayloadValidator : LegacyClinicalPayloadValidator {
    override fun validateProfile(payload: String) { ProfileCodec.decode(payload) }
    override fun validatePersonalReminders(payload: String) { PersonalReminderCodec.decode(payload) }
    override fun validateCollection(store: String, payload: String) {
        when (store) {
            "appointments" -> AppointmentCodec.decode(payload)
            "consultations" -> ConsultationCodec.decode(payload)
            "vital_records" -> VitalRecordCodec.decode(payload)
            "medical_conditions" -> MedicalConditionCodec.decode(payload)
            "vaccine_records" -> VaccineRecordCodec.decode(payload)
            "exam_records" -> ExamRecordCodec.decode(payload)
            "prenatal_notes" -> PrenatalNoteCodec.decode(payload)
            else -> error("Unknown clinical collection")
        }
    }
}
