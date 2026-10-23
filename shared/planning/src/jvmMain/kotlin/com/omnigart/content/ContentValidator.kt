package com.omnigart.content

import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

data class ValidationIssue(val path: String, val message: String)

data class ValidationReport(val exerciseCount: Int, val templateCount: Int, val issues: List<ValidationIssue>) {
    val ok: Boolean get() = issues.isEmpty()
}

/**
 * Validates on-disk exercise/template JSON against schemas and content rules:
 * unique ids, locale keys present (en-GB + uk only — no ru), cross-refs, evidence refs.
 */
class ContentValidator(private val contentRoot: Path, private val mapper: ObjectMapper = ObjectMapper()) {
    private val registry: SchemaRegistry =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)

    fun validate(): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        val exerciseSchema = loadSchema("schema/exercise.schema.json")
        val templateSchema = loadSchema("schema/workout-template.schema.json")

        val localeEn = loadLocale("locales/en-GB.json")
        val localeUk = loadLocale("locales/uk.json")
        assertNoRuLocale(issues)

        val dataDir = contentRoot.resolve("data")
        val exercises = Files.list(dataDir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "json" }.toList()
        }
        val ids = mutableSetOf<String>()
        val nodes = mutableListOf<Pair<Path, JsonNode>>()

        for (file in exercises) {
            val node = mapper.readTree(file.readText())
            nodes.add(file to node)
            exerciseSchema.validate(node).forEach { err ->
                issues += ValidationIssue(file.toString(), err.message)
            }
            val id = node.path("id").asString()
            if (!ids.add(id)) {
                issues += ValidationIssue(file.toString(), "Duplicate exercise id: $id")
            }
            if (file.name != "$id.json") {
                issues += ValidationIssue(file.toString(), "Filename must match id.json")
            }
            checkNameKeys(node, localeEn, localeUk, file, issues)
            checkEvidence(node, file, issues)
            checkLoadModel(node, file, issues)
        }

        val idSet = ids.toSet()
        for ((file, node) in nodes) {
            for (field in listOf("regressions", "progressions")) {
                node.path(field).forEach { link ->
                    val ref = link.path("exerciseId").asString()
                    if (ref.isNotBlank() && ref !in idSet) {
                        issues += ValidationIssue(file.toString(), "Broken $field link to $ref")
                    }
                }
            }
        }

        val templateDir = contentRoot.resolve("templates")
        val templates = if (Files.isDirectory(templateDir)) {
            Files.list(templateDir).use { stream ->
                stream.filter { it.isRegularFile() && it.extension == "json" }.toList()
            }
        } else {
            emptyList()
        }
        val templateIds = mutableSetOf<String>()
        for (file in templates) {
            val node = mapper.readTree(file.readText())
            templateSchema.validate(node).forEach { err ->
                issues += ValidationIssue(file.toString(), err.message)
            }
            val id = node.path("id").asString()
            if (!templateIds.add(id)) {
                issues += ValidationIssue(file.toString(), "Duplicate template id: $id")
            }
            node.path("sessions").forEach { session ->
                session.path("blocks").forEach { block ->
                    block.path("items").forEach { item ->
                        val eid = item.path("exerciseId").asString()
                        if (eid.isNotBlank() && eid !in idSet) {
                            issues +=
                                ValidationIssue(
                                    file.toString(),
                                    "Template references missing exercise $eid"
                                )
                        }
                    }
                }
            }
            if (node.path("evidenceRefs").isEmpty) {
                issues += ValidationIssue(file.toString(), "Template missing evidenceRefs")
            } else {
                checkEvidence(node, file, issues)
            }
        }

        return ValidationReport(exercises.size, templates.size, issues)
    }

    private fun loadSchema(relative: String): Schema {
        val text = contentRoot.resolve(relative).readText()
        return registry.getSchema(mapper.readTree(text))
    }

    private fun loadLocale(relative: String): Map<String, String> {
        val file = contentRoot.resolve(relative)
        if (!Files.exists(file)) return emptyMap()
        val node = mapper.readTree(file.readText())
        return node.properties().associate { it.key to it.value.asString() }
    }

    private fun assertNoRuLocale(issues: MutableList<ValidationIssue>) {
        val locales = contentRoot.resolve("locales")
        if (!Files.isDirectory(locales)) return
        Files.list(locales).use { stream ->
            stream.forEach { path ->
                val n = path.fileName.toString().lowercase()
                if (n == "ru.json" || n.startsWith("ru-") || n.contains(".ru.")) {
                    issues +=
                        ValidationIssue(path.toString(), "Russian locale files are not allowed")
                }
            }
        }
    }

    private fun checkNameKeys(
        node: JsonNode,
        en: Map<String, String>,
        uk: Map<String, String>,
        file: Path,
        issues: MutableList<ValidationIssue>
    ) {
        val enKey = node.path("nameKeys").path("enGB").asString()
        val ukKey = node.path("nameKeys").path("uk").asString()
        if (enKey.isNotBlank() && enKey !in en) {
            issues += ValidationIssue(file.toString(), "Missing en-GB string for $enKey")
        }
        if (ukKey.isNotBlank() && ukKey !in uk) {
            issues += ValidationIssue(file.toString(), "Missing uk string for $ukKey")
        }
    }

    private fun checkEvidence(node: JsonNode, file: Path, issues: MutableList<ValidationIssue>) {
        val refs = node.path("evidenceRefs")
        if (refs.isEmpty) {
            issues += ValidationIssue(
                file.toString(),
                "Content has claims but no evidenceRefs (log in OPEN_QUESTIONS if intentionally omitted)"

            )
        }
        val knownIds = loadEvidenceIds()
        refs.values().forEach { ref ->
            val evidenceId = ref.path("evidenceId").asString()
            if (evidenceId.isBlank()) {
                issues += ValidationIssue(file.toString(), "evidenceRefs entry missing evidenceId")
                return@forEach
            }
            if (!evidenceId.matches(Regex("""^[a-z][a-z0-9]*(_[a-z0-9]+)*$"""))) {
                issues += ValidationIssue(file.toString(), "evidenceId looks malformed: $evidenceId")
            }
            if (knownIds != null && evidenceId !in knownIds) {
                issues += ValidationIssue(
                    file.toString(),
                    "evidenceId not found in content/evidence catalog: $evidenceId"
                )
            }
        }
    }

    private fun loadEvidenceIds(): Set<String>? {
        val catalog = contentRoot.parent?.resolve("evidence/catalog.json") ?: return null
        if (!Files.isRegularFile(catalog)) return null
        val root = mapper.readTree(catalog.readText())
        if (!root.isArray) return null
        return root.values().mapNotNull { it.path("id").asString().takeIf { id -> id.isNotBlank() } }.toSet()
    }

    private fun checkLoadModel(node: JsonNode, file: Path, issues: MutableList<ValidationIssue>) {
        val loadModel = node.path("loadModel").asString()
        val fraction = node.path("bodyweightFraction")
        if (fraction.isMissingNode || fraction.isNull) {
            issues += ValidationIssue(file.toString(), "Missing bodyweightFraction")
            return
        }
        val proportion = fraction.path("proportion").asDouble(Double.NaN)
        val basis = fraction.path("basis").asString()
        val rationale = fraction.path("rationale").asString()
        if (proportion.isNaN() || proportion < 0.0 || proportion > 1.5) {
            issues += ValidationIssue(file.toString(), "bodyweightFraction.proportion out of range: $proportion")
        }
        if (basis !in setOf("measured", "estimate")) {
            issues += ValidationIssue(file.toString(), "bodyweightFraction.basis must be measured|estimate")
        }
        if (rationale.length < 20) {
            issues += ValidationIssue(file.toString(), "bodyweightFraction.rationale too short")
        }
        when (loadModel) {
            "external_only" -> {
                if (proportion != 0.0) {
                    issues += ValidationIssue(
                        file.toString(),
                        "external_only exercises should use bodyweightFraction.proportion 0"
                    )
                }
            }
            "bodyweight_plus_external", "bodyweight_minus_assistance" -> {
                if (proportion <= 0.0) {
                    issues += ValidationIssue(
                        file.toString(),
                        "$loadModel should have bodyweightFraction.proportion > 0"
                    )
                }
            }
            else -> {
                if (loadModel.isNotBlank()) {
                    issues += ValidationIssue(file.toString(), "Unknown loadModel: $loadModel")
                }
            }
        }
        if (!node.has("allowsAddedWeight") || !node.path("allowsAddedWeight").isBoolean) {
            issues += ValidationIssue(file.toString(), "allowsAddedWeight must be a boolean")
        }
        if (!node.has("allowsAssistance") || !node.path("allowsAssistance").isBoolean) {
            issues += ValidationIssue(file.toString(), "allowsAssistance must be a boolean")
        }
        val tags = node.path("tags").values().map { it.asString() }.toSet()
        if ("pull-up-family" in tags || "dip-family" in tags) {
            val evidenceIds = node.path("evidenceRefs").values().map { it.path("evidenceId").asString() }.toSet()
            if ("pull-up-family" in tags) {
                val hasSeg = "de_leva_segmental_1996" in evidenceIds
                if (!hasSeg && basis == "estimate") {
                    issues += ValidationIssue(
                        file.toString(),
                        "pull-up-family estimate should cite de_leva_segmental_1996"

                    )
                }
            }
            if ("dip-family" in tags) {
                if ("mckenzie_dip_shoulder_2020" !in evidenceIds &&
                    "mckenzie_dip_kinematics_2022" !in evidenceIds
                ) {
                    issues += ValidationIssue(
                        file.toString(),
                        "dip-family should cite dip shoulder-extension safety/kinematics evidence"
                    )
                }
            }
        }
    }
}
