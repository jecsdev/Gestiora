package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.ExamKind
import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.models.ExamStatus
import kotlin.test.*

class ExamRecordDraftTest {
    private val initial = ExamRecord("existing", "Registrado", completed = "2026-11-01", status = ExamStatus.COMPLETED)

    @Test fun typesAndStatusesPreserveIdentityAndHistoricalCompletionAndNormalizeText() {
        for (kind in ExamKind.entries) for (status in ExamStatus.entries) {
            val record = assertNotNull(examRecordForSave(initial, " Estudio ", kind, "2026-10-01", "2026-10-30", initial.completed!!,
                status, " Profesional ", " Resultados\nregistrados ", " Notas 😊 ", " Carpeta "))
            assertEquals("existing", record.id)
            assertEquals("Estudio", record.name)
            assertEquals(kind, record.kind)
            assertEquals("2026-10-01", record.requested)
            assertEquals("2026-10-30", record.scheduled)
            assertEquals("2026-11-01", record.completed)
            assertEquals(status, record.status)
            assertEquals("Profesional", record.physician)
            assertEquals("Resultados\nregistrados", record.results)
            assertEquals("Notas 😊", record.notes)
            assertEquals("Carpeta", record.attachment)
        }
        val corrected = assertNotNull(examRecordForSave(initial, "Estudio", ExamKind.BLOOD_TEST, "", "", "", ExamStatus.PENDING, " ", "", "", ""))
        assertNull(corrected.requested)
        assertNull(corrected.scheduled)
        assertNull(corrected.completed)
        assertNull(corrected.physician)
        assertNull(corrected.results)
        assertNull(corrected.notes)
        assertNull(corrected.attachment)
    }

    @Test fun completedRequiresValidDateAndFieldLimitsAreEnforced() {
        fun save(name: String = "Estudio", requested: String = "", scheduled: String = "", completed: String = "", status: ExamStatus = ExamStatus.PENDING,
            physician: String = "", results: String = "", notes: String = "", attachment: String = "") =
            examRecordForSave(initial, name, ExamKind.BLOOD_TEST, requested, scheduled, completed, status, physician, results, notes, attachment)
        assertNotNull(save())
        assertNull(save(name = " "))
        assertNull(save(status = ExamStatus.COMPLETED))
        assertNotNull(save(status = ExamStatus.COMPLETED, completed = "2028-02-29"))
        assertNull(save(requested = "2027-02-29"))
        assertNull(save(scheduled = "2028-13-01"))
        assertNull(save(completed = "2028-02-30"))
        assertNull(save(name = "x".repeat(513)))
        assertNull(save(physician = "x".repeat(513)))
        assertNull(save(attachment = "x".repeat(513)))
        assertNull(save(results = "x".repeat(4097)))
        assertNull(save(notes = "x".repeat(4097)))
        assertNotNull(save(name = "x".repeat(512), attachment = "x".repeat(512), results = "x".repeat(4096), notes = "x".repeat(4096)))
    }
}
