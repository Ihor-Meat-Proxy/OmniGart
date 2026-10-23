package com.omnigart.android.health

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.omnigart.shared.health.google.AuthBrowser
import com.omnigart.shared.health.google.AuthBrowserResult
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthorizationService

/**
 * Production [AuthBrowser] using AppAuth Custom Tabs for the system browser.
 * Host Activity must forward the redirect intent to [onRedirect].
 * Unverified end-to-end on Linux CI.
 */
class AppAuthBrowser(private val activity: ComponentActivity) : AuthBrowser {
    private var continuation: ((AuthBrowserResult) -> Unit)? = null
    private val authService by lazy { AuthorizationService(activity) }

    override suspend fun authorize(authorizationUrl: String, redirectUri: String): AuthBrowserResult = suspendCancellableCoroutine { cont ->
        continuation = { result ->
            if (cont.isActive) cont.resume(result)
        }
        // PKCE is applied by the KMP GoogleHealthApiClient; open that URL via AppAuth tabs.
        val customTabs = authService.createCustomTabsIntentBuilder().build()
        customTabs.launchUrl(activity, Uri.parse(authorizationUrl))
        cont.invokeOnCancellation { continuation = null }
    }

    fun onRedirect(intent: Intent) {
        val data = intent.data?.toString()
        val cb = continuation
        continuation = null
        if (data != null) {
            cb?.invoke(AuthBrowserResult.Success(data))
        } else {
            cb?.invoke(AuthBrowserResult.Cancelled("missing redirect data"))
        }
    }
}
