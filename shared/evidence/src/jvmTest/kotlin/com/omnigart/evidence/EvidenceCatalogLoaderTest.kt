package com.omnigart.evidence

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class EvidenceCatalogLoaderTest {
    private fun evidenceRoot(): Path {
        val prop = System.getProperty("omnigart.evidence.root")
        require(!prop.isNullOrBlank()) { "omnigart.evidence.root not set" }
        return Path.of(prop)
    }

    @Test
    fun catalogValidatesAndLoads() {
        val loader = EvidenceCatalogLoader(evidenceRoot())
        val report = loader.validate()
        assertTrue(report.ok, report.issues.joinToString("\n") { "${it.path}: ${it.message}" })
        assertTrue(report.entryCount >= 20)
        val repo = loader.loadRepository()
        assertTrue(repo.size == report.entryCount)
        assertTrue(repo.get("mifflin_st_jeor_1990") != null)
        assertTrue(repo.infoSheet("morton_protein_2018")?.strength == EvidenceStrength.HIGH)
    }
}
