package com.omnigart.body.fat

import com.omnigart.body.evidence.BodyEvidenceRegistry
import com.omnigart.body.models.BiologicalSex
import com.omnigart.body.models.BodyFatMethodId
import com.omnigart.body.models.BodyMeasurementSnapshot
import com.omnigart.body.models.CircumferenceSite

/**
 * U.S. Navy circumference equations (Hodgdon & Beckett 1984; NHRC ADA143890 / ADA146456).
 * Inputs in centimetres.
 */
object UsNavyStrategy : BodyFatStrategy {
    override val id: BodyFatMethodId = BodyFatMethodId.US_NAVY

    override fun estimate(snapshot: BodyMeasurementSnapshot): BodyFatEstimate {
        val height = snapshot.profile.heightCm
        if (height <= 0.0) return fail("body.error.invalid_height")
        val neck = snapshot.circumferencesCm[CircumferenceSite.NECK]
            ?: return fail("body.error.missing_neck")
        return when (snapshot.profile.sex) {
            BiologicalSex.MALE -> {
                val abdomen =
                    snapshot.circumferencesCm[CircumferenceSite.ABDOMEN]
                        ?: snapshot.circumferencesCm[CircumferenceSite.WAIST]
                        ?: return fail("body.error.missing_abdomen")
                val diff = abdomen - neck
                if (diff <= 0.0) return fail("body.error.navy_male_geometry")
                val bf =
                    86.010 * log10Safe(diff) - 70.041 * log10Safe(height) + 36.76
                ok(bf)
            }
            BiologicalSex.FEMALE -> {
                val waist =
                    snapshot.circumferencesCm[CircumferenceSite.WAIST]
                        ?: return fail("body.error.missing_waist")
                val hip =
                    snapshot.circumferencesCm[CircumferenceSite.HIP]
                        ?: return fail("body.error.missing_hip")
                val sum = waist + hip - neck
                if (sum <= 0.0) return fail("body.error.navy_female_geometry")
                val bf =
                    163.205 * log10Safe(sum) - 97.684 * log10Safe(height) - 78.387
                ok(bf)
            }
            BiologicalSex.UNSPECIFIED -> fail("body.error.sex_required")
        }
    }

    private fun ok(bf: Double) =
        BodyFatEstimate(
            methodId = id,
            bodyFatPercent = bf,
            notesEnGb =
                listOf(
                    "U.S. Navy circumference method (Hodgdon & Beckett 1984 NHRC reports).",
                    "Sensitive to tape placement; use consistent landmarks.",
                ),
            evidence = BodyEvidenceRegistry.forMethod(id),
            ok = true,
        )

    private fun fail(key: String) =
        BodyFatEstimate(
            methodId = id,
            bodyFatPercent = null,
            evidence = BodyEvidenceRegistry.forMethod(id),
            ok = false,
            errorKey = key,
        )
}
