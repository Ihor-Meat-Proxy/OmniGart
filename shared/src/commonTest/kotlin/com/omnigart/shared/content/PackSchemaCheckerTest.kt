package com.omnigart.shared.content

import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PackSchemaCheckerTest {
    @Test
    fun acceptsCurrentSchema() {
        val manifest =
            ContentManifest(
                schemaVersion = ContentSchema.CURRENT_SCHEMA_VERSION,
                packId = "t",
                packVersion = 1,
                sha256 = "abc",
                localePolicy = LocalePolicy(forbidden = listOf("ru"))
            )
        assertIs<PackVerificationResult.Accepted>(PackSchemaChecker.check(manifest))
    }

    @Test
    fun rejectsUnknownSchema() {
        val manifest =
            ContentManifest(
                schemaVersion = 999,
                packId = "t",
                packVersion = 1,
                sha256 = "abc"
            )
        val result = PackSchemaChecker.check(manifest)
        assertIs<PackVerificationResult.Rejected>(result)
        assertTrue(result.reason.contains("schemaVersion"))
    }
}
