package com.jecsdev.gestiora.data.datasources.core.room

import com.jecsdev.gestiora.data.datasources.PREGNANCY_JOURNAL_STORE

enum class AttachmentCollection(val store: String) {
    Consultation("consultations"),
    Exam("exam_records"),
    Journal(PREGNANCY_JOURNAL_STORE);

    internal fun requireAllowed(attachment: ClinicalAttachment) {
        require(this != Journal || attachment.mimeType.startsWith("image/"))
    }

    internal val prefix get() = "attachment:$store:"
    private val imagePrefix get() = "attachment:images:$store:"

    internal fun key(id: String, imageSlot: Int? = null): String {
        require(id.isNotBlank() && id.length <= 128)
        if (imageSlot == null) return prefix + id
        require(this != Journal && imageSlot in 1..MAX_IMAGES_PER_RECORD)
        return "$imagePrefix$id:$imageSlot"
    }

    internal fun ownerId(key: String): String? = when {
        key.startsWith(imagePrefix) && this != Journal -> {
            val suffix = key.removePrefix(imagePrefix)
            val id = suffix.substringBeforeLast(':', "")
            val slot = suffix.substringAfterLast(':').toIntOrNull()
            id.takeIf { it.isNotBlank() && it.length <= 128 && slot != null &&
                slot in 1..MAX_IMAGES_PER_RECORD && key(it, slot) == key }
        }
        key.startsWith(prefix) -> key.removePrefix(prefix)
        else -> null
    }

    companion object {
        const val MAX_IMAGES_PER_RECORD = 8
    }
}

class ClinicalAttachment(val name: String, val bytes: ByteArray) {
    init {
        require(name.isNotBlank() && name.length <= MAX_NAME && name.none {
            it.isISOControl() || it == '/' || it == '\\'
        })
        require(bytes.size in 1..MAX_BYTES)
    }

    val mimeType: String = when {
        bytes.beginsWith(pdf) -> "application/pdf"
        bytes.beginsWith(jpeg) -> "image/jpeg"
        bytes.beginsWith(png) -> "image/png"
        else -> throw IllegalArgumentException("Unsupported attachment format")
    }

    internal fun encode() = (name + "\n").encodeToByteArray() + bytes

    companion object {
        const val MAX_NAME = 120
        const val MAX_BYTES = 4 * 1024 * 1024
        const val MAX_COUNT = 64
        const val MAX_TOTAL_BYTES = 16 * 1024 * 1024
        const val MAX_ENVELOPE_BYTES = MAX_BYTES + MAX_NAME * 3 + 1
        internal const val CIPHER_OVERHEAD = 4 + 12 + 16

        private val pdf = "%PDF-".encodeToByteArray()
        private val jpeg = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())
        private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)

        internal fun decode(envelope: ByteArray): ClinicalAttachment {
            val newline = (0 until minOf(envelope.size, MAX_NAME * 3 + 1))
                .firstOrNull { envelope[it] == 10.toByte() } ?: error("Invalid attachment envelope")
            val name = envelope.decodeToString(endIndex = newline, throwOnInvalidSequence = true)
            val bytes = envelope.copyOfRange(newline + 1, envelope.size)
            return try {
                ClinicalAttachment(name, bytes)
            } catch (failure: IllegalArgumentException) {
                bytes.fill(0)
                throw failure
            }
        }
    }
}

private fun ByteArray.beginsWith(prefix: ByteArray) =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

class ClinicalBackup(
    val payloads: Map<String, String>,
    val attachments: Map<String, ByteArray> = emptyMap(),
) {
    fun clearAttachments() = attachments.values.forEach { it.fill(0) }
}

class AttachmentStorageLimitException : IllegalStateException("Clinical attachment storage limit reached")
