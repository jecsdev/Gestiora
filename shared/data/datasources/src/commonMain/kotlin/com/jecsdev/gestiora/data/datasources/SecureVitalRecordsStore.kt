package com.jecsdev.gestiora.data.datasources

/** Platform-encrypted vital records payload. Null means absent; read/decrypt failures throw. */
interface SecureVitalRecordsStore {
    fun read(): String?
    fun write(payload: String)
}
