package com.jecsdev.gestiora.backup

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

/** Version 1 fixes the KDF parameters. Untrusted headers cannot request more work. */
internal object BackupFile {
    private val magic = "GESTIORA".encodeToByteArray()
    private val random = SecureRandom()
    private const val VERSION = 1
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BYTES = 16
    private val headerSize = magic.size + 1 + SALT_BYTES + NONCE_BYTES
    private const val MAX_STORES = 9
    private const val MAX_PAYLOAD_BYTES = 3_000_000
    private const val MAX_FILE_BYTES = 27_001_024

    fun encrypt(payloads: Map<String, String>, password: CharArray): ByteArray {
        require(payloads.size in 1..MAX_STORES)
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val header = magic + byteArrayOf(VERSION.toByte()) + salt + nonce
        val cipher = cipher(Cipher.ENCRYPT_MODE, password, salt, nonce, header)
        val encrypted = ByteArrayOutputStream()
        encrypted.write(header)
        // Only ciphertext enters the output buffer. Separate encoded values are short-lived.
        writeEncrypted(cipher, encrypted, byteArrayOf(payloads.size.toByte()))
        payloads.forEach { (store, value) ->
            val prefix = ByteArrayOutputStream()
            val bytes = value.encodeToByteArray()
            try {
                require(bytes.size in 1..MAX_PAYLOAD_BYTES)
                DataOutputStream(prefix).use {
                    it.writeUTF(store)
                    it.writeInt(bytes.size)
                }
                writeEncrypted(cipher, encrypted, prefix.toByteArray())
                writeEncrypted(cipher, encrypted, bytes)
            } finally {
                bytes.fill(0)
            }
        }
        encrypted.write(cipher.doFinal())
        return encrypted.toByteArray().also { check(it.size <= MAX_FILE_BYTES) }
    }

    fun decrypt(source: InputStream, password: CharArray): Map<String, String> {
        val header = ByteArray(headerSize)
        val input = DataInputStream(source)
        input.readFully(header)
        require(header.copyOfRange(0, magic.size).contentEquals(magic))
        if (header[magic.size].toInt() != VERSION) throw UnsupportedBackupVersion()
        val saltStart = magic.size + 1
        val nonceStart = saltStart + SALT_BYTES
        val salt = header.copyOfRange(saltStart, nonceStart)
        val nonce = header.copyOfRange(nonceStart, headerSize)
        val ciphertext = readCiphertext(input)
        val plain = cipher(Cipher.DECRYPT_MODE, password, salt, nonce, header).doFinal(ciphertext)
        try {
            return decode(plain)
        } finally {
            plain.fill(0)
        }
    }

    private fun writeEncrypted(cipher: Cipher, destination: ByteArrayOutputStream, bytes: ByteArray) {
        cipher.update(bytes)?.let(destination::write)
    }

    private fun readCiphertext(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val chunk = ByteArray(8_192)
        while (true) {
            val size = input.read(chunk)
            if (size == -1) break
            require(size > 0 && output.size() + size <= MAX_FILE_BYTES - headerSize)
            output.write(chunk, 0, size)
        }
        require(output.size() > TAG_BYTES)
        return output.toByteArray()
    }

    private fun decode(plain: ByteArray): Map<String, String> {
        val input = DataInputStream(ByteArrayInputStream(plain))
        val count = input.readUnsignedByte()
        require(count in 1..MAX_STORES)
        return buildMap {
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
            require(input.available() == 0)
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
