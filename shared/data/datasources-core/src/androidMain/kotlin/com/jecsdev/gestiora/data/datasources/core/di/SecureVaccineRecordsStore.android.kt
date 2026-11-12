package com.jecsdev.gestiora.data.datasources.core.di

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.jecsdev.gestiora.data.datasources.SecureVaccineRecordsStore
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal actual fun secureVaccineRecordsStoreModule(): Module = module {
    single<SecureVaccineRecordsStore> { AndroidSecureVaccineRecordsStore(get<Context>()) }
}

/** VaccineRecords ciphertext stays in noBackupFilesDir; the non-exportable key stays in Android Keystore. */
internal class AndroidSecureVaccineRecordsStore(context: Context) : SecureVaccineRecordsStore {
    private val atomicFile = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))
    private val lock = Any()

    override fun read(): String? = synchronized(lock) {
        val encrypted = try {
            atomicFile.openRead().use { it.readBytes() }
        } catch (failure: FileNotFoundException) {
            if (atomicFile.baseFile.exists()) throw failure
            return@synchronized null
        }
        require(encrypted.size > HEADER.size + IV_SIZE + TAG_SIZE && encrypted.copyOfRange(0, HEADER.size).contentEquals(HEADER)) {
            "Unsupported or damaged secure vaccineRecords envelope"
        }
        val key = existingKey() ?: error("Secure vaccineRecords key unavailable")
        val iv = encrypted.copyOfRange(HEADER.size, HEADER.size + IV_SIZE)
        val ciphertext = encrypted.copyOfRange(HEADER.size + IV_SIZE, encrypted.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(HEADER)
        cipher.doFinal(ciphertext).decodeToString()
    }

    override fun write(payload: String): Unit = synchronized(lock) {
        // A restored/orphaned ciphertext must never be replaced with a newly generated key.
        val key = existingKey() ?: run {
            if (atomicFile.baseFile.exists()) error("Secure vaccineRecords key unavailable")
            createKey()
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(HEADER)
        val iv = cipher.iv
        check(iv.size == IV_SIZE) { "Unexpected secure vaccineRecords IV length" }
        val encrypted = HEADER + iv + cipher.doFinal(payload.encodeToByteArray())
        val stream = atomicFile.startWrite()
        try {
            stream.write(encrypted)
            atomicFile.finishWrite(stream)
        } catch (failure: Throwable) {
            atomicFile.failWrite(stream)
            throw failure
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun existingKey(): SecretKey? =
        (keyStore().getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey

    private fun createKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE_NAME = "pregnancy_vaccines.gpr"
        const val KEY_ALIAS = "com.jecsdev.gestiora.vaccineRecords.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE = 16
        const val TAG_BITS = 128
        val HEADER = byteArrayOf(0x47, 0x56, 0x43, 0x01)
    }
}
