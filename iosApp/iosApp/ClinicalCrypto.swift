import Argon2
import CryptoKit
import Foundation
import Security
import Shared

final class ClinicalCrypto: NSObject, IosClinicalCrypto {
    private static let payloadVersion = Data("GCP1".utf8)
    private static let backupMagic = Data("GESTIORA".utf8)
    private let keyQuery: [String: Any] = [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: "com.jecsdev.gestiora.clinical",
        kSecAttrAccount as String: "payload-key-v1",
    ]

    static func databasePath() throws -> String {
        let library = try FileManager.default.url(
            for: .libraryDirectory, in: .userDomainMask, appropriateFor: nil, create: true
        )
        var directory = library.appendingPathComponent("ProtectedClinical", isDirectory: true)
        try FileManager.default.createDirectory(
            at: directory,
            withIntermediateDirectories: true,
            attributes: [.protectionKey: FileProtectionType.complete]
        )
        var attributes = URLResourceValues()
        attributes.isExcludedFromBackup = true
        try directory.setResourceValues(attributes)
        return directory.appendingPathComponent("clinical.db").path
    }

    func seal(plain: Data, authenticatedData: Data, allowNewKey: Bool) -> Data? {
        let prefix = Self.payloadVersion
        guard let key = loadKey(allowCreate: allowNewKey),
              let box = try? AES.GCM.seal(plain, using: key, authenticating: prefix + authenticatedData),
              let bytes = box.combined else { return nil }
        return prefix + bytes
    }

    func open(sealed: Data, authenticatedData: Data) -> Data? {
        let prefix = Self.payloadVersion
        guard let key = loadKey(allowCreate: false),
              sealed.starts(with: prefix),
              let box = try? AES.GCM.SealedBox(combined: sealed.dropFirst(prefix.count)) else { return nil }
        return try? AES.GCM.open(box, using: key, authenticating: prefix + authenticatedData)
    }

    func encryptBackup(plain: Data, password: Data, version: Int32) -> Data? {
        var salt = Data(count: 16)
        let status = salt.withUnsafeMutableBytes { buffer -> OSStatus in
            guard let address = buffer.baseAddress else { return errSecParam }
            return SecRandomCopyBytes(kSecRandomDefault, buffer.count, address)
        }
        guard status == errSecSuccess, version == 1 || version == 2,
              let key = try? backupKey(password: password, salt: salt) else { return nil }
        let nonce = AES.GCM.Nonce()
        let header = Self.backupMagic + Data([UInt8(version)]) + salt + Data(nonce)
        guard let box = try? AES.GCM.seal(plain, using: key, nonce: nonce, authenticating: header) else { return nil }
        return header + box.ciphertext + box.tag
    }

    func decryptBackup(file: Data, password: Data) -> Data? {
        let versionOffset = Self.backupMagic.count
        let saltEnd = versionOffset + 1 + 16
        let headerEnd = saltEnd + 12
        guard file.count > headerEnd + 16, file.starts(with: Self.backupMagic),
              file[versionOffset] == 1 || file[versionOffset] == 2 else { return nil }
        let salt = Data(file[(versionOffset + 1)..<saltEnd])
        guard let key = try? backupKey(password: password, salt: salt),
              let box = try? AES.GCM.SealedBox(combined: file.dropFirst(saltEnd)) else { return nil }
        return try? AES.GCM.open(box, using: key, authenticating: file.prefix(headerEnd))
    }

    private func backupKey(password: Data, salt: Data) throws -> SymmetricKey {
        var bytes = try Argon2.hash(
            password: password, salt: salt, iterations: 3, memory: 65_536, parallelism: 4
        )
        defer { bytes.resetBytes(in: bytes.startIndex..<bytes.endIndex) }
        return SymmetricKey(data: bytes)
    }

    func readBackup(url: URL, maxBytes: Int32) -> Data? {
        let scoped = url.startAccessingSecurityScopedResource()
        defer { if scoped { url.stopAccessingSecurityScopedResource() } }
        var bytes: Data?
        var error: NSError?
        NSFileCoordinator(filePresenter: nil).coordinate(readingItemAt: url, options: [], error: &error) { path in
            guard let handle = try? FileHandle(forReadingFrom: path) else { return }
            defer { try? handle.close() }
            var file = Data()
            while file.count <= Int(maxBytes) {
                let remaining = Int(maxBytes) + 1 - file.count
                do {
                    guard let chunk = try handle.read(upToCount: min(65_536, remaining)), !chunk.isEmpty else {
                        bytes = file
                        return
                    }
                    file.append(chunk)
                } catch { return }
            }
        }
        return error == nil ? bytes : nil
    }

    func writeBackup(file: Data, url: URL) -> Bool {
        do {
            try file.write(to: url, options: [.atomic, .completeFileProtection])
            return true
        } catch {
            return false
        }
    }

    private func loadKey(allowCreate: Bool) -> SymmetricKey? {
        var read = keyQuery
        read[kSecMatchLimit as String] = kSecMatchLimitOne
        read[kSecReturnData as String] = true
        var item: CFTypeRef?
        let status = SecItemCopyMatching(read as CFDictionary, &item)
        if status == errSecSuccess, let data = item as? Data, data.count == 32 {
            return SymmetricKey(data: data)
        }
        guard status == errSecItemNotFound && allowCreate else { return nil }

        let key = SymmetricKey(size: .bits256)
        var add = keyQuery
        add[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        add[kSecValueData as String] = key.withUnsafeBytes { Data($0) }
        switch SecItemAdd(add as CFDictionary, nil) {
        case errSecSuccess: return key
        case errSecDuplicateItem: return loadKey(allowCreate: false)
        default: return nil
        }
    }
}
