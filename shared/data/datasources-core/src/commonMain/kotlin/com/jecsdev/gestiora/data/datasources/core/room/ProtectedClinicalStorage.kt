package com.jecsdev.gestiora.data.datasources.core.room

import com.jecsdev.gestiora.data.datasources.LegacyClinicalPayloadValidator
import com.jecsdev.gestiora.data.datasources.SecurePersonalRemindersStore
import com.jecsdev.gestiora.data.datasources.SecureProfileStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ClinicalPayloadCipher {
    fun encrypt(store: String, payload: String): ByteArray
    fun decrypt(store: String, ciphertext: ByteArray): String
}

/** Two durable import groups preserve previously migrated profile/reminder state. */
class ProtectedClinicalStorage(
    private val dao: ProtectedPayloadDao,
    private val cipher: ClinicalPayloadCipher,
    private val legacyProfile: SecureProfileStore,
    private val legacyReminders: SecurePersonalRemindersStore,
    private val validator: LegacyClinicalPayloadValidator,
    private val legacyCollections: Map<String, () -> String?> = emptyMap(),
    private val cleanupLegacy: (verifiedStores: Set<String>) -> Unit = {},
) {
    private val mutex = Mutex()
    private val stores = listOf(PROFILE, REMINDERS) + COLLECTIONS

    suspend fun canRestoreBackup(): Boolean = mutex.withLock {
        ensureImported()
        ensureCollectionsImported()
        dao.canRestoreBackup()
    }

    suspend fun backupSnapshot(): Map<String, String> = mutex.withLock {
        ensureImported()
        ensureCollectionsImported()
        dao.snapshot(stores).associate { row ->
            val payload = cipher.decrypt(row.store, row.ciphertext)
            validate(row.store, payload)
            row.store to payload
        }.also { check(PROFILE in it) }
    }

    suspend fun restoreBackup(payloads: Map<String, String>): Boolean = mutex.withLock {
        ensureImported()
        ensureCollectionsImported()
        if (!dao.canRestoreBackup()) return@withLock false
        require(PROFILE in payloads && stores.containsAll(payloads.keys))
        payloads.forEach { (store, payload) -> validate(store, payload) }
        val rows = payloads.map { (store, payload) -> ProtectedPayload(store, cipher.encrypt(store, payload)) }
        dao.restoreBackup(rows)
    }

    /** Import both groups, then authenticate/validate EVERY destination before source removal.
     * The durable markers make an interrupted cleanup retryable without reimporting stale files.
     */
    suspend fun finishMigration() = mutex.withLock {
        ensureImported()
        ensureCollectionsImported()
        val verified = mutableSetOf<String>()
        stores.forEach { store ->
            dao.read(store)?.let {
                validate(store, cipher.decrypt(store, it.ciphertext))
                verified.add(store)
            }
        }
        cleanupLegacy(verified)
    }

    suspend fun read(store: String): String? = mutex.withLock {
        requireStore(store)
        ensureImported()
        if (store in COLLECTIONS) ensureCollectionsImported()
        dao.read(store)?.let { cipher.decrypt(store, it.ciphertext) }
    }

    suspend fun write(store: String, payload: String) = mutex.withLock {
        requireStore(store)
        ensureImported()
        if (store in COLLECTIONS) ensureCollectionsImported()
        validate(store, payload)
        // Authenticate existing content even for direct storage clients before replacing it.
        dao.read(store)?.let { validate(store, cipher.decrypt(store, it.ciphertext)) }
        dao.writeClinicalData(ProtectedPayload(store, cipher.encrypt(store, payload)))
    }

    suspend fun clearProfile() = mutex.withLock {
        ensureImported()
        dao.clearClinicalProfile()
        // Keep the import marker: clearing must not resurrect the preserved source file.
        // The shared key also protects reminders and must not be deleted here.
    }

    private suspend fun ensureImported() {
        if (dao.imported(IMPORT_ID)) return
        val profile = legacyProfile.read()
        val reminders = legacyReminders.read()
        profile?.let(validator::validateProfile)
        reminders?.let(validator::validatePersonalReminders)
        // Validate the entire source before encrypting or changing database rows.
        val rows = buildList {
            profile?.let { add(ProtectedPayload(PROFILE, cipher.encrypt(PROFILE, it))) }
            reminders?.let { add(ProtectedPayload(REMINDERS, cipher.encrypt(REMINDERS, it))) }
        }
        dao.importOnce(IMPORT_ID, rows)
        // Deliberately do not delete or rewrite either legacy store.
    }

    private suspend fun ensureCollectionsImported() {
        if (dao.imported(COLLECTIONS_IMPORT_ID)) return
        check(legacyCollections.keys == COLLECTIONS) { "All legacy collection readers are required" }
        val sources = legacyCollections.mapValues { (_, read) -> read() }
        sources.forEach { (store, payload) -> payload?.let { validator.validateCollection(store, it) } }
        val rows = sources.mapNotNull { (store, payload) ->
            payload?.let { ProtectedPayload(store, cipher.encrypt(store, it)) }
        }
        dao.importCollections(COLLECTIONS_IMPORT_ID, COLLECTIONS.toList(), rows)
    }

    private fun validate(store: String, payload: String) = when (store) {
        PROFILE -> validator.validateProfile(payload)
        REMINDERS -> validator.validatePersonalReminders(payload)
        in COLLECTIONS -> validator.validateCollection(store, payload)
        else -> error("Unknown clinical store")
    }

    private fun requireStore(store: String) { require(store == PROFILE || store == REMINDERS || store in COLLECTIONS) }

    companion object {
        const val PROFILE = "profile"
        const val REMINDERS = "personal_reminders"
        const val IMPORT_ID = "profile-and-reminders-v1"
        const val COLLECTIONS_IMPORT_ID = "clinical-collections-v1"
        const val CLINICAL_DATA_USED = "clinical-data-used-v1"
        val COLLECTIONS: Set<String> = setOf(
            "appointments", "consultations", "vital_records", "medical_conditions",
            "vaccine_records", "exam_records", "prenatal_notes",
        )
    }
}
