package com.omnigart.shared.food.nutrients

/**
 * Micronutrient identifiers used for reference intakes and gap analysis.
 * Units: see [NutrientMeta.unit].
 */
enum class NutrientId(val raw: String) {
    VITAMIN_A("vitamin_a_rae_ug"),
    VITAMIN_C("vitamin_c_mg"),
    VITAMIN_D("vitamin_d_ug"),
    VITAMIN_E("vitamin_e_mg"),
    VITAMIN_K("vitamin_k_ug"),
    THIAMIN("thiamin_mg"),
    RIBOFLAVIN("riboflavin_mg"),
    NIACIN("niacin_mg"),
    VITAMIN_B6("vitamin_b6_mg"),
    FOLATE("folate_ug"),
    VITAMIN_B12("vitamin_b12_ug"),
    CALCIUM("calcium_mg"),
    IRON("iron_mg"),
    MAGNESIUM("magnesium_mg"),
    PHOSPHORUS("phosphorus_mg"),
    POTASSIUM("potassium_mg"),
    ZINC("zinc_mg"),
    SELENIUM("selenium_ug"),
    IODINE("iodine_ug")
    ;

    companion object {
        fun fromRaw(raw: String): NutrientId? = entries.firstOrNull { it.raw == raw }
    }
}

enum class NutrientUnit { MG, UG }

data class NutrientMeta(
    val id: NutrientId,
    val unit: NutrientUnit,
    /** en-GB display label (UI strings live in i18n catalogs). */
    val labelEnGb: String
)

object NutrientCatalog {
    val all: List<NutrientMeta> =
        listOf(
            NutrientMeta(NutrientId.VITAMIN_A, NutrientUnit.UG, "Vitamin A (RAE)"),
            NutrientMeta(NutrientId.VITAMIN_C, NutrientUnit.MG, "Vitamin C"),
            NutrientMeta(NutrientId.VITAMIN_D, NutrientUnit.UG, "Vitamin D"),
            NutrientMeta(NutrientId.VITAMIN_E, NutrientUnit.MG, "Vitamin E"),
            NutrientMeta(NutrientId.VITAMIN_K, NutrientUnit.UG, "Vitamin K"),
            NutrientMeta(NutrientId.THIAMIN, NutrientUnit.MG, "Thiamin (B1)"),
            NutrientMeta(NutrientId.RIBOFLAVIN, NutrientUnit.MG, "Riboflavin (B2)"),
            NutrientMeta(NutrientId.NIACIN, NutrientUnit.MG, "Niacin (B3)"),
            NutrientMeta(NutrientId.VITAMIN_B6, NutrientUnit.MG, "Vitamin B6"),
            NutrientMeta(NutrientId.FOLATE, NutrientUnit.UG, "Folate"),
            NutrientMeta(NutrientId.VITAMIN_B12, NutrientUnit.UG, "Vitamin B12"),
            NutrientMeta(NutrientId.CALCIUM, NutrientUnit.MG, "Calcium"),
            NutrientMeta(NutrientId.IRON, NutrientUnit.MG, "Iron"),
            NutrientMeta(NutrientId.MAGNESIUM, NutrientUnit.MG, "Magnesium"),
            NutrientMeta(NutrientId.PHOSPHORUS, NutrientUnit.MG, "Phosphorus"),
            NutrientMeta(NutrientId.POTASSIUM, NutrientUnit.MG, "Potassium"),
            NutrientMeta(NutrientId.ZINC, NutrientUnit.MG, "Zinc"),
            NutrientMeta(NutrientId.SELENIUM, NutrientUnit.UG, "Selenium"),
            NutrientMeta(NutrientId.IODINE, NutrientUnit.UG, "Iodine")
        )
}
