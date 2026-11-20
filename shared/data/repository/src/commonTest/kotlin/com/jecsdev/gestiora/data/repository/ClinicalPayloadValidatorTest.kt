package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.*
import kotlin.test.*

class ClinicalPayloadValidatorTest {
    @Test fun validatesEveryCollectionWithItsOwnCodecAndRejectsCrossStorePayloads() {
        val payloads = mapOf(
            "appointments" to AppointmentCodec.encode(emptyList()),
            "consultations" to ConsultationCodec.encode(emptyList()),
            "vital_records" to VitalRecordCodec.encode(emptyList()),
            "medical_conditions" to MedicalConditionCodec.encode(emptyList()),
            "vaccine_records" to VaccineRecordCodec.encode(emptyList()),
            "exam_records" to ExamRecordCodec.encode(emptyList()),
            "prenatal_notes" to PrenatalNoteCodec.encode(emptyList()),
        )
        val validator = ClinicalPayloadValidator()
        payloads.forEach { (store, payload) ->
            validator.validateCollection(store, payload)
            assertFails { validator.validateCollection(store, "broken") }
            assertFails { validator.validateCollection(store, payload.replace(":1\n", ":99\n").replace(":2\n", ":99\n")) }
            payloads.filterKeys { it != store }.values.forEach { other ->
                assertFails { validator.validateCollection(store, other) }
            }
        }
        assertFails { validator.validateCollection("unknown", payloads.values.first()) }
    }

    @Test fun acceptsExistingCodecsWithoutReencodingHistoricalValues() {
        val profile = PregnancyProfile("Lucía 😊\nR.", null, "Bolivia", "Español",
            PregnancyDating(DatingMethod.ULTRASOUND, lmp = "2026-01-01", referenceDate = "2026-03-01", referenceWeeks = 10, referenceDays = 2))
        val reminder = PersonalReminder("old-id", "Control 😊", "2028-02-29", null, ReminderKind.FOLLOW_UP, true)
        val profilePayload = ProfileCodec.encode(profile)
        val remindersPayload = PersonalReminderCodec.encode(listOf(reminder))
        ClinicalPayloadValidator().validateProfile(profilePayload)
        ClinicalPayloadValidator().validatePersonalReminders(remindersPayload)
        assertEquals(profile, ProfileCodec.decode(profilePayload))
        assertEquals(listOf(reminder), PersonalReminderCodec.decode(remindersPayload))
    }

    @Test fun rejectsUnknownAndMalformedSourcesRatherThanTreatingThemAsEmpty() {
        val validator = ClinicalPayloadValidator()
        assertFailsWith<UnsupportedProfileVersionException> { validator.validateProfile("GESTIORA_PROFILE:99\n") }
        assertFailsWith<UnsupportedPersonalRemindersVersionException> { validator.validatePersonalReminders("GESTIORA_REMINDERS:99\n") }
        assertFailsWith<CorruptProfileException> { validator.validateProfile("broken") }
        assertFailsWith<CorruptPersonalRemindersException> { validator.validatePersonalReminders("GESTIORA_REMINDERS:1\n1\n") }
    }
}
