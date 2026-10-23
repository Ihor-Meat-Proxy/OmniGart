package com.omnigart.hydration.caffeine

/**
 * EFSA NDA (2015) Scientific Opinion on the safety of caffeine.
 * DOI 10.2903/j.efsa.2015.4102
 */
object CaffeineLimits {
    const val ADULT_DAILY_MG = 400.0
    const val ADULT_SINGLE_DOSE_MG = 200.0
    const val PREGNANCY_DAILY_MG = 200.0

    data class IntakeAssessment(
        val totalMg: Double,
        val largestSingleDoseMg: Double,
        val dailyLimitMg: Double,
        val singleDoseLimitMg: Double,
        val exceedsDaily: Boolean,
        val exceedsSingleDose: Boolean,
        val pregnancyMode: Boolean,
        val messagesEnGb: List<String>,
    )

    fun assess(
        dosesMg: List<Double>,
        pregnant: Boolean = false,
    ): IntakeAssessment {
        val total = dosesMg.sumOf { it.coerceAtLeast(0.0) }
        val largest = dosesMg.maxOfOrNull { it.coerceAtLeast(0.0) } ?: 0.0
        val dailyLimit = if (pregnant) PREGNANCY_DAILY_MG else ADULT_DAILY_MG
        val singleLimit = ADULT_SINGLE_DOSE_MG
        val exceedsDaily = total > dailyLimit
        val exceedsSingle = largest > singleLimit
        val messages = buildList {
            if (pregnant) {
                add(
                    "Pregnancy daily guidance: ≤ ${PREGNANCY_DAILY_MG.toInt()} mg caffeine from all sources " +
                        "(EFSA 2015; DOI 10.2903/j.efsa.2015.4102).",
                )
            } else {
                add(
                    "Adult daily guidance: ≤ ${ADULT_DAILY_MG.toInt()} mg; single dose ≤ ${ADULT_SINGLE_DOSE_MG.toInt()} mg " +
                        "(EFSA 2015; DOI 10.2903/j.efsa.2015.4102).",
                )
            }
            if (exceedsDaily) {
                add("Today’s total (${total.toInt()} mg) exceeds the ${dailyLimit.toInt()} mg daily guidance.")
            }
            if (exceedsSingle) {
                add(
                    "A single serving (${largest.toInt()} mg) exceeds the ${singleLimit.toInt()} mg single-dose guidance.",
                )
            }
        }
        return IntakeAssessment(
            totalMg = total,
            largestSingleDoseMg = largest,
            dailyLimitMg = dailyLimit,
            singleDoseLimitMg = singleLimit,
            exceedsDaily = exceedsDaily,
            exceedsSingleDose = exceedsSingle,
            pregnancyMode = pregnant,
            messagesEnGb = messages,
        )
    }
}
