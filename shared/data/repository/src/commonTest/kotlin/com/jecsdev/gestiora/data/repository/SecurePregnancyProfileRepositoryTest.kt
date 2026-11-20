package com.jecsdev.gestiora.data.repository

import com.jecsdev.gestiora.data.datasources.SecureProfileStore
import com.jecsdev.gestiora.domain.models.DatingMethod
import com.jecsdev.gestiora.domain.models.PregnancyDating
import com.jecsdev.gestiora.domain.models.PregnancyProfile
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SecurePregnancyProfileRepositoryTest {
    private val profile = PregnancyProfile(
        motherName = "Lucía 😊\nR.",
        babyName = null,
        country = "Bolivia",
        language = "Español",
        dating = PregnancyDating(DatingMethod.ULTRASOUND, lmp = "2026-01-01", referenceDate = "2026-03-01", referenceWeeks = 10, referenceDays = 2),
    )

    @Test fun absentProfileAndUnicodeRoundTrip() = runBlocking {
        val store = FakeStore()
        val repository = SecurePregnancyProfileRepository(store)
        assertNull(repository.load())
        repository.save(profile)
        assertEquals(profile, repository.load())
        repository.clear()
        assertNull(repository.load())
    }

    @Test fun unsupportedVersionAndCorruptPayloadAreExplicit() {
        assertFailsWith<UnsupportedProfileVersionException> {
            ProfileCodec.decode(ProfileCodec.encode(profile).replaceFirst("GESTIORA_PROFILE:1", "GESTIORA_PROFILE:2"))
        }
        assertFailsWith<CorruptProfileException> { ProfileCodec.decode("GESTIORA_PROFILE:1\n99:short") }
        assertFailsWith<CorruptProfileException> { ProfileCodec.decode(ProfileCodec.encode(profile) + "unexpected") }
    }

    @Test fun corruptStoredProfileCannotBeOverwritten() = runBlocking {
        val store = FakeStore("GESTIORA_PROFILE:1\n99:broken")
        val repository = SecurePregnancyProfileRepository(store)
        assertFailsWith<CorruptProfileException> { repository.load() }
        assertFailsWith<CorruptProfileException> { repository.save(profile) }
        assertEquals(0, store.writes)
    }

    @Test fun readAndWriteFailuresPropagateWithoutTreatingDataAsAbsent() = runBlocking {
        val store = FakeStore()
        val repository = SecurePregnancyProfileRepository(store)
        store.failRead = true
        assertFailsWith<IllegalStateException> { repository.load() }
        assertFailsWith<IllegalStateException> { repository.save(profile) }
        assertEquals(0, store.writes)
        store.failRead = false
        store.failWrite = true
        assertFailsWith<IllegalStateException> { repository.save(profile) }
        assertNull(repository.load())
    }

    private class FakeStore(var payload: String? = null) : SecureProfileStore {
        var writes = 0
        var failRead = false
        var failWrite = false

        override fun read(): String? {
            if (failRead) throw IllegalStateException("read failed")
            return payload
        }

        override fun write(payload: String) {
            if (failWrite) throw IllegalStateException("write failed")
            writes++
            this.payload = payload
        }

        override fun clear() { payload = null }
    }
}
