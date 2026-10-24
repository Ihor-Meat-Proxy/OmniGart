package com.omnigart.shared.food.nutrients

import com.omnigart.shared.food.models.CatalogueFood

enum class NutrientRankBasis {
    PER_100_KCAL,
    PER_100_G
}

data class FoodNutrientRank(val food: CatalogueFood, val nutrientAmount: Double, val score: Double, val basis: NutrientRankBasis)

/**
 * Ranks catalogue foods as "best sources" for a nutrient.
 * Filters by optional category substring (case-insensitive).
 */
object BestSources {
    fun rank(
        foods: List<CatalogueFood>,
        nutrient: NutrientId,
        basis: NutrientRankBasis = NutrientRankBasis.PER_100_KCAL,
        categoryFilter: String? = null,
        limit: Int = 20
    ): List<FoodNutrientRank> {
        val filter = categoryFilter?.trim()?.lowercase()
        return foods
            .asSequence()
            .filter { food ->
                filter.isNullOrEmpty() ||
                    food.category?.lowercase()?.contains(filter) == true
            }.mapNotNull { food ->
                val amount = food.microsPer100g[nutrient.raw] ?: return@mapNotNull null
                if (amount <= 0) return@mapNotNull null
                val score =
                    when (basis) {
                        NutrientRankBasis.PER_100_G -> amount
                        NutrientRankBasis.PER_100_KCAL -> {
                            val kcal = food.energyKcalPer100g
                            if (kcal <= 0) amount else amount / kcal * 100.0
                        }
                    }
                FoodNutrientRank(food, amount, score, basis)
            }.sortedByDescending { it.score }
            .take(limit)
            .toList()
    }

    /**
     * "Fill my gaps" — for each under-filled nutrient, suggest top sources.
     */
    fun fillGaps(
        gaps: List<NutrientGap>,
        foods: List<CatalogueFood>,
        minPercentGap: Double = 80.0,
        perNutrient: Int = 3
    ): Map<NutrientId, List<FoodNutrientRank>> {
        val under =
            gaps.filter { it.percentOfReference < minPercentGap }
        return under.associate { gap ->
            gap.nutrient to
                rank(
                    foods = foods,
                    nutrient = gap.nutrient,
                    basis = NutrientRankBasis.PER_100_KCAL,
                    limit = perNutrient
                )
        }
    }
}
