package com.omnigart.evidence

import kotlinx.serialization.json.Json

/**
 * In-memory registry for ⓘ sheet lookups: claim → evidence list sorted by grade.
 */
class EvidenceRegistry(
    val entries: List<EvidenceEntry>,
    val claims: List<Claim>,
) {
    private val entryById: Map<String, EvidenceEntry> = entries.associateBy { it.id }
    private val claimById: Map<String, Claim> = claims.associateBy { it.id }
    private val byContentRef: Map<String, EvidenceEntry> =
        entries.flatMap { e ->
            buildList {
                e.contentRefKeys.forEach { key -> add(key to e) }
                e.doi?.let { add("doi:$it" to e) }
                e.pmid?.let { add("pmid:$it" to e) }
            }
        }.toMap()

    fun claim(id: String): Claim? = claimById[id]

    fun entry(id: String): EvidenceEntry? = entryById[id]

    fun resolveContentRef(kind: String, id: String): EvidenceEntry? =
        byContentRef["$kind:$id"]

    /**
     * Evidence supporting a claim, highest GRADE first, then year descending.
     * Retracted entries are excluded unless [includeRetracted] is true.
     */
    fun evidenceForClaim(
        claimId: String,
        includeRetracted: Boolean = false,
    ): List<EvidenceEntry> {
        val claim = claimById[claimId] ?: return emptyList()
        return claim.evidenceIds
            .mapNotNull { entryById[it] }
            .filter { includeRetracted || !it.retracted }
            .sortedWith(
                compareBy<EvidenceEntry> { gradeRank(it.evidenceGrade.level) }
                    .thenByDescending { it.year },
            )
    }

    fun gradeLabel(
        level: EvidenceGradeLevel,
        locale: EvidenceLocale = EvidenceLocale.EN_GB,
    ): String = GradeLabels.label(level, locale)

    companion object {
        val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
        }

        fun fromJsonStrings(
            entryJsons: List<String>,
            claimsJson: String,
        ): EvidenceRegistry {
            val entries = entryJsons.map { json.decodeFromString(EvidenceEntry.serializer(), it) }
            val registry = json.decodeFromString(ClaimsRegistry.serializer(), claimsJson)
            return EvidenceRegistry(entries, registry.claims)
        }

        fun gradeRank(level: EvidenceGradeLevel): Int = when (level) {
            EvidenceGradeLevel.HIGH -> 0
            EvidenceGradeLevel.MODERATE -> 1
            EvidenceGradeLevel.LOW -> 2
            EvidenceGradeLevel.VERY_LOW -> 3
        }
    }
}

private fun gradeRank(level: EvidenceGradeLevel): Int = EvidenceRegistry.gradeRank(level)
