package com.omnigart.shared.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LayeredContentLoaderTest {
    private fun baselineStore(
        packVersion: Int = 1,
        extraFiles: Map<String, String> = emptyMap()
    ): MapContentStore {
        val manifest =
            """
            {
              "schemaVersion": 1,
              "packId": "omnigart-baseline",
              "packVersion": $packVersion,
              "offline": true,
              "sha256": "baseline-digest",
              "sections": {
                "exercises": { "fileCount": 1, "sha256": "ex" }
              },
              "localePolicy": {
                "source": "en-GB",
                "included": ["en-GB", "uk"],
                "forbidden": ["ru"]
              }
            }
            """.trimIndent()
        val files =
            mutableMapOf(
                "manifest.json" to manifest,
                "exercises/data/back-squat.json" to """{"id":"back-squat","from":"baseline"}""",
                "exercises/locales/en-GB.json" to """{"exercise.back_squat.name":"Back squat"}"""
            )
        files.putAll(extraFiles)
        return MapContentStore.ofText(files)
    }

    private fun updateStore(
        packVersion: Int,
        sha256: String = "update-digest",
        signed: Boolean = true,
        files: Map<String, String> = emptyMap()
    ): MapContentStore {
        val manifest =
            """
            {
              "schemaVersion": 1,
              "packId": "omnigart-update",
              "packVersion": $packVersion,
              "offline": true,
              "sha256": "$sha256",
              "sections": {
                "exercises": { "fileCount": 1, "sha256": "ex2" }
              },
              "localePolicy": {
                "source": "en-GB",
                "included": ["en-GB", "uk"],
                "forbidden": ["ru"]
              }
            }
            """.trimIndent()
        val map =
            mutableMapOf(
                "manifest.json" to manifest,
                "exercises/data/back-squat.json" to """{"id":"back-squat","from":"update"}"""
            )
        map.putAll(files)
        if (signed) {
            map[UpdatePackSignatureVerifier.SIGNATURE_FILE] =
                UpdatePackSignatureVerifier.SIGNATURE_PREFIX + sha256
        }
        return MapContentStore.ofText(map)
    }

    @Test
    fun bundledAloneWorksOffline() {
        val loader = LayeredContentLoader(bundled = baselineStore())
        assertFalse(loader.updateApplied)
        assertEquals(1, loader.effectiveManifest.packVersion)
        val squat = loader.readText("exercises/data/back-squat.json")
        assertNotNull(squat)
        assertTrue(squat.contains("baseline"))
        assertTrue(loader.exists("exercises/locales/en-GB.json"))
    }

    @Test
    fun newerVerifiedUpdateOverridesBaselineFile() {
        val loader =
            LayeredContentLoader(
                bundled = baselineStore(packVersion = 1),
                updatePack = updateStore(packVersion = 2)
            )
        assertTrue(loader.updateApplied)
        assertEquals(2, loader.effectiveManifest.packVersion)
        val squat = loader.readText("exercises/data/back-squat.json")
        assertNotNull(squat)
        assertTrue(squat.contains("update"))
        // Fall through for files only in baseline
        assertTrue(loader.exists("exercises/locales/en-GB.json"))
        assertNotNull(loader.readText("exercises/locales/en-GB.json"))
    }

    @Test
    fun unsignedUpdateIsIgnored() {
        val loader =
            LayeredContentLoader(
                bundled = baselineStore(),
                updatePack = updateStore(packVersion = 9, signed = false)
            )
        assertFalse(loader.updateApplied)
        assertTrue(loader.readText("exercises/data/back-squat.json")!!.contains("baseline"))
    }

    @Test
    fun olderOrEqualUpdateIsIgnored() {
        val loader =
            LayeredContentLoader(
                bundled = baselineStore(packVersion = 3),
                updatePack = updateStore(packVersion = 3)
            )
        assertFalse(loader.updateApplied)
    }

    @Test
    fun evaluateUpdateRejectsBadSchema() {
        val bundled = baselineStore()
        val loader = LayeredContentLoader(bundled = bundled)
        val bad =
            MapContentStore.ofText(
                mapOf(
                    "manifest.json" to
                        """
                        {
                          "schemaVersion": 99,
                          "packId": "x",
                          "packVersion": 10,
                          "offline": true,
                          "sha256": "x",
                          "sections": {}
                        }
                        """.trimIndent()
                )
            )
        val result = loader.evaluateUpdate(bad)
        assertIs<UpdateEvaluation.Rejected>(result)
        assertTrue(result.reason.contains("schemaVersion"))
    }

    @Test
    fun evaluateUpdateAcceptsNewerSignedPack() {
        val loader = LayeredContentLoader(bundled = baselineStore())
        val result = loader.evaluateUpdate(updateStore(packVersion = 5))
        assertIs<UpdateEvaluation.Accepted>(result)
        assertEquals(5, result.manifest.packVersion)
    }

    @Test
    fun listMergesBaselineAndUpdatePaths() {
        val loader =
            LayeredContentLoader(
                bundled = baselineStore(),
                updatePack =
                updateStore(
                    packVersion = 2,
                    files = mapOf("exercises/data/new-move.json" to "{}")
                )
            )
        val listed = loader.list("exercises/data")
        assertTrue(listed.contains("exercises/data/back-squat.json"))
        assertTrue(listed.contains("exercises/data/new-move.json"))
    }

    @Test
    fun missingUpdateFileFallsThrough() {
        val loader =
            LayeredContentLoader(
                bundled = baselineStore(),
                updatePack =
                updateStore(
                    packVersion = 2,
                    files = emptyMap()
                )
            )
        // Update overrides squat; locale only on baseline
        assertNull(
            updateStore(packVersion = 2).readText("exercises/locales/en-GB.json")
        )
        assertNotNull(loader.readText("exercises/locales/en-GB.json"))
    }
}
