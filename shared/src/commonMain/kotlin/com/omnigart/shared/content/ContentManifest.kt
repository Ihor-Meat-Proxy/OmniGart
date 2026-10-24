package com.omnigart.shared.content

import kotlinx.serialization.Serializable

/**
 * Top-level manifest for a content pack (bundled baseline or signed update).
 *
 * [schemaVersion] must be in [ContentSchema.SUPPORTED_SCHEMA_VERSIONS].
 * [packVersion] is compared so a newer verified update overlays the baseline.
 */
@Serializable
data class ContentManifest(
    val schemaVersion: Int,
    val packId: String,
    val packVersion: Int,
    val offline: Boolean = true,
    val sha256: String,
    val sections: Map<String, ContentSectionMeta> = emptyMap(),
    val localePolicy: LocalePolicy = LocalePolicy()
)

@Serializable
data class ContentSectionMeta(val fileCount: Int, val sha256: String)

@Serializable
data class LocalePolicy(
    val source: String = "en-GB",
    val included: List<String> = listOf("en-GB", "uk"),
    val forbidden: List<String> = listOf("ru")
)

object ContentSchema {
    const val CURRENT_SCHEMA_VERSION: Int = 1
    val SUPPORTED_SCHEMA_VERSIONS: Set<Int> = setOf(1)

    /** Logical section names present in the offline baseline bundle. */
    val BASELINE_SECTIONS: Set<String> =
        setOf(
            "exercises",
            "evidence",
            "icons",
            "body-maps",
            "illustrations",
            "achievements",
            "exercise-images"
        )
}
