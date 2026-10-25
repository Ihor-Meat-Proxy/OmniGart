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
 * Validates on-disk beverage JSON: schema, unique ids, locale keys for all required locales
 * (mirrors `i18n/locales.yml`), no `ru` locale.
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
        val locales = loadRequiredLocales(issues)
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
            checkNameKeys(node, locales, file, issues)
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

    private fun loadRequiredLocales(
        issues: MutableList<BeverageValidationIssue>,
    ): Map<String, Map<String, String>> {
        val out = linkedMapOf<String, Map<String, String>>()
        for (loc in REQUIRED_LOCALES) {
            val relative = "locales/$loc.json"
            val file = contentRoot.resolve(relative)
            if (!Files.isRegularFile(file)) {
                issues += BeverageValidationIssue(file.toString(), "Missing required locale file for $loc")
                out[loc] = emptyMap()
                continue
            }
            out[loc] = loadLocale(relative)
        }
        return out
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
        locales: Map<String, Map<String, String>>,
        file: Path,
        issues: MutableList<BeverageValidationIssue>,
    ) {
        val keys =
            buildSet {
                val nameKeys = node.path("nameKeys")
                for (field in listOf("enGB", "uk")) {
                    val key = nameKeys.path(field).asText()
                    if (key.isNotBlank()) add(key)
                }
            }
        for (key in keys) {
            for ((loc, table) in locales) {
                if (key !in table) {
                    issues += BeverageValidationIssue(file.toString(), "Missing $loc locale key: $key")
                }
            }
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

    companion object {
        /** Mirrors `i18n/locales.yml` required list. Never include `ru`. */
        val REQUIRED_LOCALES: List<String> =
            listOf(
                "en-GB",
                "uk",
                "pl",
                "be",
                "kk",
                "ka",
                "es",
                "fr",
                "de",
                "cs",
                "zh-Hans",
            )
    }
}
