package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureMedicalConditionsStore
import com.jecsdev.gestiora.domain.models.ConditionStatus
import com.jecsdev.gestiora.domain.models.MedicalCondition
import kotlinx.coroutines.*
import kotlin.test.*

class SecureMedicalConditionRepositoryTest {
    private class Store(var payload: String? = null) : SecureMedicalConditionsStore {
        var failRead = false
        var failWrite = false
        override fun read(): String? { check(!failRead); return payload }
        override fun write(payload: String) { check(!failWrite); this.payload = payload }
    }
    private fun record(id: String) = MedicalCondition(id, "Condición registrada", "2028-02-29", "Dra. Pérez",
        "Indicaciones 😊\ncon : separadores", "Tratamiento registrado", "Control registrado", "2028-03-01", ConditionStatus.FOLLOW_UP, "Historia")

    @Test fun crudAndConcurrentAddsSurviveReload() = runBlocking<Unit> {
        val store = Store()
        val repo = SecureMedicalConditionRepository(store)
        assertEquals(emptyList(), repo.list())
        (0 until 25).map { async { repo.add(record(it.toString())) } }.awaitAll()
        assertEquals(25, repo.list().size)
        assertEquals(repo.list(), SecureMedicalConditionRepository(store).list())
        val changed = record("0").copy(status = ConditionStatus.RESOLVED, followUp = null, treatment = null)
        repo.update(changed)
        assertEquals(changed, SecureMedicalConditionRepository(store).list().first { it.id == "0" })
        repo.delete("0")
        assertEquals(24, SecureMedicalConditionRepository(store).list().size)
    }

    @Test fun statusesAndOptionalFieldsRoundTrip() {
        val records = ConditionStatus.entries.map { record(it.name).copy(status = it) } +
            MedicalCondition("minimal", "Nombre", "0001-01-01", "Profesional") +
            record("limits").copy(name = "n".repeat(512), professional = "p".repeat(512), date = "9999-12-31", notes = "x".repeat(4096))
        assertEquals(records, MedicalConditionCodec.decode(MedicalConditionCodec.encode(records)))
        assertEquals(emptyList(), MedicalConditionCodec.decode(MedicalConditionCodec.encode(emptyList())))
    }

    @Test fun invalidMutationsAndIoFailuresDoNotOverwrite() = runBlocking<Unit> {
        val store = Store()
        val repo = SecureMedicalConditionRepository(store)
        repo.add(record("one"))
        val original = store.payload
        assertFailsWith<IllegalArgumentException> { repo.add(record("one")) }
        assertFailsWith<IllegalArgumentException> { repo.update(record("missing")) }
        assertFailsWith<IllegalArgumentException> { repo.delete("missing") }
        val invalid = listOf(record(""), record("x".repeat(129)), record("two").copy(name = " "),
            record("two").copy(professional = "\n"), record("two").copy(name = "x".repeat(513)),
            record("two").copy(professional = "x".repeat(513)), record("two").copy(date = "2027-02-29"),
            record("two").copy(followUp = "2028-13-01"), record("two").copy(followUp = ""),
            record("two").copy(instructions = "x".repeat(4097)), record("two").copy(treatment = "x".repeat(4097)),
            record("two").copy(monitoring = "x".repeat(4097)), record("two").copy(notes = "x".repeat(4097)))
        for (item in invalid) {
            assertFailsWith<IllegalArgumentException> { repo.add(item) }
            assertEquals(original, store.payload)
        }
        store.failWrite = true
        assertFailsWith<IllegalStateException> { repo.update(record("one").copy(status = ConditionStatus.RESOLVED)) }
        assertFailsWith<IllegalStateException> { repo.delete("one") }
        assertEquals(original, store.payload)
        store.failRead = true
        assertFailsWith<IllegalStateException> { repo.list() }
        assertFailsWith<IllegalStateException> { repo.add(record("two")) }
        assertFailsWith<IllegalStateException> { repo.update(record("one")) }
        assertFailsWith<IllegalStateException> { repo.delete("one") }
        assertEquals(original, store.payload)
    }

    @Test fun malformedAndUnsupportedPayloadsArePreserved() = runBlocking<Unit> {
        val encoded = MedicalConditionCodec.encode(listOf(record("one")))
        val payloads = listOf("broken", "GESTIORA_CONDITIONS:2\n0\n", encoded.dropLast(1), encoded + "extra",
            encoded.replace("9:FOLLOW_UP", "7:UNKNOWN"), encoded.replace("2028-02-29", "2027-02-29"),
            encoded.replace("\n1\n", "\n2\n") + encoded.substringAfter("\n1\n"),
            "GESTIORA_CONDITIONS:1\n1001\n", "GESTIORA_CONDITIONS:1\n1\n99999:x", "x".repeat(1_000_001))
        for (payload in payloads) {
            val store = Store(payload)
            val repo = SecureMedicalConditionRepository(store)
            assertFailsWith<IllegalStateException> { repo.list() }
            assertFailsWith<IllegalStateException> { repo.add(record("two")) }
            assertFailsWith<IllegalStateException> { repo.update(record("one")) }
            assertFailsWith<IllegalStateException> { repo.delete("one") }
            assertEquals(payload, store.payload)
        }
        assertFailsWith<IllegalArgumentException> { MedicalConditionCodec.encode(List(1001) { record(it.toString()) }) }
        assertFailsWith<IllegalArgumentException> { MedicalConditionCodec.encode(List(250) { record(it.toString()).copy(notes = "x".repeat(4096)) }) }
    }
}
