package com.jecsdev.gestiora.data.datasources.core.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.jecsdev.gestiora.data.datasources.LegacyClinicalPayloadValidator
import com.jecsdev.gestiora.data.datasources.SecurePersonalRemindersStore
import com.jecsdev.gestiora.data.datasources.SecureProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlin.test.*

/** Isolated test-package files and Keystore alias; never opens the user's database/stores. */
class ClinicalDatabaseDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().context
    private fun open(file: File) = Room.databaseBuilder<ClinicalDatabase>(context, file.absolutePath)
        .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
    private inline fun <T> ClinicalDatabase.useDatabase(block: (ClinicalDatabase) -> T): T =
        try { block(this) } finally { close() }

    @Test fun backupRestoreRollsBackAndCannotRunAgainAfterProfileClear() = runBlocking<Unit> {
        val file = File(context.cacheDir, "clinical-backup-${UUID.randomUUID()}.db")
        val profile = ProtectedPayload(ProtectedClinicalStorage.PROFILE, byteArrayOf(1))
        try {
            open(file).useDatabase { db ->
                val dao = db.payloads()
                assertTrue(dao.canRestoreBackup())
                assertFails { dao.restoreBackup(listOf(profile, profile)) }
                assertTrue(dao.canRestoreBackup())
                assertTrue(dao.restoreBackup(listOf(profile)))
                assertFalse(dao.restoreBackup(listOf(profile)))
                dao.clearClinicalProfile()
                assertEquals(0, dao.count())
                assertFalse(dao.canRestoreBackup())
            }
            open(file).useDatabase { db -> assertFalse(db.payloads().canRestoreBackup()) }
        } finally {
            context.deleteDatabase(file.absolutePath)
        }
    }

    @Test fun sqliteRollsBackWholeImportAndReopensWithMarker() = runBlocking<Unit> {
        val file = File(context.cacheDir, "clinical-test-${UUID.randomUUID()}.db")
        try {
            open(file).useDatabase { db ->
                val dao = db.payloads()
                val row = ProtectedPayload("profile", byteArrayOf(1))
                // The second insert fails after the first; the transaction must roll back both.
                assertFails { dao.importOnce("test", listOf(row, row)) }
                assertEquals(0, dao.count())
                assertFalse(dao.imported("test"))
                dao.importOnce("test", listOf(row))
            }
            open(file).useDatabase { db ->
                assertTrue(db.payloads().imported("test"))
                db.payloads().importOnce("test", listOf(ProtectedPayload("stale", byteArrayOf(2))))
                assertEquals(1, db.payloads().count())
                assertNull(db.payloads().read("stale"))
                assertFails { db.payloads().importOnce("different", emptyList()) }
                assertEquals(1, db.payloads().count())
            }
        } finally { context.deleteDatabase(file.absolutePath) }
    }

    @Test fun secondGroupRollsBackWithoutChangingFirstGroupAndReopensIdempotently() = runBlocking<Unit> {
        val file = File(context.cacheDir, "clinical-groups-${UUID.randomUUID()}.db")
        val marker = ProtectedClinicalStorage.COLLECTIONS_IMPORT_ID
        val stores = ProtectedClinicalStorage.COLLECTIONS.toList()
        try {
            open(file).useDatabase { db ->
                val dao = db.payloads()
                val profile = ProtectedPayload("profile", byteArrayOf(1))
                dao.importOnce(ProtectedClinicalStorage.IMPORT_ID, listOf(profile))
                val appointment = ProtectedPayload("appointments", byteArrayOf(2))
                assertFails { dao.importCollections(marker, stores, listOf(appointment, appointment)) }
                assertFalse(dao.imported(marker))
                assertEquals(1, dao.count())
                assertContentEquals(profile.ciphertext, dao.read("profile")!!.ciphertext)
                // A row whose legacy source is absent still blocks an unmarked group.
                dao.write(ProtectedPayload("exam_records", byteArrayOf(3)))
                assertFails { dao.importCollections(marker, stores, listOf(appointment)) }
                assertNull(dao.read("appointments"))
                dao.delete("exam_records")
                dao.importCollections(marker, stores, listOf(appointment))
            }
            open(file).useDatabase { db ->
                val dao = db.payloads()
                assertTrue(dao.imported(marker))
                dao.importCollections(marker, stores, listOf(ProtectedPayload("appointments", byteArrayOf(99))))
                assertContentEquals(byteArrayOf(2), dao.read("appointments")!!.ciphertext)
                assertContentEquals(byteArrayOf(1), dao.read("profile")!!.ciphertext)
            }
        } finally { context.deleteDatabase(file.absolutePath) }
    }

    @Test fun encryptedImportEditClearReopenAndMissingKeyNeverFallbackToLegacy() = runBlocking<Unit> {
        val id = UUID.randomUUID().toString()
        val file = File(context.cacheDir, "clinical-test-$id.db")
        val alias = "com.jecsdev.gestiora.test.room.$id"
        var legacyReads = 0
        val profile = object : SecureProfileStore {
            override fun read(): String { legacyReads++; return "profile:Lucía 😊\n2028-02-29" }
            override fun write(payload: String) = error("Legacy mutation")
            override fun clear() = error("Legacy deletion")
        }
        val reminders = object : SecurePersonalRemindersStore {
            override fun read() = "reminders:unique-private-title"
            override fun write(payload: String) = error("Legacy mutation")
        }
        val validator = object : LegacyClinicalPayloadValidator {
            override fun validateCollection(store: String, payload: String) { require(payload.startsWith("$store:")) }
        override fun validateProfile(payload: String) { require(payload.startsWith("profile:")) }
            override fun validatePersonalReminders(payload: String) { require(payload.startsWith("reminders:")) }
        }
        try {
            open(file).useDatabase { db ->
                val cipher = AndroidClinicalPayloadCipher({ runBlocking { db.payloads().count() > 0 } }, alias)
                val storage = ProtectedClinicalStorage(db.payloads(), cipher, profile, reminders, validator)
                assertEquals("profile:Lucía 😊\n2028-02-29", storage.read(ProtectedClinicalStorage.PROFILE))
                val raw = requireNotNull(db.payloads().read(ProtectedClinicalStorage.REMINDERS)).ciphertext
                assertFalse(raw.decodeToString().contains("unique-private-title"))
                assertFails { cipher.decrypt(ProtectedClinicalStorage.PROFILE, raw) } // AAD binds store identity.
                val tampered = raw.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
                assertFails { cipher.decrypt(ProtectedClinicalStorage.REMINDERS, tampered) }
                storage.write(ProtectedClinicalStorage.REMINDERS, "reminders:edited")
                storage.clearProfile()
            }
            open(file).useDatabase { db ->
                val cipher = AndroidClinicalPayloadCipher({ runBlocking { db.payloads().count() > 0 } }, alias)
                val storage = ProtectedClinicalStorage(db.payloads(), cipher, profile, reminders, validator)
                assertNull(storage.read(ProtectedClinicalStorage.PROFILE))
                assertEquals("reminders:edited", storage.read(ProtectedClinicalStorage.REMINDERS))
                assertEquals(1, legacyReads)
                KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
                assertFails { storage.read(ProtectedClinicalStorage.REMINDERS) }
                assertFails { storage.write(ProtectedClinicalStorage.PROFILE, "profile:new") }
                assertTrue(db.payloads().imported(ProtectedClinicalStorage.IMPORT_ID))
                assertEquals(1, legacyReads)
            }
            // Inspect main/WAL bytes after closing too, not just decoded DAO results.
            listOf(file, File(file.path + "-wal"), File(file.path + "-shm")).filter { it.exists() }.forEach {
                val bytes = it.readBytes().toString(Charsets.ISO_8859_1)
                assertFalse(bytes.contains("unique-private-title"))
                assertFalse(bytes.contains("reminders:edited"))
            }
        } finally {
            context.deleteDatabase(file.absolutePath)
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        }
    }
}
