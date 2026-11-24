package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.domain.models.PregnancyJournalEntry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ProtectedPregnancyJournal(
    private val read: suspend () -> String?,
    private val write: suspend (String) -> Unit,
) {
    private val mutex = Mutex()

    suspend fun list(): List<PregnancyJournalEntry> = mutex.withLock { readEntries() }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun save(entry: PregnancyJournalEntry): PregnancyJournalEntry = mutex.withLock {
        val entries = readEntries()
        val saved = if (entry.id.isEmpty()) {
            entry.copy(id = Uuid.random().toString())
        } else {
            require(entries.any { it.id == entry.id })
            entry
        }
        write(PregnancyJournalCodec.encode(entries.filterNot { it.id == saved.id } + saved))
        saved
    }

    suspend fun delete(id: String) = mutex.withLock {
        val entries = readEntries()
        require(entries.any { it.id == id })
        write(PregnancyJournalCodec.encode(entries.filterNot { it.id == id }))
    }

    private suspend fun readEntries() = read()?.let(PregnancyJournalCodec::decode).orEmpty()
}

internal object PregnancyJournalCodec {
    private const val HEADER = "GESTIORA_JOURNAL:1\n"
    private const val MAX_PAYLOAD = 1_000_000
    private const val MAX_ENTRIES = 1000

    fun encode(entries: List<PregnancyJournalEntry>): String {
        validate(entries)
        return buildString {
            append(HEADER)
            entries.forEach { entry ->
                listOf(entry.id, entry.date, entry.title, entry.body).forEach { field ->
                    append(field.length).append(':').append(field)
                }
            }
        }.also { require(it.length <= MAX_PAYLOAD) }
    }

    fun decode(payload: String): List<PregnancyJournalEntry> {
        require(payload.length <= MAX_PAYLOAD && payload.startsWith(HEADER))
        var position = HEADER.length
        fun field(): String {
            val colon = payload.indexOf(':', position)
            require(colon - position in 1..4)
            val length = payload.substring(position, colon).toIntOrNull()
            require(length != null && length in 0..minOf(PregnancyJournalEntry.MAX_BODY, payload.length - colon - 1))
            position = colon + 1 + length
            return payload.substring(colon + 1, position)
        }
        return buildList {
            while (position < payload.length) {
                require(size < MAX_ENTRIES)
                add(PregnancyJournalEntry(field(), field(), field(), field()))
            }
        }.also(::validate)
    }

    private fun validate(entries: List<PregnancyJournalEntry>) {
        require(entries.size <= MAX_ENTRIES && entries.distinctBy { it.id }.size == entries.size)
        entries.forEach {
            require(it.id.isNotBlank() && it.id.length <= 128)
            require(validCivilDate(it.date))
            require(it.title.isNotBlank() && it.title.length <= PregnancyJournalEntry.MAX_TITLE)
            require(it.body.length <= PregnancyJournalEntry.MAX_BODY)
        }
    }
}
