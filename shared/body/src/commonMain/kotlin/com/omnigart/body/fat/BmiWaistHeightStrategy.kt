package com.omnigart.body.fat

import com.omnigart.body.evidence.BodyEvidenceRegistry
import com.omnigart.body.models.BodyFatMethodId
import com.omnigart.body.models.BodyMeasurementSnapshot
import com.omnigart.body.models.BodyMetrics
import com.omnigart.body.models.CircumferenceSite

/**
 * BMI + waist-to-height ratio with **wellness framing** (not a body-fat % and not a diagnosis).
 *
 * WHtR ≥ 0.5 is a commonly cited screening threshold (Ashwell / Browning reviews).
 * BMI bands follow conventional adult cut-points for population messaging only.
 */
object BmiWaistHeightStrategy : BodyFatStrategy {
    override val id: BodyFatMethodId = BodyFatMethodId.BMI_WAIST_HEIGHT

    const val WHT_R_SCREENING_THRESHOLD = 0.5

    override fun estimate(snapshot: BodyMeasurementSnapshot): BodyFatEstimate {
        val weight = snapshot.weightKg
            ?: return fail("body.error.missing_weight")
        val height = snapshot.profile.heightCm
        if (height <= 0.0) return fail("body.error.invalid_height")
        val bmi = BodyMetrics.bmi(weight, height)
        val waist = snapshot.circumferencesCm[CircumferenceSite.WAIST]
        val whtr = waist?.let { BodyMetrics.waistToHeightRatio(it, height) }
        val band = wellnessBandKey(bmi, whtr)
        return BodyFatEstimate(
            methodId = id,
            bodyFatPercent = null,
            bodyMassIndex = bmi,
            waistToHeightRatio = whtr,
            wellnessBandKey = band,
            notesEnGb =
                listOf(
                    "BMI and waist-to-height are population screening indices for wellness conversations — " +
                        "not diagnoses of health, fitness, or body fat.",
                    "A waist-to-height ratio around 0.5 is a commonly cited screening cut-point " +
                        "(Ashwell & Gibson 2011; Browning et al. 2010).",
                    "Muscle mass, ethnicity, age and pregnancy change how these numbers should be read.",
                ),
            evidence = BodyEvidenceRegistry.forMethod(id),
            ok = true,
        )
    }

    fun wellnessBandKey(bmi: Double, whtr: Double?): String {
        val bmiKey =
            when {
                bmi < 18.5 -> "body.wellness.bmi_under"
                bmi < 25.0 -> "body.wellness.bmi_mid"
                bmi < 30.0 -> "body.wellness.bmi_elevated"
                else -> "body.wellness.bmi_high"
            }
        val whtrKey =
            when {
                whtr == null -> "body.wellness.whtr_unknown"
                whtr < WHT_R_SCREENING_THRESHOLD -> "body.wellness.whtr_below_half"
                else -> "body.wellness.whtr_at_or_above_half"
            }
        return "$bmiKey+$whtrKey"
    }

    private fun fail(key: String) =
        BodyFatEstimate(
            methodId = id,
            bodyFatPercent = null,
            evidence = BodyEvidenceRegistry.forMethod(id),
            ok = false,
            errorKey = key,
        )
}
