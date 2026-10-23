package com.omnigart.body.fat

import com.omnigart.body.evidence.BodyEvidenceRegistry
import com.omnigart.body.models.BiologicalSex
import com.omnigart.body.models.BodyFatMethodId
import com.omnigart.body.models.BodyMeasurementSnapshot
import com.omnigart.body.models.BodyMetrics

/**
 * CUN-BAE (Clínica Universidad de Navarra – Body Adiposity Estimator).
 * Gómez-Ambrosi et al. Diabetes Care 2012; DOI 10.2337/dc11-1334.
 *
 * sex = 1 female, 0 male.
 */
object CunBaeStrategy : BodyFatStrategy {
    override val id: BodyFatMethodId = BodyFatMethodId.CUN_BAE

    override fun estimate(snapshot: BodyMeasurementSnapshot): BodyFatEstimate {
        val weight = snapshot.weightKg
            ?: return fail("body.error.missing_weight")
        val height = snapshot.profile.heightCm
        if (height <= 0.0) return fail("body.error.invalid_height")
        val sexCode =
            when (snapshot.profile.sex) {
                BiologicalSex.FEMALE -> 1.0
                BiologicalSex.MALE -> 0.0
                BiologicalSex.UNSPECIFIED ->
                    return fail("body.error.sex_required")
            }
        val age = snapshot.profile.ageYears.toDouble()
        val bmi = BodyMetrics.bmi(weight, height)
        val bf =
            -44.988 +
                (0.503 * age) +
                (10.689 * sexCode) +
                (3.172 * bmi) -
                (0.026 * bmi.squared()) +
                (0.181 * bmi * sexCode) -
                (0.02 * bmi * age) -
                (0.005 * bmi.squared() * sexCode) +
                (0.00021 * bmi.squared() * age)
        return BodyFatEstimate(
            methodId = id,
            bodyFatPercent = bf,
            bodyMassIndex = bmi,
            notesEnGb =
                listOf(
                    "CUN-BAE estimates adiposity from age, sex and BMI (Gómez-Ambrosi et al. 2012; DOI 10.2337/dc11-1334).",
                    "Estimate only — not a clinical diagnosis.",
                ),
            evidence = BodyEvidenceRegistry.forMethod(id),
            ok = true,
        )
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
