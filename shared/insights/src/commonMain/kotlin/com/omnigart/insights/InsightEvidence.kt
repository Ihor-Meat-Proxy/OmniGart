package com.omnigart.insights

/**
 * Stable insight claim ids and the evidence catalog entry ids that support them.
 * Resolve ⓘ sheets via [com.omnigart.evidence.EvidenceRepository.infoSheet].
 */
object InsightEvidence {
    const val DAILY_READINESS = "insight-daily-readiness"
    const val LOAD_TARGET = "insight-load-target"
    const val SLEEP_BALANCE_NEED = "insight-sleep-balance-need"
    const val SLEEP_BALANCE_DEBT = "insight-sleep-balance-debt"
    const val HABIT_EFFECT = "insight-habit-effect-caveats"
    const val OUT_OF_RANGE = "insight-out-of-range-baseline"
    const val WEEKLY_OVERVIEW = "insight-weekly-overview"
    const val DAY_RESERVE = "insight-day-reserve"
    const val FITNESS_AGE = "insight-fitness-age-vo2-norms"

    /** All insight claim ids that must resolve to catalog evidence. */
    val ALL: Set<String> =
        setOf(
            DAILY_READINESS,
            LOAD_TARGET,
            SLEEP_BALANCE_NEED,
            SLEEP_BALANCE_DEBT,
            HABIT_EFFECT,
            OUT_OF_RANGE,
            WEEKLY_OVERVIEW,
            DAY_RESERVE,
            FITNESS_AGE,
        )

    /**
     * Evidence catalog ids (snake_case) backing each insight claim.
     * Kept in-module so insights can ship atop the catalog without a separate claims registry.
     */
    val EVIDENCE_IDS_BY_CLAIM: Map<String, List<String>> =
        mapOf(
            DAILY_READINESS to
                listOf(
                    "plews_hrv_adaptation_2013",
                    "bellenger_hrv_status_2016",
                    "buchheit_hrv_monitoring_2014",
                    "al_haddad_hr_reliability_2011",
                    "watson_sleep_performance_2017",
                ),
            LOAD_TARGET to
                listOf(
                    "foster_srpe_2001",
                    "impellizzeri_acwr_2021",
                    "banister_trimp_1990",
                ),
            SLEEP_BALANCE_NEED to
                listOf(
                    "hirshkowitz_sleep_duration_2015",
                    "watson_sleep_performance_2017",
                ),
            SLEEP_BALANCE_DEBT to
                listOf(
                    "van_dongen_sleep_debt_2003",
                    "watson_sleep_performance_2017",
                ),
            HABIT_EFFECT to
                listOf(
                    "al_haddad_hr_reliability_2011",
                    "buchheit_hrv_monitoring_2014",
                ),
            OUT_OF_RANGE to
                listOf(
                    "buchheit_hrv_monitoring_2014",
                    "al_haddad_hr_reliability_2011",
                    "plews_hrv_adaptation_2013",
                ),
            WEEKLY_OVERVIEW to
                listOf(
                    "bellenger_hrv_status_2016",
                    "foster_srpe_2001",
                    "watson_sleep_performance_2017",
                ),
            DAY_RESERVE to
                listOf(
                    "buchheit_hrv_monitoring_2014",
                    "foster_srpe_2001",
                    "impellizzeri_acwr_2021",
                ),
            FITNESS_AGE to
                listOf(
                    "kaminsky_friend_vo2_2015",
                    "loe_hunt_vo2_2016",
                ),
        )

    fun evidenceIdsFor(claimId: String): List<String> =
        EVIDENCE_IDS_BY_CLAIM[claimId].orEmpty()
}

/** Result wrapper carrying the claim id for ⓘ linking. */
data class EvidencedInsight<T>(
    val value: T,
    val claimId: String,
    val disclaimer: String = WellnessDisclaimer.EN_GB,
)
