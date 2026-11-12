package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.ConditionStatus
import com.jecsdev.gestiora.domain.models.MedicalCondition
import kotlin.test.*

class MedicalConditionDraftTest {
    private val initial = MedicalCondition("existing", "Histórica", "2026-11-12", "Profesional")

    @Test fun editingPreservesIdentityAndNormalizesOptionalFieldsForEveryStatus() {
        for (status in ConditionStatus.entries) {
            val saved = assertNotNull(medicalConditionForSave(initial, " Condición ", "2028-02-29", " Profesional ",
                " Indicaciones\nregistradas ", " ", " Control ", "2028-03-01", status, " Notas 😊 "))
            assertEquals("existing", saved.id)
            assertEquals("Condición", saved.name)
            assertEquals("Profesional", saved.professional)
            assertEquals("Indicaciones\nregistradas", saved.instructions)
            assertNull(saved.treatment)
            assertEquals("Control", saved.monitoring)
            assertEquals("2028-03-01", saved.followUp)
            assertEquals(status, saved.status)
            assertEquals("Notas 😊", saved.notes)
        }
    }

    @Test fun requiredDatesAndFieldBoundsRejectInvalidDrafts() {
        fun save(name: String = "Condición", professional: String = "Profesional", date: String = "2028-02-29", followUp: String = "", instructions: String = "", treatment: String = "", monitoring: String = "", notes: String = "") =
            medicalConditionForSave(initial, name, date, professional, instructions, treatment, monitoring, followUp, ConditionStatus.FOLLOW_UP, notes)
        assertNull(save(name = " "))
        assertNull(save(professional = " "))
        assertNull(save(date = "2027-02-29"))
        assertNull(save(followUp = "2028-13-01"))
        assertNull(save(name = "x".repeat(513)))
        assertNull(save(professional = "x".repeat(513)))
        assertNull(save(instructions = "x".repeat(4097)))
        assertNull(save(treatment = "x".repeat(4097)))
        assertNull(save(monitoring = "x".repeat(4097)))
        assertNull(save(notes = "x".repeat(4097)))
        assertNull(assertNotNull(save()).followUp)
        assertNotNull(save(name = "x".repeat(512), professional = "x".repeat(512), notes = "x".repeat(4096)))
    }
}
