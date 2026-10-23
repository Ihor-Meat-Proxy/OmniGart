package com.omnigart.achievements

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class CatalogueValidatorTest {
    @Test
    fun cataloguePassesValidation() {
        val root = Path.of(
            System.getProperty(
                "omnigart.achievements.root",
                Path.of("").toAbsolutePath().resolve("../../content/achievements").normalize().toString(),
            ),
        )
        val report = CatalogueValidator(root).validate()
        assertTrue(
            report.ok,
            report.issues.joinToString("\n") { "${it.path}: ${it.message}" },
        )
        assertTrue(report.achievementCount >= 100, "expected ~120 achievements, got ${report.achievementCount}")
    }
}
