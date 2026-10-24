package com.omnigart.shared.food.review

import com.omnigart.shared.food.models.FoodLogDayTotals
import com.omnigart.shared.food.nutrients.NutrientGap
import com.omnigart.shared.food.nutrients.NutrientId
import com.omnigart.shared.food.targets.NutritionDayTargets
import kotlinx.serialization.Serializable

@Serializable
enum class ReviewSource {
    RULE_BASED,
    ON_DEVICE_AI
}

@Serializable
data class MorningFoodReview(
    val id: String,
    val profileId: String,
    /** Day that was analysed (yyyy-MM-dd). */
    val dayKey: String,
    val source: ReviewSource,
    val locale: String,
    val summary: String,
    val suggestions: List<String> = emptyMapSuggestions(),
    val groundedEnergyKcal: Double?,
    val groundedGaps: List<String> = emptyList(),
    val dismissed: Boolean = false,
    val createdAtEpochMs: Long,
    val notMedicalAdviceFooter: String
) {
    companion object {
        private fun emptyMapSuggestions(): List<String> = emptyList()
    }
}

/**
 * Availability of on-device generative models.
 * Platforms fill this from Apple Foundation Models / Gemini Nano checks.
 */
data class OnDeviceAiAvailability(
    val deviceEligible: Boolean,
    val featureEnabled: Boolean,
    val modelReady: Boolean,
    val reasonIfUnavailable: String? = null
) {
    val usable: Boolean get() = deviceEligible && featureEnabled && modelReady
}

/**
 * Prompt payload for on-device models — aggregated totals and food names only.
 * Models must not invent calories; output is grounded in these numbers.
 */
data class MorningReviewPromptInput(
    val locale: String,
    val dayKey: String,
    val foodNames: List<String>,
    val totals: FoodLogDayTotals,
    val targets: NutritionDayTargets,
    val gaps: List<NutrientGap>,
    val suppressDeficitLanguage: Boolean
)

object MorningReviewPromptBuilder {
    fun build(input: MorningReviewPromptInput): String {
        val gapLines =
            input.gaps
                .filter { it.percentOfReference < 80.0 }
                .take(8)
                .joinToString("\n") {
                    "- ${it.nutrient.raw}: ${fmt(it.consumed)} / ${fmt(it.reference)} " +
                        "(${fmt(it.percentOfReference)}%)"
                }
        val safety =
            if (input.suppressDeficitLanguage) {
                "Do not suggest eating less, calorie deficits, or weight loss. " +
                    "Offer gentle variety and nutrient-rich food ideas only."
            } else {
                "Use gentle wellness wording. Do not give medical advice."
            }
        return buildString {
            appendLine("You are OmniGart’s on-device morning food review.")
            appendLine("Respond in locale ${input.locale}.")
            appendLine(safety)
            appendLine("Never invent calorie or nutrient numbers; use only the figures below.")
            appendLine("Day: ${input.dayKey}")
            appendLine("Foods: ${input.foodNames.joinToString(", ").ifBlank { "(none)" }}")
            appendLine(
                "Totals — energy kcal: ${fmt(input.totals.energyKcal)}, " +
                    "protein g: ${fmt(input.totals.proteinG)}, " +
                    "carbs g: ${fmt(input.totals.carbsG)}, " +
                    "fat g: ${fmt(input.totals.fatG)}, " +
                    "fibre g: ${fmt(input.totals.fibreG)}"
            )
            appendLine(
                "Targets — energy kcal: ${input.targets.energyKcal?.let(::fmt) ?: "n/a"}, " +
                    "protein g: ${input.targets.proteinG?.let(::fmt) ?: "n/a"}, " +
                    "carbs g: ${input.targets.carbsG?.let(::fmt) ?: "n/a"}, " +
                    "fat g: ${input.targets.fatG?.let(::fmt) ?: "n/a"}"
            )
            appendLine("Nutrient gaps (consumed / reference):")
            appendLine(gapLines.ifBlank { "- none marked below 80%" })
            appendLine("Write a short summary and up to 3 gentle suggestions.")
        }
    }

    private fun fmt(v: Double): String = ((v * 10).toInt() / 10.0).toString()
}

/**
 * Non-AI fallback: totals vs targets and top nutrient gaps.
 */
