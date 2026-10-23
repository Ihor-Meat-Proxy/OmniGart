package com.omnigart.body.evidence

import com.omnigart.body.models.BodyFatMethodId

data class EvidencePointer(
    val refId: String,
    val label: String,
    val kind: String = "doi",
)

/**
 * Verified citation registry for body-composition strategies.
 * Prefer omitting a claim over inventing a DOI.
 */
object BodyEvidenceRegistry {
    val CUN_BAE =
        EvidencePointer(
            refId = "doi:10.2337/dc11-1334",
            label = "Gómez-Ambrosi et al. 2012 Diabetes Care — CUN-BAE body-fat equation",
        )
    val CUN_BAE_COMPARISON =
        EvidencePointer(
            refId = "doi:10.1371/journal.pone.0122291",
            label = "Lara et al. 2015 PLOS ONE — CUN-BAE vs regression models in adults",
        )
    val JACKSON_POLLOCK_MEN =
        EvidencePointer(
            refId = "doi:10.1079/bjn19780152",
            label = "Jackson & Pollock 1978 Br J Nutr — generalised body-density equations for men",
        )
    val JACKSON_POLLOCK_WOMEN =
        EvidencePointer(
            refId = "doi:10.1249/00005768-198023000-00009",
            label = "Jackson, Pollock & Ward 1980 Med Sci Sports Exerc — generalised equations for women",
        )
    val US_NAVY_MEN =
        EvidencePointer(
            refId = "report:ada143890",
            label = "Hodgdon & Beckett 1984 NHRC — % body fat prediction for U.S. Navy men (ADA143890)",
            kind = "report",
        )
    val US_NAVY_WOMEN =
        EvidencePointer(
            refId = "report:ada146456",
            label = "Hodgdon & Beckett 1984 NHRC — % body fat prediction for U.S. Navy women (ADA146456)",
            kind = "report",
        )
    val WHR_ASHWELL_REVIEW =
        EvidencePointer(
            refId = "doi:10.1017/s0954422410000144",
            label = "Browning et al. 2010 Nutr Res Rev — waist-to-height ratio screening systematic review",
        )
    val WHR_ASHWELL_PLEA =
        EvidencePointer(
            refId = "doi:10.2174/1876823701103010070",
            label = "Ashwell & Gibson 2011 — waist-to-height ratio as simple cardiometabolic risk indicator",
        )
    val BMI_WELLNESS_FRAMING =
        EvidencePointer(
            refId = "guideline:omnigart-bmi-wellness",
            label =
                "OmniGart framing — BMI and waist-to-height are population screening indices, " +
                    "not diagnoses of health, fitness, or body composition",
            kind = "guideline",
        )

    fun forMethod(id: BodyFatMethodId): List<EvidencePointer> =
        when (id) {
            BodyFatMethodId.CUN_BAE -> listOf(CUN_BAE, CUN_BAE_COMPARISON)
            BodyFatMethodId.US_NAVY -> listOf(US_NAVY_MEN, US_NAVY_WOMEN)
            BodyFatMethodId.JACKSON_POLLOCK_3,
            BodyFatMethodId.JACKSON_POLLOCK_7,
            -> listOf(JACKSON_POLLOCK_MEN, JACKSON_POLLOCK_WOMEN)
            BodyFatMethodId.BMI_WAIST_HEIGHT ->
                listOf(WHR_ASHWELL_REVIEW, WHR_ASHWELL_PLEA, BMI_WELLNESS_FRAMING)
        }

    val all: List<EvidencePointer> =
        listOf(
            CUN_BAE,
            CUN_BAE_COMPARISON,
            JACKSON_POLLOCK_MEN,
            JACKSON_POLLOCK_WOMEN,
            US_NAVY_MEN,
            US_NAVY_WOMEN,
            WHR_ASHWELL_REVIEW,
            WHR_ASHWELL_PLEA,
            BMI_WELLNESS_FRAMING,
        )
}
