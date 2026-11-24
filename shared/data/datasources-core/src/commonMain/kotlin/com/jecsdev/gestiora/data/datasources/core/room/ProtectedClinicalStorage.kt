package com.jecsdev.gestiora.data.datasources.core.room

import com.jecsdev.gestiora.data.datasources.LegacyClinicalPayloadValidator
import com.jecsdev.gestiora.data.datasources.PREGNANCY_JOURNAL_STORE
import com.jecsdev.gestiora.data.datasources.SecurePersonalRemindersStore
import com.jecsdev.gestiora.data.datasources.SecureProfileStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ClinicalPayloadCipher {
    fun encryptBytes(store: String, payload: ByteArray): ByteArray
    fun decryptBytes(store: String, ciphertext: ByteArray): ByteArray

    fun encrypt(store: String, payload: String): ByteArray {
        val bytes = payload.encodeToByteArray()
        return try { encryptBytes(store, bytes) } finally { bytes.fill(0) }
    }

    fun decrypt(store: String, ciphertext: ByteArray): String {
        val bytes = decryptBytes(store, ciphertext)
        return try { bytes.decodeToString(throwOnInvalidSequence = true) } finally { bytes.fill(0) }
    }
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
    private val legacyStores = listOf(PROFILE, REMINDERS) + COLLECTIONS
    private val stores = legacyStores + PREGNANCY_JOURNAL_STORE

    suspend fun canRestoreBackup(): Boolean = mutex.withLock {
        ensureImported()
        ensureCollectionsImported()
        dao.canRestoreBackup()
    }

    suspend fun backupSnapshot(): ClinicalBackup = mutex.withLock {
        ensureImported()
        ensureCollectionsImported()
        val attachmentSizes = dao.attachmentSizes()
        check(attachmentSizes.size <= ClinicalAttachment.MAX_COUNT)
        check(attachmentSizes.sumOf { it.size.toLong() - ClinicalAttachment.CIPHER_OVERHEAD } <= ClinicalAttachment.MAX_TOTAL_BYTES)
        val rows = dao.snapshot(stores + attachmentSizes.map { it.store })
        val payloads = rows.filter { it.store in stores }.associate { row ->
            val payload = cipher.decrypt(row.store, row.ciphertext)
            validate(row.store, payload)
            row.store to payload
        }.also { check(PROFILE in it) }
        val attachments = mutableMapOf<String, ByteArray>()
        try {
            for (row in rows.filterNot { it.store in stores }) {
                attachments[row.store] = cipher.decryptBytes(row.store, row.ciphertext)
            }
            validateAttachments(payloads, attachments)
            ClinicalBackup(payloads, attachments)
        } catch (failure: Throwable) {
            attachments.values.forEach { it.fill(0) }
            throw failure
        }
    }

    suspend fun restoreBackup(backup: ClinicalBackup): Boolean = mutex.withLock {
        ensureImported()
        ensureCollectionsImported()
        if (!dao.canRestoreBackup()) return@withLock false
        val payloads = backup.payloads
        require(PROFILE in payloads && stores.containsAll(payloads.keys))
        payloads.forEach { (store, payload) -> validate(store, payload) }
        validateAttachments(payloads, backup.attachments)
        val rows = payloads.map { (store, payload) -> ProtectedPayload(store, cipher.encrypt(store, payload)) } +
            backup.attachments.map { (key, bytes) -> ProtectedPayload(key, cipher.encryptBytes(key, bytes)) }
        dao.restoreBackup(rows)
    }

    suspend fun readAttachment(
        collection: AttachmentCollection,
        id: String,
        imageSlot: Int? = null,
    ): ClinicalAttachment? = mutex.withLock {
        readAttachmentByKey(collection.key(id, imageSlot))
    }

    suspend fun imageSlots(collection: AttachmentCollection, id: String): List<Int> = mutex.withLock {
        val keys = dao.attachmentSizes().mapTo(mutableSetOf()) { it.store }
        (1..AttachmentCollection.MAX_IMAGES_PER_RECORD).filter { collection.key(id, it) in keys }
    }

    private suspend fun readAttachmentByKey(key: String): ClinicalAttachment? = dao.read(key)?.let { row ->
        val plain = cipher.decryptBytes(key, row.ciphertext)
        try { ClinicalAttachment.decode(plain) } finally { plain.fill(0) }
    }

    suspend fun writeAttachment(
        collection: AttachmentCollection,
        id: String,
        attachment: ClinicalAttachment,
        imageSlot: Int? = null,
    ) = mutex.withLock {
        collection.requireAllowed(attachment)
        require(imageSlot == null || attachment.mimeType.startsWith("image/"))
        ensureImported()
        ensureCollectionsImported()
        val key = collection.key(id, imageSlot)
        val owner = checkNotNull(dao.read(collection.store))
        val payload = cipher.decrypt(collection.store, owner.ciphertext)
        require(id in validator.attachmentOwnerIds(collection.store, payload))
        val plain = attachment.encode()
        try {
            val others = dao.attachmentSizes().filterNot { it.store == key }
            val available = ClinicalAttachment.MAX_TOTAL_BYTES -
                others.sumOf { it.size.toLong() - ClinicalAttachment.CIPHER_OVERHEAD }
            if (others.size >= ClinicalAttachment.MAX_COUNT || plain.size > available) {
                throw AttachmentStorageLimitException()
            }
            dao.writeClinicalData(ProtectedPayload(key, cipher.encryptBytes(key, plain)))
        } finally {
            plain.fill(0)
        }
    }

    suspend fun deleteAttachment(collection: AttachmentCollection, id: String, imageSlot: Int? = null) = mutex.withLock {
        dao.delete(collection.key(id, imageSlot))
    }

    /** Import both groups, then authenticate/validate EVERY destination before source removal.
     * The durable markers make an interrupted cleanup retryable without reimporting stale files.
     */
    suspend fun finishMigration() = mutex.withLock {
        ensureImported()
        ensureCollectionsImported()
        val verified = mutableSetOf<String>()
        legacyStores.forEach { store ->
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
        val collection = AttachmentCollection.entries.find { it.store == store }
        val attachments = if (collection == null) emptyList() else
            dao.attachmentSizes().map { it.store }.filter { collection.ownerId(it) != null }
        val removed = if (collection == null || attachments.isEmpty()) emptyList() else {
            val remaining = validator.attachmentOwnerIds(store, payload)
            attachments.filter { collection.ownerId(it) !in remaining }
        }
        dao.writeClinicalData(ProtectedPayload(store, cipher.encrypt(store, payload)), removed)
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
        PREGNANCY_JOURNAL_STORE, in COLLECTIONS -> validator.validateCollection(store, payload)
        else -> error("Unknown clinical store")
    }

    private fun requireStore(store: String) { require(store in stores) }

    private fun validateAttachments(payloads: Map<String, String>, attachments: Map<String, ByteArray>) {
        require(attachments.size <= ClinicalAttachment.MAX_COUNT)
        require(attachments.values.sumOf { it.size.toLong() } <= ClinicalAttachment.MAX_TOTAL_BYTES)
        val owners = mutableMapOf<AttachmentCollection, Set<String>>()
        for ((key, bytes) in attachments) {
            val collection = requireNotNull(AttachmentCollection.entries.find { it.ownerId(key) != null })
            val id = requireNotNull(collection.ownerId(key))
            val matching = owners.getOrPut(collection) {
                validator.attachmentOwnerIds(collection.store, requireNotNull(payloads[collection.store]))
            }
            require(id in matching)
            require(bytes.size in 1..ClinicalAttachment.MAX_ENVELOPE_BYTES)
            val attachment = ClinicalAttachment.decode(bytes)
            try {
                collection.requireAllowed(attachment)
                require(key == collection.key(id) || attachment.mimeType.startsWith("image/"))
            } finally {
                attachment.bytes.fill(0)
            }
        }
    }

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
