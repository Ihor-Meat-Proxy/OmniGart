package com.omnigart.shared.health.google

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class GoogleHealthOAuthTest {
    @Test
    fun pkceSignInStoresTokens() = runTest {
        val store = InMemorySecureTokenStore()
        val browser =
            FakeAuthBrowser(
                result =
                AuthBrowserResult.Success(
                    "com.omnigart:/oauth2redirect?code=auth-code-1"
                )
            )
        val engine =
            MockEngine { request ->
                assertTrue(request.url.toString().contains("oauth2/token"))
                respond(
                    content =
                    """
                            {
                              "access_token":"access-1",
                              "refresh_token":"refresh-1",
                              "expires_in":3600,
                              "token_type":"Bearer",
                              "scope":"activity heartrate sleep"
                            }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        val http =
            HttpClient(engine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        val client =
            GoogleHealthApiClient(
                config =
                GoogleHealthOAuthConfig(
                    clientId = "test-client",
                    redirectUri = "com.omnigart:/oauth2redirect"
                ),
                browser = browser,
                tokenStore = store,
                http = http
            )
        assertTrue(client.signIn())
        assertTrue(client.isSignedIn())
        assertNotNull(browser.lastAuthorizationUrl)
        assertTrue(browser.lastAuthorizationUrl?.contains("code_challenge") == true)
        assertTrue(browser.lastAuthorizationUrl?.contains("activity") == true)
        client.signOut()
        assertFalse(client.isSignedIn())
    }

    @Test
    fun cancelledBrowserDoesNotSignIn() = runTest {
        val client =
            GoogleHealthApiClient(
                config =
                GoogleHealthOAuthConfig(
                    clientId = "test-client",
                    redirectUri = "com.omnigart:/oauth2redirect"
                ),
                browser = FakeAuthBrowser(AuthBrowserResult.Cancelled()),
                tokenStore = InMemorySecureTokenStore(),
                http = HttpClient(MockEngine { respond("", HttpStatusCode.OK) })
            )
        assertFalse(client.signIn())
        assertEquals(false, client.isSignedIn())
    }
}
