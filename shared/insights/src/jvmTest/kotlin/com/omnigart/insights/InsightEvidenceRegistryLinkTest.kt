package com.omnigart.insights

import com.omnigart.evidence.EvidenceCatalogLoader
import com.omnigart.evidence.EvidenceRepository
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class InsightEvidenceRegistryLinkTest {

    @Test
    fun everyInsightClaim_resolvesToCatalogEvidenceWithDoiOrPmid() {
        val root =
            Path.of(
                System.getProperty("omnigart.evidence.root")
                    ?: error("omnigart.evidence.root system property required"),
            )
        val repository: EvidenceRepository = EvidenceCatalogLoader(root).loadRepository()

        for (claimId in InsightEvidence.ALL) {
            val evidenceIds = InsightEvidence.evidenceIdsFor(claimId)
            assertTrue(evidenceIds.isNotEmpty(), "claim $claimId has no evidence ids")
            val entries = evidenceIds.mapNotNull { repository.get(it) }
            assertTrue(
                entries.size == evidenceIds.size,
                "claim $claimId missing catalog entries: " +
                    evidenceIds.filter { repository.get(it) == null },
            )
            assertTrue(
                entries.any { !it.doi.isNullOrBlank() || !it.pmid.isNullOrBlank() },
                "claim $claimId must link at least one entry with DOI or PMID",
            )
            assertTrue(
                entries.any { !it.doi.isNullOrBlank() },
                "claim $claimId should include at least one verified DOI entry",
            )
            assertTrue(
                entries.none { it.retracted },
                "claim $claimId must not rely on retracted entries",
            )
        }
    }
}
