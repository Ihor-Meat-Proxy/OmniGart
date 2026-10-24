package com.omnigart.shared.health.store.export

import com.omnigart.shared.health.store.LocalHealthWorkout
import kotlinx.serialization.Serializable

/**
 * OmniGart health export JSON schema (re-importable).
 *
 * Documented in `docs/HEALTH_DATA_MAPPING.md` § Export formats.
 */
@Serializable
data class OmniGartHealthExportDocument(
    val schema: String = SCHEMA_ID,
    val schemaVersion: Int = SCHEMA_VERSION,
    val exportedAt: String,
    val workouts: List<LocalHealthWorkout>
) {
    companion object {
        const val SCHEMA_ID = "omnigart.health.export"
        const val SCHEMA_VERSION = 1
    }
}

enum class HealthExportFormat {
    JSON,
    CSV_WORKOUTS,
    CSV_HEART_RATE,
    CSV_SAMPLES,
    GPX,
    TCX,

    /**
     * FIT binary. Not produced in-tree: no AGPL-compatible FOSS FIT writer
     * is vendored yet (Garmin FIT SDK is proprietary). See mapping doc.
     */
    FIT_UNAVAILABLE
}

data class HealthExportArtifact(
    val format: HealthExportFormat,
    val fileName: String,
    val mimeType: String,
    val utf8Content: String?,
    val note: String? = null
)
