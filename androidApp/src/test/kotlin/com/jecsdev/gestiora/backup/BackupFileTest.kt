package com.jecsdev.gestiora.backup

import com.jecsdev.gestiora.data.datasources.core.room.ClinicalBackup
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertArrayEquals
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
        val backup = ClinicalBackup(payloads)
        val file = BackupFile.encrypt(backup, password)

        assertEquals(1, file["GESTIORA".length].toInt())
        assertEquals(payloads, BackupFile.decrypt(file.inputStream(), password).payloads)
        assertFalse(file.decodeToString().contains("Lucía"))
        assertFalse(file.contentEquals(BackupFile.encrypt(backup, password)))
    }

    @Test fun rejectsWrongPasswordTamperedAndIncompatibleFiles() {
        val password = "gestiora test phrase".toCharArray()
        val file = BackupFile.encrypt(ClinicalBackup(mapOf("profile" to "sample")), password)
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

    @Test fun attachmentBackupsPreserveBinaryBytes() {
        val key = "attachment:exam_records:exam-1"
        val original = "Résultat.png\n".encodeToByteArray() + byteArrayOf(0, -1, 127)
        val backup = ClinicalBackup(mapOf("profile" to "sample"), mapOf(key to original))
        val password = "gestiora test phrase".toCharArray()
        val file = BackupFile.encrypt(backup, password)
        assertEquals(2, file["GESTIORA".length].toInt())
        val restored = BackupFile.decrypt(file.inputStream(), password)
        assertEquals(backup.payloads, restored.payloads)
        assertArrayEquals(original, restored.attachments.getValue(key))
    }
}
