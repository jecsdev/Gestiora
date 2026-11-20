package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecurePrenatalNotesStore
import com.jecsdev.gestiora.domain.models.*
import kotlinx.coroutines.*
import kotlin.test.*

class SecurePrenatalNoteRepositoryTest {
    private class Store(var payload: String? = null) : SecurePrenatalNotesStore {
        var failWrite = false
        var failRead = false
        override fun read(): String? { check(!failRead); return payload }
        override fun write(payload: String) { check(!failWrite); this.payload = payload }
    }
    private fun note(id: String) = PrenatalNote(id, "Pregunta 😊", "Contenido\ncon : separadores", NoteCategory.DOCTOR_QUESTIONS, NoteTaskStatus.PENDING, "2028-02-29")

    @Test fun crudAndConcurrentAddsPreserveNotes() = runBlocking {
        val repo = SecurePrenatalNoteRepository(Store())
        assertEquals(emptyList(), repo.list())
        (0 until 25).map { async { repo.add(note(it.toString())) } }.awaitAll()
        assertEquals(25, repo.list().size)
        val updated = note("0").copy(status = NoteTaskStatus.COMPLETED, body = null, followUp = null)
        repo.update(updated)
        assertEquals(updated, repo.list().first { it.id == "0" })
        repo.delete("0")
        assertEquals(24, repo.list().size)
    }

    @Test fun invalidOperationsAndFailuresDoNotOverwrite() = runBlocking {
        val store = Store()
        val repo = SecurePrenatalNoteRepository(store)
        repo.add(note("one"))
        val original = store.payload
        assertFailsWith<IllegalArgumentException> { repo.add(note("one")) }
        assertFailsWith<IllegalArgumentException> { repo.update(note("missing")) }
        assertFailsWith<IllegalArgumentException> { repo.delete("missing") }
        for (invalid in listOf(note("two").copy(title = " "), note("two").copy(body = "x".repeat(4097)), note("two").copy(followUp = "2027-02-29"))) {
            assertFailsWith<IllegalArgumentException> { repo.add(invalid) }
        }
        store.failWrite = true
        assertFailsWith<IllegalStateException> { repo.update(note("one").copy(title = "Changed")) }
        assertEquals(original, store.payload)
        store.failRead = true
        assertFailsWith<IllegalStateException> { repo.list() }
        assertFailsWith<IllegalStateException> { repo.add(note("two")) }
        assertEquals(original, store.payload)
    }

    @Test fun corruptAndUnknownPayloadsArePreserved() = runBlocking {
        val encoded = PrenatalNoteCodec.encode(listOf(note("one")))
        for (payload in listOf("broken", "GESTIORA_NOTES:2\n0\n", encoded.dropLast(1), encoded + "extra", encoded.replace("PENDING", "UNKNOWN"))) {
            val store = Store(payload)
            val repo = SecurePrenatalNoteRepository(store)
            assertFailsWith<IllegalStateException> { repo.list() }
            assertFailsWith<IllegalStateException> { repo.add(note("two")) }
            assertEquals(payload, store.payload)
        }
        for (category in NoteCategory.entries) for (status in NoteTaskStatus.entries) {
            val records = listOf(note("one").copy(category = category, status = status))
            assertEquals(records, PrenatalNoteCodec.decode(PrenatalNoteCodec.encode(records)))
        }
    }
}
