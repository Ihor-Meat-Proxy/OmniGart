package com.omnigart.shared.domain

import kotlinx.serialization.Serializable

/**
 * User-defined resistance band in the equipment profile.
 * [estimatedAssistanceKg] is approximate — band tension varies with stretch;
 * UIs should label defaults as approximate and keep the value editable.
 */
@Serializable
data class ResistanceBand(
    val id: String,
    /** Display colour or custom name (e.g. "Red", "Light"). */
    val label: String,
    /** Estimated assistance in kilograms (editable). */
    val estimatedAssistanceKg: Double,
    /** When true, UI should mark the estimate as approximate. */
    val approximate: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "band id must not be blank" }
        require(label.isNotBlank()) { "band label must not be blank" }
        require(estimatedAssistanceKg >= 0.0) { "estimatedAssistanceKg must be non-negative" }
    }
}

@Serializable
data class BandEquipmentProfile(val bands: List<ResistanceBand> = emptyList()) {
    fun find(id: String): ResistanceBand? = bands.find { it.id == id }

    /** Sum of estimated assistance for one or more selected bands (multi-band). */
    fun totalAssistanceKg(selectedBandIds: List<String>): Double {
        if (selectedBandIds.isEmpty()) return 0.0
        return selectedBandIds.sumOf { id ->
            find(id)?.estimatedAssistanceKg
                ?: error("Unknown band id: $id")
        }
    }

    companion object {
        /**
         * Common starter defaults (approximate kg). Users edit after defining their set.
         * Values are product placeholders, not measured calibrations.
         */
        fun defaultStarterBands(): BandEquipmentProfile =
            BandEquipmentProfile(
                bands =
                    listOf(
                        ResistanceBand(id = "band-light", label = "Light", estimatedAssistanceKg = 10.0),
                        ResistanceBand(id = "band-medium", label = "Medium", estimatedAssistanceKg = 20.0),
                        ResistanceBand(id = "band-heavy", label = "Heavy", estimatedAssistanceKg = 30.0),
                        ResistanceBand(id = "band-x-heavy", label = "X-Heavy", estimatedAssistanceKg = 40.0)
                    )
            )
    }
}
