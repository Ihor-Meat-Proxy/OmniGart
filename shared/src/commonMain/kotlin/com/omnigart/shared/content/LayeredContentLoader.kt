package com.omnigart.shared.content

import kotlinx.serialization.json.Json

/**
 * Bundle-first content loader.
 *
 * Layering: **bundled baseline** &lt; **verified update pack** (newer [ContentManifest.packVersion]
 * only, after signature + schema checks). Missing files in the update fall through to the
 * baseline so partial packs work. The app must remain fully offline on the bundled layer alone.
 *
 * Server role (not implemented here): signed content updates between releases, optional large
 * media (videos), food offline packs, and remote config — never required for core offline use.
 */
class LayeredContentLoader(
    private val bundled: ContentStore,
    private val updatePack: ContentStore? = null,
    private val bundledVerifier: PackSignatureVerifier = BundledTrustVerifier,
    private val updateVerifier: PackSignatureVerifier = UpdatePackSignatureVerifier(),
    private val json: Json = defaultJson
) {
    private val bundledManifest: ContentManifest =
        readManifest(bundled)
            ?: error("Bundled content missing manifest.json")

    private val activeUpdate: Pair<ContentManifest, ContentStore>?

    init {
        when (val schema = PackSchemaChecker.check(bundledManifest)) {
            is PackVerificationResult.Rejected ->
                error("Bundled manifest failed schema check: ${schema.reason}")
            PackVerificationResult.Accepted -> Unit
        }
        when (val trust = bundledVerifier.verify(bundledManifest, bundled)) {
            is PackVerificationResult.Rejected ->
                error("Bundled pack failed trust check: ${trust.reason}")
            PackVerificationResult.Accepted -> Unit
        }

        activeUpdate =
            if (updatePack == null) {
                null
            } else {
                val updateManifest = readManifest(updatePack)
                if (updateManifest == null) {
                    null
                } else {
                    val schema = PackSchemaChecker.check(updateManifest)
                    val sig = updateVerifier.verify(updateManifest, updatePack)
                    val newer = updateManifest.packVersion > bundledManifest.packVersion
                    if (schema is PackVerificationResult.Accepted &&
                        sig is PackVerificationResult.Accepted &&
                        newer
                    ) {
                        updateManifest to updatePack
                    } else {
                        null
                    }
                }
            }
    }

    val baselineManifest: ContentManifest get() = bundledManifest

    val effectiveManifest: ContentManifest
        get() = activeUpdate?.first ?: bundledManifest

    val updateApplied: Boolean get() = activeUpdate != null

    fun exists(relativePath: String): Boolean {
        val update = activeUpdate?.second
        if (update != null && update.exists(relativePath)) return true
        return bundled.exists(relativePath)
    }

    fun readBytes(relativePath: String): ByteArray? {
        val update = activeUpdate?.second
        if (update != null && update.exists(relativePath)) {
            return update.readBytes(relativePath)
        }
        return bundled.readBytes(relativePath)
    }

    fun readText(relativePath: String): String? = readBytes(relativePath)?.decodeToString()

    fun list(prefix: String = ""): List<String> {
        val fromBundled = bundled.list(prefix)
        val update = activeUpdate?.second ?: return fromBundled
        val fromUpdate = update.list(prefix)
        return (fromBundled + fromUpdate).distinct().sorted()
    }

    /**
     * Explains why an update pack was or was not applied (for diagnostics / tests).
     */
    fun evaluateUpdate(candidate: ContentStore): UpdateEvaluation {
        val manifest =
            readManifest(candidate)
                ?: return UpdateEvaluation.Rejected("Missing manifest.json")
        when (val schema = PackSchemaChecker.check(manifest)) {
            is PackVerificationResult.Rejected ->
                return UpdateEvaluation.Rejected(schema.reason)
            PackVerificationResult.Accepted -> Unit
        }
        when (val sig = updateVerifier.verify(manifest, candidate)) {
            is PackVerificationResult.Rejected ->
                return UpdateEvaluation.Rejected(sig.reason)
            PackVerificationResult.Accepted -> Unit
        }
        if (manifest.packVersion <= bundledManifest.packVersion) {
            return UpdateEvaluation.Rejected(
                "Update packVersion ${manifest.packVersion} is not newer than " +
                    "baseline ${bundledManifest.packVersion}"
            )
        }
        return UpdateEvaluation.Accepted(manifest)
    }

    private fun readManifest(store: ContentStore): ContentManifest? {
        val text = store.readText(MANIFEST_FILE) ?: return null
        return json.decodeFromString(ContentManifest.serializer(), text)
    }

    companion object {
        const val MANIFEST_FILE = "manifest.json"

        val defaultJson: Json =
            Json {
                ignoreUnknownKeys = true
                isLenient = false
            }
    }
}

sealed class UpdateEvaluation {
    data class Accepted(val manifest: ContentManifest) : UpdateEvaluation()

    data class Rejected(val reason: String) : UpdateEvaluation()
}
