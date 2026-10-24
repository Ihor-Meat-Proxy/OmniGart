import CryptoKit
import Foundation
import Shared

/// CryptoKit AES-GCM seal for the food diary JSON blob (at-rest encryption).
enum FoodDiaryCrypto {
    static func makeEncryptor(keyData: Data) -> FoodAtRestEncryptor {
        AesGcmFoodEncryptor(key: SymmetricKey(data: keyData))
    }

    static func loadOrCreateKey() -> Data {
        let tag = "com.omnigart.food.diary.key"
        if let existing = KeychainFoodKey.load(tag: tag) {
            return existing
        }
        var bytes = [UInt8](repeating: 0, count: 32)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        let data = Data(bytes)
        KeychainFoodKey.save(tag: tag, data: data)
        return data
    }
}

private final class AesGcmFoodEncryptor: FoodAtRestEncryptor {
    private let key: SymmetricKey

    init(key: SymmetricKey) {
        self.key = key
    }

    func seal(plainUtf8: String) -> String {
        let data = Data(plainUtf8.utf8)
        guard let sealed = try? AES.GCM.seal(data, using: key),
              let combined = sealed.combined else {
            return plainUtf8
        }
        return combined.base64EncodedString()
    }

    func open(sealed: String) -> String {
        guard let raw = Data(base64Encoded: sealed),
              let box = try? AES.GCM.SealedBox(combined: raw),
              let plain = try? AES.GCM.open(box, using: key),
              let text = String(data: plain, encoding: .utf8) else {
            return "{}"
        }
        return text
    }
}

private enum KeychainFoodKey {
    static func save(tag: String, data: Data) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: tag,
            kSecValueData as String: data
        ]
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
    }

    static func load(tag: String) -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: tag,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess else { return nil }
        return item as? Data
    }
}
