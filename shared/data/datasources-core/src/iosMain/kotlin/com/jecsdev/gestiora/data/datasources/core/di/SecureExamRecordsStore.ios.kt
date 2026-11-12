package com.jecsdev.gestiora.data.datasources.core.di

import com.jecsdev.gestiora.data.datasources.SecureExamRecordsStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

internal actual fun secureExamRecordsStoreModule(): Module = module {
    single<SecureExamRecordsStore> { IosSecureExamRecordsStore() }
}

/** A single foreground-only, non-migrating Keychain item for the pregnancy examRecords. */
@OptIn(ExperimentalForeignApi::class)
internal class IosSecureExamRecordsStore : SecureExamRecordsStore {
    override fun read(): String? = withQuery { query ->
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            when (status) {
                errSecItemNotFound -> null
                errSecSuccess -> {
                    val data: CFDataRef = result.value?.reinterpret()
                        ?: error("Keychain returned no examRecords data")
                    try {
                        val size = CFDataGetLength(data).toInt()
                        val bytes = CFDataGetBytePtr(data) ?: error("Keychain returned invalid examRecords data")
                        ByteArray(size) { bytes[it].toByte() }.decodeToString()
                    } finally {
                        CFRelease(data)
                    }
                }
                else -> error("Keychain examRecords read failed ($status)")
            }
        }
    }

    override fun write(payload: String): Unit = withQuery { query ->
        val encoded = payload.encodeToByteArray()
        check(encoded.isNotEmpty()) { "Secure examRecords payload is empty" }
        val value = encoded.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), encoded.size.toLong()) }
            ?: error("Unable to encode secure examRecords")
        try {
            val update = CFDictionaryCreateMutable(null, 0, null, null)
                ?: error("Unable to prepare Keychain update")
            try {
                CFDictionarySetValue(update, kSecValueData, value)
                CFDictionarySetValue(update, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                when (val status = SecItemUpdate(query, update)) {
                    errSecSuccess -> Unit
                    errSecItemNotFound -> {
                        CFDictionarySetValue(query, kSecValueData, value)
                        CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                        val addStatus = SecItemAdd(query, null)
                        check(addStatus == errSecSuccess) { "Keychain examRecords add failed ($addStatus)" }
                    }
                    else -> error("Keychain examRecords update failed ($status)")
                }
            } finally {
                CFRelease(update)
            }
        } finally {
            CFRelease(value)
        }
    }

    private inline fun <T> withQuery(block: (CFMutableDictionaryRef) -> T): T = memScoped {
        val service = CFStringCreateWithCString(null, "com.jecsdev.gestiora.examRecords".cstr.ptr, kCFStringEncodingUTF8)
            ?: error("Unable to prepare Keychain service")
        val account = CFStringCreateWithCString(null, "pregnancy-exams-v1".cstr.ptr, kCFStringEncodingUTF8)
            ?: error("Unable to prepare Keychain account")
        val query = CFDictionaryCreateMutable(null, 0, null, null)
            ?: error("Unable to prepare Keychain query")
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, service)
            CFDictionarySetValue(query, kSecAttrAccount, account)
            block(query)
        } finally {
            CFRelease(query)
            CFRelease(account)
            CFRelease(service)
        }
    }
}
