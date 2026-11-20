package com.jecsdev.gestiora.data.datasources.core.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.jecsdev.gestiora.data.datasources.LegacyClinicalPayloadValidator
import com.jecsdev.gestiora.data.datasources.core.di.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.util.UUID
import kotlin.test.*

/** All files and keys belong to the isolated, self-instrumenting test APK, not Gestiora. */
class LegacyCleanupDeviceTest {
    @Test fun encryptedLegacyStoresImportReadBackAndCleanupLeavesRoomAndUnrelatedFiles() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().context
        check(context.packageName == "com.jecsdev.gestiora.data_datasources_core.test")
        val file = File(context.cacheDir, "cleanup-${UUID.randomUUID()}.db")
        val unrelated = File(context.noBackupFilesDir, "keep-${UUID.randomUUID()}.txt")
        val alias = "gestiora-cleanup-test-${UUID.randomUUID()}"
        val profile = AndroidSecureProfileStore(context)
        val reminders = AndroidSecurePersonalRemindersStore(context)
        val appointments = AndroidSecureAppointmentsStore(context)
        val consultations = AndroidSecureConsultationsStore(context)
        val vitals = AndroidSecureVitalRecordsStore(context)
        val conditions = AndroidSecureMedicalConditionsStore(context)
        val vaccines = AndroidSecureVaccineRecordsStore(context)
        val exams = AndroidSecureExamRecordsStore(context)
        val notes = AndroidSecurePrenatalNotesStore(context)
        val validator = object : LegacyClinicalPayloadValidator {
            override fun validateProfile(payload: String) { require(payload == "profile:fixture") }
            override fun validatePersonalReminders(payload: String) { require(payload == "reminders:fixture") }
            override fun validateCollection(store: String, payload: String) { require(payload == "$store:fixture") }
        }
        val db = Room.databaseBuilder<ClinicalDatabase>(context, file.absolutePath)
            .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
        try {
            unrelated.writeText("keep")
            profile.write("profile:fixture"); reminders.write("reminders:fixture")
            appointments.write("appointments:fixture"); consultations.write("consultations:fixture")
            vitals.write("vital_records:fixture"); conditions.write("medical_conditions:fixture")
            vaccines.write("vaccine_records:fixture"); exams.write("exam_records:fixture"); notes.write("prenatal_notes:fixture")
            assertFailsWith<IllegalStateException> { cleanupLegacyClinicalFiles(context, emptySet()) }
            assertEquals("profile:fixture", profile.read())
            assertEquals("appointments:fixture", appointments.read())
            val dao = db.payloads()
            val storage = ProtectedClinicalStorage(dao, AndroidClinicalPayloadCipher(
                hasCiphertext = { runBlocking(Dispatchers.IO) { dao.count() > 0 } }, keyAlias = alias,
            ), profile, reminders, validator, mapOf(
                "appointments" to appointments::read, "consultations" to consultations::read,
                "vital_records" to vitals::read, "medical_conditions" to conditions::read,
                "vaccine_records" to vaccines::read, "exam_records" to exams::read, "prenatal_notes" to notes::read,
            )) { cleanupLegacyClinicalFiles(context, it) }
            storage.finishMigration()
            assertEquals(9, dao.count())
            assertNull(profile.read()); assertNull(reminders.read()); assertNull(appointments.read()); assertNull(exams.read())
            assertEquals("profile:fixture", storage.read("profile"))
            ProtectedClinicalStorage.COLLECTIONS.forEach { assertEquals("$it:fixture", storage.read(it)) }
            assertEquals("keep", unrelated.readText())
            val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            assertFalse(keys.containsAlias("com.jecsdev.gestiora.profile.v1"))
            assertTrue(keys.containsAlias(alias))
            storage.clearProfile()
            storage.finishMigration()
            assertNull(storage.read("profile"))
            assertEquals("reminders:fixture", storage.read("personal_reminders"))
        } finally {
            db.close(); context.deleteDatabase(file.absolutePath); unrelated.delete()
            cleanupLegacyClinicalFiles(context, ProtectedClinicalStorage.COLLECTIONS + "profile" + "personal_reminders")
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        }
    }
}
