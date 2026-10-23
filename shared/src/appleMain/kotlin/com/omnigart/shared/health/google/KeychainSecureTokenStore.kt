package com.omnigart.shared.health.google

/**
 * Apple token storage is provided by Swift Keychain code and registered at app launch.
 * Kotlin keeps a process-wide delegate so [GoogleHealthApiClient] stays platform-agnostic.
 *
 * Unverified on Linux CI — wire [AppleSecureTokenStoreRegistry.delegate] from Swift.
 */
object AppleSecureTokenStoreRegistry {
    var delegate: SecureTokenStore = InMemorySecureTokenStore()
}

class KeychainSecureTokenStore : SecureTokenStore {
    override suspend fun read(key: String): String? = AppleSecureTokenStoreRegistry.delegate.read(key)

    override suspend fun write(key: String, value: String) {
        AppleSecureTokenStoreRegistry.delegate.write(key, value)
    }

    override suspend fun delete(key: String) {
        AppleSecureTokenStoreRegistry.delegate.delete(key)
    }
}
