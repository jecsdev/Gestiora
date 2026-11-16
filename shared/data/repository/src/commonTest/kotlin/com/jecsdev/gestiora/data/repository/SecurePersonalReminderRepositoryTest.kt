package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecurePersonalRemindersStore
import com.jecsdev.gestiora.domain.models.PersonalReminder
import com.jecsdev.gestiora.domain.models.ReminderKind
import kotlinx.coroutines.*
import kotlin.test.*

class SecurePersonalReminderRepositoryTest {
    private class Store(var payload: String? = null) : SecurePersonalRemindersStore {
        var failRead = false
        var failWrite = false
        override fun read(): String? { check(!failRead); return payload }
        override fun write(payload: String) { check(!failWrite); this.payload = payload }
    }
    private fun record(id: String) = PersonalReminder(id, "Pendiente 😊\ncon :", "2028-02-29", "09:30", ReminderKind.FOLLOW_UP)

    @Test fun crudConcurrentWritesAndReloadPreserveEveryKindAndAllDayEntries() = runBlocking {
        val store = Store()
        val repo = SecurePersonalReminderRepository(store)
        assertEquals(emptyList(), repo.list())
        (0 until 25).map { async { repo.add(record(it.toString())) } }.awaitAll()
        ReminderKind.entries.forEach { repo.add(record(it.name).copy(kind = it, time = null, done = true)) }
        assertEquals(30, repo.list().size)
        assertEquals(repo.list(), SecurePersonalReminderRepository(store).list())
        val changed = record("0").copy(time = null, done = true)
        repo.update(changed)
        assertEquals(changed, SecurePersonalReminderRepository(store).list().first())
        repo.delete("0")
        assertEquals(29, repo.list().size)
    }

    @Test fun validationAndIoFailureNeverOverwriteExistingPayload() = runBlocking {
        val store = Store()
        val repo = SecurePersonalReminderRepository(store)
        repo.add(record("one"))
        val original = store.payload
        val invalid = listOf(record(""), record("x").copy(title = " "), record("x").copy(title = "x".repeat(513)),
            record("x").copy(date = "2027-02-29"), record("x").copy(time = "24:00"), record("x").copy(time = "1e:00"), record("x").copy(time = ""))
        invalid.forEach { assertFailsWith<IllegalArgumentException> { repo.add(it) } }
        assertFailsWith<IllegalArgumentException> { repo.add(record("one")) }
        assertFailsWith<IllegalArgumentException> { repo.update(record("missing")) }
        assertFailsWith<IllegalArgumentException> { repo.delete("missing") }
        store.failWrite = true
        assertFailsWith<IllegalStateException> { repo.update(record("one").copy(done = true)) }
        store.failRead = true
        assertFailsWith<IllegalStateException> { repo.add(record("two")) }
        assertEquals(original, store.payload)
    }

    @Test fun corruptUnknownTruncatedAndOversizedPayloadsRemainUntouched() = runBlocking<Unit> {
        val encoded = PersonalReminderCodec.encode(listOf(record("one")))
        val invalid = listOf("broken", "GESTIORA_REMINDERS:2\n0\n", encoded.dropLast(1), encoded + "extra",
            encoded.replace("FOLLOW_UP", "FOLLOW_XX"), encoded.replace("false", "maybe"),
            encoded.replace("2028-02-29", "2027-02-29"), "GESTIORA_REMINDERS:1\n1001\n",
            "GESTIORA_REMINDERS:1\n1\n99999:x", "x".repeat(1_000_001),
            encoded.replace("\n1\n", "\n2\n") + encoded.substringAfter("\n1\n"))
        invalid.forEach { payload ->
            val store = Store(payload)
            val repo = SecurePersonalReminderRepository(store)
            assertFailsWith<IllegalStateException> { repo.list() }
            assertFailsWith<IllegalStateException> { repo.add(record("two")) }
            assertFailsWith<IllegalStateException> { repo.update(record("one")) }
            assertFailsWith<IllegalStateException> { repo.delete("one") }
            assertEquals(payload, store.payload)
        }
        assertFailsWith<IllegalArgumentException> { PersonalReminderCodec.encode(List(1001) { record(it.toString()) }) }
    }
}
