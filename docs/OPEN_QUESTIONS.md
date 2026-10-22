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

## Evidence registry

19. **Watson sleep year** — Plan text said “Watson 2015 sleep”; the verifiable sports-medicine review seeded is Watson 2017 (`10.1249/JSR.0000000000000418`). Confirm whether a different 2015 paper was intended.
20. **Schoenfeld 2016/2017 volume** — Seeded as one MA (`10.1080/02640414.2016.1210197`, Crossref year 2017) plus the 2018 SCJ volume guidelines. Confirm if a second distinct 2017 volume trial/MA should be listed.
21. **Baevsky SI primary source** — English methods overview (`10.12710/cardiometry.2017.10.6676`) is seeded; prefer a specific primary methods monograph (without adding a `ru` locale)?
22. **Karvonen / Foster DOIs** — Karvonen 1957 has PMID only; Foster 2001 has a Lippincott-style DOI that Crossref accepts. Confirm whether textbook-only technique refs (NSCA) should stay `unverified` forever or gain ISBN metadata.
23. **Claim ↔ exercise binding** — Exercises still embed legacy `evidenceRefs`; claims cover formulas/planning. Should each exercise technique bullet get its own claim id in a follow-up, or is family-level mapping enough?
24. **Retraction Watch feed** — Network job uses Crossref update/relation signals. Wire the Retraction Watch public data dump on a scheduled job, or keep Crossref-only?
25. **Contact email for polite APIs** — Tooling uses `evidence@omnigart.app` in User-Agent. Confirm mailbox or replace via `OMNIGART_EVIDENCE_EMAIL`.
