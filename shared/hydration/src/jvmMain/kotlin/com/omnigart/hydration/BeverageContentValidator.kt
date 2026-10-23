package com.omnigart.hydration

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

data class BeverageValidationIssue(
    val path: String,
    val message: String,
)

data class BeverageValidationReport(
    val beverageCount: Int,
    val issues: List<BeverageValidationIssue>,
) {
    val ok: Boolean get() = issues.isEmpty()
}

/**
 * Validates on-disk beverage JSON: schema, unique ids, en-GB + uk locale keys, no `ru` locale.
 */
class BeverageContentValidator(
    private val contentRoot: Path,
    private val mapper: ObjectMapper = ObjectMapper(),
) {
    private val registry: SchemaRegistry =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)

    fun validate(): BeverageValidationReport {
        val issues = mutableListOf<BeverageValidationIssue>()
        val schema = loadSchema("schema/beverage.schema.json")
        val localeEn = loadLocale("locales/en-GB.json")
        val localeUk = loadLocale("locales/uk.json")
        assertNoRuLocale(issues)

        val dataDir = contentRoot.resolve("data")
        val files =
            Files.list(dataDir).use { stream ->
                stream.filter { it.isRegularFile() && it.extension == "json" }.toList()
            }
        val ids = mutableSetOf<String>()

        for (file in files) {
            val node = mapper.readTree(file.readText())
            schema.validate(node).forEach { err ->
                issues += BeverageValidationIssue(file.toString(), err.message)
            }
            val id = node.path("id").asText()
            if (!ids.add(id)) {
                issues += BeverageValidationIssue(file.toString(), "Duplicate beverage id: $id")
            }
            if (file.name != "$id.json") {
                issues += BeverageValidationIssue(file.toString(), "Filename must match id.json")
            }
            checkNameKeys(node, localeEn, localeUk, file, issues)
            checkEvidence(node, file, issues)
            val factor = node.path("hydrationFactor").asDouble()
            if (factor <= 0.0) {
                issues += BeverageValidationIssue(file.toString(), "hydrationFactor must be > 0")
            }
        }

        return BeverageValidationReport(beverageCount = ids.size, issues = issues)
    }

    private fun loadSchema(relative: String): Schema {
        val text = contentRoot.resolve(relative).readText()
        return registry.getSchema(text)
    }

    private fun loadLocale(relative: String): Map<String, String> {
        val node = mapper.readTree(contentRoot.resolve(relative).readText())
        val out = mutableMapOf<String, String>()
        node.properties().forEach { (k, v) -> out[k] = v.asText() }
        return out
    }

    private fun assertNoRuLocale(issues: MutableList<BeverageValidationIssue>) {
        val locales = contentRoot.resolve("locales")
        if (!Files.isDirectory(locales)) return
        Files.list(locales).use { stream ->
            stream.forEach { path ->
                if (path.name.startsWith("ru")) {
                    issues += BeverageValidationIssue(path.toString(), "Russian locale files are not allowed")
                }
            }
        }
    }

    private fun checkNameKeys(
        node: JsonNode,
        en: Map<String, String>,
        uk: Map<String, String>,
        file: Path,
        issues: MutableList<BeverageValidationIssue>,
    ) {
        val enKey = node.path("nameKeys").path("enGB").asText()
        val ukKey = node.path("nameKeys").path("uk").asText()
        if (enKey.isNotBlank() && enKey !in en) {
            issues += BeverageValidationIssue(file.toString(), "Missing en-GB locale key: $enKey")
        }
        if (ukKey.isNotBlank() && ukKey !in uk) {
            issues += BeverageValidationIssue(file.toString(), "Missing uk locale key: $ukKey")
        }
    }

    private fun checkEvidence(
        node: JsonNode,
        file: Path,
        issues: MutableList<BeverageValidationIssue>,
    ) {
        node.path("evidenceRefs").forEach { ref ->
            val kind = ref.path("kind").asText()
            val id = ref.path("id").asText()
            if (kind == "doi" && !id.matches(Regex("""^10\.\d{4,9}/\S+$"""))) {
                issues += BeverageValidationIssue(file.toString(), "DOI looks malformed: $id")
            }
        }
    }
}
