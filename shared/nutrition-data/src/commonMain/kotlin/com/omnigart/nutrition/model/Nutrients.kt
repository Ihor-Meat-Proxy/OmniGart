package com.omnigart.nutrition.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Nutrient amounts normalized per 100 g (or 100 ml when [basis] is [NutrientBasis.PER_100ML]).
 * Energy is stored in kilocalories; [energyKj] is derived when absent (× 4.184).
 */
@Serializable
data class Nutrients(
    val basis: NutrientBasis = NutrientBasis.PER_100G,
    val energyKcal: Double? = null,
    val energyKj: Double? = null,
    val fatG: Double? = null,
    val saturatedFatG: Double? = null,
    val carbohydratesG: Double? = null,
    val sugarsG: Double? = null,
    val fiberG: Double? = null,
    val proteinsG: Double? = null,
    val saltG: Double? = null,
    val sodiumMg: Double? = null,
) {
    fun energyKjOrDerived(): Double? =
        energyKj ?: energyKcal?.times(KJ_PER_KCAL)

    fun energyKcalOrDerived(): Double? =
        energyKcal ?: energyKj?.div(KJ_PER_KCAL)

    /** Scale nutrients from the 100 g/ml basis to [gramsOrMl] of product. */
    fun forServing(gramsOrMl: Double): Nutrients {
        val factor = gramsOrMl / 100.0
        fun Double?.scale(): Double? = this?.times(factor)
        return copy(
            energyKcal = energyKcal.scale(),
            energyKj = energyKj.scale(),
            fatG = fatG.scale(),
            saturatedFatG = saturatedFatG.scale(),
            carbohydratesG = carbohydratesG.scale(),
            sugarsG = sugarsG.scale(),
            fiberG = fiberG.scale(),
            proteinsG = proteinsG.scale(),
            saltG = saltG.scale(),
            sodiumMg = sodiumMg.scale(),
        )
    }

    companion object {
        const val KJ_PER_KCAL: Double = 4.184
    }
}

@Serializable
enum class NutrientBasis {
    @SerialName("per_100g")
    PER_100G,

    @SerialName("per_100ml")
    PER_100ML,
}
