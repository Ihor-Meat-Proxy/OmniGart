package com.omnigart.content

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentValidatorTest {
    private fun contentRoot(): Path {
        val prop = System.getProperty("omnigart.content.root")
        check(!prop.isNullOrBlank()) { "omnigart.content.root system property required" }
        return Path.of(prop)
    }

    @Test
    fun libraryPassesSchemaUniqueIdsCrossRefsAndLocales() {
        val report = ContentValidator(contentRoot()).validate()
        assertTrue(
            report.ok,
            report.issues.joinToString("\n") { "${it.path}: ${it.message}" }
        )
        assertTrue(
            report.exerciseCount in 110..150,
            "expected ~120 exercises, got ${report.exerciseCount}"
        )
        assertTrue(
            report.templateCount >= 15,
            "expected ≥15 templates, got ${report.templateCount}"
        )
    }

    @Test
    fun noRussianLocaleFilePresent() {
        val locales = contentRoot().resolve("locales")
        val names = locales.toFile().list()?.toList().orEmpty()
        assertTrue(names.none { it.startsWith("ru") }, "found ru locale: $names")
        assertTrue("en-GB.json" in names)
        assertTrue("uk.json" in names)
    }

    @Test
    fun everyExerciseHasAtLeastOneEvidenceRef() {
        val data = contentRoot().resolve("data").toFile()
        val missing = data.listFiles { f -> f.extension == "json" }.orEmpty().filter { file ->
            !file.readText().contains("\"evidenceRefs\"") ||
                file.readText().contains("\"evidenceRefs\": []")
        }.map { it.name }
        assertEquals(emptyList(), missing)
    }
}
