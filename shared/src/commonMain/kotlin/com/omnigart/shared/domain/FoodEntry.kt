package com.omnigart.shared.domain

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Offline food log row (manual entry). Totals are wellness coaching aids, not medical advice.
 */
@Serializable
data class FoodEntry(
    val id: String,
    val profileId: String,
    val name: String,
    val energyKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val loggedAt: Instant
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(energyKcal >= 0) { "energyKcal must be non-negative" }
        require(proteinG >= 0) { "proteinG must be non-negative" }
        require(carbsG >= 0) { "carbsG must be non-negative" }
        require(fatG >= 0) { "fatG must be non-negative" }
    }
}

@Serializable
data class FoodDayTotals(val energyKcal: Double, val proteinG: Double, val carbsG: Double, val fatG: Double, val entryCount: Int)
