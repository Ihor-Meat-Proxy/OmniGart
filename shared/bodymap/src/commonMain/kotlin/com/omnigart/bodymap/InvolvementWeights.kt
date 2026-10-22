package com.omnigart.bodymap

import kotlinx.serialization.Serializable

/**
 * Configurable weights for counting hard/effective sets toward per-muscle training load.
 *
 * Evidence basis (honest):
 * Pelland et al. (2025) multi-level meta-regressions of weekly RT volume found that
 * quantifying *indirect* (secondary / synergist) sets as **0.5** (“fractional”) fit
 * hypertrophy outcomes better than counting them fully (“total”) or ignoring them
 * (“direct”). See SportRχiv DOI [10.51224/srxiv.460](https://doi.org/10.51224/srxiv.460)
 * and the ACSM abstract (MSSE 2025).
 *
 * OmniGart defaults match that fractional convention:
 * - primary (direct) involvement → 1.0
 * - secondary (indirect) involvement → 0.5
 *
 * These are **defaults**, not physiological constants. Callers may override for
 * sport-specific heuristics or A/B tests. This is not a claim that every secondary
 * muscle receives exactly half the stimulus of a primary.
 */
@Serializable
data class InvolvementWeights(
    val primary: Double = 1.0,
    val secondary: Double = 0.5,
) {
    init {
        require(primary >= 0.0) { "primary weight must be ≥ 0" }
        require(secondary >= 0.0) { "secondary weight must be ≥ 0" }
    }

    companion object {
        /** Pelland et al. 2025 fractional set counting defaults. */
        val FractionalPelland2025: InvolvementWeights = InvolvementWeights(1.0, 0.5)

        val DirectOnly: InvolvementWeights = InvolvementWeights(1.0, 0.0)

        val Total: InvolvementWeights = InvolvementWeights(1.0, 1.0)
    }
}
