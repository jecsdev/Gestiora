package com.jecsdev.gestiora.data.datasources

/** Non-medical display preference, separate from protected prenatal records. */
interface ThemePreferenceStore {
    fun read(): String?
    fun write(value: String)
}
