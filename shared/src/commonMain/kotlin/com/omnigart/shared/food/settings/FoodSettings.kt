package com.omnigart.shared.food.settings

import kotlinx.serialization.Serializable

@Serializable
data class FoodSettings(
    val profileId: String,
    /** When true, never call online Open Food Facts / Search-a-licious. */
    val offlineOnly: Boolean = false,
    /** Optional on-device AI morning food review (off by default). */
    val morningAiReviewEnabled: Boolean = false,
    /** Local minutes after midnight for the morning review notification. */
    val morningReviewMinutes: Int = 8 * 60,
    /**
     * Start-of-day minutes; prefer [com.omnigart.shared.food.targets.NutritionTargetsReader.dayStartMinutes]
     * when the nutrition-model PR is wired. Kept here so food logging can coordinate.
     */
    val dayStartMinutes: Int? = null,
    /** User overrides for micronutrient reference intakes (nutrient raw → amount). */
    val referenceOverrides: Map<String, Double> = emptyMap()
)
