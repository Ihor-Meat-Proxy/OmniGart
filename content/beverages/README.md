# Beverage catalogue

Pure content pack for OmniGart hydration tracking. Loaded by `shared/hydration` — no app-shell UI here.

## Layout

- `data/*.json` — one beverage per file (`id.json`)
- `schema/beverage.schema.json` — JSON Schema (Draft 2020-12)
- `locales/en-GB.json`, `locales/uk.json` — display strings only (no `ru`)

## Counting default

Evidence-based default: **hydration-adjusted** (volume × BHI). See `hydration.counting.info` locale string and Maughan et al. 2016 (DOI `10.3945/ajcn.115.114769`).

## References

Verified DOIs only — see each beverage’s `evidenceRefs` and `docs/specs/hydration-health-sync.md`.
