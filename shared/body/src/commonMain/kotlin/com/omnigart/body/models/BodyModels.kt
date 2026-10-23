package com.omnigart.body.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class BiologicalSex {
    @SerialName("female") FEMALE,
    @SerialName("male") MALE,
    @SerialName("unspecified") UNSPECIFIED,
}

@Serializable
enum class CircumferenceSite {
    @SerialName("neck") NECK,
    @SerialName("chest") CHEST,
    @SerialName("waist") WAIST,
    @SerialName("abdomen") ABDOMEN,
    @SerialName("hip") HIP,
    @SerialName("upper_arm") UPPER_ARM,
    @SerialName("forearm") FOREARM,
    @SerialName("thigh") THIGH,
    @SerialName("calf") CALF,
}

@Serializable
enum class SkinfoldSite {
    @SerialName("chest") CHEST,
    @SerialName("midaxillary") MIDAXILLARY,
    @SerialName("triceps") TRICEPS,
    @SerialName("subscapular") SUBSCAPULAR,
    @SerialName("abdomen") ABDOMEN,
    @SerialName("suprailiac") SUPRAILIAC,
    @SerialName("thigh") THIGH,
}

@Serializable
enum class BodyFatMethodId {
    @SerialName("cun_bae") CUN_BAE,
    @SerialName("us_navy") US_NAVY,
    @SerialName("jackson_pollock_3") JACKSON_POLLOCK_3,
    @SerialName("jackson_pollock_7") JACKSON_POLLOCK_7,
    @SerialName("bmi_waist_height") BMI_WAIST_HEIGHT,
}

@Serializable
data class WeightSample(
    val id: String,
    val epochMs: Long,
    /** Body mass in kilograms. */
    val massKg: Double,
    val source: String = "manual",
)

@Serializable
data class CircumferenceSample(
    val id: String,
    val epochMs: Long,
    val site: CircumferenceSite,
    /** Circumference in centimetres. */
    val valueCm: Double,
    val source: String = "manual",
)

@Serializable
data class SkinfoldSample(
    val id: String,
    val epochMs: Long,
    val site: SkinfoldSite,
    /** Skinfold thickness in millimetres. */
    val valueMm: Double,
    val source: String = "manual",
)

/**
 * Metadata only for progress photos. Image bytes stay on-device; never written to Health platforms.
 */
@Serializable
data class ProgressPhotoMeta(
    val id: String,
    val capturedAtEpochMs: Long,
    /** App-private relative path or content URI string — local device only. */
    val localUri: String,
    val poseHintKey: String? = null,
    val note: String? = null,
    val associatedWeightId: String? = null,
    /** Always true in the model contract — cloud/Health export must refuse this type. */
    val localOnly: Boolean = true,
) {
    init {
        require(localOnly) { "Progress photos must remain local-only" }
    }
}

@Serializable
data class BodyProfile(
    val sex: BiologicalSex,
    val ageYears: Int,
    /** Standing height in centimetres. */
    val heightCm: Double,
)

@Serializable
data class BodyMeasurementSnapshot(
    val profile: BodyProfile,
    val weightKg: Double? = null,
    val circumferencesCm: Map<CircumferenceSite, Double> = emptyMap(),
    val skinfoldsMm: Map<SkinfoldSite, Double> = emptyMap(),
)

object BodyMetrics {
    fun bmi(massKg: Double, heightCm: Double): Double {
        require(massKg > 0.0 && heightCm > 0.0)
        val m = heightCm / 100.0
        return massKg / (m * m)
    }

    fun waistToHeightRatio(waistCm: Double, heightCm: Double): Double {
        require(waistCm > 0.0 && heightCm > 0.0)
        return waistCm / heightCm
    }
}
