package com.omnigart.body.fat

import com.omnigart.body.evidence.BodyEvidenceRegistry
import com.omnigart.body.models.BiologicalSex
import com.omnigart.body.models.BodyFatMethodId
import com.omnigart.body.models.BodyMeasurementSnapshot
import com.omnigart.body.models.SkinfoldSite

/**
 * Jackson–Pollock skinfold body-density equations + Siri %fat.
 * Men: Jackson & Pollock 1978 (DOI 10.1079/bjn19780152).
 * Women: Jackson, Pollock & Ward 1980 (DOI 10.1249/00005768-198023000-00009).
 */
object JacksonPollock3Strategy : BodyFatStrategy {
    override val id: BodyFatMethodId = BodyFatMethodId.JACKSON_POLLOCK_3

    private val maleSites =
        listOf(SkinfoldSite.CHEST, SkinfoldSite.ABDOMEN, SkinfoldSite.THIGH)
    private val femaleSites =
        listOf(SkinfoldSite.TRICEPS, SkinfoldSite.SUPRAILIAC, SkinfoldSite.THIGH)

    override fun estimate(snapshot: BodyMeasurementSnapshot): BodyFatEstimate =
        jacksonEstimate(snapshot, id, maleSites, femaleSites, threeSite = true)
}

object JacksonPollock7Strategy : BodyFatStrategy {
    override val id: BodyFatMethodId = BodyFatMethodId.JACKSON_POLLOCK_7

    private val sites =
        listOf(
            SkinfoldSite.CHEST,
            SkinfoldSite.MIDAXILLARY,
            SkinfoldSite.TRICEPS,
            SkinfoldSite.SUBSCAPULAR,
            SkinfoldSite.ABDOMEN,
            SkinfoldSite.SUPRAILIAC,
            SkinfoldSite.THIGH,
        )

    override fun estimate(snapshot: BodyMeasurementSnapshot): BodyFatEstimate =
        jacksonEstimate(snapshot, id, sites, sites, threeSite = false)
}

private fun jacksonEstimate(
    snapshot: BodyMeasurementSnapshot,
    methodId: BodyFatMethodId,
    maleSites: List<SkinfoldSite>,
    femaleSites: List<SkinfoldSite>,
    threeSite: Boolean,
): BodyFatEstimate {
    val age = snapshot.profile.ageYears.toDouble()
    val (sites, density) =
        when (snapshot.profile.sex) {
            BiologicalSex.MALE -> {
                val sum = sumSites(snapshot, maleSites) ?: return jpFail(methodId, "body.error.missing_skinfolds")
                val bd =
                    if (threeSite) {
                        1.10938 - (0.0008267 * sum) + (0.0000016 * sum * sum) - (0.0002574 * age)
                    } else {
                        1.112 - (0.00043499 * sum) + (0.00000055 * sum * sum) - (0.00028826 * age)
                    }
                maleSites to bd
            }
            BiologicalSex.FEMALE -> {
                val sum = sumSites(snapshot, femaleSites) ?: return jpFail(methodId, "body.error.missing_skinfolds")
                val bd =
                    if (threeSite) {
                        1.0994921 - (0.0009929 * sum) + (0.0000023 * sum * sum) - (0.0001392 * age)
                    } else {
                        1.097 - (0.00046971 * sum) + (0.00000056 * sum * sum) - (0.00012828 * age)
                    }
                femaleSites to bd
            }
            BiologicalSex.UNSPECIFIED -> return jpFail(methodId, "body.error.sex_required")
        }
    if (density <= 0.0) return jpFail(methodId, "body.error.invalid_density")
    val bf = siriBodyFatPercent(density)
    return BodyFatEstimate(
        methodId = methodId,
        bodyFatPercent = bf,
        notesEnGb =
            listOf(
                "Jackson–Pollock ${if (threeSite) "3" else "7"}-site body density with Siri %fat.",
                "Sites used: ${sites.joinToString { it.name.lowercase() }}.",
                "Caliper skill and hydration state affect results.",
            ),
        evidence = BodyEvidenceRegistry.forMethod(methodId),
        ok = true,
    )
}

private fun sumSites(
    snapshot: BodyMeasurementSnapshot,
    sites: List<SkinfoldSite>,
): Double? {
    var sum = 0.0
    for (site in sites) {
        val v = snapshot.skinfoldsMm[site] ?: return null
        if (v <= 0.0) return null
        sum += v
    }
    return sum
}

private fun jpFail(methodId: BodyFatMethodId, key: String) =
    BodyFatEstimate(
        methodId = methodId,
        bodyFatPercent = null,
        evidence = BodyEvidenceRegistry.forMethod(methodId),
        ok = false,
        errorKey = key,
    )
