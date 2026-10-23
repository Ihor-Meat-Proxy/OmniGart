package com.omnigart.achievements

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import com.omnigart.achievements.dsl.RuleDsl
import com.omnigart.achievements.model.AchievementDefinition
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

data class CatalogueIssue(val path: String, val message: String)

data class CatalogueReport(
    val achievementCount: Int,
    val byCategory: Map<String, Int>,
    val issues: List<CatalogueIssue>,
) {
    val ok: Boolean get() = issues.isEmpty()
}

/**
 * JVM-side catalogue validator: schema, unique ids, locale keys, icon refs, rule DSL.
 * Complements the Node CI tool; used from Gradle tests.
 */
class CatalogueValidator(
    private val contentRoot: Path,
    private val designIconsManifest: Path? = null,
    private val mapper: ObjectMapper = ObjectMapper(),
) {
    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)

    fun validate(): CatalogueReport {
        val issues = mutableListOf<CatalogueIssue>()
        val schemaNode = mapper.readTree(contentRoot.resolve("schema/achievement.schema.json").readText())
        val schema = registry.getSchema(schemaNode)

        val localeEn = loadLocale("locales/en-GB.json")
        val localeUk = loadLocale("locales/uk.json")
        if (Files.exists(contentRoot.resolve("locales/ru.json"))) {
            issues += CatalogueIssue("locales/ru.json", "Russian locale must not exist")
        }

        val icons = loadAllowedIcons()
        val dataDir = contentRoot.resolve("data")
        val files = Files.list(dataDir).use { stream ->
            stream.filter { it.isRegularFile() && it.extension == "json" }.toList()
        }

        val ids = mutableSetOf<String>()
        val byCategory = mutableMapOf<String, Int>()
        var count = 0

        for (file in files) {
            val array = mapper.readTree(file.readText())
            if (!array.isArray) {
                issues += CatalogueIssue(file.toString(), "Expected JSON array")
                continue
            }
            for (node in array) {
                count++
                schema.validate(node).forEach { err ->
                    issues += CatalogueIssue(file.toString(), err.message)
                }
                val id = node.path("id").asText()
                val category = node.path("category").asText()
                byCategory[category] = (byCategory[category] ?: 0) + 1
                if (!ids.add(id)) {
                    issues += CatalogueIssue(file.toString(), "Duplicate id: $id")
                }
                checkKey(node, "titleKey", localeEn, localeUk, file, issues)
                checkKey(node, "descriptionKey", localeEn, localeUk, file, issues)
                val icon = node.path("iconId").asText()
                if (icon.isNotBlank() && icon !in icons) {
                    issues += CatalogueIssue(file.toString(), "$id: unknown iconId $icon")
                }
                try {
                    val def = CatalogueJson.parseAchievement(node.toString())
                    RuleDsl.validate(def.rule).forEach {
                        issues += CatalogueIssue(file.toString(), "$id: ${it.message}")
                    }
                    healthyGuards(def, file, issues)
                } catch (e: Exception) {
                    issues += CatalogueIssue(file.toString(), "$id: parse failed: ${e.message}")
                }
            }
        }

        return CatalogueReport(count, byCategory.toSortedMap(), issues)
    }

    private fun healthyGuards(def: AchievementDefinition, file: Path, issues: MutableList<CatalogueIssue>) {
        if (def.category.name.equals("BODY", ignoreCase = true) ||
            def.category == com.omnigart.achievements.model.AchievementCategory.BODY
        ) {
            if (def.rule.filters?.excludeWeightLossRate != true) {
                issues += CatalogueIssue(file.toString(), "${def.id}: body must set excludeWeightLossRate")
            }
        }
    }

    private fun checkKey(
        node: JsonNode,
        field: String,
        en: Map<String, String>,
        uk: Map<String, String>,
        file: Path,
        issues: MutableList<CatalogueIssue>,
    ) {
        val key = node.path(field).asText()
        val id = node.path("id").asText()
        if (key !in en) issues += CatalogueIssue(file.toString(), "$id: missing en-GB $field=$key")
        if (key !in uk) issues += CatalogueIssue(file.toString(), "$id: missing uk $field=$key")
    }

    private fun loadLocale(rel: String): Map<String, String> {
        val node = mapper.readTree(contentRoot.resolve(rel).readText())
        val out = mutableMapOf<String, String>()
        val it = node.properties().iterator()
        while (it.hasNext()) {
            val entry = it.next()
            out[entry.key] = entry.value.asText()
        }
        return out
    }

    private fun loadAllowedIcons(): Set<String> {
        val allowed = mutableSetOf<String>()
        val proposed = contentRoot.resolve("proposed-icons.json")
        if (Files.exists(proposed)) {
            mapper.readTree(proposed.readText()).path("icons").forEach { allowed += it.path("name").asText() }
        }
        val manifest = designIconsManifest
            ?: contentRoot.resolve("../../design/icons/manifest.json").normalize()
        if (Files.exists(manifest)) {
            mapper.readTree(manifest.readText()).path("icons").forEach { allowed += it.path("name").asText() }
        }
        return allowed
    }
}
