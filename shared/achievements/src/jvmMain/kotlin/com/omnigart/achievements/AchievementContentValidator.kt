package com.omnigart.achievements

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.Schema
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

data class AchievementValidationIssue(
    val path: String,
    val message: String,
)

data class AchievementValidationReport(
    val achievementCount: Int,
    val challengeCount: Int,
    val issues: List<AchievementValidationIssue>,
) {
    val ok: Boolean get() = issues.isEmpty()
}

/**
 * Validates achievement catalog JSON, challenge templates, locale keys (en-GB + uk only — no ru),
 * and icon id references against the design icon manifest when present.
 */
class AchievementContentValidator(
    private val contentRoot: Path,
    private val repoRoot: Path = contentRoot.parent?.parent ?: contentRoot,
    private val mapper: ObjectMapper = ObjectMapper(),
) {
    private val registry: SchemaRegistry =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)

    fun validate(): AchievementValidationReport {
        val issues = mutableListOf<AchievementValidationIssue>()
        assertNoRuLocale(issues)

        val achievementSchema = loadSchema("schema/achievement.schema.json")
        val challengeSchema = loadSchema("schema/challenge-template.schema.json")

        val catalogPath = contentRoot.resolve("data/achievements.json")
        val challengePath = contentRoot.resolve("data/challenge-templates.json")
        val localeEn = loadLocale("locales/en-GB.json")
        val localeUk = loadLocale("locales/uk.json")
        val iconIds = loadIconIds()

        var achievementCount = 0
        var challengeCount = 0

        if (!catalogPath.isRegularFile()) {
            issues += AchievementValidationIssue(catalogPath.toString(), "Missing achievements catalog")
        } else {
            val node = mapper.readTree(catalogPath.readText())
            achievementSchema.validate(node).forEach {
                issues += AchievementValidationIssue(catalogPath.toString(), it.message)
            }
            val ids = mutableSetOf<String>()
            val achievements = node.path("achievements")
            achievementCount = achievements.size()
            achievements.forEach { ach ->
                val id = ach.path("id").asText()
                if (!ids.add(id)) {
                    issues += AchievementValidationIssue(catalogPath.toString(), "Duplicate achievement id: $id")
                }
                val titleKey = ach.path("titleKey").asText()
                val descKey = ach.path("descriptionKey").asText()
                if (titleKey !in localeEn) {
                    issues += AchievementValidationIssue(catalogPath.toString(), "Missing en-GB key: $titleKey")
                }
                if (descKey !in localeEn) {
                    issues += AchievementValidationIssue(catalogPath.toString(), "Missing en-GB key: $descKey")
                }
                if (titleKey !in localeUk) {
                    issues += AchievementValidationIssue(catalogPath.toString(), "Missing uk key: $titleKey")
                }
                if (descKey !in localeUk) {
                    issues += AchievementValidationIssue(catalogPath.toString(), "Missing uk key: $descKey")
                }
                val iconId = ach.path("iconId").asText()
                if (iconIds != null && iconId !in iconIds) {
                    issues += AchievementValidationIssue(catalogPath.toString(), "Unknown iconId: $iconId for $id")
                }
                checkHealthyRule(ach, catalogPath, issues)
            }

            // Round-trip Kotlin decode
            runCatching { decodeAchievementCatalog(catalogPath.readText()) }
                .onFailure {
                    issues += AchievementValidationIssue(
                        catalogPath.toString(),
                        "Kotlin decode failed: ${it.message}",
                    )
                }
        }

        if (!challengePath.isRegularFile()) {
            issues += AchievementValidationIssue(challengePath.toString(), "Missing challenge templates")
        } else {
            val node = mapper.readTree(challengePath.readText())
            challengeSchema.validate(node).forEach {
                issues += AchievementValidationIssue(challengePath.toString(), it.message)
            }
            val templates = node.path("templates")
            challengeCount = templates.size()
            templates.forEach { t ->
                val titleKey = t.path("titleKey").asText()
                val descKey = t.path("descriptionKey").asText()
                if (titleKey !in localeEn || descKey !in localeEn) {
                    issues += AchievementValidationIssue(challengePath.toString(), "Missing en-GB challenge keys for ${t.path("id").asText()}")
                }
                if (titleKey !in localeUk || descKey !in localeUk) {
                    issues += AchievementValidationIssue(challengePath.toString(), "Missing uk challenge keys for ${t.path("id").asText()}")
                }
                if (t.path("localOnly").asBoolean(false) != true || t.path("social").asBoolean(true)) {
                    issues += AchievementValidationIssue(challengePath.toString(), "Challenges must be localOnly=true and social=false")
                }
            }
            runCatching { decodeChallengeTemplates(challengePath.readText()) }
                .onFailure {
                    issues += AchievementValidationIssue(
                        challengePath.toString(),
                        "Kotlin decode failed: ${it.message}",
                    )
                }
        }

        return AchievementValidationReport(achievementCount, challengeCount, issues)
    }

    private fun checkHealthyRule(ach: JsonNode, path: Path, issues: MutableList<AchievementValidationIssue>) {
        val id = ach.path("id").asText()
        val metric = ach.path("rule").path("metric").asText()
        val banned = listOf(
            "calorie_deficit",
            "calories_under",
            "weight_lost",
            "weight_loss",
            "bmi_drop",
            "dehydration",
            "water_excess",
            "train_on_low_readiness",
        )
        if (metric in banned) {
            issues += AchievementValidationIssue(path.toString(), "Unhealthy metric banned: $metric ($id)")
        }
    }

    private fun loadSchema(relative: String): Schema {
        val text = contentRoot.resolve(relative).readText()
        return registry.getSchema(text)
    }

    private fun loadLocale(relative: String): Map<String, String> {
        val file = contentRoot.resolve(relative)
        if (!file.exists()) return emptyMap()
        val node = mapper.readTree(file.readText())
        val out = mutableMapOf<String, String>()
        val fields = node.properties()
        for ((k, v) in fields) {
            out[k] = v.asText()
        }
        return out
    }

    private fun assertNoRuLocale(issues: MutableList<AchievementValidationIssue>) {
        val ru = contentRoot.resolve("locales/ru.json")
        if (ru.exists()) {
            issues += AchievementValidationIssue(ru.toString(), "Russian locale is not allowed")
        }
        Files.list(contentRoot.resolve("locales")).use { stream ->
            stream.forEach { p ->
                if (p.fileName.toString().startsWith("ru")) {
                    issues += AchievementValidationIssue(p.toString(), "Russian locale is not allowed")
                }
            }
        }
    }

    private fun loadIconIds(): Set<String>? {
        val manifest = repoRoot.resolve("design/icons/manifest.json")
        if (!manifest.isRegularFile()) return null
        val node = mapper.readTree(manifest.readText())
        val ids = mutableSetOf<String>()
        node.path("icons").forEach { icon -> ids += icon.path("name").asText() }
        return ids
    }
}
