package com.omnigart.evidence

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class EvidenceContentLoadTest {

    @Test
    fun loadsSeededRegistryFromDisk() {
        val root = Path.of(
            System.getProperty("omnigart.evidence.root")
                ?: error("omnigart.evidence.root system property required"),
        )
        val entriesDir = root.resolve("entries")
        val claimsPath = root.resolve("claims/registry.json")
        assertTrue(Files.isDirectory(entriesDir), "missing $entriesDir")
        assertTrue(Files.isRegularFile(claimsPath), "missing $claimsPath")

        val entryJsons = Files.list(entriesDir).use { stream ->
            stream.filter { it.fileName.toString().endsWith(".json") }
                .map { Files.readString(it) }
                .toList()
        }
        val claimsJson = Files.readString(claimsPath)
        val registry = EvidenceRegistry.fromJsonStrings(entryJsons, claimsJson)

        assertTrue(registry.entries.isNotEmpty())
        assertTrue(registry.claims.isNotEmpty())
        val sample = registry.claims.first()
        assertTrue(registry.evidenceForClaim(sample.id).isNotEmpty())
    }
}
