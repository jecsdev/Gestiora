package com.jecsdev.gestiora.presentation.ui

import com.jecsdev.gestiora.data.datasources.core.room.ClinicalPayloadCipher
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.posix.memcpy

/** CryptoKit is provided by the Swift host. Sealed data starts with a format version. */
interface IosClinicalCrypto {
    fun seal(plain: NSData, authenticatedData: NSData, allowNewKey: Boolean): NSData?
    fun open(sealed: NSData, authenticatedData: NSData): NSData?
    fun encryptBackup(plain: NSData, password: NSData, version: Int): NSData?
    fun decryptBackup(file: NSData, password: NSData): NSData?
    fun readBackup(url: NSURL, maxBytes: Int): NSData?
    fun writeBackup(file: NSData, url: NSURL): Boolean
}

internal class IosClinicalCipher(
    private val crypto: IosClinicalCrypto,
    private val hasCiphertext: () -> Boolean,
) : ClinicalPayloadCipher {
    override fun encryptBytes(store: String, payload: ByteArray): ByteArray =
        checkNotNull(crypto.seal(payload.toData(), store.encodeToByteArray().toData(), !hasCiphertext())) {
            "iOS clinical encryption failed"
        }.toBytes()

    override fun decryptBytes(store: String, ciphertext: ByteArray): ByteArray =
        checkNotNull(crypto.open(ciphertext.toData(), store.encodeToByteArray().toData())) {
            "iOS clinical authentication failed"
        }.toBytes()
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal fun ByteArray.toData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toBytes(): ByteArray = ByteArray(length.toInt()).also { result ->
    if (result.isNotEmpty()) result.usePinned { memcpy(it.addressOf(0), bytes, length) }
}
