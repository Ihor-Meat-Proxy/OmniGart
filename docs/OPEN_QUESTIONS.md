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

## Achievements (provisional)

22. **Freeze token economy** — Default catalogue allows freezes on rest-tolerant streaks (`maxFreezePerStreak` 2–3). Confirm earn rate (e.g. one token per N honoured rest days) when the app layer wires inventory.
23. **Weekly rest tolerance defaults** — Strength/run/swim/yoga use `restDayTolerancePerWeek: 1–2`. Confirm per-discipline defaults vs a single user setting.
24. **Sleep consistency window** — Catalogue uses midpoint variance ≤ 60 minutes. Confirm clinical/product preference (45–90 min?).
25. **Volume display units** — Engine stores `volume_kg`; UI should show tonnes for `strength.volume_tonnes`. Confirm rounding and localisation of unit labels.
26. **Challenge calendar** — Monthly/weekly challenges use the evaluation timezone. Confirm whether “current month” follows the user’s home zone or the device zone after travel.
27. **Secret achievements** — Several `hidden: true` entries exist. Confirm reveal copy and whether secrets appear greyed or omitted until unlock.
28. **Icon authorship** — Provisional icons in `content/achievements/proposed-icons.json` (`challenge`, `discovery`, `freeze-token`, `personal-record`). Design follow-up to author SVGs into `design/icons`.
29. **SQLDelight / persistence** — Engine is pure Kotlin state; table shapes for `EvaluationState` TBD when Phase 0 scaffold lands.
30. **Notification copy for back-fill** — Key `achievement.backfill.summary` reserved; confirm en-GB/uk wording with product.
31. **Ukrainian sports terminology for achievements** — Working drafts in `content/achievements/locales/uk.json`; confirm with native-speaking coach (same as exercise terminology Q4).
32. **Remaining locale pack** — Achievements ship **en-GB + uk only**. Localisation follow-up should add other locales from the key list below (never `ru`).

### New achievement string keys (en-GB + uk shipped)

Keys follow `achievement.<id>.title` and `achievement.<id>.description`. Full list is the union of keys in `content/achievements/locales/en-GB.json` (240 keys for 120 achievements). Categories and ids:

- **general** (16): `first_workout`, `disciplines_explorer`, `weekly_consistency`, `logging_habit`, `workout_milestones`, `honour_rest`, `freeze_wise`, `morning_or_evening_rhythm`, `multi_discipline_week`, `session_minutes`, `early_consistency`, `balanced_month`, `secret_comeback`, `year_of_movement`, `share_ready`, `evidence_curious`
- **strength** (20): `first_session`, `sessions`, `volume_tonnes`, `sets_logged`, `weekly_practice`, `pr_squat`, `pr_hinge`, `pr_press`, `pr_pull`, `long_session`, `session_volume`, `month_challenge`, `compound_focus`, `secret_pr_night`, `deload_honour`, `year_volume`, `three_week_block`, `unilateral_sets`, `upper_lower_week`, `bodyweight_base`
- **running** (15): `first_run`, `distance_lifetime_km`, `sessions`, `5k`, `10k`, `half`, `marathon`, `easy_streak`, `week_distance`, `pace_pr`, `month_challenge`, `secret_sunrise`, `long_run`, `walk_run_start`, `trail_or_road`
- **swimming** (12): `first_swim`, `distance_lifetime`, `sessions`, `stroke_variety`, `session_distance`, `rhythm`, `month_challenge`, `drill_focus`, `secret_flip`, `week_distance`, `butterfly_try`, `im_builder`
- **yoga** (10): `first_practice`, `minutes`, `sessions`, `streak`, `long_flow`, `month_challenge`, `mobility_pair`, `secret_stillness`, `week_minutes`, `year_practice`
- **pilates** (8): `first_session`, `minutes`, `sessions`, `streak`, `core_focus`, `month_challenge`, `secret_hundred`, `week_minutes`
- **nutrition** (12): `first_log`, `days_logged`, `streak`, `week_complete`, `month_habit`, `balanced_plate_days`, `secret_breakfast`, `rolling_30`, `variety_week`, `hundred_club`, `gentle_return`, `year_of_logs`
- **water** (8): `first_goal`, `streak`, `days`, `week_challenge`, `month_challenge`, `secret_sip`, `rolling_consistent`, `train_and_drink`
- **recovery** (11): `first_sleep_import`, `sleep_consistency`, `sleep_days`, `rest_day_honour`, `month_sleep`, `readiness_listen`, `secret_nap_boundary`, `week_consistency`, `hrv_checkin`, `balance_block`, `stress_down_day`
- **body** (8): `first_measurement`, `measurement_days`, `streak`, `month_checkins`, `kinds_variety`, `secret_tape`, `quarter_habit`, `year_of_checkins`

Also reserved: `achievement.backfill.summary` (notification summary; not yet in locale files — confirm copy before adding).
