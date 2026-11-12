package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureExamRecordsStore
import com.jecsdev.gestiora.domain.models.ExamRecord
import com.jecsdev.gestiora.domain.models.ExamStatus
import com.jecsdev.gestiora.domain.models.ExamKind
import kotlinx.coroutines.*
import kotlin.test.*

class SecureExamRecordRepositoryTest {
    private class Store(var payload: String? = null) : SecureExamRecordsStore {
        var failRead = false
        var failWrite = false
        override fun read(): String? { check(!failRead); return payload }
        override fun write(payload: String) { check(!failWrite); this.payload = payload }
    }
    private fun record(id: String) = ExamRecord(id, "Estudio registrado", ExamKind.BLOOD_TEST, "2028-02-28", "2028-02-29", "2028-03-01", ExamStatus.COMPLETED,
        "Profesional", "Resultados registrados 😊", "Nota\ncon : separadores", "Informe físico")

    @Test fun crudConcurrentAddsAndReloadPreserveHistoricalDates() = runBlocking<Unit> {
        val store = Store()
        val repo = SecureExamRecordRepository(store)
        assertEquals(emptyList(), repo.list())
        (0 until 25).map { async { repo.add(record(it.toString())) } }.awaitAll()
        assertEquals(25, repo.list().size)
        assertEquals(repo.list(), SecureExamRecordRepository(store).list())
        val changed = record("0").copy(status = ExamStatus.SCHEDULED, physician = null, notes = null)
        repo.update(changed)
        assertEquals(changed, SecureExamRecordRepository(store).list().first { it.id == "0" })
        assertEquals("2028-03-01", repo.list().first().completed)
        repo.delete("0")
        assertEquals(24, SecureExamRecordRepository(store).list().size)
    }

    @Test fun statusesNullsUnicodeAndLimitsRoundTrip() {
        val records = ExamKind.entries.flatMap { kind -> ExamStatus.entries.map { status -> record(kind.name + status.name).copy(kind = kind, status = status) } } +
            ExamRecord("minimal", "Nombre") +
            record("limits").copy(name = "n".repeat(512), physician = "p".repeat(512), attachment = "r".repeat(512), requested = "0001-01-01", scheduled = "0001-01-01", completed = "9999-12-31", results = "x".repeat(4096), notes = "x".repeat(4096))
        assertEquals(records, ExamRecordCodec.decode(ExamRecordCodec.encode(records)))
        assertEquals(emptyList(), ExamRecordCodec.decode(ExamRecordCodec.encode(emptyList())))
    }

    @Test fun invalidFieldsAndIoFailuresNeverOverwrite() = runBlocking<Unit> {
        val store = Store()
        val repo = SecureExamRecordRepository(store)
        repo.add(record("one"))
        val original = store.payload
        assertFailsWith<IllegalArgumentException> { repo.add(record("one")) }
        assertFailsWith<IllegalArgumentException> { repo.update(record("missing")) }
        assertFailsWith<IllegalArgumentException> { repo.delete("missing") }
        val invalid = listOf(record(""), record("x".repeat(129)), record("two").copy(name = " "),
            record("two").copy(name = "x".repeat(513)), record("two").copy(physician = "x".repeat(513)),
            record("two").copy(attachment = "x".repeat(513)), record("two").copy(results = "x".repeat(4097)),
            record("two").copy(notes = "x".repeat(4097)), record("two").copy(requested = "2027-02-29"),
            record("two").copy(requested = ""), record("two").copy(scheduled = "2027-02-29"), record("two").copy(scheduled = ""),
            record("two").copy(completed = "2028-13-01"), record("two").copy(completed = ""), record("two").copy(completed = null))
        for (item in invalid) {
            assertFailsWith<IllegalArgumentException> { repo.add(item) }
            assertEquals(original, store.payload)
        }
        store.failWrite = true
        assertFailsWith<IllegalStateException> { repo.update(record("one").copy(name = "Changed")) }
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
        val encoded = ExamRecordCodec.encode(listOf(record("one")))
        val payloads = listOf("broken", "GESTIORA_EXAMS:2\n0\n", encoded.dropLast(1), encoded + "extra",
            encoded.replace("9:COMPLETED", "7:UNKNOWN"), encoded.replace("10:BLOOD_TEST", "7:UNKNOWN"), encoded.replace("2028-03-01", "2027-02-29"),
            encoded.replace("\n1\n", "\n2\n") + encoded.substringAfter("\n1\n"),
            "GESTIORA_EXAMS:1\n1001\n", "GESTIORA_EXAMS:1\n1\n99999:x", "x".repeat(1_000_001))
        for (payload in payloads) {
            val store = Store(payload)
            val repo = SecureExamRecordRepository(store)
            assertFailsWith<IllegalStateException> { repo.list() }
            assertFailsWith<IllegalStateException> { repo.add(record("two")) }
            assertFailsWith<IllegalStateException> { repo.update(record("one")) }
            assertFailsWith<IllegalStateException> { repo.delete("one") }
            assertEquals(payload, store.payload)
        }
        assertFailsWith<IllegalArgumentException> { ExamRecordCodec.encode(List(1001) { record(it.toString()) }) }
        assertFailsWith<IllegalArgumentException> { ExamRecordCodec.encode(List(250) { record(it.toString()).copy(notes = "x".repeat(4096)) }) }
    }
}
