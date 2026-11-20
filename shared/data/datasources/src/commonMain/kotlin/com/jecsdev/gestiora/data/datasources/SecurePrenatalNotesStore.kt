package com.jecsdev.gestiora.data.datasources

/** Platform-encrypted notes payload. Null means absent; read/decrypt failures throw. */
interface SecurePrenatalNotesStore {
    fun read(): String?
    fun write(payload: String)
}
