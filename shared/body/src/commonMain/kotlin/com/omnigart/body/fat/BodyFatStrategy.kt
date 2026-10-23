package com.omnigart.body.fat

import com.omnigart.body.evidence.BodyEvidenceRegistry
import com.omnigart.body.evidence.EvidencePointer
import com.omnigart.body.models.BodyFatMethodId
import com.omnigart.body.models.BodyMeasurementSnapshot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

data class BodyFatEstimate(
    val methodId: BodyFatMethodId,
    /** Estimated body fat percentage (0–100 scale), when the method yields one. */
    val bodyFatPercent: Double?,
    val bodyMassIndex: Double? = null,
    val waistToHeightRatio: Double? = null,
    val wellnessBandKey: String? = null,
    val notesEnGb: List<String> = emptyList(),
    val evidence: List<EvidencePointer> = emptyList(),
    val ok: Boolean,
    val errorKey: String? = null,
)

interface BodyFatStrategy {
    val id: BodyFatMethodId

    fun estimate(snapshot: BodyMeasurementSnapshot): BodyFatEstimate
}

object BodyFatStrategies {
    /** Product default: CUN-BAE (age, sex, BMI) — no calipers or tape required beyond height/weight. */
    val DEFAULT: BodyFatStrategy = CunBaeStrategy

    val all: List<BodyFatStrategy> =
        listOf(
            CunBaeStrategy,
            UsNavyStrategy,
            JacksonPollock3Strategy,
            JacksonPollock7Strategy,
            BmiWaistHeightStrategy,
        )

    fun byId(id: BodyFatMethodId): BodyFatStrategy =
        all.first { it.id == id }
}

/** Siri (1961) conversion from body density (g/ml) to % fat. */
internal fun siriBodyFatPercent(bodyDensity: Double): Double =
    (495.0 / bodyDensity) - 450.0

internal fun requireFinite(value: Double, label: String): Double {
    require(value.isFinite() && value > 0.0) { "$label must be a positive finite number" }
    return value
}

internal fun lnNatural(x: Double): Double = ln(x)

internal fun log10Safe(x: Double): Double = log10(x)

internal fun Double.squared(): Double = this.pow(2)
