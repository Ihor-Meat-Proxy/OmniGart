package com.omnigart.shared.health.google

/**
 * Platform hooks for OAuth (system browser / AppAuth) and secure token storage.
 * No OmniGart server — tokens stay on device (Keychain / EncryptedSharedPreferences).
 */
interface AuthBrowser {
    /**
     * Opens an authorization URL and returns the redirect URI query/fragment payload
     * after the user completes (or cancels) the flow.
     */
    suspend fun authorize(authorizationUrl: String, redirectUri: String): AuthBrowserResult
}

sealed class AuthBrowserResult {
    data class Success(val redirectUri: String) : AuthBrowserResult()

    data class Cancelled(val reason: String? = null) : AuthBrowserResult()

    data class Failed(val message: String) : AuthBrowserResult()
}

interface SecureTokenStore {
    suspend fun read(key: String): String?

    suspend fun write(key: String, value: String)

    suspend fun delete(key: String)
}

data class OAuthTokenSet(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAtEpochMs: Long?,
    val scope: String?,
    val tokenType: String = "Bearer"
)

data class GoogleHealthOAuthConfig(
    val clientId: String,
    val redirectUri: String,
    val authorizationEndpoint: String = DEFAULT_AUTH_ENDPOINT,
    val tokenEndpoint: String = DEFAULT_TOKEN_ENDPOINT,
    val scopes: Set<String> = DEFAULT_SCOPES
) {
    companion object {
        /**
         * Assumed consumer Google Health / Fitbit Web API endpoints (see docs/OPEN_QUESTIONS.md).
         * Base API host is configured separately on [GoogleHealthApiClient].
         */
        const val DEFAULT_AUTH_ENDPOINT = "https://www.fitbit.com/oauth2/authorize"
        const val DEFAULT_TOKEN_ENDPOINT = "https://api.fitbit.com/oauth2/token"
        val DEFAULT_SCOPES: Set<String> =
            setOf(
                "activity",
                "heartrate",
                "sleep",
                "nutrition",
                "weight",
                "location",
                "profile"
            )
    }
}
