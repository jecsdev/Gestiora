package com.jecsdev.gestiora.data.datasources.core.room

/** Plaintext layout of the Android v1/v2 backup format, including DataOutput's modified UTF-8 keys. */
object PortableBackupPayload {
    private const val MAX_STORES = 10
    private const val MAX_PAYLOAD = 3_000_000
    private const val MAX_V1_FILE = MAX_STORES * MAX_PAYLOAD + 1_024
    private const val HEADER_AND_TAG = 8 + 1 + 16 + 12 + 16
    const val MAX_FILE = MAX_V1_FILE + ClinicalAttachment.MAX_TOTAL_BYTES + ClinicalAttachment.MAX_COUNT * 512 + 1

    fun validateFile(file: ByteArray): Int {
        require(file.size in (HEADER_AND_TAG + 1)..MAX_FILE)
        require(file.decodeToString(endIndex = 8) == "GESTIORA")
        val version = file[8].toInt()
        if (version !in 1..2) throw UnsupportedPortableBackupVersion()
        require(file.size <= fileLimit(version))
        return version
    }

    fun encode(backup: ClinicalBackup): Pair<Int, ByteArray> {
        require(backup.payloads.size in 1..MAX_STORES)
        require(backup.attachments.size <= ClinicalAttachment.MAX_COUNT)
        require(backup.attachments.values.all { it.size in 1..ClinicalAttachment.MAX_ENVELOPE_BYTES })
        require(backup.attachments.values.sumOf { it.size.toLong() } <= ClinicalAttachment.MAX_TOTAL_BYTES)
        val version = if (backup.attachments.isEmpty()) 1 else 2
        val payloads = mutableMapOf<String, ByteArray>()
        try {
            for ((key, value) in backup.payloads) {
                val bytes = value.encodeToByteArray()
                payloads[key] = bytes
                require(bytes.size in 1..MAX_PAYLOAD)
            }
            fun sizeOf(entries: Map<String, ByteArray>) = entries.entries.sumOf { (key, value) ->
                require(key.modifiedLength() in 1..65_535)
                2L + key.modifiedLength() + 4 + value.size
            }
            val size = 1L + sizeOf(payloads) + if (version == 2) 1L + sizeOf(backup.attachments) else 0L
            require(size <= fileLimit(version) - HEADER_AND_TAG)
            val result = ByteArray(size.toInt())
            var offset = 0
            fun put(byte: Int) { result[offset++] = byte.toByte() }
            fun putEntries(entries: Map<String, ByteArray>) {
                put(entries.size)
                for ((key, value) in entries) {
                    val length = key.modifiedLength()
                    put(length shr 8)
                    put(length)
                    for (character in key) {
                        val char = character.code
                        when (char) {
                            in 1..127 -> put(char)
                            in 0..2047 -> {
                                put(0xc0 or (char shr 6))
                                put(0x80 or (char and 0x3f))
                            }
                            else -> {
                                put(0xe0 or (char shr 12))
                                put(0x80 or ((char shr 6) and 0x3f))
                                put(0x80 or (char and 0x3f))
                            }
                        }
                    }
                    for (shift in 24 downTo 0 step 8) put(value.size shr shift)
                    value.copyInto(result, destinationOffset = offset)
                    offset += value.size
                }
            }
            putEntries(payloads)
            if (version == 2) putEntries(backup.attachments)
            return version to result
        } finally {
            payloads.values.forEach { it.fill(0) }
        }
    }

    fun decode(plain: ByteArray, version: Int): ClinicalBackup {
        require(plain.size in 1..(fileLimit(version) - HEADER_AND_TAG))
        val reader = BackupReader(plain)
        val count = reader.byte()
        require(count in 1..MAX_STORES)
        val payloads = buildMap {
            repeat(count) {
                val key = reader.key()
                require(key !in this)
                val bytes = reader.entry(MAX_PAYLOAD)
                try { put(key, bytes.decodeToString(throwOnInvalidSequence = true)) } finally { bytes.fill(0) }
            }
        }
        val attachments = mutableMapOf<String, ByteArray>()
        try {
            if (version == 2) {
                val countAttachments = reader.byte()
                require(countAttachments in 1..ClinicalAttachment.MAX_COUNT)
                var remaining = ClinicalAttachment.MAX_TOTAL_BYTES
                repeat(countAttachments) {
                    val key = reader.key()
                    require(key !in attachments)
                    val bytes = reader.entry(minOf(remaining, ClinicalAttachment.MAX_ENVELOPE_BYTES))
                    attachments[key] = bytes
                    remaining -= bytes.size
                }
            }
            require(reader.finished)
            return ClinicalBackup(payloads, attachments)
        } catch (failure: Throwable) {
            attachments.values.forEach { it.fill(0) }
            throw failure
        }
    }

    private fun fileLimit(version: Int) = when (version) {
        1 -> MAX_V1_FILE
        2 -> MAX_FILE
        else -> error("Unsupported backup version")
    }

    private fun String.modifiedLength(): Int = sumOf { char ->
        when (char.code) {
            in 1..127 -> 1
            in 0..2047 -> 2
            else -> 3
        }
    }

    private class BackupReader(private val bytes: ByteArray) {
        private var offset = 0
        val finished get() = offset == bytes.size

        fun byte(): Int {
            require(offset < bytes.size)
            return bytes[offset++].toInt() and 0xff
        }

        fun key(): String {
            val size = (byte() shl 8) or byte()
            require(size in 1..(bytes.size - offset))
            val end = offset + size
            fun continuation(): Int {
                require(offset < end)
                val value = byte()
                require(value and 0xc0 == 0x80)
                return value and 0x3f
            }
            return buildString {
                while (offset < end) {
                    val first = byte()
                    val value = when (first) {
                        in 1..127 -> first
                        in 0xc0..0xdf -> ((first and 0x1f) shl 6) or continuation()
                        in 0xe0..0xef -> ((first and 0x0f) shl 12) or (continuation() shl 6) or continuation()
                        else -> error("Invalid backup key")
                    }
                    append(value.toChar())
                }
            }
        }

        fun entry(limit: Int): ByteArray {
            var length = 0
            repeat(4) { length = (length shl 8) or byte() }
            require(length in 1..minOf(limit, bytes.size - offset))
            return bytes.copyOfRange(offset, offset + length).also { offset += length }
        }
    }
}

class UnsupportedPortableBackupVersion : IllegalArgumentException()
