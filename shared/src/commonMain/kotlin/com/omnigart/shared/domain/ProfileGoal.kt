package com.omnigart.shared.domain

import kotlinx.serialization.Serializable

/**
 * Primary training focus chosen during onboarding / profile edit.
 * Wellness coaching intent only — not a medical prescription.
 */
@Serializable
enum class ProfileGoal {
    GENERAL_FITNESS,
    STRENGTH,
    ENDURANCE,
    MOBILITY,
    FAT_LOSS_SUPPORT
}
