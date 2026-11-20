package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureVitalRecordsStore
import com.jecsdev.gestiora.domain.models.VitalRecord
import com.jecsdev.gestiora.domain.models.VitalType
import kotlinx.coroutines.*
import kotlin.test.*

class SecureVitalRecordRepositoryTest {
    private class Store(var payload: String? = null) : SecureVitalRecordsStore {
        var failRead = false
        var failWrite = false
        override fun read(): String? { check(!failRead); return payload }
        override fun write(payload: String) { check(!failWrite); this.payload = payload }
    }
    private fun record(id: String) = VitalRecord(id, "2028-02-29", VitalType.WEIGHT, 62.75, notes = "Nota 😊\ncon : separadores")

    @Test fun crudAndConcurrentWritesSurviveReload() = runBlocking<Unit> {
        val store = Store()
        val repo = SecureVitalRecordRepository(store)
        assertEquals(emptyList(), repo.list())
        (0 until 25).map { async { repo.add(record(it.toString())) } }.awaitAll()
        assertEquals(25, repo.list().size)
        assertEquals(repo.list(), SecureVitalRecordRepository(store).list())
        val changed = record("0").copy(type = VitalType.SYMPTOM, value = null, text = "Descripción registrada", notes = null)
        repo.update(changed)
        assertEquals(changed, SecureVitalRecordRepository(store).list().first { it.id == "0" })
        repo.delete("0")
        assertEquals(24, SecureVitalRecordRepository(store).list().size)
    }

    @Test fun allTypesAndBoundaryValuesRoundTrip() {
        val records = VitalType.entries.map { type ->
            VitalRecord(type.name, "0001-01-01", type,
                value = if (type.isNumeric) 0.0 else null,
                value2 = if (type == VitalType.BLOOD_PRESSURE) 20.0 else null,
                label = if (type == VitalType.CUSTOM) "Medida personalizada" else null,
                text = if (!type.isNumeric) "Descripción\ncon 😊" else null)
        } + listOf(record("decimal"), VitalRecord("upper", "9999-12-31", VitalType.BLOOD_PRESSURE, 400.0, 200.0))
        assertEquals(records, VitalRecordCodec.decode(VitalRecordCodec.encode(records)))
        assertEquals(emptyList(), VitalRecordCodec.decode(VitalRecordCodec.encode(emptyList())))
    }

    @Test fun invalidFieldsAndIoFailuresNeverOverwrite() = runBlocking<Unit> {
        val store = Store()
        val repo = SecureVitalRecordRepository(store)
        repo.add(record("one"))
        val original = store.payload
        assertFailsWith<IllegalArgumentException> { repo.add(record("one")) }
        assertFailsWith<IllegalArgumentException> { repo.update(record("missing")) }
        assertFailsWith<IllegalArgumentException> { repo.delete("missing") }
        val invalid = listOf(record(""), record("x".repeat(129)), record("two").copy(date = "2027-02-29"),
            record("two").copy(value = null), record("two").copy(value = -0.1), record("two").copy(value = 400.1),
            record("two").copy(value = Double.NaN), record("two").copy(value = Double.POSITIVE_INFINITY), record("two").copy(value = Double.NEGATIVE_INFINITY),
            record("two").copy(value2 = 50.0), record("two").copy(text = "Hidden"), record("two").copy(label = "Hidden"),
            record("two").copy(notes = "x".repeat(4097)),
            record("two").copy(type = VitalType.BLOOD_PRESSURE),
            record("two").copy(type = VitalType.BLOOD_PRESSURE, value2 = 19.9),
            record("two").copy(type = VitalType.BLOOD_PRESSURE, value2 = 200.1),
            record("two").copy(type = VitalType.BLOOD_PRESSURE, value2 = Double.NaN),
            record("two").copy(type = VitalType.BLOOD_PRESSURE, value2 = Double.POSITIVE_INFINITY),
            record("two").copy(type = VitalType.CUSTOM), record("two").copy(type = VitalType.CUSTOM, label = " "),
            record("two").copy(type = VitalType.CUSTOM, label = "x".repeat(513)),
            record("two").copy(type = VitalType.SYMPTOM, value = null),
            record("two").copy(type = VitalType.ULTRASOUND, value = null, text = " "),
            record("two").copy(type = VitalType.SYMPTOM, value = null, text = "x".repeat(4097)),
            record("two").copy(type = VitalType.SYMPTOM, text = "Visible text"))
        for (item in invalid) {
            assertFailsWith<IllegalArgumentException> { repo.add(item) }
            assertEquals(original, store.payload)
        }
        store.failWrite = true
        assertFailsWith<IllegalStateException> { repo.update(record("one").copy(value = 63.0)) }
        assertFailsWith<IllegalStateException> { repo.delete("one") }
        assertEquals(original, store.payload)
        store.failRead = true
        assertFailsWith<IllegalStateException> { repo.list() }
        assertFailsWith<IllegalStateException> { repo.add(record("two")) }
        assertEquals(original, store.payload)
    }

    @Test fun malformedOrUnsupportedPayloadsArePreserved() = runBlocking<Unit> {
        val encoded = VitalRecordCodec.encode(listOf(record("one")))
        val payloads = listOf("broken", "GESTIORA_VITALS:2\n0\n", encoded.dropLast(1), encoded + "extra",
            encoded.replace("6:WEIGHT", "7:UNKNOWN"), encoded.replace("2028-02-29", "2027-02-29"),
            encoded.replace("5:62.75", "3:NaN"), encoded.replace("5:62.75", "8:Infinity"),
            encoded.replace("5:62.75", "3:bad"), encoded.replace("\n1\n", "\n2\n") + encoded.substringAfter("\n1\n"),
            "GESTIORA_VITALS:1\n1001\n", "GESTIORA_VITALS:1\n1\n99999:x")
        for (payload in payloads) {
            val store = Store(payload)
            val repo = SecureVitalRecordRepository(store)
            assertFailsWith<IllegalStateException> { repo.list() }
            assertFailsWith<IllegalStateException> { repo.add(record("two")) }
            assertFailsWith<IllegalStateException> { repo.update(record("one")) }
            assertFailsWith<IllegalStateException> { repo.delete("one") }
            assertEquals(payload, store.payload)
        }
        assertFailsWith<IllegalArgumentException> { VitalRecordCodec.encode(List(1001) { record(it.toString()) }) }
        assertFailsWith<IllegalArgumentException> { VitalRecordCodec.encode(List(250) { record(it.toString()).copy(notes = "x".repeat(4096)) }) }
    }
}
