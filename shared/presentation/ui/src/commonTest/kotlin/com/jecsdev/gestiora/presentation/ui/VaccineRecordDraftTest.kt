package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.models.VaccineStatus
import kotlin.test.*

class VaccineRecordDraftTest {
    private val initial = VaccineRecord("existing", "Registrada", administered = "2026-11-01", status = VaccineStatus.ADMINISTERED)

    @Test fun statusesPreserveIdentityAndHistoricalApplicationWhileNormalizingOptionalText() {
        for (status in VaccineStatus.entries) {
            val record = assertNotNull(vaccineRecordForSave(initial, " Vacuna ", " Dosis ", "2026-10-30", initial.administered!!,
                status, " Profesional ", " Lugar ", " Carnet 😊 ", " Notas\nregistradas "))
            assertEquals("existing", record.id)
            assertEquals("Vacuna", record.name)
            assertEquals("Dosis", record.dose)
            assertEquals("2026-11-01", record.administered)
            assertEquals("2026-10-30", record.scheduled)
            assertEquals(status, record.status)
            assertEquals("Profesional", record.provider)
            assertEquals("Lugar", record.location)
            assertEquals("Carnet 😊", record.proof)
            assertEquals("Notas\nregistradas", record.notes)
        }
        val corrected = assertNotNull(vaccineRecordForSave(initial, "Vacuna", " ", "", "", VaccineStatus.PENDING, "", "", "", ""))
        assertNull(corrected.administered)
        assertNull(corrected.scheduled)
        assertNull(corrected.dose)
        assertNull(corrected.provider)
        assertNull(corrected.location)
        assertNull(corrected.proof)
        assertNull(corrected.notes)
    }

    @Test fun administeredRequiresValidDateAndAllInputLimitsAreEnforced() {
        fun save(name: String = "Vacuna", dose: String = "", scheduled: String = "", administered: String = "", status: VaccineStatus = VaccineStatus.PENDING,
            provider: String = "", location: String = "", proof: String = "", notes: String = "") =
            vaccineRecordForSave(initial, name, dose, scheduled, administered, status, provider, location, proof, notes)
        assertNotNull(save())
        assertNull(save(name = " "))
        assertNull(save(status = VaccineStatus.ADMINISTERED))
        assertNotNull(save(status = VaccineStatus.ADMINISTERED, administered = "2028-02-29"))
        assertNull(save(scheduled = "2027-02-29"))
        assertNull(save(administered = "2028-13-01"))
        assertNull(save(name = "x".repeat(513)))
        assertNull(save(dose = "x".repeat(513)))
        assertNull(save(provider = "x".repeat(513)))
        assertNull(save(location = "x".repeat(513)))
        assertNull(save(proof = "x".repeat(513)))
        assertNull(save(notes = "x".repeat(4097)))
        assertNotNull(save(name = "x".repeat(512), proof = "x".repeat(512), notes = "x".repeat(4096)))
    }
}
