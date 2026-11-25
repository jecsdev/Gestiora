package com.jecsdev.gestiora.data.datasources.core.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.jecsdev.gestiora.data.datasources.*
import com.jecsdev.gestiora.data.datasources.core.di.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSThread

internal actual fun <T> storageCall(block: suspend () -> T): T {
    check(!NSThread.isMainThread) { "Clinical storage must run off the main thread" }
    return runBlocking(Dispatchers.Default) { block() }
}

fun iosClinicalStorageModule(
    path: String,
    clinicalCipher: (hasCiphertext: () -> Boolean) -> ClinicalPayloadCipher,
): Module = module {
    single {
        Room.databaseBuilder<ClinicalDatabase>(name = path)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }
    single {
        val dao = get<ClinicalDatabase>().payloads()
        ProtectedClinicalStorage(
            dao,
            clinicalCipher { storageCall { dao.count() > 0 } },
            IosSecureProfileStore(),
            IosSecurePersonalRemindersStore(),
            get(),
            legacyCollections = mapOf(
                "appointments" to IosSecureAppointmentsStore()::read,
                "consultations" to IosSecureConsultationsStore()::read,
                "vital_records" to IosSecureVitalRecordsStore()::read,
                "medical_conditions" to IosSecureMedicalConditionsStore()::read,
                "vaccine_records" to IosSecureVaccineRecordsStore()::read,
                "exam_records" to IosSecureExamRecordsStore()::read,
                "prenatal_notes" to IosSecurePrenatalNotesStore()::read,
            ),
        )
    }
    single<SecureProfileStore> { RoomProfileStore(get()) }
    single<SecurePersonalRemindersStore> { RoomPersonalRemindersStore(get()) }
    single<SecureAppointmentsStore> { RoomAppointmentsStore(get()) }
    single<SecureConsultationsStore> { RoomConsultationsStore(get()) }
    single<SecureVitalRecordsStore> { RoomVitalRecordsStore(get()) }
    single<SecureMedicalConditionsStore> { RoomMedicalConditionsStore(get()) }
    single<SecureVaccineRecordsStore> { RoomVaccineRecordsStore(get()) }
    single<SecureExamRecordsStore> { RoomExamRecordsStore(get()) }
    single<SecurePrenatalNotesStore> { RoomPrenatalNotesStore(get()) }
}
