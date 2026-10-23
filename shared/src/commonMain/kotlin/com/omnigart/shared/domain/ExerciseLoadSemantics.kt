package com.omnigart.shared.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * How an exercise contributes to effective load.
 * Mirrors content/exercises schema fields added for pull-up / dip families.
 * Safe defaults apply when content is older or fields are omitted.
 */
@Serializable
enum class LoadModel {
    @SerialName("bodyweight_plus_external")
    BODYWEIGHT_PLUS_EXTERNAL,

    @SerialName("bodyweight_minus_assistance")
    BODYWEIGHT_MINUS_ASSISTANCE,

    @SerialName("external_only")
    EXTERNAL_ONLY
}

@Serializable
enum class BodyweightFractionBasis {
    @SerialName("measured")
    MEASURED,

    @SerialName("estimate")
    ESTIMATE
}

/**
 * Share of body mass moved by the trainee (0 for external-only).
 * [basis] is stored as a string in content JSON (`measured` | `estimate`); use [basisEnum] when parsing.
 */
@Serializable
data class BodyweightFraction(
    val proportion: Double = 0.0,
    val basis: String = "estimate",
    val rationale: String = ""
) {
    init {
        require(proportion in 0.0..1.5) { "bodyweightFraction.proportion must be in 0..1.5" }
    }

    val basisEnum: BodyweightFractionBasis
        get() =
            when (basis.lowercase()) {
                "measured" -> BodyweightFractionBasis.MEASURED
                else -> BodyweightFractionBasis.ESTIMATE
            }

    val isEstimated: Boolean get() = basisEnum == BodyweightFractionBasis.ESTIMATE
}

/**
 * Load-related fields from the exercise library (or safe defaults when absent).
 */
@Serializable
data class ExerciseLoadSemantics(
    val loadModel: LoadModel = LoadModel.EXTERNAL_ONLY,
    val bodyweightFraction: BodyweightFraction = DEFAULT_EXTERNAL_FRACTION,
    val allowsAddedWeight: Boolean = true,
    val allowsAssistance: Boolean = false
) {
    val usesBodyweight: Boolean
        get() =
            loadModel == LoadModel.BODYWEIGHT_PLUS_EXTERNAL ||
                loadModel == LoadModel.BODYWEIGHT_MINUS_ASSISTANCE

    companion object {
        val DEFAULT_EXTERNAL_FRACTION =
            BodyweightFraction(
                proportion = 0.0,
                basis = "estimate",
                rationale = "Default external-only placeholder; content JSON should override."
            )

        val EXTERNAL_ONLY =
            ExerciseLoadSemantics(
                loadModel = LoadModel.EXTERNAL_ONLY,
                bodyweightFraction = DEFAULT_EXTERNAL_FRACTION,
                allowsAddedWeight = true,
                allowsAssistance = false
            )

        /**
         * Build from optional content fields. Missing values fall back to safe defaults
         * (external-only, no assistance, added weight allowed).
         */
        fun fromOptional(
            loadModel: LoadModel? = null,
            bodyweightFraction: BodyweightFraction? = null,
            allowsAddedWeight: Boolean? = null,
            allowsAssistance: Boolean? = null
        ): ExerciseLoadSemantics {
            val model = loadModel ?: LoadModel.EXTERNAL_ONLY
            return ExerciseLoadSemantics(
                loadModel = model,
                bodyweightFraction = bodyweightFraction ?: defaultFractionFor(model),
                allowsAddedWeight = allowsAddedWeight ?: defaultAllowsAdded(model),
                allowsAssistance = allowsAssistance ?: defaultAllowsAssistance(model)
            )
        }

        /**
         * Infer when only [trackingTypeName] is known (legacy content without load fields).
         * `bodyweight_reps` → plus-external with [fallbackProportion]; otherwise external-only.
         */
        fun fromTrackingType(
            trackingTypeName: String?,
            fallbackProportion: Double = 1.0
        ): ExerciseLoadSemantics {
            if (trackingTypeName == "bodyweight_reps") {
                return ExerciseLoadSemantics(
                    loadModel = LoadModel.BODYWEIGHT_PLUS_EXTERNAL,
                    bodyweightFraction =
                        BodyweightFraction(
                            proportion = fallbackProportion.coerceIn(0.0, 1.5),
                            basis = "estimate",
                            rationale = "Inferred from trackingType=bodyweight_reps; content should supply loadModel."
                        ),
                    allowsAddedWeight = true,
                    allowsAssistance = true
                )
            }
            return EXTERNAL_ONLY
        }

        private fun defaultFractionFor(model: LoadModel): BodyweightFraction =
            when (model) {
                LoadModel.EXTERNAL_ONLY -> DEFAULT_EXTERNAL_FRACTION
                LoadModel.BODYWEIGHT_PLUS_EXTERNAL,
                LoadModel.BODYWEIGHT_MINUS_ASSISTANCE ->
                    BodyweightFraction(
                        proportion = 1.0,
                        basis = "estimate",
                        rationale = "Default full body mass until content supplies bodyweightFraction."
                    )
            }

        private fun defaultAllowsAdded(model: LoadModel): Boolean =
            when (model) {
                LoadModel.BODYWEIGHT_MINUS_ASSISTANCE -> false
                else -> true
            }

        private fun defaultAllowsAssistance(model: LoadModel): Boolean =
            when (model) {
                LoadModel.EXTERNAL_ONLY -> false
                LoadModel.BODYWEIGHT_PLUS_EXTERNAL -> true
                LoadModel.BODYWEIGHT_MINUS_ASSISTANCE -> true
            }
    }
}

/**
 * Which one-hand-friendly load controls the set logger should show for an exercise.
 * Added weight is “+kg”; assistance is “−kg”; bands use the equipment-profile picker.
 */
data class SetLoadLoggingControls(
    val showAddedWeight: Boolean,
    val showAssistanceKg: Boolean,
    val showBandPicker: Boolean,
    val loadModel: LoadModel
) {
    companion object {
        fun forSemantics(semantics: ExerciseLoadSemantics, hasBandsEquipment: Boolean = false): SetLoadLoggingControls =
            SetLoadLoggingControls(
                showAddedWeight = semantics.allowsAddedWeight,
                showAssistanceKg = semantics.allowsAssistance,
                showBandPicker = semantics.allowsAssistance && hasBandsEquipment,
                loadModel = semantics.loadModel
            )
    }
}
