package com.jecsdev.gestiora.data.datasources.core.room

import com.jecsdev.gestiora.data.datasources.SecurePrenatalNotesStore
import com.jecsdev.gestiora.data.datasources.SecureExamRecordsStore
import com.jecsdev.gestiora.data.datasources.SecureVaccineRecordsStore
import com.jecsdev.gestiora.data.datasources.SecureMedicalConditionsStore
import com.jecsdev.gestiora.data.datasources.SecureVitalRecordsStore
import com.jecsdev.gestiora.data.datasources.SecureConsultationsStore
import com.jecsdev.gestiora.data.datasources.SecureAppointmentsStore
import com.jecsdev.gestiora.data.datasources.SecurePersonalRemindersStore
import com.jecsdev.gestiora.data.datasources.SecureProfileStore

// Existing store contracts are synchronous. Repositories call them off the main thread.
internal expect fun <T> storageCall(block: suspend () -> T): T

internal class RoomProfileStore(private val storage: ProtectedClinicalStorage) : SecureProfileStore {
    override fun read(): String? = storageCall { storage.read(ProtectedClinicalStorage.PROFILE) }
    override fun write(payload: String) = storageCall { storage.write(ProtectedClinicalStorage.PROFILE, payload) }
    override fun clear() = storageCall { storage.clearProfile() }
}

internal class RoomPersonalRemindersStore(private val storage: ProtectedClinicalStorage) : SecurePersonalRemindersStore {
    override fun read(): String? = storageCall { storage.read(ProtectedClinicalStorage.REMINDERS) }
    override fun write(payload: String) = storageCall { storage.write(ProtectedClinicalStorage.REMINDERS, payload) }
}

internal class RoomAppointmentsStore(private val storage: ProtectedClinicalStorage) : SecureAppointmentsStore {
    override fun read(): String? = storageCall { storage.read("appointments") }
    override fun write(payload: String) = storageCall { storage.write("appointments", payload) }
}
internal class RoomConsultationsStore(private val storage: ProtectedClinicalStorage) : SecureConsultationsStore {
    override fun read(): String? = storageCall { storage.read("consultations") }
    override fun write(payload: String) = storageCall { storage.write("consultations", payload) }
}
internal class RoomVitalRecordsStore(private val storage: ProtectedClinicalStorage) : SecureVitalRecordsStore {
    override fun read(): String? = storageCall { storage.read("vital_records") }
    override fun write(payload: String) = storageCall { storage.write("vital_records", payload) }
}
internal class RoomMedicalConditionsStore(private val storage: ProtectedClinicalStorage) : SecureMedicalConditionsStore {
    override fun read(): String? = storageCall { storage.read("medical_conditions") }
    override fun write(payload: String) = storageCall { storage.write("medical_conditions", payload) }
}
internal class RoomVaccineRecordsStore(private val storage: ProtectedClinicalStorage) : SecureVaccineRecordsStore {
    override fun read(): String? = storageCall { storage.read("vaccine_records") }
    override fun write(payload: String) = storageCall { storage.write("vaccine_records", payload) }
}
internal class RoomExamRecordsStore(private val storage: ProtectedClinicalStorage) : SecureExamRecordsStore {
    override fun read(): String? = storageCall { storage.read("exam_records") }
    override fun write(payload: String) = storageCall { storage.write("exam_records", payload) }
}
internal class RoomPrenatalNotesStore(private val storage: ProtectedClinicalStorage) : SecurePrenatalNotesStore {
    override fun read(): String? = storageCall { storage.read("prenatal_notes") }
    override fun write(payload: String) = storageCall { storage.write("prenatal_notes", payload) }
}