package com.omnigart.android.food

import com.omnigart.shared.food.review.OnDeviceAiAvailability
import com.omnigart.shared.food.review.OnDeviceAiGenerator

/**
 * Gemini Nano / ML Kit GenAI availability for morning food review.
 * Fully on-device; no cloud fallback. Returns not-ready until AICore is product-wired.
 */
object FoodGeminiNanoAvailability {
    fun check(): OnDeviceAiAvailability = OnDeviceAiAvailability(
        deviceEligible = false,
        featureEnabled = false,
        modelReady = false,
        reasonIfUnavailable = "On-device model not available on this device"
    )

    fun generator(): OnDeviceAiGenerator? = null
}
