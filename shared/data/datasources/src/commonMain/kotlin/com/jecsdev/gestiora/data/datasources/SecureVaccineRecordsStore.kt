package com.jecsdev.gestiora.data.datasources

/** Platform-encrypted vaccineRecords payload. Null means absent; read/decrypt failures throw. */
interface SecureVaccineRecordsStore {
    fun read(): String?
    fun write(payload: String)
}
