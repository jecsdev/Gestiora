package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.PrenatalNote
import com.jecsdev.gestiora.domain.models.NoteCategory
import com.jecsdev.gestiora.domain.models.NoteTaskStatus

class CorruptPrenatalNotesException : IllegalStateException("Stored notes are corrupt")
class UnsupportedPrenatalNotesVersionException : IllegalStateException("Stored notes version is unsupported")

internal object PrenatalNoteCodec {
    private const val HEADER = "GESTIORA_NOTES:1\n"
    fun encode(notes: List<PrenatalNote>): String {
        require(notes.size <= 1000 && notes.map { it.id }.toSet().size == notes.size)
        notes.forEach(::validatePrenatalNote)
        val raw = buildString {
            append(HEADER).append(notes.size).append('\n')
            notes.forEach { note ->
                listOf(note.id, note.title, note.body, note.category.name, note.status.name, note.followUp).forEach {
                    if (it == null) append("-1:") else append(it.length).append(':').append(it)
                }
            }
        }
        require(raw.length <= 1_000_000)
        return raw
    }

    fun decode(raw: String): List<PrenatalNote> {
        if (raw.length > 1_000_000 || !raw.startsWith("GESTIORA_NOTES:")) throw CorruptPrenatalNotesException()
        if (!raw.startsWith(HEADER)) throw UnsupportedPrenatalNotesVersionException()
        val end = raw.indexOf('\n', HEADER.length)
        if (end < 0 || end - HEADER.length !in 1..4) throw CorruptPrenatalNotesException()
        val countText = raw.substring(HEADER.length, end)
        if (countText.any { it !in '0'..'9' }) throw CorruptPrenatalNotesException()
        val count = countText.toIntOrNull()?.takeIf { it in 0..1000 } ?: throw CorruptPrenatalNotesException()
        var position = end + 1
        fun field(): String? {
            val colon = raw.indexOf(':', position)
            if (colon < 0 || colon - position !in 1..5) throw CorruptPrenatalNotesException()
            val token = raw.substring(position, colon)
            if (token == "-1") { position = colon + 1; return null }
            if (token.any { it !in '0'..'9' }) throw CorruptPrenatalNotesException()
            val length = token.toIntOrNull()?.takeIf { it <= 4096 } ?: throw CorruptPrenatalNotesException()
            if (length > raw.length - colon - 1) throw CorruptPrenatalNotesException()
            position = colon + 1 + length
            return raw.substring(colon + 1, position)
        }
        fun required() = field() ?: throw CorruptPrenatalNotesException()
        val notes = List(count) {
            val id = required()
            val title = required()
            val body = field()
            val categoryName = required()
            val statusName = required()
            PrenatalNote(id, title, body,
                NoteCategory.entries.firstOrNull { it.name == categoryName } ?: throw CorruptPrenatalNotesException(),
                NoteTaskStatus.entries.firstOrNull { it.name == statusName } ?: throw CorruptPrenatalNotesException(), field())
        }
        if (position != raw.length || notes.map { it.id }.toSet().size != notes.size) throw CorruptPrenatalNotesException()
        try { notes.forEach(::validatePrenatalNote) } catch (_: IllegalArgumentException) { throw CorruptPrenatalNotesException() }
        return notes
    }
}

internal fun validatePrenatalNote(note: PrenatalNote) {
    require(note.id.isNotBlank() && note.id.length <= 128)
    require(note.title.isNotBlank() && note.title.length <= 512)
    require((note.body?.length ?: 0) <= 4096)
    note.followUp?.let { require(validCivilDate(it)) }
}
