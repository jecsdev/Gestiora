package com.jecsdev.gestiora.data.datasources.core.room

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore

/** Only these former clinical stores are eligible. Never enumerate/delete the storage directory. */
internal fun cleanupLegacyClinicalFiles(context: Context, verifiedStores: Set<String>) {
    val stores = mapOf(
        "pregnancy_profile.gpr" to ("profile" to "profile"),
        "pregnancy_reminders.gpr" to ("personalReminders" to "personal_reminders"),
        "pregnancy_appointments.gpr" to ("appointments" to "appointments"),
        "pregnancy_consultations.gpr" to ("consultations" to "consultations"),
        "pregnancy_vitals.gpr" to ("vitalRecords" to "vital_records"),
        "pregnancy_conditions.gpr" to ("medicalConditions" to "medical_conditions"),
        "pregnancy_vaccines.gpr" to ("vaccineRecords" to "vaccine_records"),
        "pregnancy_exams.gpr" to ("examRecords" to "exam_records"),
        "pregnancy_notes.gpr" to ("prenatalNotes" to "prenatal_notes"),
    )
    val root = context.noBackupFilesDir.canonicalFile
    // Check the entire deletion scope BEFORE removing any file. Profile is the only store
    // with an intentional row-delete API; collection deletion writes an encoded empty list.
    stores.forEach { (name, entry) ->
        check(entry.second == "profile" || entry.second in verifiedStores ||
            listOf("", ".bak", ".new").none { File(root, name + it).exists() }) {
            "Legacy collection has no verified destination; cleanup refused"
        }
    }
    val keys = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    stores.forEach { (name, entry) ->
        val file = File(root, name)
        check(file.canonicalFile.parentFile == root)
        AtomicFile(file).delete()
        check(listOf("", ".bak", ".new").none { File(root, name + it).exists() }) {
            "Legacy clinical file removal failed"
        }
        // Delete the old per-file key only after all its AtomicFile variants are gone.
        keys.deleteEntry("com.jecsdev.gestiora.${entry.first}.v1")
    }
}
