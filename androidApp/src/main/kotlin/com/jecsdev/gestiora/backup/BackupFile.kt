package com.jecsdev.gestiora.backup

import com.jecsdev.gestiora.data.datasources.core.room.ClinicalAttachment
import com.jecsdev.gestiora.data.datasources.core.room.ClinicalBackup
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters

internal class UnsupportedBackupVersion : Exception()

/** Both versions fix the KDF parameters. Untrusted headers cannot request more work. */
internal object BackupFile {
    private val magic = "GESTIORA".encodeToByteArray()
    private val random = SecureRandom()
    private const val TEXT_VERSION = 1
    private const val ATTACHMENT_VERSION = 2
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BYTES = 16
    private val headerSize = magic.size + 1 + SALT_BYTES + NONCE_BYTES
    private const val MAX_STORES = 9
    private const val MAX_PAYLOAD_BYTES = 3_000_000
    private const val MAX_V1_BYTES = 27_001_024
    private const val MAX_V2_BYTES = MAX_V1_BYTES + ClinicalAttachment.MAX_TOTAL_BYTES +
        ClinicalAttachment.MAX_COUNT * 512 + 1

    fun encrypt(backup: ClinicalBackup, password: CharArray): ByteArray {
        val payloads = backup.payloads
        val attachments = backup.attachments
        require(payloads.size in 1..MAX_STORES)
        require(attachments.size <= ClinicalAttachment.MAX_COUNT)
        require(attachments.values.sumOf { it.size.toLong() } <= ClinicalAttachment.MAX_TOTAL_BYTES)
        val version = if (attachments.isEmpty()) TEXT_VERSION else ATTACHMENT_VERSION
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val header = magic + byteArrayOf(version.toByte()) + salt + nonce
        val cipher = cipher(Cipher.ENCRYPT_MODE, password, salt, nonce, header)
        val encrypted = ByteArrayOutputStream()
        encrypted.write(header)
        // Only ciphertext enters the output buffer. Separate encoded values are short-lived.
        writeEncrypted(cipher, encrypted, byteArrayOf(payloads.size.toByte()))
        payloads.forEach { (store, value) ->
            val bytes = value.encodeToByteArray()
            try {
                require(bytes.size in 1..MAX_PAYLOAD_BYTES)
                writeEntry(cipher, encrypted, store, bytes)
            } finally {
                bytes.fill(0)
            }
        }
        if (version == ATTACHMENT_VERSION) {
            writeEncrypted(cipher, encrypted, byteArrayOf(attachments.size.toByte()))
            attachments.forEach { (key, bytes) ->
                require(bytes.size in 1..ClinicalAttachment.MAX_ENVELOPE_BYTES)
                writeEntry(cipher, encrypted, key, bytes)
            }
        }
        encrypted.write(cipher.doFinal())
        check(encrypted.size() <= fileLimit(version))
        return encrypted.toByteArray()
    }

    fun decrypt(source: InputStream, password: CharArray): ClinicalBackup {
        val header = ByteArray(headerSize)
        val input = DataInputStream(source)
        input.readFully(header)
        require(header.copyOfRange(0, magic.size).contentEquals(magic))
        val version = header[magic.size].toInt()
        val limit = fileLimit(version)
        val saltStart = magic.size + 1
        val nonceStart = saltStart + SALT_BYTES
        val salt = header.copyOfRange(saltStart, nonceStart)
        val nonce = header.copyOfRange(nonceStart, headerSize)
        val ciphertext = readCiphertext(input, limit)
        val plain = cipher(Cipher.DECRYPT_MODE, password, salt, nonce, header).doFinal(ciphertext)
        try {
            return decode(plain, version)
        } finally {
            plain.fill(0)
        }
    }

    private fun fileLimit(version: Int): Int = when (version) {
        TEXT_VERSION -> MAX_V1_BYTES
        ATTACHMENT_VERSION -> MAX_V2_BYTES
        else -> throw UnsupportedBackupVersion()
    }

    private fun writeEntry(cipher: Cipher, output: ByteArrayOutputStream, key: String, bytes: ByteArray) {
        val prefix = ByteArrayOutputStream()
        DataOutputStream(prefix).use {
            it.writeUTF(key)
            it.writeInt(bytes.size)
        }
        writeEncrypted(cipher, output, prefix.toByteArray())
        writeEncrypted(cipher, output, bytes)
    }

    private fun writeEncrypted(cipher: Cipher, destination: ByteArrayOutputStream, bytes: ByteArray) {
        cipher.update(bytes)?.let(destination::write)
    }

    private fun readCiphertext(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(8_192)
        while (true) {
            val size = input.read(chunk)
            if (size == -1) break
            require(size > 0 && output.size() + size <= limit - headerSize)
            output.write(chunk, 0, size)
        }
        require(output.size() > TAG_BYTES)
        return output.toByteArray()
    }

    private fun decode(plain: ByteArray, version: Int): ClinicalBackup {
        val input = DataInputStream(ByteArrayInputStream(plain))
        val count = input.readUnsignedByte()
        require(count in 1..MAX_STORES)
        val payloads = buildMap {
            repeat(count) {
                val store = input.readUTF()
                val length = input.readInt()
                require(store !in this && length in 1..minOf(MAX_PAYLOAD_BYTES, input.available()))
                val bytes = ByteArray(length)
                try {
                    input.readFully(bytes)
                    put(store, bytes.decodeToString(throwOnInvalidSequence = true))
                } finally {
                    bytes.fill(0)
                }
            }
        }
        val attachments = mutableMapOf<String, ByteArray>()
        try {
            if (version == ATTACHMENT_VERSION) {
                val attachmentCount = input.readUnsignedByte()
                require(attachmentCount in 1..ClinicalAttachment.MAX_COUNT)
                var remaining = ClinicalAttachment.MAX_TOTAL_BYTES
                repeat(attachmentCount) {
                    val key = input.readUTF()
                    val length = input.readInt()
                    require(key !in attachments && length in 1..minOf(
                        ClinicalAttachment.MAX_ENVELOPE_BYTES, remaining, input.available(),
                    ))
                    remaining -= length
                    val bytes = ByteArray(length)
                    attachments[key] = bytes
                    input.readFully(bytes)
                }
            }
            require(input.available() == 0)
            return ClinicalBackup(payloads, attachments)
        } catch (failure: Throwable) {
            attachments.values.forEach { it.fill(0) }
            throw failure
        }
    }

    private fun cipher(mode: Int, password: CharArray, salt: ByteArray, nonce: ByteArray, header: ByteArray): Cipher {
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(65_536)
            .withIterations(3)
            .withParallelism(4)
            .withSalt(salt)
            .build()
        val key = ByteArray(32)
        try {
            Argon2BytesGenerator().apply {
                init(parameters)
                generateBytes(password, key)
            }
            return Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BYTES * 8, nonce))
                updateAAD(header)
            }
        } finally {
            key.fill(0)
        }
    }
}
