package com.omnigart.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvidenceRegistryTest {

    private val sampleEntries = listOf(
        """
        {
          "id": "high-paper",
          "type": "meta-analysis",
          "title": "High paper",
          "authors": ["A"],
          "year": 2018,
          "keyFinding": { "enGB": "High finding sentence.", "uk": "Високий висновок тут." },
          "evidenceGrade": { "level": "high", "rationale": "Large MA of RCTs." },
          "usedByClaimIds": ["claim-a"],
          "retracted": false,
          "lastVerified": "2026-10-22",
          "verificationStatus": "verified"
        }
        """.trimIndent(),
        """
        {
          "id": "low-paper",
          "type": "cohort",
          "title": "Low paper",
          "authors": ["B"],
          "year": 2020,
          "keyFinding": { "enGB": "Low finding sentence.", "uk": "Низький висновок тут." },
          "evidenceGrade": { "level": "low", "rationale": "Small cohort only." },
          "usedByClaimIds": ["claim-a"],
          "retracted": false,
          "lastVerified": "2026-10-22",
          "verificationStatus": "verified",
          "contentRefKeys": ["doi:10.example/low"]
        }
        """.trimIndent(),
        """
        {
          "id": "retracted-paper",
          "type": "rct",
          "title": "Retracted paper",
          "authors": ["C"],
          "year": 2019,
          "keyFinding": { "enGB": "Should be hidden.", "uk": "Має бути сховано." },
          "evidenceGrade": { "level": "high", "rationale": "Was high before retraction." },
          "usedByClaimIds": ["claim-a"],
          "retracted": true,
          "lastVerified": "2026-10-22",
          "verificationStatus": "retracted"
        }
        """.trimIndent(),
    )

    private val claimsJson = """
        {
          "version": 1,
          "claims": [
            {
              "id": "claim-a",
              "category": "formula",
              "textKeys": {
                "enGB": "claim.a.text",
                "uk": "claim.a.text"
              },
              "evidenceIds": ["low-paper", "high-paper", "retracted-paper"]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun evidenceForClaim_sortedByGrade_excludesRetracted() {
        val registry = EvidenceRegistry.fromJsonStrings(sampleEntries, claimsJson)
        val list = registry.evidenceForClaim("claim-a")
        assertEquals(listOf("high-paper", "low-paper"), list.map { it.id })
    }

    @Test
    fun evidenceForClaim_canIncludeRetracted() {
        val registry = EvidenceRegistry.fromJsonStrings(sampleEntries, claimsJson)
        val list = registry.evidenceForClaim("claim-a", includeRetracted = true)
        assertEquals(3, list.size)
        assertTrue(list.any { it.id == "retracted-paper" })
        // Both high-grade; newer year first among equals.
        assertEquals("retracted-paper", list.first().id)
    }

    @Test
    fun resolveContentRef_works() {
        val registry = EvidenceRegistry.fromJsonStrings(sampleEntries, claimsJson)
        val hit = registry.resolveContentRef("doi", "10.example/low")
        assertEquals("low-paper", hit?.id)
    }

    @Test
    fun gradeLabels_enAndUk() {
        assertEquals("High confidence", GradeLabels.label(EvidenceGradeLevel.HIGH))
        assertEquals("Висока впевненість", GradeLabels.label(EvidenceGradeLevel.HIGH, EvidenceLocale.UK))
        assertTrue(GradeLabels.shortBlurb(EvidenceGradeLevel.VERY_LOW).contains("uncertain"))
    }
}
