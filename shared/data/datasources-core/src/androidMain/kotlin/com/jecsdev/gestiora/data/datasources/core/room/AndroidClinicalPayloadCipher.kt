package com.jecsdev.gestiora.data.datasources.core.room

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypt before Room sees a payload, so SQLite journals/WAL never receive clinical plaintext. */
internal class AndroidClinicalPayloadCipher(
    private val hasCiphertext: () -> Boolean,
    private val keyAlias: String = "com.jecsdev.gestiora.room.clinical.v1",
) : ClinicalPayloadCipher {
    private val lock = Any()

    override fun encrypt(store: String, payload: String): ByteArray = synchronized(lock) {
        val key = existingKey() ?: run {
            check(!hasCiphertext()) { "Clinical database key unavailable" }
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generator.generateKey()
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(HEADER + store.encodeToByteArray())
        check(cipher.iv.size == IV_SIZE)
        HEADER + cipher.iv + cipher.doFinal(payload.encodeToByteArray())
    }

    override fun decrypt(store: String, ciphertext: ByteArray): String = synchronized(lock) {
        require(ciphertext.size >= HEADER.size + IV_SIZE + 16 && ciphertext.take(HEADER.size).toByteArray().contentEquals(HEADER)) {
            "Unsupported or damaged clinical database envelope"
        }
        val key = existingKey() ?: error("Clinical database key unavailable")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, ciphertext.copyOfRange(HEADER.size, HEADER.size + IV_SIZE)))
        cipher.updateAAD(HEADER + store.encodeToByteArray())
        cipher.doFinal(ciphertext.copyOfRange(HEADER.size + IV_SIZE, ciphertext.size)).decodeToString(throwOnInvalidSequence = true)
    }

    private fun existingKey(): SecretKey? =
        (KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey

    private companion object {
        const val IV_SIZE = 12
        val HEADER = byteArrayOf(0x47, 0x52, 0x44, 0x01)
    }
}
