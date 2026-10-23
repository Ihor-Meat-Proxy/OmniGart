# Evidence strength grades (GRADE-inspired)

OmniGart uses a simplified GRADE-style scale for the ⓘ strength badge. Grades are **product rules**, not a formal GRADE panel output.

| Grade | When to assign |
| --- | --- |
| **high** | Large meta-analysis or systematic review of RCTs with consistent effects; or an authoritative multi-society guideline grounded in such reviews. |
| **moderate** | Meta-analysis / systematic review with limitations (heterogeneity, indirectness, or mostly non-RCT inputs); large well-conducted RCT; strong consensus guideline. |
| **low** | Single RCT or small cluster of trials; observational/cohort evidence; textbook synthesis of mixed primary studies; classical formulas with limited validation samples. |
| **very-low** | Expert opinion, product caveats, historical formulas without modern validation, case series, or conflicted/low-quality sources kept only for provenance. |

## Starting points by `type`

| `type` | Default starting grade | Typical adjustments |
| --- | --- | --- |
| `meta-analysis` | high | Downgrade for serious inconsistency, risk of bias, or industry-only endpoints. |
| `systematic-review` | moderate–high | Downgrade if narrative-only or few included trials. |
| `rct` | moderate | Upgrade if very large and confirmatory; downgrade if small/open-label. |
| `cohort` | low | Rarely above moderate. |
| `guideline` | moderate–high | Reflect the guideline’s own evidence rating when stated. |
| `textbook` | low | Technique norms, not causal claims. |
| `consensus` | low–moderate | Expert panels without formal systematic review → low. |

## App policy

- Prefer meta-analyses or large trials for claims shown to users.
- Retracted works must set `retracted: true` and must not power user-facing claims.
- Neutral `keyFinding` copy: state what the source reported, not marketing language.
- Locales: `en-GB` source string plus `uk` translation key; never add a `ru` locale.
