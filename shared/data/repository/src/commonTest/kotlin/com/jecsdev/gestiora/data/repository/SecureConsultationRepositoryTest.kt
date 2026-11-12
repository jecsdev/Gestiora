package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureConsultationsStore
import com.jecsdev.gestiora.domain.models.Consultation
import kotlinx.coroutines.*
import kotlin.test.*

class SecureConsultationRepositoryTest {
    private class Store(var payload: String? = null) : SecureConsultationsStore {
        var failRead = false
        var failWrite = false
        override fun read(): String? { check(!failRead); return payload }
        override fun write(payload: String) { check(!failWrite); this.payload = payload }
    }
    private fun record(id: String) = Consultation(id, "2028-02-29", "24 sem 2 d", "Dra. Pérez",
        "Observaciones 😊\ncon : separadores", "Indicaciones", "Medicamento registrado", "Examen registrado", "Seguimiento clínico", "2028-03-01")

    @Test fun crudAndConcurrentAddsSurviveNewRepository() = runBlocking {
        val store = Store()
        val repo = SecureConsultationRepository(store)
        assertEquals(emptyList(), repo.list())
        (0 until 25).map { async { repo.add(record(it.toString())) } }.awaitAll()
        assertEquals(25, repo.list().size)
        assertEquals(repo.list(), SecureConsultationRepository(store).list())
        val changed = record("0").copy(doctor = null, nextDate = null, observations = "Editada")
        repo.update(changed)
        assertEquals(changed, SecureConsultationRepository(store).list().first { it.id == "0" })
        assertEquals("24 sem 2 d", repo.list().first().gestationalAge)
        repo.delete("0")
        assertEquals(24, SecureConsultationRepository(store).list().size)
    }

    @Test fun invalidMutationsAndIoFailuresPreserveData() = runBlocking {
        val store = Store()
        val repo = SecureConsultationRepository(store)
        repo.add(record("one"))
        val original = store.payload
        assertFailsWith<IllegalArgumentException> { repo.add(record("one")) }
        assertFailsWith<IllegalArgumentException> { repo.update(record("missing")) }
        assertFailsWith<IllegalArgumentException> { repo.delete("missing") }
        val invalid = listOf(record(""), record("two").copy(date = "2027-02-29"),
            record("two").copy(nextDate = "2028-13-01"), record("two").copy(doctor = "x".repeat(513)),
            record("two").copy(gestationalAge = "x".repeat(513)), record("two").copy(followUp = "x".repeat(4097)))
        for (item in invalid) assertFailsWith<IllegalArgumentException> { repo.add(item) }
        assertEquals(original, store.payload)
        store.failWrite = true
        assertFailsWith<IllegalStateException> { repo.update(record("one").copy(doctor = "Changed")) }
        assertFailsWith<IllegalStateException> { repo.delete("one") }
        assertEquals(original, store.payload)
        store.failRead = true
        assertFailsWith<IllegalStateException> { repo.list() }
        assertFailsWith<IllegalStateException> { repo.add(record("two")) }
        assertEquals(original, store.payload)
    }

    @Test fun corruptUnsupportedAndDuplicatePayloadsAreNotOverwritten() = runBlocking<Unit> {
        val encoded = ConsultationCodec.encode(listOf(record("one")))
        val payloads = listOf("broken", "GESTIORA_CONSULTATIONS:2\n0\n", encoded.dropLast(1), encoded + "extra",
            encoded.replace("2028-02-29", "2027-02-29"), encoded.replace("\n1\n", "\n2\n") + encoded.substringAfter("\n1\n"),
            "GESTIORA_CONSULTATIONS:1\n1001\n", "GESTIORA_CONSULTATIONS:1\n1\n99999:x")
        for (payload in payloads) {
            val store = Store(payload)
            val repo = SecureConsultationRepository(store)
            assertFailsWith<IllegalStateException> { repo.list() }
            assertFailsWith<IllegalStateException> { repo.add(record("two")) }
            assertFailsWith<IllegalStateException> { repo.update(record("one")) }
            assertFailsWith<IllegalStateException> { repo.delete("one") }
            assertEquals(payload, store.payload)
        }
        val minimal = Consultation("minimal", "0001-01-01")
        assertEquals(listOf(minimal, record("full")), ConsultationCodec.decode(ConsultationCodec.encode(listOf(minimal, record("full")))))
        assertFailsWith<IllegalArgumentException> { ConsultationCodec.encode(List(1001) { Consultation(it.toString(), "2028-02-29") }) }
    }
}
