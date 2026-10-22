package com.omnigart.planning

object EvidenceCatalog {
    val SCHOENFELD_VOLUME_2016 = EvidencePointer(
        refId = "doi:10.1080/02640414.2016.1210197",
        label = "Schoenfeld et al. 2016 — weekly RT volume dose–response (hypertrophy meta-analysis)"
    )
    val PELLAND_DOSE_2025 = EvidencePointer(
        refId = "doi:10.1007/s40279-025-02344-w",
        label = "Pelland et al. 2025/26 — RT volume/frequency meta-regressions (fractional sets)"
    )
    val SCHOENFELD_FREQ_2016 = EvidencePointer(
        refId = "doi:10.1007/s40279-016-0543-8",
        label = "Schoenfeld et al. 2016 — RT frequency ≥2×/week favours hypertrophy when volume not equated"
    )
    val SCHOENFELD_FREQ_2019 = EvidencePointer(
        refId = "doi:10.1080/02640414.2018.1555906",
        label = "Schoenfeld, Grgic & Krieger 2019 — volume-equated frequency effects small/null for hypertrophy"
    )
    val ACSM_RT_2026 = EvidencePointer(
        refId = "doi:10.1249/MSS.0000000000003897",
        label = "ACSM 2026 Position Stand — RT prescription overview of reviews"
    )
    val ACSM_GARBER_2011 = EvidencePointer(
        refId = "doi:10.1249/MSS.0b013e318213fefb",
        label = "Garber et al. 2011 ACSM — quantity/quality of exercise guidance"
    )
    val SEILER_2010 = EvidencePointer(
        refId = "doi:10.1123/ijspp.5.3.276",
        label = "Seiler 2010 — ~80/20 intensity distribution in endurance athletes (descriptive)"
    )
    val MUNOZ_POLARIZED = EvidencePointer(
        refId = "pmid:23752040",
        label = "Muñoz et al. — polarized vs between-thresholds in recreational runners"
    )
    val TEN_PERCENT_CAVEAT = EvidencePointer(
        refId = "guideline:omnigart-mileage-caveat",
        label = "OmniGart caveat — fixed ~10% weekly mileage rule lacks strong RCT support; use readiness/injury first"
    )
}
