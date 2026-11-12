package com.jecsdev.gestiora.data.datasources

/** Platform-encrypted examRecords payload. Null means absent; read/decrypt failures throw. */
interface SecureExamRecordsStore {
    fun read(): String?
    fun write(payload: String)
}
