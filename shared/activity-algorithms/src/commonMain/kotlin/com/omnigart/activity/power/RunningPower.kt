package com.omnigart.activity.power

import com.omnigart.activity.evidence.Citations

/**
 * Optional running power estimate from speed, grade, and body mass.
 *
 * Model (simplified cost-of-transport form after di Prampero et al.):
 *
 * ```
 * P ≈ m · v · (Cr + g · grade)
 * ```
 *
 * where Cr ≈ 3.6–4.0 J·kg⁻¹·m⁻¹ on flat ground, v in m·s⁻¹, grade as
 * rise/run (decimal), g = 9.80665 m·s⁻². Air drag and elasticity are omitted.
 *
 * Citation: [Citations.DI_PRAMPERO_COST_OF_RUNNING].
 */
data class RunningPowerEstimate(
    val watts: Double,
    val wattsPerKg: Double,
    val velocityMs: Double,
    val grade: Double,
    val costOfTransportJPerKgM: Double,
)

object RunningPower {
    const val CITATION: String = Citations.DI_PRAMPERO_COST_OF_RUNNING
    const val G: Double = 9.80665

    /** Default flat-ground cost of transport (J·kg⁻¹·m⁻¹). */
    const val DEFAULT_CR: Double = 3.8

    fun estimate(
        bodyMassKg: Double,
        velocityMs: Double,
        grade: Double = 0.0,
        costOfTransport: Double = DEFAULT_CR,
    ): RunningPowerEstimate? {
        if (bodyMassKg <= 0.0 || velocityMs <= 0.0) return null
        require(costOfTransport > 0.0)
        val effective = costOfTransport + G * grade
        if (effective <= 0.0) return null
        val watts = bodyMassKg * velocityMs * effective
        return RunningPowerEstimate(
            watts = watts,
            wattsPerKg = watts / bodyMassKg,
            velocityMs = velocityMs,
            grade = grade,
            costOfTransportJPerKgM = costOfTransport,
        )
    }

    fun fromPaceSecondsPerKm(
        bodyMassKg: Double,
        secondsPerKm: Double,
        grade: Double = 0.0,
        costOfTransport: Double = DEFAULT_CR,
    ): RunningPowerEstimate? {
        if (secondsPerKm <= 0.0) return null
        val v = 1000.0 / secondsPerKm
        return estimate(bodyMassKg, v, grade, costOfTransport)
    }
}
