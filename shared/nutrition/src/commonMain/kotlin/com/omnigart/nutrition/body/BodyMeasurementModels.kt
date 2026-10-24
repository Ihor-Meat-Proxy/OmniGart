package com.omnigart.nutrition.body

/**
 * Local body inputs for the nutrition model.
 * Weight, waist, hips, chest, arm, thigh, body fat % — stored on-device.
 * Waist-to-height ratio is shown and not used in the energy formula.
 */

enum class MeasurementSite {
    WAIST,
    HIPS,
    CHEST,
    ARM,
    THIGH,
}

data class LocalWeightEntry(
    val id: String,
    val epochMs: Long,
    val massKg: Double,
    /** Sync id for append-only platform writes (`omnigart.weight.<id>`). */
    val syncIdentifier: String,
    val syncVersion: Long = 1L,
    val source: String = "manual",
    val platformLinked: Boolean = false,
)

data class LocalMeasurementEntry(
    val id: String,
    val epochMs: Long,
    val site: MeasurementSite,
    val valueCm: Double,
    val source: String = "manual",
)

data class LocalBodyFatEntry(
    val id: String,
    val epochMs: Long,
    /** Percentage 0–100. */
    val bodyFatPercent: Double,
    val source: String = "manual",
)

data class LocalBodySnapshot(
    val weightKg: Double?,
    val heightCm: Double?,
    val bodyFatPercent: Double?,
    val circumferencesCm: Map<MeasurementSite, Double>,
    val waistToHeightRatio: Double?,
)

object BodyMetricsDisplay {
    fun waistToHeightRatio(waistCm: Double, heightCm: Double): Double {
        require(waistCm > 0.0 && heightCm > 0.0)
        return waistCm / heightCm
    }
}

/**
 * Append-only weigh-in identity helpers (never rewrite platform samples).
 */
object WeighInSyncIds {
    fun syncIdentifier(localId: String): String = "omnigart.weight.$localId"

    fun shouldAttemptPlatformWrite(entry: LocalWeightEntry): Boolean =
        !entry.platformLinked && entry.syncVersion >= 1L
}
