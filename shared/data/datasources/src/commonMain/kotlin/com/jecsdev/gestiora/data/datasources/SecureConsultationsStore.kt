package com.jecsdev.gestiora.data.datasources

/** Platform-encrypted consultations payload. Null means absent; read/decrypt failures throw. */
interface SecureConsultationsStore {
    fun read(): String?
    fun write(payload: String)
}
