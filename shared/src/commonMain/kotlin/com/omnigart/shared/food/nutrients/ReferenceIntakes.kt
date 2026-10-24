package com.omnigart.shared.food.nutrients

/**
 * Bundled adult reference intakes for wellness coaching.
 * Values approximate EFSA Dietary Reference Values and IOM/NIH DRIs for adults.
 * Wording in UI: "reference intake" — not medical advice.
 *
 * User overrides and upper limits are applied via [ReferenceIntakeResolver].
 */
enum class BiologicalSexBand { FEMALE, MALE, UNSPECIFIED }

enum class AgeBand {
    CHILD_1_3,
    CHILD_4_8,
    CHILD_9_13,
    TEEN_14_18,
    ADULT_19_50,
    ADULT_51_70,
    ADULT_70_PLUS
}

data class ReferenceIntake(
    val nutrient: NutrientId,
    /** Population reference intake / RDA-like amount. */
    val amount: Double,
    /** Tolerable upper intake level when published; null if none bundled. */
    val upperLimit: Double? = null
)

object BundledReferenceIntakes {
    /**
     * Adult 19–50 reference intakes (approximate blended EFSA PRI / IOM RDA).
     * Sex-specific iron differs; other values use shared adult figures.
     */
    fun forAdult(sex: BiologicalSexBand): Map<NutrientId, ReferenceIntake> {
        val iron =
            when (sex) {
                BiologicalSexBand.FEMALE -> 18.0
                BiologicalSexBand.MALE, BiologicalSexBand.UNSPECIFIED -> 8.0
            }
        return mapOf(
            NutrientId.VITAMIN_A to ReferenceIntake(NutrientId.VITAMIN_A, 700.0, 3_000.0),
            NutrientId.VITAMIN_C to ReferenceIntake(NutrientId.VITAMIN_C, 80.0, 2_000.0),
            NutrientId.VITAMIN_D to ReferenceIntake(NutrientId.VITAMIN_D, 15.0, 100.0),
            NutrientId.VITAMIN_E to ReferenceIntake(NutrientId.VITAMIN_E, 15.0, 1_000.0),
            NutrientId.VITAMIN_K to ReferenceIntake(NutrientId.VITAMIN_K, 90.0, null),
            NutrientId.THIAMIN to ReferenceIntake(NutrientId.THIAMIN, 1.1, null),
            NutrientId.RIBOFLAVIN to ReferenceIntake(NutrientId.RIBOFLAVIN, 1.3, null),
            NutrientId.NIACIN to ReferenceIntake(NutrientId.NIACIN, 16.0, 35.0),
            NutrientId.VITAMIN_B6 to ReferenceIntake(NutrientId.VITAMIN_B6, 1.3, 100.0),
            NutrientId.FOLATE to ReferenceIntake(NutrientId.FOLATE, 400.0, 1_000.0),
            NutrientId.VITAMIN_B12 to ReferenceIntake(NutrientId.VITAMIN_B12, 2.4, null),
            NutrientId.CALCIUM to ReferenceIntake(NutrientId.CALCIUM, 1_000.0, 2_500.0),
            NutrientId.IRON to ReferenceIntake(NutrientId.IRON, iron, 45.0),
            NutrientId.MAGNESIUM to ReferenceIntake(NutrientId.MAGNESIUM, 320.0, 350.0),
            NutrientId.PHOSPHORUS to ReferenceIntake(NutrientId.PHOSPHORUS, 700.0, 4_000.0),
            NutrientId.POTASSIUM to ReferenceIntake(NutrientId.POTASSIUM, 3_400.0, null),
            NutrientId.ZINC to ReferenceIntake(NutrientId.ZINC, 11.0, 40.0),
            NutrientId.SELENIUM to ReferenceIntake(NutrientId.SELENIUM, 55.0, 400.0),
            NutrientId.IODINE to ReferenceIntake(NutrientId.IODINE, 150.0, 1_100.0)
        )
    }

    fun ageBand(ageYears: Int): AgeBand = when {
        ageYears < 4 -> AgeBand.CHILD_1_3
        ageYears < 9 -> AgeBand.CHILD_4_8
        ageYears < 14 -> AgeBand.CHILD_9_13
        ageYears < 19 -> AgeBand.TEEN_14_18
        ageYears < 51 -> AgeBand.ADULT_19_50
        ageYears < 71 -> AgeBand.ADULT_51_70
        else -> AgeBand.ADULT_70_PLUS
    }
}

class ReferenceIntakeResolver(private val overrides: Map<NutrientId, Double> = emptyMap()) {
    fun resolve(ageYears: Int, sex: BiologicalSexBand): Map<NutrientId, ReferenceIntake> {
        // Bundled tables focus on adults; other bands currently inherit adult values
        // scaled gently for children (wellness aid only).
        val base = BundledReferenceIntakes.forAdult(sex)
        val band = BundledReferenceIntakes.ageBand(ageYears)
        val scale =
            when (band) {
                AgeBand.CHILD_1_3 -> 0.4
                AgeBand.CHILD_4_8 -> 0.55
                AgeBand.CHILD_9_13 -> 0.75
                AgeBand.TEEN_14_18 -> 0.9
                AgeBand.ADULT_19_50 -> 1.0
                AgeBand.ADULT_51_70 -> 1.0
                AgeBand.ADULT_70_PLUS -> 1.0
            }
        return base.mapValues { (id, intake) ->
            val amount = overrides[id] ?: (intake.amount * scale)
            intake.copy(amount = amount)
        }
    }
}

data class NutrientGap(
    val nutrient: NutrientId,
    val reference: Double,
    val consumed: Double,
    val gap: Double,
    val percentOfReference: Double
)

object NutrientGapAnalyzer {
    fun gaps(consumed: Map<String, Double>, reference: Map<NutrientId, ReferenceIntake>): List<NutrientGap> = reference.values
        .map { ri ->
            val c = consumed[ri.nutrient.raw] ?: 0.0
            val gap = (ri.amount - c).coerceAtLeast(0.0)
            val pct = if (ri.amount > 0) (c / ri.amount) * 100.0 else 0.0
            NutrientGap(ri.nutrient, ri.amount, c, gap, pct)
        }.sortedBy { it.percentOfReference }
}
