package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.domain.models.NoteTaskStatus
import com.jecsdev.gestiora.domain.models.PrenatalNote

internal fun filterNotes(notes: List<PrenatalNote>, query: String, status: NoteTaskStatus?): List<PrenatalNote> {
    val text = query.trim()
    return notes.filter { note ->
        (status == null || note.status == status) &&
            (text.isEmpty() || note.title.contains(text, ignoreCase = true) || note.body.orEmpty().contains(text, ignoreCase = true))
    }
}

/** Includes overdue tasks: a missed date must not make a pending task disappear. */
internal fun datedPendingNotes(notes: List<PrenatalNote>): List<PrenatalNote> = notes
    .filter { it.status == NoteTaskStatus.PENDING && it.followUp != null }
    .sortedWith(compareBy(PrenatalNote::followUp, PrenatalNote::id))
    .take(3)
