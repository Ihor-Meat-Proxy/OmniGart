package com.omnigart.planning

/**
 * Planning-layer pointers into [content/evidence] stable ids.
 * Full bibliographic metadata and ⓘ sheet copy live in the evidence catalog.
 */
object EvidenceCatalog {
    val SCHOENFELD_VOLUME_2016 = EvidencePointer(
        refId = "schoenfeld_volume_2016",
        label = "Schoenfeld et al. 2016 — weekly RT volume dose–response (hypertrophy meta-analysis)"
    )
    val PELLAND_DOSE_2025 = EvidencePointer(
        refId = "pelland_rt_dose_2025",
        label = "Pelland et al. 2025/26 — RT volume/frequency meta-regressions (fractional sets)"
    )
    val SCHOENFELD_FREQ_2016 = EvidencePointer(
        refId = "schoenfeld_frequency_2016",
        label = "Schoenfeld et al. 2016 — RT frequency ≥2×/week favours hypertrophy when volume not equated"
    )
    val SCHOENFELD_FREQ_2019 = EvidencePointer(
        refId = "schoenfeld_frequency_2019",
        label = "Schoenfeld, Grgic & Krieger 2019 — volume-equated frequency effects small/null for hypertrophy"
    )
    val ACSM_RT_2026 = EvidencePointer(
        refId = "acsm_resistance_training_2026",
        label = "ACSM 2026 Position Stand — RT prescription overview of reviews"
    )
    val ACSM_GARBER_2011 = EvidencePointer(
        refId = "garber_acsm_quantity_2011",
        label = "Garber et al. 2011 ACSM — quantity/quality of exercise guidance"
    )
    val SEILER_2010 = EvidencePointer(
        refId = "seiler_intensity_distribution_2010",
        label = "Seiler 2010 — ~80/20 intensity distribution in endurance athletes (descriptive)"
    )
    val MUNOZ_POLARIZED = EvidencePointer(
        refId = "munoz_polarized_runners_2014",
        label = "Muñoz et al. — polarized vs between-thresholds in recreational runners"
    )
    val TEN_PERCENT_CAVEAT = EvidencePointer(
        refId = "omnigart_mileage_caveat",
        label = "OmniGart caveat — fixed ~10% weekly mileage rule lacks strong RCT support; use readiness/injury first"
    )
}
