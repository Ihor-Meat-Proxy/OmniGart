package com.omnigart.shared.health.google

import com.omnigart.shared.health.BodyMeasurement
import com.omnigart.shared.health.HealthWorkout
import com.omnigart.shared.health.HeartRateSample
import com.omnigart.shared.health.HrvSample
import com.omnigart.shared.health.NutritionEntry
import com.omnigart.shared.health.SleepSession
import com.omnigart.shared.health.TimeRange
import com.omnigart.shared.health.WaterIntake
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * KMP Google Health API client (OAuth PKCE + polling). No app backend.
 *
 * Endpoint shape follows the Fitbit Web API under Google (documented assumption).
 */
class GoogleHealthApiClient(
    private val config: GoogleHealthOAuthConfig,
    private val browser: AuthBrowser,
    private val tokenStore: SecureTokenStore,
    private val http: HttpClient = defaultHttpClient(),
    private val apiBaseUrl: String = DEFAULT_API_BASE,
    private val clock: Clock = Clock.System
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    suspend fun isSignedIn(): Boolean = currentAccessToken() != null

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun signIn(): Boolean {
        val verifier = pkceVerifier()
        val challenge = pkceChallenge(verifier)
        tokenStore.write(KEY_PKCE, verifier)
        val url =
            URLBuilder()
                .takeFrom(config.authorizationEndpoint)
                .apply {
                    parameters.append("response_type", "code")
                    parameters.append("client_id", config.clientId)
                    parameters.append("redirect_uri", config.redirectUri)
                    parameters.append("scope", config.scopes.joinToString(" "))
                    parameters.append("code_challenge", challenge)
                    parameters.append("code_challenge_method", "S256")
                }.buildString()
        return when (val result = browser.authorize(url, config.redirectUri)) {
            is AuthBrowserResult.Success -> {
                val code = extractQueryParam(result.redirectUri, "code") ?: return false
                exchangeCode(code, verifier)
                true
            }
            else -> false
        }
    }

    suspend fun signOut() {
        tokenStore.delete(KEY_TOKENS)
        tokenStore.delete(KEY_PKCE)
    }

    suspend fun refreshIfNeeded(): OAuthTokenSet? {
        val tokens = loadTokens() ?: return null
        val now = clock.now().toEpochMilliseconds()
        val expires = tokens.expiresAtEpochMs
        if (expires != null && now < expires - 60_000) return tokens
        val refresh = tokens.refreshToken ?: return tokens
        return refreshTokens(refresh)
    }

    suspend fun pollHeartRate(range: TimeRange): List<HeartRateSample> {
        val token = requireToken()
        // Simplified polling surface — real mapping depends on provider payloads.
        val text =
            http.get(
                "$apiBaseUrl/1/user/-/activities/heart/date/${dateKey(
                    range.start
                )}/${dateKey(range.end)}.json"
            ) {
                bearerAuth(token.accessToken)
            }.bodyAsText()
        return parseHeartRateFallback(text, range)
    }

    suspend fun pollSleep(range: TimeRange): List<SleepSession> {
        requireToken()
        // Placeholder empty until provider schemas are locked (OPEN_QUESTIONS).
        return emptyList()
    }

    suspend fun pollWorkouts(range: TimeRange): List<HealthWorkout> {
        requireToken()
        return emptyList()
    }

    suspend fun pollNutrition(range: TimeRange): List<NutritionEntry> {
        requireToken()
        return emptyList()
    }

    suspend fun pollWater(range: TimeRange): List<WaterIntake> {
        requireToken()
        return emptyList()
    }

    suspend fun pollBody(range: TimeRange): List<BodyMeasurement> {
        requireToken()
        return emptyList()
    }

    suspend fun pollHrv(range: TimeRange): List<HrvSample> {
        requireToken()
        return emptyList()
    }

    private suspend fun requireToken(): OAuthTokenSet = refreshIfNeeded() ?: error("Not signed in to Google Health API")

    private suspend fun currentAccessToken(): String? = loadTokens()?.accessToken

    private suspend fun loadTokens(): OAuthTokenSet? {
        val raw = tokenStore.read(KEY_TOKENS) ?: return null
        return runCatching { json.decodeFromString(OAuthTokenSetSurrogate.serializer(), raw) }
            .getOrNull()
            ?.toModel()
    }

    private suspend fun saveTokens(tokens: OAuthTokenSet) {
        tokenStore.write(
            KEY_TOKENS,
            json.encodeToString(OAuthTokenSetSurrogate.serializer(), tokens.toSurrogate())
        )
    }

    private suspend fun exchangeCode(code: String, verifier: String) {
        val response =
            http.submitForm(
                url = config.tokenEndpoint,
                formParameters =
                Parameters.build {
                    append("client_id", config.clientId)
                    append("grant_type", "authorization_code")
                    append("redirect_uri", config.redirectUri)
                    append("code", code)
                    append("code_verifier", verifier)
                }
            ) {
                header("Accept", "application/json")
            }.body<TokenResponse>()
        saveTokens(response.toTokenSet(clock))
    }

    private suspend fun refreshTokens(refreshToken: String): OAuthTokenSet {
        val response =
            http.submitForm(
                url = config.tokenEndpoint,
                formParameters =
                Parameters.build {
                    append("client_id", config.clientId)
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                }
            ) {
                header("Accept", "application/json")
            }.body<TokenResponse>()
        val set = response.toTokenSet(clock, fallbackRefresh = refreshToken)
        saveTokens(set)
        return set
    }

    private fun parseHeartRateFallback(body: String, range: TimeRange): List<HeartRateSample> {
        // Until provider JSON is finalized, do not invent samples from opaque payloads.
        if (body.isBlank()) return emptyList()
        return emptyList()
    }

    private fun dateKey(instant: kotlin.time.Instant): String {
        // YYYY-MM-DD UTC — sufficient for daily poll endpoints.
        val epochDay = instant.toEpochMilliseconds() / 86_400_000L
        val days = epochDay
        // Use kotlinx-datetime via Instant only; format simply from epoch is messy —
        // callers should prefer typed DTOs once schema is locked. Placeholder:
        return days.toString()
    }

    companion object {
        const val DEFAULT_API_BASE = "https://api.fitbit.com"
        private const val KEY_TOKENS = "google_health.tokens"
        private const val KEY_PKCE = "google_health.pkce"

        fun defaultHttpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                )
            }
        }
    }
}

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val scope: String? = null,
    @SerialName("token_type") val tokenType: String? = null
)

