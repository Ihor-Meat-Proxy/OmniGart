package com.omnigart.bodymap

import com.omnigart.content.Muscle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

data class BodyMapIssue(
    val path: String,
    val message: String,
)

data class BodyMapValidationReport(
    val exerciseCount: Int,
    val mappedMuscleCount: Int,
    val issues: List<BodyMapIssue>,
) {
    val ok: Boolean get() = issues.isEmpty()
}

/**
 * Ensures every muscle id used in the exercise library exists in the body-map
 * taxonomy→regions mapping, and that Kotlin [TaxonomyRegions] covers the schema enum.
 */
class BodyMapValidator(
    private val contentRoot: Path,
    private val bodyMapRoot: Path,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun validate(): BodyMapValidationReport {
        val issues = mutableListOf<BodyMapIssue>()
        val mappingFile = bodyMapRoot.resolve("mapping/taxonomy-to-regions.json")
        if (!Files.isRegularFile(mappingFile)) {
            issues += BodyMapIssue(mappingFile.toString(), "taxonomy-to-regions.json missing")
            return BodyMapValidationReport(0, 0, issues)
        }
        val root = json.parseToJsonElement(mappingFile.readText()).jsonObject
        val mappings = root["mappings"]!!.jsonObject
        val mappedIds = mappings.keys.toSet()

        for (muscle in Muscle.entries) {
            val id = muscle.name.lowercase().let {
                // Serial names use underscores already via enum names matching
                muscle.toTaxonomyId()
            }
            if (id !in mappedIds) {
                issues += BodyMapIssue(
                    mappingFile.toString(),
                    "Muscle enum $muscle ($id) missing from taxonomy-to-regions.json",
                )
            }
            if (!TaxonomyRegions.covers(muscle)) {
                issues += BodyMapIssue(
                    "TaxonomyRegions.kt",
                    "Muscle $muscle missing from Kotlin TaxonomyRegions.mappings",
                )
            }
        }

        val dataDir = contentRoot.resolve("data")
        val files = Files.list(dataDir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "json" }.toList()
        }
        for (file in files) {
            val node = json.parseToJsonElement(file.readText()).jsonObject
            for (field in listOf("primaryMuscles", "secondaryMuscles")) {
                val arr = node[field]?.jsonArray ?: continue
                for (el in arr) {
                    val mid = el.jsonPrimitive.content
                    if (mid !in mappedIds) {
                        issues += BodyMapIssue(file.toString(), "Unknown muscle id '$mid' in $field")
                    }
                }
            }
        }

        return BodyMapValidationReport(
            exerciseCount = files.size,
            mappedMuscleCount = mappedIds.size,
            issues = issues,
        )
    }
}

private fun Muscle.toTaxonomyId(): String = when (this) {
    Muscle.QUADRICEPS -> "quadriceps"
    Muscle.HAMSTRINGS -> "hamstrings"
    Muscle.GLUTEUS_MAXIMUS -> "gluteus_maximus"
    Muscle.GLUTEUS_MEDIUS -> "gluteus_medius"
    Muscle.CALVES -> "calves"
    Muscle.HIP_FLEXORS -> "hip_flexors"
    Muscle.ADDUCTORS -> "adductors"
    Muscle.PECTORALIS_MAJOR -> "pectoralis_major"
    Muscle.LATISSIMUS_DORSI -> "latissimus_dorsi"
    Muscle.TRAPEZIUS -> "trapezius"
    Muscle.RHOMBOIDS -> "rhomboids"
    Muscle.REAR_DELTOID -> "rear_deltoid"
    Muscle.FRONT_DELTOID -> "front_deltoid"
    Muscle.LATERAL_DELTOID -> "lateral_deltoid"
    Muscle.BICEPS -> "biceps"
    Muscle.TRICEPS -> "triceps"
    Muscle.FOREARMS -> "forearms"
    Muscle.RECTUS_ABDOMINIS -> "rectus_abdominis"
    Muscle.OBLIQUES -> "obliques"
    Muscle.TRANSVERSE_ABDOMINIS -> "transverse_abdominis"
    Muscle.ERECTOR_SPINAE -> "erector_spinae"
    Muscle.FULL_BODY -> "full_body"
    Muscle.ROTATOR_CUFF -> "rotator_cuff"
    Muscle.SERRATUS_ANTERIOR -> "serratus_anterior"
    Muscle.TIBIALIS_ANTERIOR -> "tibialis_anterior"
    Muscle.NECK -> "neck"
}
