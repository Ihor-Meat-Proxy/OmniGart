package com.omnigart.shared.domain

import kotlinx.serialization.Serializable

/**
 * Self-reported daily activity band for wellness energy estimates (not a clinical PAL assay).
 */
@Serializable
enum class ActivityLevel {
    SEDENTARY,
    LIGHT,
    MODERATE,
    ACTIVE,
    VERY_ACTIVE
}
