package com.omnigart.android

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
        composeRule.setContent {
            OmniGartTheme {
                OmniGartPhoneShell(
                    summaryLines = listOf("OmniGart", "BMR 1349 kcal")
                )
            }
        }
        composeRule.onNodeWithTag("phone_shell").assertIsDisplayed()
        composeRule.onNodeWithTag("brand_title").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_profile").assertIsDisplayed()
        composeRule.onNodeWithTag("tab_food").assertIsDisplayed()
    }
}
