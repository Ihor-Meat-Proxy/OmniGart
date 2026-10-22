# Open questions (autopilot log)

Owner is away — decisions below are provisional. Do not block merges on these.

## Content & citations

1. **Exercise-specific EMG/biomechanics refs** — Many movements currently cite NSCA essentials + ACSM RT overview rather than movement-specific trials. Prefer adding verified meta-analyses/EMG papers per pattern family in a later pass; omit rather than invent DOIs.
2. **Yoga / pilates evidence bar** — Pose/exercise descriptions are technique-accurate but mostly backed by ACSM flexibility/neuromotor guidance, not pose-specific RCTs. Should we require discipline-specific systematic reviews before marking a claim “supported”?
3. **Swim stroke coaching cues** — Competing coach systems (e.g. different kick timings). Confirm preferred cue dialect for freestyle catch and breaststroke kick legality notes.
4. **Ukrainian terminology** — Confirm preferred sports terms (e.g. присід vs сідати, станова тяга variants) with a native-speaking coach; current strings are working drafts.
5. **“Russian twist” naming** — English exercise name retained; no `ru` locale. Rename to “seated rotation” for neutrality?

## Schema & product

6. **Media pipeline** — `media.*Key` placeholders assume a future CDN layout. Confirm key scheme with design-system PR.
7. **SQLDelight mapping** — This PR ships JSON + Kotlin models only. Confirm table shapes when Phase 0 scaffold lands.
8. **Equipment model** — Is `none` vs `bodyweight` redundant for filters? Presets currently treat both as always available.
9. **Tracking types** — Need swim-specific (laps × distance × rest) richer than `laps`/`distance`?
10. **Contraindications severity** — Free-text tags now; structured ICD-ish codes later?

## Planning engine

11. **Sex as input** — Included for future absolute strength norms / relative load heuristics; unused in v1 rules beyond profile plumbing. Keep, gate, or remove?
12. **Health/readiness sources** — Apple Health / Health Connect field mapping TBD; readiness is an enum stub.
13. **Deload every 6 weeks** — Heuristic; confirm with owner preference vs readiness-only deloads.
14. **AI model allow-list** — Re-verify OpenRouter ZDR / no-training endpoints at ship time; slugs churn.
15. **EU in-region** — `eu.openrouter.ai` needs Business/Enterprise; confirm billing plan before enabling in production.
16. **Concurrent training interference** — Lift+run template exists; engine does not yet model interference quantitatively.

## Process

17. **Target library size** — Shipped ~129 exercises (≈89 strength). Grow toward a large commercial-library scale later or keep curated & cited?
18. **Template localisation** — Session names use i18n keys; confirm UK copy review workflow.

## Evidence catalog

19. **ACSM 2026 position-stand authors** — Catalog lists lead authors + society; confirm preferred short citation line for the ⓘ sheet.
20. **Foster sRPE DOI variants** — Catalog uses `10.1519/00124278-200102000-00019` (also registered as `10.1519/1533-4287(2001)015<0109:anatme>2.0.co;2`). Confirm preferred canonical DOI string.
21. **Karvonen 1957 vs 1988** — Both catalogued; confirm which the HR-zone UI should surface by default.
22. **Phase 0 `docs/evidence/citations.json`** — When the foundation PR merges, migrate/replace with `content/evidence` ids (legacy map already covers formula keys).
23. **Retraction Watch bulk feed** — Verifier uses Crossref `update-to` / `updated-by` today; optional scheduled ingest of Retraction Watch via Crossref relation data if owner wants stricter coverage.
24. **UK key-finding review** — Working translations in `content/evidence/locales/uk.json`; native coach pass pending (same as exercise strings).
