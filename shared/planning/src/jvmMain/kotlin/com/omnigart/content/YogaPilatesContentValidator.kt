package com.omnigart.content

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
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

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
 * Validates bundled yoga + pilates content packs: schema, unique ids, locale keys for all
 * required locales (mirrors `i18n/locales.yml`; never ru), cross-refs, evidence registry
 * entry ids (against allowlist and optional content/evidence/entries).
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

        val yogaLocales = loadRequiredLocales(yogaRoot, issues)
        val pilatesLocales = loadRequiredLocales(pilatesRoot, issues)

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
            val id = node.path("id").asString()
            if (!poseIds.add(id)) issues += ValidationIssue(file.toString(), "Duplicate pose id $id")
            if (file.name != "$id.json") issues += ValidationIssue(file.toString(), "Filename must match id.json")
            if (node.path("bundled").asBoolean(false) != true) {
                issues += ValidationIssue(file.toString(), "bundled must be true (bundle-first)")
            }
            checkKeys(node, listOf("englishNameKeys", "sanskritNameKeys"), yogaLocales, file, issues)
            checkEvidenceIds(node, yogaEvidenceAllow, onDiskEvidence, file, issues)
            checkContraindicationFraming(node, file, issues)
        }

        val flowFiles = listJson(yogaRoot.resolve("data/flows"))
        for (file in flowFiles) {
            val node = mapper.readTree(file.readText())
            flowSchema.validate(node).forEach { issues += ValidationIssue(file.toString(), it.message) }
            val id = node.path("id").asString()
            if (file.name != "$id.json") issues += ValidationIssue(file.toString(), "Filename must match id.json")
            if (node.path("bundled").asBoolean(false) != true) {
                issues += ValidationIssue(file.toString(), "bundled must be true (bundle-first)")
            }
            checkKeys(node, listOf("nameKeys", "descriptionKeys"), yogaLocales, file, issues)
            checkEvidenceIds(node, yogaEvidenceAllow, onDiskEvidence, file, issues)
            node.path("poseIds").forEach { ref ->
                val pid = ref.asString()
                if (pid !in poseIds) issues += ValidationIssue(file.toString(), "Flow references missing pose $pid")
            }
            node.path("sequence").forEach { step ->
                val pid = step.path("poseId").asString()
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
            val id = node.path("id").asString()
            if (!exerciseIds.add(id)) issues += ValidationIssue(file.toString(), "Duplicate exercise id $id")
            if (file.name != "$id.json") issues += ValidationIssue(file.toString(), "Filename must match id.json")
            if (node.path("bundled").asBoolean(false) != true) {
                issues += ValidationIssue(file.toString(), "bundled must be true (bundle-first)")
            }
            checkKeys(node, listOf("nameKeys"), pilatesLocales, file, issues)
            checkEvidenceIds(node, pilatesEvidenceAllow, onDiskEvidence, file, issues)
            checkContraindicationFraming(node, file, issues)
        }

        val sessionFiles = listJson(pilatesRoot.resolve("data/sessions"))
        for (file in sessionFiles) {
            val node = mapper.readTree(file.readText())
            sessionSchema.validate(node).forEach { issues += ValidationIssue(file.toString(), it.message) }
            val id = node.path("id").asString()
            if (file.name != "$id.json") issues += ValidationIssue(file.toString(), "Filename must match id.json")
            if (node.path("bundled").asBoolean(false) != true) {
                issues += ValidationIssue(file.toString(), "bundled must be true (bundle-first)")
            }
            checkKeys(node, listOf("nameKeys", "descriptionKeys"), pilatesLocales, file, issues)
            checkEvidenceIds(node, pilatesEvidenceAllow, onDiskEvidence, file, issues)
            node.path("exerciseIds").forEach { ref ->
                val eid = ref.asString()
                if (eid !in exerciseIds) {
                    issues += ValidationIssue(file.toString(), "Session references missing exercise $eid")
                }
            }
            node.path("blocks").forEach { block ->
                val titleKey = block.path("titleKey").asString()
                if (titleKey.isNotBlank()) {
                    for ((loc, table) in pilatesLocales) {
                        if (titleKey !in table) {
                            issues += ValidationIssue(file.toString(), "Missing $loc block title $titleKey")
                        }
                    }
                }
                block.path("items").forEach { item ->
                    val eid = item.path("exerciseId").asString()
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
            val text = note.asString()
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
        refs.values().forEach { ref ->
            val id = ref.asString()
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
        locales: Map<String, Map<String, String>>,
        file: Path,
        issues: MutableList<ValidationIssue>
    ) {
        val keys =
            buildSet {
                for (field in fields) {
                    val obj = node.path(field)
                    if (obj.isMissingNode || obj.isNull) continue
                    for (localeField in listOf("enGB", "uk")) {
                        val key = obj.path(localeField).asString()
                        if (key.isNotBlank()) add(key)
                    }
                }
            }
        for (key in keys) {
            for ((loc, table) in locales) {
                if (key !in table) {
                    issues += ValidationIssue(file.toString(), "Missing $loc string for $key")
                }
            }
        }
    }

    private fun loadSchema(root: Path, relative: String): Schema {
        val text = root.resolve(relative).readText()
        return registry.getSchema(text)
    }

    private fun loadRequiredLocales(
        root: Path,
        issues: MutableList<ValidationIssue>
    ): Map<String, Map<String, String>> {
        val out = linkedMapOf<String, Map<String, String>>()
        for (loc in ContentValidator.REQUIRED_LOCALES) {
            val relative = "locales/$loc.json"
            val file = root.resolve(relative)
            if (!file.isRegularFile()) {
                issues += ValidationIssue(file.toString(), "Missing required locale file for $loc")
                out[loc] = emptyMap()
                continue
            }
            out[loc] = loadLocale(root, relative)
        }
        return out
    }

    private fun loadLocale(root: Path, relative: String): Map<String, String> {
        val file = root.resolve(relative)
        if (!file.isRegularFile()) return emptyMap()
        val node = mapper.readTree(file.readText())
        val out = mutableMapOf<String, String>()
        for ((k, v) in node.properties()) out[k] = v.asString()
        return out
    }

    private fun loadAllowlist(path: Path): Set<String> {
        if (!path.isRegularFile()) return emptySet()
        val node = mapper.readTree(path.readText())
        return node.path("ids").values().map { it.asString() }.toSet()
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
