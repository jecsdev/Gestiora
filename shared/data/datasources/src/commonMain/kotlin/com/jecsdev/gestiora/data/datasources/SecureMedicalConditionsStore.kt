package com.jecsdev.gestiora.data.datasources

/** Platform-encrypted medicalConditions payload. Null means absent; read/decrypt failures throw. */
interface SecureMedicalConditionsStore {
    fun read(): String?
    fun write(payload: String)
}
