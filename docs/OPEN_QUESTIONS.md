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

17. **Target library size** — Library grew with pull-up/dip families (~145 total). Grow toward a several-hundred-exercise catalog later or keep curated & cited?
18. **Template localisation** — Session names use i18n keys; confirm UK copy review workflow.
20. **`bodyweightFraction` for dips** — No published GRF %BM study for parallel-bar dips was verified; current values are estimates from de Leva segmental logic. Prefer a measured study if one appears.
21. **`loadModel` backfill** — Non pull/dip exercises received heuristic loadModel/fraction defaults; review edge cases (carries, swimming, holds) before treating proportions as analytics-grade.

## Body map

B1. **Figure fidelity** — Figures adapt MIT MuscleMap paths. Further refine subdivisions (pec heads / VL / soleus cuts), commission bespoke illustrations later, or keep iterating on the adapter?
B2. **Unilateral highlighting** — Paths are L/R mirrored today; should unilateral sets tint only the working side once session laterality is logged?
B3. **Secondary weight** — Default 0.5 follows Pelland et al. 2025 fractional counting. Keep global, or allow per-pattern overrides (e.g. rows → biceps < 0.5)?
B4. **TVA / rotator-cuff proxies** — Surface proxies may overstate visible load. Drop from heatmap fills and keep taxonomy-only, or add dashed “deep” overlays?
B5. **Sex figure default** — `UNSPECIFIED` currently falls back to the male figure. Prefer a neutral third silhouette, or require sex before showing heatmaps?
B6. **Intensity binning** — Equal-width vs log / landmark-anchored bins for single workouts — confirm preference after first user tests.
B7. **Path JSON packaging** — Ship JSON in both app bundles, or generate Swift/Compose path tables in `design/generated` like icons?

## Translation / i18n (content catalogs)

T1. **Native-speaker review queue** — All new `pl`/`be`/`kk`/`ka`/`es`/`fr`/`de`/`cs`/`zh-Hans` strings (and expanded `uk` body copy) are `machine-drafted`. Who owns first-pass review per language?
T2. **Kazakh gym loans** — Some `kk` strength names still use international/gym loans that overlap Russian orthography (e.g. присед). Prefer fully nativised Kazakh sports-federation forms when a reviewer supplies them?
T3. **Belarusian orthography** — Confirm Narkamaŭka standard throughout vs any classical (taraškievica) preference for public copy.
T4. **Georgian strength loans** — Confirm whether სკუატი / დედლიფტი-style loans are preferred over fully calqued Georgian for gym UX.
T5. **Pilates naming** — Keep English PMA names in European locales (common in studios) or always localise (e.g. PL “Setka” for The Hundred)?
T6. **App UI locales** — When Phase 0 scaffold merges, should the Translations job also require Android `values-*` / iOS xcstrings parity for every required locale immediately, or gate platform mirrors behind a follow-up?
T7. **Required check** — Confirm GitHub branch protection adds the exact job name `Translations`.
T8. **Pull-up/dip grip glossary** — Machine-drafted translations for PR #5 keys are in all required locales; confirm preferred local forms for chin-up, neutral grip, archer, Korean dip, typewriter, commando with native reviewers.
T9. **Glossary term least-certain set** — Especially `uk` hip-thrust loan, `kk`/`ka` press/pull nomenclature, `zh-Hans` face-pull / hip-thrust gym slang; plus new pull/dip variation names.

## Domain content i18n stacks

D1. **Merge order** — Prefer merge stacked i18n PRs into each source branch before merging the source to main once Translations is required on main (#26).
D2. **Achievements #11 vs #14** — Two incompatible catalogs (0 key overlap). Confirm which lands; both have stacked i18n PRs.
D3. **Yoga Sanskrit policy** — Confirmed keep Sanskrit strings identical to en-GB across locales; localise English/common names only.
