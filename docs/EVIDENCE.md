# Evidence registry

Every user-facing statement, goal, formula and plan explanation in OmniGart should link through an ⓘ sheet to one or more registry entries. Prefer **meta-analyses and large trials**; never invent DOIs.

## Layout

| Path | Role |
|------|------|
| `content/evidence/schema/` | JSON Schema for entries and the claims registry |
| `content/evidence/entries/` | One JSON file per source (`{id}.json`) |
| `content/evidence/claims/registry.json` | Claim id → evidence ids + i18n text keys |
| `content/evidence/locales/` | `en-GB.json` and `uk.json` only (no `ru`) |
| `content/evidence/reports/` | Network verification reports |
| `shared/evidence/` | Pure Kotlin Multiplatform lookup APIs |
| `tooling/evidence/` | Offline validate + Crossref/PubMed verify scripts |

## Inclusion criteria

1. **Prefer** meta-analyses, systematic reviews, large RCTs, and major society guidelines.
2. **Allow** cohorts, consensus statements, and coaching textbooks when they are the honest best source (e.g. technique norms) — grade them lower.
3. **Require** a resolvable DOI and/or PMID when one exists. If none exists (historical papers, product caveats), set `doi`/`pmid` to `null`, mark `verificationStatus: unverified`, and explain in `limitations`.
4. **Never invent** DOIs, PMIDs, effect sizes, or sample sizes. Leave fields null and list the gap.
5. **Locales**: en-GB + Ukrainian (`uk`) only. Do not add Russian.

## Evidence grades (GRADE-like)

| Level | Meaning in OmniGart |
|-------|---------------------|
| `high` | Strong review-level or large consistent RCT evidence; further research unlikely to overturn the core claim. |
| `moderate` | Good evidence with important residual uncertainty (heterogeneity, indirectness, or single-population derivation). |
| `low` | Limited trials, acute surrogates (e.g. EMG), historical methods, or strong confounding. |
| `very-low` | Heuristics, caveats, or estimates that are highly uncertain — still shown honestly. |

Each entry must include a short `evidenceGrade.rationale` and plain-language `keyFinding` in en-GB and uk.

## Claims

- Every formula, goal, programming rule, nutrition target, recovery metric, or technique claim gets a **claim id**.
- Claims map to **one or more** evidence ids.
- User-visible claim copy lives in `locales/` behind `claim.*.text` keys.
- The ⓘ sheet loads `EvidenceRegistry.evidenceForClaim(claimId)` (sorted high → very-low, then newer year).

## How to add a source

1. Confirm the DOI/PMID on Crossref / PubMed (or confirm none exists).
2. Add `content/evidence/entries/<id>.json` matching `evidence-entry.schema.json`.
3. Add or update a claim in `claims/registry.json` and locale strings.
4. If content JSON uses `evidenceRefs`, set `contentRefKeys` (e.g. `doi:10.1234/...`) so offline CI can resolve them.
5. Run offline validation:

```bash
cd tooling/evidence && npm ci && npm run validate
```

6. Optionally run network verification (rate-limited):

```bash
cd tooling/evidence && npm run verify:network
```

7. Run Kotlin tests:

```bash
./gradlew :shared:evidence:jvmTest
```

## Review cadence

| Check | When |
|-------|------|
| Schema, unique ids, claim↔evidence links, content ref coverage, no `ru` locale | Every PR / push touching evidence, content refs, planning catalog, or tooling (`Evidence` CI job) |
| Crossref + PubMed metadata match + retraction signals | Weekly schedule + `workflow_dispatch` (not on every PR) |
| Human spot-check of grades and claim wording | When adding a family of claims or after a major planning change |

## Seed / regenerate

Canonical seed data can be regenerated with:

```bash
python3 tooling/evidence/seed_registry.py
```

Prefer editing individual entry JSON after the first seed; re-running the seed overwrites `entries/`.
