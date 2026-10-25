package com.omnigart.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.omnigart.shared.OmniGartCore
import com.omnigart.shared.data.DatabaseDriverFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneShellSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shellShowsBrandAndTabs() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val core = OmniGartCore(DatabaseDriverFactory(context))
        val profile = core.activeProfile()
        composeRule.setContent {
            OmniGartTheme {
                OmniGartPhoneShell(core = core, profileId = profile.id)
            }
        }
        composeRule.onNodeWithTag("phone_shell").assertIsDisplayed()
        composeRule.onNodeWithTag("brand_title").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_profile").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_food").assertIsDisplayed()
        composeRule.onNodeWithTag("calc_settings").performScrollTo().assertIsDisplayed()
    }
}
