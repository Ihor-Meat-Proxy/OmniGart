package com.omnigart.content

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YogaPilatesContentValidatorTest {
    private fun yogaRoot(): Path = Path.of(System.getProperty("omnigart.yoga.root") ?: error("omnigart.yoga.root required"))

    private fun pilatesRoot(): Path = Path.of(System.getProperty("omnigart.pilates.root") ?: error("omnigart.pilates.root required"))

    @Test
    fun bundledLibrariesValidateWithExpectedCounts() {
        val report = YogaPilatesContentValidator(yogaRoot(), pilatesRoot()).validate()
        assertTrue(report.ok, report.issues.joinToString("\n") { "${it.path}: ${it.message}" })
        assertEquals(61, report.yogaPoseCount)
        assertEquals(12, report.yogaFlowCount)
        assertEquals(50, report.pilatesExerciseCount)
        assertEquals(10, report.pilatesSessionCount)
    }

    @Test
    fun everyPoseHasSanskritAndEnglishKeys() {
        val poses = yogaRoot().resolve("data/poses").toFile().listFiles { f -> f.extension == "json" }.orEmpty()
        assertTrue(poses.size >= 60)
        poses.forEach { file ->
            val text = file.readText()
            assertTrue("englishNameKeys" in text, file.name)
            assertTrue("sanskritNameKeys" in text, file.name)
            assertTrue("\"bundled\": true" in text, file.name)
        }
    }

    @Test
    fun contraindicationNotesUseGeneralSafetyFraming() {
        val yogaFiles = yogaRoot().resolve("data/poses").toFile().listFiles { f -> f.extension == "json" }.orEmpty()
        val pilatesFiles =
            pilatesRoot().resolve("data/exercises").toFile().listFiles { f -> f.extension == "json" }.orEmpty()
        (yogaFiles.toList() + pilatesFiles.toList()).forEach { file ->
            if ("contraindicationNotes" in file.readText()) {
                assertTrue(
                    file.readText().lowercase().contains("general safety"),
                    "${file.name} missing general safety framing"
                )
            }
        }
    }

    @Test
    fun allRequiredLocalesPresentAndNeverRussian() {
        listOf(yogaRoot(), pilatesRoot()).forEach { root ->
            val names = root.resolve("locales").toFile().list()?.toList().orEmpty()
            assertTrue(names.none { it.startsWith("ru") }, names.toString())
            for (loc in ContentValidator.REQUIRED_LOCALES) {
                assertTrue("$loc.json" in names, "${root.fileName} missing $loc.json")
            }
        }
    }

    @Test
    fun pilatesApparatusCoverage() {
        val files = pilatesRoot().resolve("data/exercises").toFile().listFiles { f -> f.extension == "json" }.orEmpty()
        val apparatuses = files.map { it.readText() }.map { text ->
            Regex(""""apparatus":\s*"([^"]+)"""").find(text)!!.groupValues[1]
        }
        assertTrue(apparatuses.count { it == "mat" } >= 25)
        assertTrue(apparatuses.count { it == "reformer" } >= 18)
    }
}
