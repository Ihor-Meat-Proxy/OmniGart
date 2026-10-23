package com.omnigart.content

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

data class MovementValidationReport(
    val yogaPoseCount: Int,
    val yogaFlowCount: Int,
    val pilatesExerciseCount: Int,
    val pilatesSessionCount: Int,
    val issues: List<ValidationIssue>
) {
    val ok: Boolean get() = issues.isEmpty()
}

/**
 * Validates bundled yoga + pilates content packs: schema, unique ids, locale keys (en-GB + uk only),
 * cross-refs, evidence registry entry ids (against allowlist and optional content/evidence/entries).
 */
class YogaPilatesContentValidator(
    private val yogaRoot: Path,
    private val pilatesRoot: Path,
    private val repoRoot: Path = yogaRoot.parent?.parent ?: yogaRoot,
    private val mapper: ObjectMapper = ObjectMapper()
) {
    private val registry: SchemaRegistry =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)

    fun validate(): MovementValidationReport {
        val issues = mutableListOf<ValidationIssue>()
        assertNoRu(yogaRoot, issues)
        assertNoRu(pilatesRoot, issues)

        val yogaEn = loadLocale(yogaRoot, "locales/en-GB.json")
        val yogaUk = loadLocale(yogaRoot, "locales/uk.json")
        val pilatesEn = loadLocale(pilatesRoot, "locales/en-GB.json")
        val pilatesUk = loadLocale(pilatesRoot, "locales/uk.json")

        val yogaEvidenceAllow = loadAllowlist(yogaRoot.resolve("known-evidence-ids.json"))
        val pilatesEvidenceAllow = loadAllowlist(pilatesRoot.resolve("known-evidence-ids.json"))
        val onDiskEvidence = loadOnDiskEvidenceIds()

        val poseSchema = loadSchema(yogaRoot, "schema/pose.schema.json")
        val flowSchema = loadSchema(yogaRoot, "schema/flow.schema.json")
        val exerciseSchema = loadSchema(pilatesRoot, "schema/exercise.schema.json")
        val sessionSchema = loadSchema(pilatesRoot, "schema/session.schema.json")

        val poseIds = mutableSetOf<String>()
        val poseFiles = listJson(yogaRoot.resolve("data/poses"))
        for (file in poseFiles) {
            val node = mapper.readTree(file.readText())
            poseSchema.validate(node).forEach { issues += ValidationIssue(file.toString(), it.message) }
            val id = node.path("id").asText()
            if (!poseIds.add(id)) issues += ValidationIssue(file.toString(), "Duplicate pose id $id")
            if (file.name != "$id.json") issues += ValidationIssue(file.toString(), "Filename must match id.json")
            if (node.path("bundled").asBoolean(false) != true) {
                issues += ValidationIssue(file.toString(), "bundled must be true (bundle-first)")
            }
            checkKeys(node, listOf("englishNameKeys", "sanskritNameKeys"), yogaEn, yogaUk, file, issues)
            checkEvidenceIds(node, yogaEvidenceAllow, onDiskEvidence, file, issues)
            checkContraindicationFraming(node, file, issues)
        }

        val flowFiles = listJson(yogaRoot.resolve("data/flows"))
        for (file in flowFiles) {
            val node = mapper.readTree(file.readText())
            flowSchema.validate(node).forEach { issues += ValidationIssue(file.toString(), it.message) }
            val id = node.path("id").asText()
            if (file.name != "$id.json") issues += ValidationIssue(file.toString(), "Filename must match id.json")
            if (node.path("bundled").asBoolean(false) != true) {
                issues += ValidationIssue(file.toString(), "bundled must be true (bundle-first)")
            }
            checkKeys(node, listOf("nameKeys", "descriptionKeys"), yogaEn, yogaUk, file, issues)
            checkEvidenceIds(node, yogaEvidenceAllow, onDiskEvidence, file, issues)
            node.path("poseIds").forEach { ref ->
                val pid = ref.asText()
                if (pid !in poseIds) issues += ValidationIssue(file.toString(), "Flow references missing pose $pid")
            }
            node.path("sequence").forEach { step ->
                val pid = step.path("poseId").asText()
                if (pid.isNotBlank() && pid !in poseIds) {
                    issues += ValidationIssue(file.toString(), "Sequence references missing pose $pid")
                }
            }
        }

        val exerciseIds = mutableSetOf<String>()
        val exerciseFiles = listJson(pilatesRoot.resolve("data/exercises"))
        for (file in exerciseFiles) {
            val node = mapper.readTree(file.readText())
            exerciseSchema.validate(node).forEach { issues += ValidationIssue(file.toString(), it.message) }
            val id = node.path("id").asText()
            if (!exerciseIds.add(id)) issues += ValidationIssue(file.toString(), "Duplicate exercise id $id")
            if (file.name != "$id.json") issues += ValidationIssue(file.toString(), "Filename must match id.json")
            if (node.path("bundled").asBoolean(false) != true) {
                issues += ValidationIssue(file.toString(), "bundled must be true (bundle-first)")
            }
            checkKeys(node, listOf("nameKeys"), pilatesEn, pilatesUk, file, issues)
            checkEvidenceIds(node, pilatesEvidenceAllow, onDiskEvidence, file, issues)
            checkContraindicationFraming(node, file, issues)
        }

        val sessionFiles = listJson(pilatesRoot.resolve("data/sessions"))
        for (file in sessionFiles) {
            val node = mapper.readTree(file.readText())
            sessionSchema.validate(node).forEach { issues += ValidationIssue(file.toString(), it.message) }
            val id = node.path("id").asText()
            if (file.name != "$id.json") issues += ValidationIssue(file.toString(), "Filename must match id.json")
            if (node.path("bundled").asBoolean(false) != true) {
                issues += ValidationIssue(file.toString(), "bundled must be true (bundle-first)")
            }
            checkKeys(node, listOf("nameKeys", "descriptionKeys"), pilatesEn, pilatesUk, file, issues)
            checkEvidenceIds(node, pilatesEvidenceAllow, onDiskEvidence, file, issues)
            node.path("exerciseIds").forEach { ref ->
                val eid = ref.asText()
                if (eid !in exerciseIds) {
                    issues += ValidationIssue(file.toString(), "Session references missing exercise $eid")
                }
            }
            node.path("blocks").forEach { block ->
                val titleKey = block.path("titleKey").asText()
                if (titleKey.isNotBlank() && titleKey !in pilatesEn) {
                    issues += ValidationIssue(file.toString(), "Missing en-GB block title $titleKey")
                }
                if (titleKey.isNotBlank() && titleKey !in pilatesUk) {
                    issues += ValidationIssue(file.toString(), "Missing uk block title $titleKey")
                }
                block.path("items").forEach { item ->
                    val eid = item.path("exerciseId").asText()
                    if (eid.isNotBlank() && eid !in exerciseIds) {
                        issues += ValidationIssue(file.toString(), "Block references missing exercise $eid")
                    }
                }
            }
        }

        return MovementValidationReport(
            yogaPoseCount = poseFiles.size,
            yogaFlowCount = flowFiles.size,
            pilatesExerciseCount = exerciseFiles.size,
            pilatesSessionCount = sessionFiles.size,
            issues = issues
        )
    }

    private fun checkContraindicationFraming(node: JsonNode, file: Path, issues: MutableList<ValidationIssue>) {
        node.path("contraindicationNotes").forEach { note ->
            val text = note.asText()
            if (text.isNotBlank() && !text.lowercase().contains("general safety")) {
                issues += ValidationIssue(
                    file.toString(),
                    "Contraindication notes must be framed as general safety (got: ${text.take(48)}…)"
                )
            }
        }
    }

    private fun checkEvidenceIds(
        node: JsonNode,
        allow: Set<String>,
        onDisk: Set<String>?,
        file: Path,
        issues: MutableList<ValidationIssue>
    ) {
        val refs = node.path("evidenceEntryIds")
        if (refs.isEmpty) {
            issues += ValidationIssue(file.toString(), "Missing evidenceEntryIds")
            return
        }
        refs.forEach { ref ->
            val id = ref.asText()
            if (id !in allow) {
                issues += ValidationIssue(file.toString(), "evidenceEntryId not in known allowlist: $id")
            }
            if (onDisk != null && id !in onDisk) {
                issues += ValidationIssue(
                    file.toString(),
                    "evidenceEntryId missing from content/evidence/entries: $id"
                )
            }
        }
    }

    private fun checkKeys(
        node: JsonNode,
        fields: List<String>,
        en: Map<String, String>,
        uk: Map<String, String>,
        file: Path,
        issues: MutableList<ValidationIssue>
    ) {
        for (field in fields) {
            val obj = node.path(field)
            if (obj.isMissingNode || obj.isNull) continue
            listOf("enGB", "uk").forEach { localeField ->
                val key = obj.path(localeField).asText()
                if (key.isBlank()) return@forEach
                if (key !in en) issues += ValidationIssue(file.toString(), "Missing en-GB string for $key")
                if (key !in uk) issues += ValidationIssue(file.toString(), "Missing uk string for $key")
            }
        }
    }

    private fun loadSchema(root: Path, relative: String): Schema {
        val text = root.resolve(relative).readText()
        return registry.getSchema(text)
    }

    private fun loadLocale(root: Path, relative: String): Map<String, String> {
        val file = root.resolve(relative)
        if (!file.isRegularFile()) return emptyMap()
        val node = mapper.readTree(file.readText())
        val out = mutableMapOf<String, String>()
        for ((k, v) in node.properties()) out[k] = v.asText()
        return out
    }

    private fun loadAllowlist(path: Path): Set<String> {
        if (!path.isRegularFile()) return emptySet()
        val node = mapper.readTree(path.readText())
        return node.path("ids").map { it.asText() }.toSet()
    }

    private fun loadOnDiskEvidenceIds(): Set<String>? {
        val dir = repoRoot.resolve("content/evidence/entries")
        if (!dir.isDirectory()) return null
        return Files.list(dir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "json" }
                .map { it.fileName.toString().removeSuffix(".json") }
                .toList()
                .toSet()
        }
    }

    private fun assertNoRu(root: Path, issues: MutableList<ValidationIssue>) {
        val locales = root.resolve("locales")
        if (!locales.isDirectory()) return
        Files.list(locales).use { stream ->
            stream.forEach { path ->
                val n = path.fileName.toString().lowercase()
                if (n == "ru.json" || n.startsWith("ru-")) {
                    issues += ValidationIssue(path.toString(), "Russian locale files are not allowed")
                }
            }
        }
    }

    private fun listJson(dir: Path): List<Path> {
        if (!dir.isDirectory()) return emptyList()
        return Files.list(dir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "json" }.sorted().toList()
        }
    }
}