object RuleBasedMorningReview {
    const val FOOTER_EN_GB: String =
        "This is a wellness summary, not medical advice."

    fun build(
        id: String,
        profileId: String,
        dayKey: String,
        locale: String,
        totals: FoodLogDayTotals,
        targets: NutritionDayTargets,
        gaps: List<NutrientGap>,
        foodNames: List<String>,
        suppressDeficitLanguage: Boolean,
        createdAtEpochMs: Long,
        footer: String = FOOTER_EN_GB
    ): MorningFoodReview {
        val energyLine =
            targets.energyKcal?.let { t ->
                "Energy logged ${fmt(totals.energyKcal)} kcal versus reference intake ${fmt(t)} kcal."
            } ?: "Energy logged ${fmt(totals.energyKcal)} kcal."
        val topGaps =
            gaps.filter { it.percentOfReference < 80.0 }.take(3)
        val gapSuggestions =
            topGaps.map {
                "Consider foods richer in ${label(it.nutrient)} " +
                    "(${fmt(it.percentOfReference)}% of reference intake yesterday)."
            }
        val variety =
            if (foodNames.size < 3 && !suppressDeficitLanguage) {
                listOf("Logging a few more meals can make daily patterns clearer over time.")
            } else {
                emptyList()
            }
        val suggestions =
            (gapSuggestions + variety).take(3).ifEmpty {
                listOf("Yesterday’s log looks balanced against your reference intakes.")
            }
        return MorningFoodReview(
            id = id,
            profileId = profileId,
            dayKey = dayKey,
            source = ReviewSource.RULE_BASED,
            locale = locale,
            summary = energyLine + " Foods: " +
                foodNames.take(8).joinToString(", ").ifBlank { "none logged" } + ".",
            suggestions = suggestions,
            groundedEnergyKcal = totals.energyKcal,
            groundedGaps = topGaps.map { "${it.nutrient.raw}:${fmt(it.percentOfReference)}" },
            createdAtEpochMs = createdAtEpochMs,
            notMedicalAdviceFooter = footer
        )
    }

    private fun label(id: NutrientId): String = id.raw.replace('_', ' ')

    private fun fmt(v: Double): String = ((v * 10).toInt() / 10.0).toString()
}

/** Sync on-device generator (Foundation Models / Gemini Nano). No cloud. */
fun interface OnDeviceAiGenerator {
    fun generate(prompt: String): String?
}

/**
 * Orchestrates morning review: prefers on-device AI when available and enabled;
 * otherwise rule-based. Never uses a cloud fallback.
 */
class MorningFoodReviewService(private val aiGenerate: OnDeviceAiGenerator? = null) {
    fun review(
        input: MorningReviewPromptInput,
        availability: OnDeviceAiAvailability,
        aiEnabled: Boolean,
        id: String,
        profileId: String,
        createdAtEpochMs: Long,
        footer: String = RuleBasedMorningReview.FOOTER_EN_GB
    ): MorningFoodReview {
        val suppress = input.suppressDeficitLanguage
        val generator = aiGenerate
        if (aiEnabled && availability.usable && generator != null) {
            val prompt = MorningReviewPromptBuilder.build(input)
            val text = runCatching { generator.generate(prompt) }.getOrNull()
            if (!text.isNullOrBlank()) {
                return MorningFoodReview(
                    id = id,
                    profileId = profileId,
                    dayKey = input.dayKey,
                    source = ReviewSource.ON_DEVICE_AI,
                    locale = input.locale,
                    summary = text.trim(),
                    suggestions = emptyList(),
                    groundedEnergyKcal = input.totals.energyKcal,
                    groundedGaps =
                    input.gaps
                        .filter { it.percentOfReference < 80.0 }
                        .take(5)
                        .map { "${it.nutrient.raw}:${it.percentOfReference}" },
                    createdAtEpochMs = createdAtEpochMs,
                    notMedicalAdviceFooter = footer
                )
            }
        }
        return RuleBasedMorningReview.build(
            id = id,
            profileId = profileId,
            dayKey = input.dayKey,
            locale = input.locale,
            totals = input.totals,
            targets = input.targets,
            gaps = input.gaps,
            foodNames = input.foodNames,
            suppressDeficitLanguage = suppress,
            createdAtEpochMs = createdAtEpochMs,
            footer = footer
        )
    }
}
