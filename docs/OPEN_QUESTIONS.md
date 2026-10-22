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
19. **Additional locale keys (en-GB/uk only in this PR)** — Another agent should translate these `exercise.*.name` keys beyond en-GB/uk:
    - `exercise.archer_pull_up.name`
    - `exercise.band_assisted_dip.name`
    - `exercise.bench_dip.name`
    - `exercise.close_grip_pull_up.name`
    - `exercise.commando_pull_up.name`
    - `exercise.korean_dip.name`
    - `exercise.l_sit_pull_up.name`
    - `exercise.machine_assisted_dip.name`
    - `exercise.machine_assisted_pull_up.name`
    - `exercise.negative_dip.name`
    - `exercise.negative_pull_up.name`
    - `exercise.neutral_grip_pull_up.name`
    - `exercise.scapular_pull_up.name`
    - `exercise.straight_bar_dip.name`
    - `exercise.typewriter_pull_up.name`
    - `exercise.weighted_dip.name`
    - `exercise.wide_grip_pull_up.name`
    Also refresh display strings for expanded existing keys (`pull_up`, `chin_up`, `inverted_row`, `weighted_pull_up`, `assisted_pull_up_band`, `ring_dip`, `dip_parallel_bar`, `chest_to_bar_pull_up`) if non-en/uk locales already exist elsewhere.
20. **`bodyweightFraction` for dips** — No published GRF %BM study for parallel-bar dips was verified; current values are estimates from de Leva segmental logic. Prefer a measured study if one appears.
21. **`loadModel` backfill** — Non pull/dip exercises received heuristic loadModel/fraction defaults; review edge cases (carries, swimming, holds) before treating proportions as analytics-grade.

## § Hydration

Provisional — do not block merges. IDs are `H1`…`Hn` so they do not collide with numeric items from other PRs.

- **H1.** **Plant-milk BHI** — Oat/soy/almond defaulted to 1.0 (estimated). Soy’s protein profile might behave closer to dairy; run or cite a BHI trial before raising factors.
- **H2.** **Milk-based coffees BHI** — Cappuccino/latte/flat white use interpolated factors (1.15–1.25) between coffee (1.0) and milk (~1.5). Confirm or measure.
- **H3.** **Kefir BHI** — Estimated from full-fat milk (1.50). Acceptable until a fermented-dairy BHI exists?
- **H4.** **Wine BHI** — Defaulted to 1.0 like Maughan lager; higher ABV may increase diuresis at larger intakes. Keep 1.0 or apply an alcohol discount?
- **H5.** **ORS vs sports drink** — Catalogue ships a sports drink at BHI 1.0 (Maughan). Add a separate oral-rehydration entry at BHI 1.54?
- **H6.** **Food-water fraction** — Beverage targets use 20% from food. Expose as a user setting?
- **H7.** **Weight heuristic constant** — 35 ml/kg labelled weak. Prefer 30 ml/kg, 40 ml/kg, or hide the method entirely?
- **H8.** **Hyponatraemia ceiling** — Absolute caution at 6 L/day and 1.5× target / +2 L. Confirm clinical comfort with these product thresholds (citation is Hew-Butler 2015 consensus, which emphasises drinking to thirst rather than a fixed litre cap).
- **H9.** **Intensity sweat heuristics** — 0.4 / 0.8 / 1.2 L/h for light/moderate/hard. Replace with sport-specific tables?
- **H10.** **Health caloric drinks** — Always write NutritionRecord/HKCorrelation for any energy > 0, or only when user opts into nutrition sync?
- **H11.** **Late-caffeine threshold** — Drake used 400 mg; should the insight trigger on any caffeine in the 6 h window (current) or only above a mg floor?
- **H12.** **Reminder copy / localisation** — Notification strings not shipped in this module; confirm en-GB + uk ownership in the app shell.
