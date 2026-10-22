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

## Body map

B1. **Figure fidelity** — Figures adapt MIT MuscleMap paths. Further refine subdivisions (pec heads / VL / soleus cuts), commission bespoke illustrations later, or keep iterating on the adapter?
B2. **Unilateral highlighting** — Paths are L/R mirrored today; should unilateral sets tint only the working side once session laterality is logged?
B3. **Secondary weight** — Default 0.5 follows Pelland et al. 2025 fractional counting. Keep global, or allow per-pattern overrides (e.g. rows → biceps < 0.5)?
B4. **TVA / rotator-cuff proxies** — Surface proxies may overstate visible load. Drop from heatmap fills and keep taxonomy-only, or add dashed “deep” overlays?
B5. **Sex figure default** — `UNSPECIFIED` currently falls back to the male figure. Prefer a neutral third silhouette, or require sex before showing heatmaps?
B6. **Intensity binning** — Equal-width vs log / landmark-anchored bins for single workouts — confirm preference after first user tests.
B7. **Path JSON packaging** — Ship JSON in both app bundles, or generate Swift/Compose path tables in `design/generated` like icons?

## § Body composition

Provisional — do not block merges. IDs are `BC1`…`BCn` (body composition) so they do not collide with numeric items or body-map `B1`…`Bn`.

- **BC1.** **Default body-fat method** — CUN-BAE is the product default (height/weight/age/sex only). Confirm vs U.S. Navy for users who already tape-measure.
- **BC2.** **Siri vs Brozek** — Skinfold density currently uses Siri. Offer Brozek as an option for older adults / different assumptions?
- **BC3.** **Extra girths in Health** — Only waist has a stable cross-platform Health type today; keep hip/neck/arms OmniGart-only or map via vendor extensions?
- **BC4.** **Progress photo cloud backup** — Contract is local-only. If encrypted private backup is added later, keep Health export forbidden?
- **BC5.** **EWMA alpha** — Default 0.3 for weight/girth series. Expose per-metric alphas in settings?
- **BC6.** **Ethnicity adjustments** — BMI/WHtR wellness bands are population averages; any locale-specific screening copy beyond en-GB/uk?
- **BC7.** **Navy abdomen vs waist** — Male equation prefers abdomen, falls back to waist. Confirm landmark copy in the app shell.
