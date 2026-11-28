package com.jecsdev.gestiora.data.datasources

/** Uses the existing versioned codecs before any legacy payload is committed to Room. */
interface LegacyClinicalPayloadValidator {
    fun validateProfile(payload: String)
    fun validatePersonalReminders(payload: String)
    fun validateCollection(store: String, payload: String)
    fun attachmentOwnerIds(store: String, payload: String): Set<String> = error("Unsupported attachment collection")
}
