package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.NoteTaskStatus
import com.jecsdev.gestiora.domain.models.PrenatalNote
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteQueriesTest {
    @Test fun searchCombinesBodyTitleAndTaskFilter() {
        val notes = listOf(
            PrenatalNote("1", "Pregunta", "Consulta de mañana", status = NoteTaskStatus.PENDING),
            PrenatalNote("2", "CONSULTA", status = NoteTaskStatus.COMPLETED),
            PrenatalNote("3", "Otra nota"),
        )
        assertEquals(listOf("1", "2"), filterNotes(notes, " consulta ", null).map { it.id })
        assertEquals(listOf("1"), filterNotes(notes, "consulta", NoteTaskStatus.PENDING).map { it.id })
        assertEquals(notes, filterNotes(notes, "  ", null))
        assertEquals(emptyList(), filterNotes(notes, "inexistente", null))
    }

    @Test fun datedPendingTasksIncludeOverdueAndExcludeCompletedOrUndated() {
        val notes = listOf(
            PrenatalNote("later", "Futura", status = NoteTaskStatus.PENDING, followUp = "2027-01-01"),
            PrenatalNote("undated", "Sin fecha", status = NoteTaskStatus.PENDING),
            PrenatalNote("completed", "Hecha", status = NoteTaskStatus.COMPLETED, followUp = "2025-01-01"),
            PrenatalNote("none", "Sin tarea", followUp = "2025-01-01"),
            PrenatalNote("overdue", "Pasada", status = NoteTaskStatus.PENDING, followUp = "2025-02-01"),
            PrenatalNote("near", "Cercana", status = NoteTaskStatus.PENDING, followUp = "2026-11-13"),
            PrenatalNote("today", "Hoy", status = NoteTaskStatus.PENDING, followUp = "2026-11-12"),
        )
        assertEquals(listOf("overdue", "today", "near"), datedPendingNotes(notes).map { it.id })
        assertEquals(emptyList(), datedPendingNotes(emptyList()))
    }
}
