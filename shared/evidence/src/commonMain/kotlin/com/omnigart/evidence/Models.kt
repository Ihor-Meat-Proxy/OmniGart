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
    @SerialName("consensus-statement") CONSENSUS_STATEMENT,
    @SerialName("book-coaching-source") BOOK_COACHING_SOURCE,
    @SerialName("report") REPORT,
}

@Serializable
enum class EvidenceGradeLevel {
    @SerialName("high") HIGH,
    @SerialName("moderate") MODERATE,
    @SerialName("low") LOW,
    @SerialName("very-low") VERY_LOW,
}

@Serializable
enum class VerificationStatus {
    @SerialName("verified") VERIFIED,
    @SerialName("unverified") UNVERIFIED,
    @SerialName("mismatched") MISMATCHED,
    @SerialName("retracted") RETRACTED,
}

@Serializable
enum class ClaimCategory {
    @SerialName("formula") FORMULA,
    @SerialName("goal") GOAL,
    @SerialName("programming") PROGRAMMING,
    @SerialName("nutrition") NUTRITION,
    @SerialName("recovery") RECOVERY,
    @SerialName("technique") TECHNIQUE,
    @SerialName("safety") SAFETY,
    @SerialName("cardio") CARDIO,
    @SerialName("body-comp") BODY_COMP,
    @SerialName("load-monitoring") LOAD_MONITORING,
    @SerialName("other") OTHER,
}

@Serializable
data class LocalizedFinding(
    val enGB: String,
    val uk: String,
)

@Serializable
data class EvidenceGrade(
    val level: EvidenceGradeLevel,
    val rationale: String,
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
    val openAccess: Boolean = false,
    val population: String? = null,
    val sampleSize: Int? = null,
    val studyCount: Int? = null,
    val keyFinding: LocalizedFinding,
    val effectSize: String? = null,
    val evidenceGrade: EvidenceGrade,
    val limitations: List<String> = emptyList(),
    val usedByClaimIds: List<String> = emptyList(),
    val retracted: Boolean = false,
    val retractionNotes: String? = null,
    val lastVerified: String,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val contentRefKeys: List<String> = emptyList(),
)

@Serializable
data class ClaimTextKeys(
    val enGB: String,
    val uk: String,
)

@Serializable
data class Claim(
    val id: String,
    val category: ClaimCategory,
    val textKeys: ClaimTextKeys,
    val evidenceIds: List<String>,
    val notes: String? = null,
)

@Serializable
data class ClaimsRegistry(
    val version: Int,
    val claims: List<Claim>,
)
