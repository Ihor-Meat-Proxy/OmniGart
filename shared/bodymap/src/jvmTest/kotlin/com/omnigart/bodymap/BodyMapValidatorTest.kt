package com.omnigart.bodymap

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class BodyMapValidatorTest {
    private fun contentRoot(): Path {
        val prop = System.getProperty("omnigart.content.root")
        check(!prop.isNullOrBlank()) { "omnigart.content.root required" }
        return Path.of(prop)
    }

    private fun bodyMapRoot(): Path {
        val prop = System.getProperty("omnigart.bodymap.root")
        check(!prop.isNullOrBlank()) { "omnigart.bodymap.root required" }
        return Path.of(prop)
    }

    @Test
    fun everyExerciseMuscleExistsInBodyMap() {
        val report = BodyMapValidator(contentRoot(), bodyMapRoot()).validate()
        assertTrue(
            report.ok,
            report.issues.joinToString("\n") { "${it.path}: ${it.message}" },
        )
        assertTrue(report.exerciseCount >= 120)
        assertTrue(report.mappedMuscleCount >= 20)
    }
}
