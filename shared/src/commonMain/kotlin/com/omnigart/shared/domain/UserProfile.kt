package com.omnigart.shared.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Biological sex used by equations that are sex-specific (Mifflin–St Jeor, CUN-BAE, EFSA water)
 * and for choosing a body-map figure. Phase models male/female only; see docs/OPEN_QUESTIONS.md.
 */
@Serializable
enum class BiologicalSex {
    MALE,
    FEMALE
}

@Serializable
enum class UnitSystem {
    METRIC,
    IMPERIAL
}

/**
 * Local-first user profile. All fields are stored on-device.
 */
@Serializable
data class UserProfile(
    val id: String,
    val biologicalSex: BiologicalSex,
    /** Height in centimetres (canonical storage). */
    val heightCm: Double,
    /** Body mass in kilograms (canonical storage). */
    val weightKg: Double,
    val dateOfBirth: LocalDate,
    val preferredUnits: UnitSystem = UnitSystem.METRIC,
    /** Optional waist circumference in centimetres. */
    val waistCm: Double? = null,
    /** Optional hip circumference in centimetres. */
    val hipCm: Double? = null,
    val primaryGoal: ProfileGoal = ProfileGoal.GENERAL_FITNESS,
    val strengthLevel: StrengthLevel = StrengthLevel.BEGINNER,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    /** True after the user finishes (or skips past) first-launch onboarding. */
    val onboardingCompleted: Boolean = false
) {
    init {
        require(heightCm > 0) { "heightCm must be positive" }
        require(weightKg > 0) { "weightKg must be positive" }
        waistCm?.let { require(it > 0) { "waistCm must be positive when set" } }
        hipCm?.let { require(it > 0) { "hipCm must be positive when set" } }
    }
}
