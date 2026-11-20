package com.jecsdev.gestiora.data.datasources

/** Platform-encrypted profile payload. Null means absent; read/decrypt failures throw. */
interface SecureProfileStore {
    fun read(): String?
    fun write(payload: String)
    fun clear()
}
