package com.omnigart.evidence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EvidenceRepositoryTest {
    private val sampleJson =
        """
        [
          {
            "id": "schoenfeld_volume_2016",
            "type": "meta-analysis",
            "title": "Dose-response relationship between weekly resistance training volume and increases in muscle mass: A systematic review and meta-analysis",
            "authors": ["Schoenfeld BJ", "Ogborn D", "Krieger JW"],
            "year": 2016,
            "journal": "Journal of Sports Sciences",
            "doi": "10.1080/02640414.2016.1210197",
            "openAccess": false,
            "population": "Healthy adults",
            "keyFinding": {
              "enGB": "Meta-analysis reported greater hypertrophy with higher weekly set volumes, with diminishing returns at the upper end of studied ranges.",
              "keys": {
                "enGB": "evidence.schoenfeld_volume_2016.key_finding",
                "uk": "evidence.schoenfeld_volume_2016.key_finding"
              }
            },
            "evidenceStrength": "high",
            "licenceNote": "Fair dealing for criticism/review.",
            "retracted": false,
            "lastVerified": "2026-10-22"
          }
        ]
        """.trimIndent()

    @Test
    fun parsesAndBuildsInfoSheet() {
        val repo = EvidenceCatalogParser.repositoryFromCatalogJson(sampleJson)
        assertEquals(1, repo.size)
        val sheet = repo.infoSheet("schoenfeld_volume_2016")
        assertNotNull(sheet)
        assertEquals(EvidenceStrength.HIGH, sheet.strength)
        assertEquals("High", sheet.strengthLabel)
        assertTrue(sheet.linkUrl!!.startsWith("https://doi.org/"))
        assertTrue(sheet.plainLanguageSummary.contains("hypertrophy"))
        assertTrue(sheet.citationLine.contains("2016"))
    }
}
