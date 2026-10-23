package com.omnigart.evidence

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EvidenceType {
    @SerialName("meta-analysis") META_ANALYSIS,
    @SerialName("systematic-review") SYSTEMATIC_REVIEW,
    @SerialName("rct") RCT,
    @SerialName("cohort") COHORT,
    @SerialName("guideline") GUIDELINE,
    @SerialName("textbook") TEXTBOOK,
    @SerialName("consensus") CONSENSUS,
}

/**
 * GRADE-inspired strength badge. Assignment rules: content/evidence/GRADE_RULES.md
 */
@Serializable
enum class EvidenceStrength {
    @SerialName("high") HIGH,
    @SerialName("moderate") MODERATE,
    @SerialName("low") LOW,
    @SerialName("very-low") VERY_LOW,
}

@Serializable
data class KeyFindingKeys(
    val enGB: String,
    val uk: String,
)

@Serializable
data class KeyFinding(
    val enGB: String,
    val keys: KeyFindingKeys,
)

@Serializable
data class EvidenceEntry(
    val id: String,
    val type: EvidenceType,
    val title: String,
    val authors: List<String>,
    val year: Int,
    val journal: String? = null,
    val doi: String? = null,
    val pmid: String? = null,
    val pmcid: String? = null,
    val url: String? = null,
    val openAccess: Boolean,
    val sampleSize: Int? = null,
    val population: String? = null,
    val keyFinding: KeyFinding,
    val evidenceStrength: EvidenceStrength,
    val licenceNote: String,
    val retracted: Boolean = false,
    val lastVerified: String,
    val usedBy: List<String> = emptyList(),
)

/**
 * Plain model for the ⓘ bottom sheet / detail card in apps.
 */
data class EvidenceInfoSheet(
    val evidenceId: String,
    val plainLanguageSummary: String,
    val strength: EvidenceStrength,
    val strengthLabel: String,
    val typeLabel: String,
    val citationLine: String,
    val linkUrl: String?,
    val openAccess: Boolean,
    val retracted: Boolean,
    val sampleSize: Int?,
    val population: String?,
    val year: Int,
)
