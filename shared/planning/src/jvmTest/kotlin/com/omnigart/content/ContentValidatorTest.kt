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

    @Test
    fun everyExerciseDeclaresLoadModelAndBodyweightFraction() {
        val data = contentRoot().resolve("data").toFile()
        val missing = data.listFiles { f -> f.extension == "json" }.orEmpty().mapNotNull { file ->
            val text = file.readText()
            when {
                "\"loadModel\"" !in text -> file.name to "loadModel"
                "\"bodyweightFraction\"" !in text -> file.name to "bodyweightFraction"
                "\"allowsAddedWeight\"" !in text -> file.name to "allowsAddedWeight"
                "\"allowsAssistance\"" !in text -> file.name to "allowsAssistance"
                else -> null
            }
        }
        assertEquals(emptyList(), missing)
    }

    @Test
    fun pullUpAndDipFamiliesArePresentWithLinkedProgressions() {
        val data = contentRoot().resolve("data")
        val pullExpected = listOf(
            "pull-up", "chin-up", "neutral-grip-pull-up", "wide-grip-pull-up", "close-grip-pull-up",
            "commando-pull-up", "l-sit-pull-up", "archer-pull-up", "typewriter-pull-up",
            "scapular-pull-up", "negative-pull-up", "assisted-pull-up-band",
            "machine-assisted-pull-up", "weighted-pull-up", "inverted-row"
        )
        val dipExpected = listOf(
            "dip-parallel-bar", "bench-dip", "ring-dip", "straight-bar-dip", "korean-dip",
            "band-assisted-dip", "machine-assisted-dip", "weighted-dip", "negative-dip"
        )
        for (id in pullExpected + dipExpected) {
            assertTrue(
                data.resolve("$id.json").toFile().exists(),
                "missing exercise file $id.json"
            )
        }
        val chin = data.resolve("chin-up.json").toFile().readText()
        assertTrue("\"biceps\"" in chin && chin.indexOf("\"primaryMuscles\"") < chin.indexOf("\"biceps\""))
        assertTrue("youdas_pullup_chinup_emg_2010" in chin, "chin-up should cite Youdas 2010 evidence id")
        val dip = data.resolve("dip-parallel-bar.json").toFile().readText()
        assertTrue(
            "mckenzie_dip_shoulder_2020" in dip || "mckenzie_dip_kinematics_2022" in dip,
            "parallel-bar dip should cite shoulder-extension safety/kinematics evidence id"
        )
        assertTrue("\"loadModel\"" in dip)
    }
}
