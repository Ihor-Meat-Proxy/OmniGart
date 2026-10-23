package com.omnigart.shared.content

/**
 * Result of signature + schema checks for an update pack.
 * Bundled baseline is trusted via the app binary / store signature and skips crypto.
 */
sealed class PackVerificationResult {
    data object Accepted : PackVerificationResult()

    data class Rejected(val reason: String) : PackVerificationResult()
}

fun interface PackSignatureVerifier {
    fun verify(manifest: ContentManifest, store: ContentStore): PackVerificationResult
}

/**
 * Trust-on-first-use for the **bundled** baseline only (shipped inside the app).
 * Never use for server-downloaded packs.
 */
object BundledTrustVerifier : PackSignatureVerifier {
    override fun verify(manifest: ContentManifest, store: ContentStore): PackVerificationResult = PackVerificationResult.Accepted
}

/**
 * Minimal signature gate for update packs.
 *
 * Production will verify an Ed25519 (or similar) signature over `manifest.sha256`
 * using the embedded OmniGart content public key. This implementation accepts a pack
 * only when [store] contains `signature.txt` whose body equals
 * `test-ok:<manifest.sha256>` — enough for unit tests and local dry-runs until the
 * real key material is wired.
 */
class UpdatePackSignatureVerifier(private val requireSignatureFile: Boolean = true) : PackSignatureVerifier {
    override fun verify(manifest: ContentManifest, store: ContentStore): PackVerificationResult {
        if (!requireSignatureFile) {
            return PackVerificationResult.Rejected("Update packs must present a signature")
        }
        val sig =
            store.readText(SIGNATURE_FILE)?.trim()
                ?: return PackVerificationResult.Rejected("Missing $SIGNATURE_FILE")
        val expected = "$SIGNATURE_PREFIX${manifest.sha256}"
        return if (sig == expected) {
            PackVerificationResult.Accepted
        } else {
            PackVerificationResult.Rejected("Signature mismatch")
        }
    }

    companion object {
        const val SIGNATURE_FILE = "signature.txt"
        const val SIGNATURE_PREFIX = "test-ok:"
    }
}

object PackSchemaChecker {
    fun check(manifest: ContentManifest): PackVerificationResult {
        if (manifest.schemaVersion !in ContentSchema.SUPPORTED_SCHEMA_VERSIONS) {
            return PackVerificationResult.Rejected(
                "Unsupported schemaVersion ${manifest.schemaVersion}"
            )
        }
        if (manifest.packVersion < 1) {
            return PackVerificationResult.Rejected("packVersion must be >= 1")
        }
        if (manifest.sha256.isBlank()) {
            return PackVerificationResult.Rejected("manifest sha256 required")
        }
        val forbidden = manifest.localePolicy.forbidden
        if ("ru" !in forbidden && forbidden.isNotEmpty()) {
            // Soft rule: if a forbidden list is present it should include ru.
            return PackVerificationResult.Rejected("localePolicy.forbidden must include ru")
        }
        return PackVerificationResult.Accepted
    }
}
