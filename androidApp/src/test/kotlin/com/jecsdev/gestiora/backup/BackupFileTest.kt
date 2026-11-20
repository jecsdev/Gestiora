package com.jecsdev.gestiora.backup

import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupFileTest {
    @Test fun roundTripPreservesAllStorePayloadsAndNeverReusesHeader() {
        val stores = listOf(
            "profile", "personal_reminders", "appointments", "consultations", "vital_records",
            "medical_conditions", "vaccine_records", "exam_records", "prenatal_notes",
        )
        val payloads = stores.associateWith { "Lucía — rendez-vous 🌿\n$it" }
        val password = "una frase segura con acento 🍃".toCharArray()
        val file = BackupFile.encrypt(payloads, password)

        assertEquals(payloads, BackupFile.decrypt(file.inputStream(), password))
        assertFalse(file.decodeToString().contains("Lucía"))
        assertFalse(file.contentEquals(BackupFile.encrypt(payloads, password)))
    }

    @Test fun rejectsWrongPasswordTamperedAndIncompatibleFiles() {
        val password = "gestiora test phrase".toCharArray()
        val file = BackupFile.encrypt(mapOf("profile" to "sample"), password)
        assertThrows(AEADBadTagException::class.java) {
            BackupFile.decrypt(file.inputStream(), "a different password".toCharArray())
        }
        val tampered = file.copyOf()
        tampered[tampered.lastIndex] = (tampered.last().toInt() xor 1).toByte()
        assertThrows(AEADBadTagException::class.java) { BackupFile.decrypt(tampered.inputStream(), password) }

        val unknownVersion = file.copyOf()
        unknownVersion["GESTIORA".length] = 99
        assertThrows(UnsupportedBackupVersion::class.java) {
            BackupFile.decrypt(unknownVersion.inputStream(), password)
        }
    }
}
