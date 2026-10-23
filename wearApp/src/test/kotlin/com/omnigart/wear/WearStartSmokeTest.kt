package com.omnigart.wear

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w227dp-h227dp-small-notlong-round-watch")
class WearStartSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun startScreenShowsBrand() {
        composeRule.onNodeWithTag("wear_start").assertIsDisplayed()
        composeRule.onNodeWithTag("wear_brand").assertIsDisplayed()
        composeRule.onNodeWithTag("wear_start_button").assertIsDisplayed()
    }
}
