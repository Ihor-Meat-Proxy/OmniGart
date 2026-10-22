package com.omnigart.content

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
            val id = node.path("id").asText()
            if (!ids.add(id)) {
                issues += ValidationIssue(file.toString(), "Duplicate exercise id: $id")
            }
            if (file.name != "$id.json") {
                issues += ValidationIssue(file.toString(), "Filename must match id.json")
            }
            checkNameKeys(node, localeEn, localeUk, file, issues)
            checkEvidence(node, file, issues)
        }

        val idSet = ids.toSet()
        for ((file, node) in nodes) {
            for (field in listOf("regressions", "progressions")) {
                node.path(field).forEach { link ->
                    val ref = link.path("exerciseId").asText()
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
            val id = node.path("id").asText()
            if (!templateIds.add(id)) {
                issues += ValidationIssue(file.toString(), "Duplicate template id: $id")
            }
            node.path("sessions").forEach { session ->
                session.path("blocks").forEach { block ->
                    block.path("items").forEach { item ->
                        val eid = item.path("exerciseId").asText()
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
        return node.properties().associate { it.key to it.value.asText() }
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
        val enKey = node.path("nameKeys").path("enGB").asText()
        val ukKey = node.path("nameKeys").path("uk").asText()
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
                "Exercise has technique/safety claims but no evidenceRefs (log in OPEN_QUESTIONS if intentionally omitted)"
            )
        }
        refs.forEach { ref ->
            val kind = ref.path("kind").asText()
            val id = ref.path("id").asText()
            if (kind == "doi" && !id.matches(Regex("""^10\.\d{4,9}/\S+$"""))) {
                issues += ValidationIssue(file.toString(), "DOI looks malformed: $id")
            }
            if (kind == "pmid" && !id.matches(Regex("""^\d+$"""))) {
                issues += ValidationIssue(file.toString(), "PMID looks malformed: $id")
            }
        }
    }
}
