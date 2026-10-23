package com.omnigart.endurance

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

data class EnduranceIssue(val path: String, val message: String)

data class EnduranceReport(
    val runningWorkoutTypes: Int,
    val runningPlans: Int,
    val swimStrokes: Int,
    val swimDrills: Int,
    val swimSessions: Int,
    val issues: List<EnduranceIssue>,
) {
    val ok: Boolean get() = issues.isEmpty()
}

/**
 * Validates bundled running/swimming content packs (schemas, locales, cross-refs).
 * No pacing/CSS algorithms — content structure only.
 */
class EnduranceContentValidator(
    private val runningRoot: Path,
    private val swimmingRoot: Path,
    private val mapper: ObjectMapper = ObjectMapper(),
) {
    private val registry: SchemaRegistry =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)

    fun validate(): EnduranceReport {
        val issues = mutableListOf<EnduranceIssue>()
        val runWorkouts = validateRunning(issues)
        val swim = validateSwimming(issues)
        return EnduranceReport(
            runningWorkoutTypes = runWorkouts.first,
            runningPlans = runWorkouts.second,
            swimStrokes = swim.first,
            swimDrills = swim.second,
            swimSessions = swim.third,
            issues = issues,
        )
    }

    private fun validateRunning(issues: MutableList<EnduranceIssue>): Pair<Int, Int> {
        assertNoRu(runningRoot, issues)
        val en = loadLocale(runningRoot, "locales/en-GB.json")
        val uk = loadLocale(runningRoot, "locales/uk.json")
        checkLocaleParity(runningRoot, en, uk, issues)
        val workoutSchema = loadSchema(runningRoot, "schema/workout-type.schema.json")
        val planSchema = loadSchema(runningRoot, "schema/race-plan.schema.json")
        val workoutIds = mutableSetOf<String>()
        for (file in listJson(runningRoot.resolve("workout-types"))) {
            val node = mapper.readTree(file.readText())
            validateNode(workoutSchema, node, file, issues)
            val id = node.path("id").asText()
            if (!workoutIds.add(id)) issues += EnduranceIssue(file.toString(), "Duplicate workout type id: $id")
            if (file.name != "$id.json") issues += EnduranceIssue(file.toString(), "Filename must match id.json")
            checkNameKeys(node, en, uk, file, issues)
            checkEvidence(node, file, issues)
        }
        var planCount = 0
        for (file in listJson(runningRoot.resolve("plans"))) {
            planCount++
            val node = mapper.readTree(file.readText())
            validateNode(planSchema, node, file, issues)
            val id = node.path("id").asText()
            if (file.name != "$id.json") issues += EnduranceIssue(file.toString(), "Filename must match id.json")
            checkNameKeys(node, en, uk, file, issues)
            checkEvidence(node, file, issues)
            node.path("intensityDistribution").path("notesKey").asText().takeIf { it.isNotBlank() }?.let { key ->
                if (key !in en || key !in uk) issues += EnduranceIssue(file.toString(), "Missing distribution locale: $key")
            }
            node.path("progression").path("notesKey").asText().takeIf { it.isNotBlank() }?.let { key ->
                if (key !in en || key !in uk) issues += EnduranceIssue(file.toString(), "Missing progression locale: $key")
            }
            if (!node.path("progression").path("tenPercentRuleCaveat").asBoolean(false)) {
                issues += EnduranceIssue(file.toString(), "Plans must set tenPercentRuleCaveat=true")
            }
            node.path("weeklyTemplates").forEach { week ->
                week.path("sessions").forEach { session ->
                    val ref = session.path("workoutTypeId").asText()
                    if (ref.isNotBlank() && ref !in workoutIds) {
                        issues += EnduranceIssue(file.toString(), "Unknown workoutTypeId: $ref")
                    }
                }
            }
        }
        return workoutIds.size to planCount
    }

    private fun validateSwimming(issues: MutableList<EnduranceIssue>): Triple<Int, Int, Int> {
        assertNoRu(swimmingRoot, issues)
        val en = loadLocale(swimmingRoot, "locales/en-GB.json")
        val uk = loadLocale(swimmingRoot, "locales/uk.json")
        checkLocaleParity(swimmingRoot, en, uk, issues)
        val strokeSchema = loadSchema(swimmingRoot, "schema/stroke.schema.json")
        val drillSchema = loadSchema(swimmingRoot, "schema/drill.schema.json")
        val sessionSchema = loadSchema(swimmingRoot, "schema/session.schema.json")
        val strokeIds = mutableSetOf<String>()
        for (file in listJson(swimmingRoot.resolve("strokes"))) {
            val node = mapper.readTree(file.readText())
            validateNode(strokeSchema, node, file, issues)
            val id = node.path("id").asText()
            if (!strokeIds.add(id)) issues += EnduranceIssue(file.toString(), "Duplicate stroke id: $id")
            if (file.name != "$id.json") issues += EnduranceIssue(file.toString(), "Filename must match id.json")
            checkNameKeys(node, en, uk, file, issues)
            val summaryKey = node.path("summaryKey").asText()
            if (summaryKey.isNotBlank() && (summaryKey !in en || summaryKey !in uk)) {
                issues += EnduranceIssue(file.toString(), "Missing summary locale: $summaryKey")
            }
            checkEvidence(node, file, issues)
        }
        val drillIds = mutableSetOf<String>()
        for (file in listJson(swimmingRoot.resolve("drills"))) {
            val node = mapper.readTree(file.readText())
            validateNode(drillSchema, node, file, issues)
            val id = node.path("id").asText()
            if (!drillIds.add(id)) issues += EnduranceIssue(file.toString(), "Duplicate drill id: $id")
            if (file.name != "$id.json") issues += EnduranceIssue(file.toString(), "Filename must match id.json")
            checkNameKeys(node, en, uk, file, issues)
            checkEvidence(node, file, issues)
            node.path("strokeIds").forEach { s ->
                val ref = s.asText()
                if (ref !in strokeIds) issues += EnduranceIssue(file.toString(), "Unknown strokeId: $ref")
            }
        }
        var sessionCount = 0
        for (file in listJson(swimmingRoot.resolve("sessions"))) {
            sessionCount++
            val node = mapper.readTree(file.readText())
            validateNode(sessionSchema, node, file, issues)
            val id = node.path("id").asText()
            if (file.name != "$id.json") issues += EnduranceIssue(file.toString(), "Filename must match id.json")
            checkNameKeys(node, en, uk, file, issues)
            checkEvidence(node, file, issues)
            node.path("strokeIds").forEach { s ->
                val ref = s.asText()
                if (ref.isNotBlank() && ref !in strokeIds) {
                    issues += EnduranceIssue(file.toString(), "Unknown session strokeId: $ref")
                }
            }
            node.path("blocks").forEach { block ->
                val desc = block.path("descriptionKey").asText()
                if (desc.isNotBlank() && (desc !in en || desc !in uk)) {
                    issues += EnduranceIssue(file.toString(), "Missing block locale: $desc")
                }
                block.path("drillIds").forEach { d ->
                    val ref = d.asText()
                    if (ref.isNotBlank() && ref !in drillIds) {
                        issues += EnduranceIssue(file.toString(), "Unknown drillId: $ref")
                    }
                }
            }
            val usesCss = node.path("usesCriticalSwimSpeed").asBoolean(false)
            val category = node.path("category").asText()
            if (category == "css_intervals" && !usesCss) {
                issues += EnduranceIssue(file.toString(), "css_intervals sessions must set usesCriticalSwimSpeed=true")
            }
        }
        return Triple(strokeIds.size, drillIds.size, sessionCount)
    }

    private fun listJson(dir: Path): List<Path> {
        if (!Files.isDirectory(dir)) return emptyList()
        return Files.list(dir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "json" }.sorted().toList()
        }
    }

    private fun loadSchema(root: Path, relative: String): Schema =
        registry.getSchema(root.resolve(relative).readText())

    private fun loadLocale(root: Path, relative: String): Map<String, String> {
        val node = mapper.readTree(root.resolve(relative).readText())
        val out = mutableMapOf<String, String>()
        node.fields().forEachRemaining { (k, v) -> out[k] = v.asText() }
        return out
    }

    private fun checkLocaleParity(
        root: Path,
        en: Map<String, String>,
        uk: Map<String, String>,
        issues: MutableList<EnduranceIssue>,
    ) {
        if (en.keys != uk.keys) {
            val onlyEn = en.keys - uk.keys
            val onlyUk = uk.keys - en.keys
            issues += EnduranceIssue(root.toString(), "Locale key mismatch en-only=$onlyEn uk-only=$onlyUk")
        }
    }

    private fun assertNoRu(root: Path, issues: MutableList<EnduranceIssue>) {
        val locales = root.resolve("locales")
        if (!Files.isDirectory(locales)) return
        Files.list(locales).use { stream ->
            stream.forEach { path ->
                if (path.name.startsWith("ru")) {
                    issues += EnduranceIssue(path.toString(), "Russian locale files are not allowed")
                }
            }
        }
    }

    private fun validateNode(schema: Schema, node: JsonNode, file: Path, issues: MutableList<EnduranceIssue>) {
        schema.validate(node).forEach { err ->
            issues += EnduranceIssue(file.toString(), err.message)
        }
    }

    private fun checkNameKeys(
        node: JsonNode,
        en: Map<String, String>,
        uk: Map<String, String>,
        file: Path,
        issues: MutableList<EnduranceIssue>,
    ) {
        val enKey = node.path("nameKeys").path("enGB").asText()
        val ukKey = node.path("nameKeys").path("uk").asText()
        if (enKey.isNotBlank() && enKey !in en) issues += EnduranceIssue(file.toString(), "Missing en-GB key: $enKey")
        if (ukKey.isNotBlank() && ukKey !in uk) issues += EnduranceIssue(file.toString(), "Missing uk key: $ukKey")
    }

    private fun checkEvidence(node: JsonNode, file: Path, issues: MutableList<EnduranceIssue>) {
        node.path("evidenceRefs").forEach { ref ->
            val kind = ref.path("kind").asText()
            val id = ref.path("id").asText()
            if (kind == "doi" && !id.matches(Regex("""^10\.\d{4,9}/\S+$"""))) {
                issues += EnduranceIssue(file.toString(), "DOI looks malformed: $id")
            }
        }
    }
}
