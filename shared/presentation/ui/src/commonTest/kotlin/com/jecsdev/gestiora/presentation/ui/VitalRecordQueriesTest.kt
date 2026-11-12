package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.models.VitalType
import kotlin.test.*

class VitalRecordQueriesTest {
    @Test fun decimalInputDoesNotCoerceBlankOrInvalidValuesToZero() {
        assertEquals(62.5, parseVitalNumber(" 62,5 "))
        assertEquals(62.5, parseVitalNumber("62.5"))
        assertEquals(0.5, parseVitalNumber(",5"))
        assertEquals(0.0, parseVitalNumber("0"))
        assertEquals(0.0000001, parseVitalNumber(formatVitalNumber(0.0000001)))
        assertEquals(100.0, parseVitalNumber("1e2"))
        for (invalid in listOf("", " ", "NaN", "Infinity", "1e999", "1,234.5", "1.234,5", "1 234", "1,2,3", "12kg", "-", ".")) {
            assertNull(parseVitalNumber(invalid), invalid)
        }
    }

    @Test fun historyAndSummaryUseDatesTypesAndStableTies() {
        val records = listOf(
            VitalRecord("later-b", "2026-11-12", VitalType.WEIGHT, 63.0),
            VitalRecord("old", "2026-10-01", VitalType.WEIGHT, 62.5),
            VitalRecord("bp", "2026-11-11", VitalType.BLOOD_PRESSURE, 120.0, 80.0),
            VitalRecord("later-a", "2026-11-12", VitalType.WEIGHT, 64.0),
            VitalRecord("text", "2026-11-13", VitalType.SYMPTOM, text = "Registro"),
        )
        assertEquals(listOf("text", "later-a", "later-b", "bp", "old"), orderedVitalRecords(records).map { it.id })
        assertEquals("later-a", latestVitalRecord(records.reversed(), VitalType.WEIGHT)?.id)
        assertEquals("bp", latestVitalRecord(records, VitalType.BLOOD_PRESSURE)?.id)
        assertNull(latestVitalRecord(records, VitalType.FETAL_HEART_RATE))
        assertNull(latestVitalRecord(emptyList(), VitalType.WEIGHT))
        assertEquals("120/80 mmHg", vitalRecordValue(records[2]))
        assertEquals("62.5 kg", vitalRecordValue(records[1]))
        assertEquals("Registro", vitalRecordValue(records.last()))
        assertEquals("0 kg", vitalRecordValue(VitalRecord("zero", "2026-11-12", VitalType.WEIGHT, 0.0)))
    }

    @Test fun changingTypeClearsHiddenFieldsAndKeepsOnlyApplicableRequiredValues() {
        val initial = VitalRecord("record", "2026-11-12", VitalType.BLOOD_PRESSURE, 120.0, 80.0, notes = "Histórica")
        fun draft(type: VitalType, value: String = "65,5", value2: String = "80", label: String = "Medida", text: String = "Descripción") =
            vitalRecordForSave(initial, initial.date, type, value, value2, label, text, " Notas ")
        val weight = assertNotNull(draft(VitalType.WEIGHT))
        assertEquals(65.5, weight.value)
        assertNull(weight.value2)
        assertNull(weight.label)
        assertNull(weight.text)
        assertEquals("Notas", weight.notes)
        val symptom = assertNotNull(draft(VitalType.SYMPTOM))
        assertEquals("Descripción", symptom.text)
        assertNull(symptom.value)
        assertNull(symptom.value2)
        assertNull(symptom.label)
        assertEquals("Medida", assertNotNull(draft(VitalType.CUSTOM)).label)
        assertNull(draft(VitalType.WEIGHT, value = ""))
        assertNull(draft(VitalType.WEIGHT, value = "401"))
        assertNull(draft(VitalType.BLOOD_PRESSURE, value2 = ""))
        assertNull(draft(VitalType.CUSTOM, label = " "))
        assertNull(draft(VitalType.ULTRASOUND, text = " "))
        assertNull(vitalRecordForSave(initial, "2027-02-29", initial.type, "120", "80", "", "", ""))
    }
}
