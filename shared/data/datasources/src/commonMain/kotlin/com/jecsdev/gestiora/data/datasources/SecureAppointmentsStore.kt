package com.jecsdev.gestiora.data.datasources

/** Platform-encrypted appointments payload. Null means absent; read/decrypt failures throw. */
interface SecureAppointmentsStore {
    fun read(): String?
    fun write(payload: String)
}
