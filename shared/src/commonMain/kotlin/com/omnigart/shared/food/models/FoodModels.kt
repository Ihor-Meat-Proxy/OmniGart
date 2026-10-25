package com.omnigart.shared.food.models

import com.omnigart.shared.i18n.OmniLocales
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Provenance of a logged food row.
 * Neutral ids only — never competitor product names.
 */
@Serializable
enum class FoodSource {
    CUSTOM,
    USDA,
    OPEN_FOOD_FACTS
}

@Serializable
enum class MealGroup {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACKS,
    CUSTOM
}

/**
 * Rich diary row. Extends the Phase 0 [com.omnigart.shared.domain.FoodEntry] macros
 * with meal grouping, provenance, and optional micronutrients (per logged serving).
 */
@Serializable
data class FoodLogEntry(
    val id: String,
    val profileId: String,
    val name: String,
    val mealGroup: MealGroup = MealGroup.SNACKS,
    val customMealLabel: String? = null,
    val source: FoodSource = FoodSource.CUSTOM,
    val barcode: String? = null,
    val catalogueId: String? = null,
    val servingGrams: Double? = null,
    val energyKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fibreG: Double = 0.0,
    val sugarsG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    /** Micronutrient amounts keyed by [NutrientId].raw (mg/µg as defined by the nutrient). */
    val micros: Map<String, Double> = emptyMap(),
    val favourite: Boolean = false,
    val loggedAt: Instant
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(energyKcal >= 0 && proteinG >= 0 && carbsG >= 0 && fatG >= 0) {
            "macros must be non-negative"
        }
    }
}

@Serializable
data class FoodLogDayTotals(
    val energyKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fibreG: Double = 0.0,
    val sugarsG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val micros: Map<String, Double> = emptyMap(),
    val entryCount: Int = 0
) {
    companion object {
        fun from(entries: List<FoodLogEntry>): FoodLogDayTotals {
            if (entries.isEmpty()) return FoodLogDayTotals()
            val micros = linkedMapOf<String, Double>()
            for (e in entries) {
                for ((k, v) in e.micros) {
                    micros[k] = (micros[k] ?: 0.0) + v
                }
            }
            return FoodLogDayTotals(
                energyKcal = entries.sumOf { it.energyKcal },
                proteinG = entries.sumOf { it.proteinG },
                carbsG = entries.sumOf { it.carbsG },
                fatG = entries.sumOf { it.fatG },
                fibreG = entries.sumOf { it.fibreG },
                sugarsG = entries.sumOf { it.sugarsG },
                sodiumMg = entries.sumOf { it.sodiumMg },
                micros = micros,
                entryCount = entries.size
            )
        }
    }
}

@Serializable
data class CustomFood(
    val id: String,
    val profileId: String,
    val name: String,
    val energyKcalPer100g: Double,
    val proteinGPer100g: Double = 0.0,
    val carbsGPer100g: Double = 0.0,
    val fatGPer100g: Double = 0.0,
    val fibreGPer100g: Double = 0.0,
    val defaultServingG: Double = 100.0,
    val barcode: String? = null,
    val source: FoodSource = FoodSource.CUSTOM
)

@Serializable
data class CatalogueFood(
    val id: String,
    val name: String,
    val brands: String? = null,
    val barcode: String? = null,
    val category: String? = null,
    val energyKcalPer100g: Double,
    val proteinGPer100g: Double = 0.0,
    val carbsGPer100g: Double = 0.0,
    val fatGPer100g: Double = 0.0,
    val fibreGPer100g: Double = 0.0,
    val sugarsGPer100g: Double = 0.0,
    val sodiumMgPer100g: Double = 0.0,
    val microsPer100g: Map<String, Double> = emptyMap(),
    val source: FoodSource,
    /** Localised display names keyed by BCP-47 (en-GB, uk, …). */
    val namesByLocale: Map<String, String> = emptyMap()
) {
    fun displayName(locale: String): String {
        val resolved = OmniLocales.resolve(locale)
        return namesByLocale[resolved]
            ?: namesByLocale[resolved.substringBefore('-')]
            ?: namesByLocale["en-GB"]
            ?: name
    }

    fun forGrams(grams: Double): ServingNutrients {
        val f = grams / 100.0
        return ServingNutrients(
            energyKcal = energyKcalPer100g * f,
            proteinG = proteinGPer100g * f,
            carbsG = carbsGPer100g * f,
            fatG = fatGPer100g * f,
            fibreG = fibreGPer100g * f,
            sugarsG = sugarsGPer100g * f,
            sodiumMg = sodiumMgPer100g * f,
            micros = microsPer100g.mapValues { it.value * f }
        )
    }
}

@Serializable
data class ServingNutrients(
    val energyKcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fibreG: Double = 0.0,
    val sugarsG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val micros: Map<String, Double> = emptyMap()
)

/** Quick-add of energy/macros without a catalogue row. */
@Serializable
data class QuickMacroAdd(
    val name: String,
    val energyKcal: Double,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0
)

/** Quick serving presets for catalogue foods. */
enum class QuickServing {
    ONE_SERVING,
    PER_100_G,
    PACK
}

fun CatalogueFood.gramsFor(serving: QuickServing, packGrams: Double = 100.0): Double = when (serving) {
    QuickServing.ONE_SERVING -> 100.0
    QuickServing.PER_100_G -> 100.0
    QuickServing.PACK -> packGrams.coerceAtLeast(1.0)
}
