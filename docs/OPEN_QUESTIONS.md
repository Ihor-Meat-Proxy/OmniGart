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

17. **Target library size** — Shipped ~129 exercises (≈89 strength). Grow toward Hevy-scale later or keep curated & cited?
18. **Template localisation** — Session names use i18n keys; confirm UK copy review workflow.

## Translation / i18n (content catalogs)

19. **Native-speaker review queue** — All new `pl`/`be`/`kk`/`ka`/`es`/`fr`/`de`/`cs`/`zh-Hans` strings (and expanded `uk` body copy) are `machine-drafted`. Who owns first-pass review per language?
20. **Kazakh gym loans** — Some `kk` strength names still use international/gym loans that overlap Russian orthography (e.g. присед). Prefer fully nativised Kazakh sports-federation forms when a reviewer supplies them?
21. **Belarusian orthography** — Confirm Narkamaŭka standard throughout vs any classical (taraškievica) preference for public copy.
22. **Georgian strength loans** — Confirm whether სკუატი / დედლიფტი-style loans are preferred over fully calqued Georgian for gym UX.
23. **Pilates naming** — Keep English PMA names in European locales (common in studios) or always localise (e.g. PL “Setka” for The Hundred)?
24. **App UI locales** — When Phase 0 scaffold merges, should the Translations job also require Android `values-*` / iOS xcstrings parity for every required locale immediately, or gate platform mirrors behind a follow-up?
25. **Required check** — Confirm GitHub branch protection adds the exact job name `Translations`.
26. **Glossary term least-certain set** — See PR description; especially `uk` hip-thrust loan, `kk`/`ka` press/pull nomenclature, `zh-Hans` face-pull / hip-thrust gym slang.
