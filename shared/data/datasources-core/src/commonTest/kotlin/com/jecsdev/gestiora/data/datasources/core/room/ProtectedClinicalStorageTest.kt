package com.jecsdev.gestiora.data.datasources.core.room

import com.jecsdev.gestiora.data.datasources.LegacyClinicalPayloadValidator
import com.jecsdev.gestiora.data.datasources.SecurePersonalRemindersStore
import com.jecsdev.gestiora.data.datasources.SecureProfileStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class ProtectedClinicalStorageTest {
    @Test fun clinicalBackupRestoresAllStoresAndRefusesAnExistingInstallation() = runBlocking {
        val source = Fixture()
        val original = ProtectedClinicalStorage(
            source.dao, source.cipher, source.profile, source.reminders, validator,
            ProtectedClinicalStorage.COLLECTIONS.associateWith { store -> { "$store:example" } },
        )
        val snapshot = original.backupSnapshot()
        assertEquals(9, snapshot.payloads.size)

        val target = Fixture()
        target.profile.raw = null
        target.reminders.raw = null
        val restored = ProtectedClinicalStorage(
            target.dao, target.cipher, target.profile, target.reminders, validator,
            ProtectedClinicalStorage.COLLECTIONS.associateWith { { null } },
        )
        assertTrue(restored.canRestoreBackup())
        assertFailsWith<IllegalArgumentException> {
            restored.restoreBackup(ClinicalBackup(snapshot.payloads + ("appointments" to "invalid")))
        }
        assertTrue(restored.canRestoreBackup())
        assertTrue(restored.restoreBackup(snapshot))
        assertEquals(snapshot.payloads, restored.backupSnapshot().payloads)
        assertFalse(restored.restoreBackup(snapshot))
        assertFalse(original.restoreBackup(snapshot))
    }

    @Test fun verifiedCleanupRetriesWithoutReimportAndDoesNotResurrectClearedProfile() = runBlocking {
        val f = Fixture()
        var cleanups = 0
        val sources = ProtectedClinicalStorage.COLLECTIONS.associateWith { store -> { "$store:original" } }
        fun open() = ProtectedClinicalStorage(f.dao, f.cipher, f.profile, f.reminders, validator, sources) {
            cleanups++
            assertTrue(ProtectedClinicalStorage.IMPORT_ID in f.dao.markers)
            assertTrue(ProtectedClinicalStorage.COLLECTIONS_IMPORT_ID in f.dao.markers)
            if (cleanups == 1) error("Interrupted filesystem cleanup")
        }
        assertFailsWith<IllegalStateException> { open().finishMigration() }
        assertEquals(9, f.dao.rows.size)
        open().write("appointments", "appointments:edited")
        open().clearProfile()
        f.profile.failRead = true
        open().finishMigration()
        assertEquals(2, cleanups)
        assertNull(open().read(ProtectedClinicalStorage.PROFILE))
        assertEquals("appointments:edited", open().read("appointments"))
    }

    @Test fun cleanupRefusedIfAnyDestinationFailsAuthenticationOrValidation() = runBlocking {
        val f = Fixture()
        var cleaned = false
        val sources = ProtectedClinicalStorage.COLLECTIONS.associateWith { store -> { "$store:original" } }
        val storage = ProtectedClinicalStorage(f.dao, f.cipher, f.profile, f.reminders, validator, sources) { cleaned = true }
        storage.read("appointments")
        f.dao.rows["vital_records"] = ProtectedPayload("vital_records", "tampered".encodeToByteArray())
        assertFailsWith<IllegalStateException> { storage.finishMigration() }
        assertFalse(cleaned)
        f.dao.rows["vital_records"] = ProtectedPayload("vital_records", "vital_records|invalid".encodeToByteArray())
        assertFailsWith<IllegalArgumentException> { storage.finishMigration() }
        assertFalse(cleaned)
    }

    @Test fun cleanupRefusedWhenSecondImportGroupCannotComplete() = runBlocking {
        val f = Fixture()
        var cleaned = false
        val sources = ProtectedClinicalStorage.COLLECTIONS.associateWith { store -> { if (store == "exam_records") "broken" else "$store:original" } }
        val storage = ProtectedClinicalStorage(f.dao, f.cipher, f.profile, f.reminders, validator, sources) { cleaned = true }
        assertFailsWith<IllegalArgumentException> { storage.finishMigration() }
        assertFalse(cleaned)
        assertFalse(f.dao.imported(ProtectedClinicalStorage.COLLECTIONS_IMPORT_ID))
        assertEquals(2, f.dao.rows.size)
    }
    private class Profile(var raw: String? = "profile:Lucía") : SecureProfileStore {
        var reads = 0
        var failRead = false
        override fun read(): String? { reads++; check(!failRead); return raw }
        override fun write(payload: String) { error("Legacy must stay untouched") }
        override fun clear() { error("Legacy must stay untouched") }
    }
    private class Reminders(var raw: String? = "reminders:one") : SecurePersonalRemindersStore {
        override fun read() = raw
        override fun write(payload: String) { error("Legacy must stay untouched") }
    }
    private val validator = object : LegacyClinicalPayloadValidator {
        override fun validateCollection(store: String, payload: String) { require(payload.startsWith("$store:")) }
        override fun validateProfile(payload: String) { require(payload.startsWith("profile:")) }
        override fun validatePersonalReminders(payload: String) { require(payload.startsWith("reminders:")) }
        override fun attachmentOwnerIds(store: String, payload: String) =
            payload.substringAfter(':').split(',').filter { it.isNotEmpty() }.toSet()
    }
    // Test double only; actual AES/Keystore and SQLite transaction tests live in androidDeviceTest.
    private class Cipher : ClinicalPayloadCipher {
        var failEncrypt = false
        override fun encryptBytes(store: String, payload: ByteArray): ByteArray {
            check(!failEncrypt)
            return "$store|".encodeToByteArray() + payload
        }
        override fun decryptBytes(store: String, ciphertext: ByteArray): ByteArray {
            val prefix = "$store|".encodeToByteArray()
            check(ciphertext.size >= prefix.size && prefix.indices.all { ciphertext[it] == prefix[it] })
            return ciphertext.copyOfRange(prefix.size, ciphertext.size)
        }
    }
    private class Dao : ProtectedPayloadDao() {
        val rows = mutableMapOf<String, ProtectedPayload>()
        var marker = false
        val markers = mutableSetOf<String>()
        var failImport = false
        var failWrite = false
        var imports = 0
        override suspend fun read(store: String) = rows[store]
        override suspend fun count() = rows.size
        override suspend fun imported(id: String) = id in markers
        override suspend fun insert(payloads: List<ProtectedPayload>) { payloads.forEach { check(!rows.containsKey(it.store)); rows[it.store] = it } }
        override suspend fun markImported(marker: LegacyImport) { markers.add(marker.id); this.marker = true }
        override suspend fun write(payload: ProtectedPayload) { check(!failWrite); rows[payload.store] = payload }
        override suspend fun delete(store: String) { rows.remove(store) }
        override suspend fun attachmentSizes() = rows.values.filter { it.store.startsWith("attachment:") }
            .map { AttachmentRowSize(it.store, it.ciphertext.size) }
        override suspend fun deleteAttachments(stores: List<String>) { stores.forEach { rows.remove(it) } }
        override suspend fun importOnce(id: String, payloads: List<ProtectedPayload>) {
            check(!failImport)
            if (marker) return
            super.importOnce(id, payloads)
            imports++
        }
    }
    private class Fixture {
        val profile = Profile()
        val reminders = Reminders()
        val dao = Dao()
        val cipher = Cipher()
    }
    private fun Fixture.storage() = ProtectedClinicalStorage(dao, cipher, profile, reminders, validator)

    @Test fun attachmentsRestoreOnlyWithAnOwnerAndFollowRecordDeletion() = runBlocking {
        val emptyCollections = ProtectedClinicalStorage.COLLECTIONS.associateWith { { null as String? } }
        fun Fixture.open() = ProtectedClinicalStorage(dao, cipher, profile, reminders, validator, emptyCollections)
        val source = Fixture().open()
        val target = Fixture().apply { profile.raw = null; reminders.raw = null }.open()
        val kind = AttachmentCollection.Exam
        val file = ClinicalAttachment("écographie.pdf", "%PDF-example".encodeToByteArray())
        source.write(kind.store, "exam_records:exam-1")
        assertFailsWith<IllegalArgumentException> { source.writeAttachment(kind, "missing", file) }
        source.writeAttachment(kind, "exam-1", file)
        val backup = source.backupSnapshot()

        val orphaned = ClinicalBackup(backup.payloads - kind.store, backup.attachments)
        assertFailsWith<IllegalArgumentException> { target.restoreBackup(orphaned) }
        assertTrue(target.canRestoreBackup())
        assertTrue(target.restoreBackup(backup))
        val restored = assertNotNull(target.readAttachment(kind, "exam-1"))
        assertEquals(file.name, restored.name)
        assertContentEquals(file.bytes, restored.bytes)
        target.write(kind.store, "exam_records:")
        assertNull(target.readAttachment(kind, "exam-1"))
        assertTrue(target.backupSnapshot().attachments.isEmpty())
    }

    @Test fun secondGroupPreservesFirstGroupAndNeverResurrectsLegacyCollections() = runBlocking {
        val f = Fixture()
        val first = f.storage()
        first.write(ProtectedClinicalStorage.REMINDERS, "reminders:edited")
        first.clearProfile()
        var reads = 0
        val sources = ProtectedClinicalStorage.COLLECTIONS.associateWith { store ->
            { reads++; "$store:Lucía 😊\n2028-02-29" }
        }
        fun open() = ProtectedClinicalStorage(f.dao, f.cipher, f.profile, f.reminders, validator, sources)
        val storage = open()
        ProtectedClinicalStorage.COLLECTIONS.forEach { store ->
            assertEquals("$store:Lucía 😊\n2028-02-29", storage.read(store))
            storage.write(store, "$store:empty")
        }
        f.profile.failRead = true
        val reopened = open()
        assertNull(reopened.read(ProtectedClinicalStorage.PROFILE))
        assertEquals("reminders:edited", reopened.read(ProtectedClinicalStorage.REMINDERS))
        ProtectedClinicalStorage.COLLECTIONS.forEach { assertEquals("$it:empty", reopened.read(it)) }
        assertEquals(7, reads)
        assertEquals(
            setOf(ProtectedClinicalStorage.IMPORT_ID, ProtectedClinicalStorage.COLLECTIONS_IMPORT_ID, ProtectedClinicalStorage.CLINICAL_DATA_USED),
            f.dao.markers,
        )
    }

    @Test fun invalidCollectionOrEncryptionFailureLeavesFirstGroupAccessibleAndSecondRetryable() = runBlocking {
        val f = Fixture()
        f.storage().clearProfile()
        var bad = true
        val sources = ProtectedClinicalStorage.COLLECTIONS.associateWith { store ->
            { if (bad && store == "prenatal_notes") "invalid" else "$store:valid" }
        }
        val storage = ProtectedClinicalStorage(f.dao, f.cipher, f.profile, f.reminders, validator, sources)
        assertFailsWith<IllegalArgumentException> { storage.read("appointments") }
        assertFalse(f.dao.imported(ProtectedClinicalStorage.COLLECTIONS_IMPORT_ID))
        assertEquals(setOf(ProtectedClinicalStorage.REMINDERS), f.dao.rows.keys)
        assertNull(storage.read(ProtectedClinicalStorage.PROFILE))
        bad = false
        f.cipher.failEncrypt = true
        assertFailsWith<IllegalStateException> { storage.read("appointments") }
        assertFalse(f.dao.imported(ProtectedClinicalStorage.COLLECTIONS_IMPORT_ID))
        f.cipher.failEncrypt = false
        assertEquals("appointments:valid", storage.read("appointments"))
    }

    @Test fun secondGroupChecksEvenAbsentSourceConflictsAndMarksEmptyImports() = runBlocking {
        val f = Fixture()
        f.storage().read(ProtectedClinicalStorage.PROFILE)
        val sources = ProtectedClinicalStorage.COLLECTIONS.associateWith { { null as String? } }
        val storage = ProtectedClinicalStorage(f.dao, f.cipher, f.profile, f.reminders, validator, sources)
        val existing = ProtectedPayload("exam_records", byteArrayOf(1))
        f.dao.rows[existing.store] = existing
        assertFailsWith<IllegalStateException> { storage.read("appointments") }
        assertSame(existing, f.dao.rows[existing.store])
        assertFalse(f.dao.imported(ProtectedClinicalStorage.COLLECTIONS_IMPORT_ID))
        f.dao.rows.remove(existing.store)
        assertNull(storage.read("appointments"))
        assertTrue(f.dao.imported(ProtectedClinicalStorage.COLLECTIONS_IMPORT_ID))
    }

    @Test fun importsBothAndReopeningDoesNotReadLegacyOrResurrectClearedProfile() = runBlocking {
        val f = Fixture()
        val storage = f.storage()
        assertEquals(f.profile.raw, storage.read(ProtectedClinicalStorage.PROFILE))
        assertEquals(f.reminders.raw, storage.read(ProtectedClinicalStorage.REMINDERS))
        storage.write(ProtectedClinicalStorage.REMINDERS, "reminders:edited")
        storage.clearProfile()
        f.profile.failRead = true
        val reopened = f.storage()
        assertNull(reopened.read(ProtectedClinicalStorage.PROFILE))
        assertEquals("reminders:edited", reopened.read(ProtectedClinicalStorage.REMINDERS))
        assertEquals(1, f.profile.reads)
        assertEquals(1, f.dao.imports)
        assertEquals("profile:Lucía", f.profile.raw)
        assertEquals("reminders:one", f.reminders.raw)
    }

    @Test fun invalidSecondStorePreventsAllWritesAndCanBeRetried() = runBlocking {
        val f = Fixture()
        f.reminders.raw = "unsupported"
        assertFailsWith<IllegalArgumentException> { f.storage().read(ProtectedClinicalStorage.PROFILE) }
        assertTrue(f.dao.rows.isEmpty())
        assertFalse(f.dao.marker)
        f.reminders.raw = "reminders:recovered"
        assertEquals("profile:Lucía", f.storage().read(ProtectedClinicalStorage.PROFILE))
        assertEquals(2, f.dao.rows.size)
    }

    @Test fun ioEncryptionAndCommitFailuresDoNotMarkImportComplete() = runBlocking {
        val f = Fixture()
        f.profile.failRead = true
        assertFailsWith<IllegalStateException> { f.storage().read(ProtectedClinicalStorage.PROFILE) }
        f.profile.failRead = false
        f.cipher.failEncrypt = true
        assertFailsWith<IllegalStateException> { f.storage().read(ProtectedClinicalStorage.PROFILE) }
        f.cipher.failEncrypt = false
        f.dao.failImport = true
        assertFailsWith<IllegalStateException> { f.storage().read(ProtectedClinicalStorage.PROFILE) }
        assertFalse(f.dao.marker)
        assertTrue(f.dao.rows.isEmpty())
        f.dao.failImport = false
        assertEquals("profile:Lucía", f.storage().read(ProtectedClinicalStorage.PROFILE))
    }

    @Test fun unmarkedExistingDatabaseIsNeverOverwritten() = runBlocking {
        val f = Fixture()
        val existing = ProtectedPayload(ProtectedClinicalStorage.PROFILE, byteArrayOf(1, 2, 3))
        f.dao.rows[existing.store] = existing
        assertFailsWith<IllegalStateException> { f.storage().read(ProtectedClinicalStorage.PROFILE) }
        assertSame(existing, f.dao.rows[existing.store])
        assertFalse(f.dao.marker)
    }

    @Test fun emptyImportHasDurableMarkerAndConcurrentReadsImportOnce() = runBlocking {
        val f = Fixture()
        f.profile.raw = null
        f.reminders.raw = null
        val storage = f.storage()
        coroutineScope { (1..20).map { async { assertNull(storage.read(ProtectedClinicalStorage.PROFILE)) } }.awaitAll() }
        assertEquals(1, f.dao.imports)
        assertTrue(f.dao.marker)
        f.profile.raw = "profile:stale"
        assertNull(f.storage().read(ProtectedClinicalStorage.PROFILE))
    }

    @Test fun corruptCiphertextAndWriteErrorsNeverReplaceStoredValue() = runBlocking {
        val f = Fixture()
        val storage = f.storage()
        storage.read(ProtectedClinicalStorage.PROFILE)
        f.dao.failWrite = true
        assertFailsWith<IllegalStateException> { storage.write(ProtectedClinicalStorage.PROFILE, "profile:changed") }
        f.dao.failWrite = false
        assertEquals("profile:Lucía", storage.read(ProtectedClinicalStorage.PROFILE))
        val corrupt = ProtectedPayload(ProtectedClinicalStorage.PROFILE, byteArrayOf(0))
        f.dao.rows[corrupt.store] = corrupt
        assertFailsWith<IllegalStateException> { storage.write(ProtectedClinicalStorage.PROFILE, "profile:changed") }
        assertSame(corrupt, f.dao.rows[corrupt.store])
    }
}
