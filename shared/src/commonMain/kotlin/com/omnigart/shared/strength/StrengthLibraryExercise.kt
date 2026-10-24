package com.omnigart.shared.strength

/**
 * Compact exercise row used by [StrengthProgramGenerator].
 * Muscle and equipment ids match the content-pack vocabulary (en-GB scientific naming).
 */
data class StrengthLibraryExercise(
    val id: String,
    val movementPattern: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val equipment: List<String>,
    val level: String,
    val mechanics: String,
    val unilateral: Boolean,
    val loadModel: String,
    val bodyweightFraction: Double,
    val allowsAddedWeight: Boolean,
    val allowsAssistance: Boolean,
    val regressions: List<String>,
    val progressions: List<String>
) {
    fun usesBodyweightLoad(): Boolean = loadModel == "bodyweight_plus_external" || loadModel == "bodyweight_minus_assistance"
}
