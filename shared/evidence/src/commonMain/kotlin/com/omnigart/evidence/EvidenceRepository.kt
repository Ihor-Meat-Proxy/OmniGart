package com.omnigart.evidence

/**
 * Lookup API for evidence catalog entries and ⓘ sheet models.
 */
class EvidenceRepository(
    private val entriesById: Map<String, EvidenceEntry>,
) {
    val size: Int get() = entriesById.size

    fun get(id: String): EvidenceEntry? = entriesById[id]

    fun require(id: String): EvidenceEntry =
        entriesById[id] ?: error("Unknown evidence id: $id")

    fun all(): Collection<EvidenceEntry> = entriesById.values

    fun byType(type: EvidenceType): List<EvidenceEntry> =
        entriesById.values.filter { it.type == type }.sortedBy { it.id }

    fun countsByType(): Map<EvidenceType, Int> =
        entriesById.values.groupingBy { it.type }.eachCount()

    /**
     * Build the ⓘ sheet model. [summaryLocale] selects en-GB inline text today;
     * apps should resolve [EvidenceEntry.keyFinding.keys] via i18n for uk.
     */
    fun infoSheet(
        id: String,
        summaryOverride: String? = null,
    ): EvidenceInfoSheet? {
        val entry = get(id) ?: return null
        return entry.toInfoSheet(summaryOverride)
    }

    companion object {
        fun fromEntries(entries: Iterable<EvidenceEntry>): EvidenceRepository {
            val map = linkedMapOf<String, EvidenceEntry>()
            for (entry in entries) {
                require(entry.id !in map) { "Duplicate evidence id: ${entry.id}" }
                map[entry.id] = entry
            }
            return EvidenceRepository(map)
        }
    }
}

fun EvidenceEntry.toInfoSheet(summaryOverride: String? = null): EvidenceInfoSheet =
    EvidenceInfoSheet(
        evidenceId = id,
        plainLanguageSummary = summaryOverride ?: keyFinding.enGB,
        strength = evidenceStrength,
        strengthLabel = evidenceStrength.badgeLabel(),
        typeLabel = type.displayLabel(),
        citationLine = citationLine(),
        linkUrl = outboundUrl(),
        openAccess = openAccess,
        retracted = retracted,
        sampleSize = sampleSize,
        population = population,
        year = year,
    )

fun EvidenceEntry.citationLine(): String {
    val lead = authors.take(3).joinToString(", ")
    val etAl = if (authors.size > 3) " et al." else ""
    val venue = journal?.let { " $it." } ?: ""
    return "$lead$etAl ($year).$venue"
}

fun EvidenceEntry.outboundUrl(): String? =
    when {
        !doi.isNullOrBlank() -> "https://doi.org/$doi"
        !url.isNullOrBlank() -> url
        !pmid.isNullOrBlank() -> "https://pubmed.ncbi.nlm.nih.gov/$pmid/"
        !pmcid.isNullOrBlank() -> "https://www.ncbi.nlm.nih.gov/pmc/articles/$pmcid/"
        else -> null
    }

fun EvidenceStrength.badgeLabel(): String =
    when (this) {
        EvidenceStrength.HIGH -> "High"
        EvidenceStrength.MODERATE -> "Moderate"
        EvidenceStrength.LOW -> "Low"
        EvidenceStrength.VERY_LOW -> "Very low"
    }

fun EvidenceType.displayLabel(): String =
    when (this) {
        EvidenceType.META_ANALYSIS -> "Meta-analysis"
        EvidenceType.SYSTEMATIC_REVIEW -> "Systematic review"
        EvidenceType.RCT -> "Randomised controlled trial"
        EvidenceType.COHORT -> "Cohort / observational"
        EvidenceType.GUIDELINE -> "Guideline"
        EvidenceType.TEXTBOOK -> "Textbook"
        EvidenceType.CONSENSUS -> "Consensus / caveat"
    }
