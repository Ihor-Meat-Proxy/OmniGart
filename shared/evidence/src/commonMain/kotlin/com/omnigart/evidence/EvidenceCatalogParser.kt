package com.omnigart.evidence

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object EvidenceCatalogParser {
    val json: Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = false
            explicitNulls = false
        }

    fun parseCatalog(text: String): List<EvidenceEntry> =
        json.decodeFromString(ListSerializer(EvidenceEntry.serializer()), text)

    fun parseEntry(text: String): EvidenceEntry =
        json.decodeFromString(EvidenceEntry.serializer(), text)

    fun repositoryFromCatalogJson(text: String): EvidenceRepository =
        EvidenceRepository.fromEntries(parseCatalog(text))
}
