# Evidence catalog

Every statement, goal, formula, and plan claim in OmniGart should link to a stable evidence id (ⓘ).

## Layout

| Path | Role |
| --- | --- |
| `schema/evidence-entry.schema.json` | JSON Schema for one entry |
| `schema/evidence-catalog.schema.json` | Array wrapper for `catalog.json` |
| `entries/*.json` | One file per source |
| `catalog.json` | Aggregated array (must match `entries/`) |
| `locales/en-GB.json` / `uk.json` | Key-finding strings |
| `legacy-id-map.json` | Maps old DOI/PMID/textbook ids → evidence ids |
| `GRADE_RULES.md` | Strength-badge assignment rules |

## Types

`meta-analysis` · `systematic-review` · `rct` · `cohort` · `guideline` · `textbook` · `consensus`

## Content pointers

Exercise and template JSON use:

```json
{ "evidenceId": "schoenfeld_volume_2016", "supports": ["hypertrophy", "programming"] }
```

Apps load metadata via `shared/evidence` (`EvidenceRepository.infoSheet(id)`).

## Tooling

- Rebuild catalog: `python3 scripts/build_evidence_catalog.py`
- Migrate legacy refs: `python3 scripts/migrate_evidence_refs.py`
- Verify DOIs/PMIDs: `cd tooling/evidence-verify && npm ci && npm run verify`
- CI job name: **Evidence** (`.github/workflows/evidence.yml`)

No `ru` locale — en-GB + uk only.
