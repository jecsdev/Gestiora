package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureVaccineRecordsStore
import com.jecsdev.gestiora.domain.models.VaccineRecord
import com.jecsdev.gestiora.domain.models.VaccineStatus
import kotlinx.coroutines.*
import kotlin.test.*

class SecureVaccineRecordRepositoryTest {
    private class Store(var payload: String? = null) : SecureVaccineRecordsStore {
        var failRead = false
        var failWrite = false
        override fun read(): String? { check(!failRead); return payload }
        override fun write(payload: String) { check(!failWrite); this.payload = payload }
    }
    private fun record(id: String) = VaccineRecord(id, "Vacuna registrada", "Dosis indicada", "2028-02-29", "2028-03-01", VaccineStatus.ADMINISTERED,
        "Profesional", "Lugar", "Carnet físico 😊", "Nota\ncon : separadores")

    @Test fun crudConcurrentAddsAndReloadPreserveHistoricalDates() = runBlocking<Unit> {
        val store = Store()
        val repo = SecureVaccineRecordRepository(store)
        assertEquals(emptyList(), repo.list())
        (0 until 25).map { async { repo.add(record(it.toString())) } }.awaitAll()
        assertEquals(25, repo.list().size)
        assertEquals(repo.list(), SecureVaccineRecordRepository(store).list())
        val changed = record("0").copy(status = VaccineStatus.POSTPONED, provider = null, notes = null)
        repo.update(changed)
        assertEquals(changed, SecureVaccineRecordRepository(store).list().first { it.id == "0" })
        assertEquals("2028-03-01", repo.list().first().administered)
        repo.delete("0")
        assertEquals(24, SecureVaccineRecordRepository(store).list().size)
    }

    @Test fun statusesNullsUnicodeAndLimitsRoundTrip() {
        val records = VaccineStatus.entries.map { record(it.name).copy(status = it) } +
            VaccineRecord("minimal", "Nombre") +
            record("limits").copy(name = "n".repeat(512), dose = "d".repeat(512), provider = "p".repeat(512), location = "l".repeat(512), proof = "r".repeat(512), scheduled = "0001-01-01", administered = "9999-12-31", notes = "x".repeat(4096))
        assertEquals(records, VaccineRecordCodec.decode(VaccineRecordCodec.encode(records)))
        assertEquals(emptyList(), VaccineRecordCodec.decode(VaccineRecordCodec.encode(emptyList())))
    }

    @Test fun invalidFieldsAndIoFailuresNeverOverwrite() = runBlocking<Unit> {
        val store = Store()
        val repo = SecureVaccineRecordRepository(store)
        repo.add(record("one"))
        val original = store.payload
        assertFailsWith<IllegalArgumentException> { repo.add(record("one")) }
        assertFailsWith<IllegalArgumentException> { repo.update(record("missing")) }
        assertFailsWith<IllegalArgumentException> { repo.delete("missing") }
        val invalid = listOf(record(""), record("x".repeat(129)), record("two").copy(name = " "),
            record("two").copy(name = "x".repeat(513)), record("two").copy(dose = "x".repeat(513)),
            record("two").copy(provider = "x".repeat(513)), record("two").copy(location = "x".repeat(513)),
            record("two").copy(proof = "x".repeat(513)), record("two").copy(notes = "x".repeat(4097)),
            record("two").copy(scheduled = "2027-02-29"), record("two").copy(scheduled = ""),
            record("two").copy(administered = "2028-13-01"), record("two").copy(administered = ""),
            record("two").copy(administered = null))
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
        val encoded = VaccineRecordCodec.encode(listOf(record("one")))
        val payloads = listOf("broken", "GESTIORA_VACCINES:2\n0\n", encoded.dropLast(1), encoded + "extra",
            encoded.replace("12:ADMINISTERED", "7:UNKNOWN"), encoded.replace("2028-03-01", "2027-02-29"),
            encoded.replace("\n1\n", "\n2\n") + encoded.substringAfter("\n1\n"),
            "GESTIORA_VACCINES:1\n1001\n", "GESTIORA_VACCINES:1\n1\n99999:x", "x".repeat(1_000_001))
        for (payload in payloads) {
            val store = Store(payload)
            val repo = SecureVaccineRecordRepository(store)
            assertFailsWith<IllegalStateException> { repo.list() }
            assertFailsWith<IllegalStateException> { repo.add(record("two")) }
            assertFailsWith<IllegalStateException> { repo.update(record("one")) }
            assertFailsWith<IllegalStateException> { repo.delete("one") }
            assertEquals(payload, store.payload)
        }
        assertFailsWith<IllegalArgumentException> { VaccineRecordCodec.encode(List(1001) { record(it.toString()) }) }
        assertFailsWith<IllegalArgumentException> { VaccineRecordCodec.encode(List(250) { record(it.toString()).copy(notes = "x".repeat(4096)) }) }
    }
}
