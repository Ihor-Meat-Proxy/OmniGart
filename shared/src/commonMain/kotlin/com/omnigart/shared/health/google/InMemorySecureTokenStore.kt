package com.omnigart.shared.health.google

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Test / preview token store (not for production secrets). */
class InMemorySecureTokenStore : SecureTokenStore {
    private val mutex = Mutex()
    private val map = mutableMapOf<String, String>()

    override suspend fun read(key: String): String? = mutex.withLock { map[key] }

    override suspend fun write(key: String, value: String) {
        mutex.withLock { map[key] = value }
    }

    override suspend fun delete(key: String) {
        mutex.withLock { map.remove(key) }
    }
}

/** Controllable browser for OAuth unit tests. */
class FakeAuthBrowser(var result: AuthBrowserResult = AuthBrowserResult.Cancelled("not configured")) : AuthBrowser {
    var lastAuthorizationUrl: String? = null
        private set

    override suspend fun authorize(authorizationUrl: String, redirectUri: String): AuthBrowserResult {
        lastAuthorizationUrl = authorizationUrl
        return result
    }
}
