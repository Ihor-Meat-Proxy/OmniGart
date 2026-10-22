package com.omnigart.evidence

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

data class EvidenceValidationIssue(
    val path: String,
    val message: String,
)

data class EvidenceValidationReport(
    val entryCount: Int,
    val issues: List<EvidenceValidationIssue>,
) {
    val ok: Boolean get() = issues.isEmpty()
}

/**
 * Loads and optionally schema-validates on-disk evidence catalog under content/evidence.
 */
class EvidenceCatalogLoader(
    private val evidenceRoot: Path,
    private val mapper: ObjectMapper = ObjectMapper(),
) {
    private val registry: SchemaRegistry =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)

    fun loadRepository(): EvidenceRepository {
        val catalog = evidenceRoot.resolve("catalog.json")
        require(Files.isRegularFile(catalog)) { "Missing catalog.json at $catalog" }
        return EvidenceCatalogParser.repositoryFromCatalogJson(catalog.readText())
    }

    fun validate(): EvidenceValidationReport {
        val issues = mutableListOf<EvidenceValidationIssue>()
        val schemaFile = evidenceRoot.resolve("schema/evidence-entry.schema.json")
        val schema = registry.getSchema(mapper.readTree(schemaFile.readText()))
        val entriesDir = evidenceRoot.resolve("entries")
        val files =
            Files.list(entriesDir).use { stream ->
                stream.filter { it.isRegularFile() && it.extension == "json" }.toList()
            }
        val ids = mutableSetOf<String>()
        for (file in files) {
            val node = mapper.readTree(file.readText())
            schema.validate(node).forEach { err ->
                issues += EvidenceValidationIssue(file.toString(), err.message)
            }
            val id = node.path("id").asText()
            if (!ids.add(id)) {
                issues += EvidenceValidationIssue(file.toString(), "Duplicate evidence id: $id")
            }
            if (file.name != "$id.json") {
                issues += EvidenceValidationIssue(file.toString(), "Filename must match id.json")
            }
            if (node.path("retracted").asBoolean(false)) {
                issues +=
                    EvidenceValidationIssue(
                        file.toString(),
                        "Retracted entry present — must not power user-facing claims",
                    )
            }
        }

        val catalogPath = evidenceRoot.resolve("catalog.json")
        if (Files.isRegularFile(catalogPath)) {
            val catalog = mapper.readTree(catalogPath.readText())
            if (!catalog.isArray) {
                issues += EvidenceValidationIssue(catalogPath.toString(), "catalog.json must be an array")
            } else {
                val catalogIds = catalog.map { it.path("id").asText() }.toSet()
                if (catalogIds != ids) {
                    issues +=
                        EvidenceValidationIssue(
                            catalogPath.toString(),
                            "catalog.json ids diverge from entries/ (catalog=${catalogIds.size}, entries=${ids.size})",
                        )
                }
            }
        } else {
            issues += EvidenceValidationIssue(catalogPath.toString(), "Missing catalog.json")
        }

        val localeEn = evidenceRoot.resolve("locales/en-GB.json")
        val localeUk = evidenceRoot.resolve("locales/uk.json")
        if (Files.isRegularFile(localeEn) && Files.isRegularFile(localeUk)) {
            val en = mapper.readTree(localeEn.readText())
            val uk = mapper.readTree(localeUk.readText())
            for (file in files) {
                val node = mapper.readTree(file.readText())
                val key = node.path("keyFinding").path("keys").path("enGB").asText()
                if (key.isNotBlank() && !en.has(key)) {
                    issues += EvidenceValidationIssue(file.toString(), "Missing en-GB string for $key")
                }
                if (key.isNotBlank() && !uk.has(key)) {
                    issues += EvidenceValidationIssue(file.toString(), "Missing uk string for $key")
                }
            }
        }

        val ru = evidenceRoot.resolve("locales/ru.json")
        if (Files.exists(ru)) {
            issues += EvidenceValidationIssue(ru.toString(), "Russian locale must not be present")
        }

        return EvidenceValidationReport(entryCount = ids.size, issues = issues)
    }
}