private fun TokenResponse.toTokenSet(clock: Clock, fallbackRefresh: String? = null): OAuthTokenSet {
    val expires =
        expiresIn?.let { clock.now().toEpochMilliseconds() + it * 1000 }
    return OAuthTokenSet(
        accessToken = accessToken,
        refreshToken = refreshToken ?: fallbackRefresh,
        expiresAtEpochMs = expires,
        scope = scope,
        tokenType = tokenType ?: "Bearer"
    )
}

@Serializable
private data class OAuthTokenSetSurrogate(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAtEpochMs: Long? = null,
    val scope: String? = null,
    val tokenType: String = "Bearer"
)

private fun OAuthTokenSet.toSurrogate() = OAuthTokenSetSurrogate(accessToken, refreshToken, expiresAtEpochMs, scope, tokenType)

private fun OAuthTokenSetSurrogate.toModel() = OAuthTokenSet(accessToken, refreshToken, expiresAtEpochMs, scope, tokenType)

@OptIn(ExperimentalEncodingApi::class)
private fun pkceVerifier(): String {
    val bytes = Random.Default.nextBytes(32)
    return Base64.UrlSafe.encode(bytes).trimEnd('=')
}

@OptIn(ExperimentalEncodingApi::class)
private fun pkceChallenge(verifier: String): String {
    // S256: BASE64URL(SHA256(verifier)). Use a tiny pure-Kotlin SHA-256 for KMP.
    val hash = Sha256Common.digest(verifier.encodeToByteArray())
    return Base64.UrlSafe.encode(hash).trimEnd('=')
}

private fun extractQueryParam(uri: String, name: String): String? {
    val qIndex =
        uri.indexOf('?').takeIf { it >= 0 } ?: uri.indexOf('#').takeIf { it >= 0 } ?: return null
    val query = uri.substring(qIndex + 1)
    return query.split('&').mapNotNull {
        val parts = it.split('=', limit = 2)
        if (parts.size == 2 && parts[0] == name) parts[1] else null
    }.firstOrNull()
}
