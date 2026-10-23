# Open questions, assumptions, and decisions needing review

Autopilot log (Phase 0 + merged design / planning). Each item: **context**, **choice**, **alternatives**. Do not block merges on these.

## Branch naming vs cloud agent prefix

- **Context:** Owner rule prefers `feat/...` / `chore/...`; Cursor Cloud Agent requires `cursor/<name>-2ab3` for PR association.
- **Choice:** Use `cursor/phase-0-foundation-2ab3` (feature branch, never `main`).
- **Alternatives:** Rename locally to `feat/phase-0-foundation` for humans and keep cloud mapping documented.

## XcodeGen vs Tuist

- **Context:** Reproducible Apple project without committing brittle `.xcodeproj` as source of truth.
- **Choice:** **XcodeGen** (`iosApp/project.yml`).
- **Alternatives:** Tuist (stronger for large native graphs); hand-maintained Xcode project.

## Library versions (Oct 2026)

- **Context:** Owner requires latest **stable** as of Oct 2026.
- **Choice:** Kotlin **2.4.21**, AGP **9.4.1**, Gradle **9.8.2**, SQLDelight **2.4.1**, Compose BOM **2026.09.00**, Material 3 **1.4.0**, Wear Compose **1.7.1**, Coroutines **1.11.0**, Datetime **0.8.0**, Serialization **1.12.0**. Kotlin **2.5.0** and Material3 **1.5.0** were still pre-release.
- **Alternatives:** Track 2.5/1.5 betas for Material 3 Expressive APIs earlier.

## Material 3 Expressive

- **Context:** Product asks for Material 3 Expressive on Android.
- **Choice:** Ship Material 3 **1.4.0** (latest stable) via BOM; treat full Expressive component set as follow-up when 1.5 stable lands.
- **Alternatives:** Depend on `1.5.0-beta02` now.

## Biological sex model

- **Context:** Mifflin–St Jeor, CUN-BAE, EFSA water are binary sex equations.
- **Choice:** `BiologicalSex { MALE, FEMALE }` only in Phase 0.
- **Alternatives:** Add `UNSPECIFIED` with explicit equation opt-in; separate gender identity field for UI.

## Discipline level thresholds

- **Context:** Spec defines structures (4 strength levels; running newbie+5; swimming newbie+4) but not numeric cut-offs.
- **Choice:** Placeholder thresholds in `DisciplineClassifier` (months / weekly km / pace / 5 km / swim metres). Documented here as **provisional**.
- **Alternatives:** Import coach-authored tables from content packs.

## Water target extras

- **Context:** EFSA base + drink fraction are cited; weight factor, training bonus, heat bonus, and 4 L cap are product rules.
- **Choice:** Implement exactly as Phase 0 spec.
- **Alternatives:** ACSM sweat-rate protocols; climate heat-index instead of fixed 27 °C.

## Readiness heuristic

- **Context:** Point bands and SWC framing are product rules; Buchheit 2014 informs HRV SWC concept only.
- **Choice:** Implement literal Phase 0 scoring.
- **Alternatives:** Banister / TRIMP load; validated readiness questionnaires.

## Database encryption

- **Context:** Spec plans encrypted DB; SQLCipher KMP story varies by platform.
- **Choice:** Plain SQLite in Phase 0; keep `DatabaseDriverFactory` as the injection point.
- **Alternatives:** SQLCipher Android + SQLCipher Apple; EncryptedSharedPreferences for keys only.

## watchOS KMP framework linking

- **Context:** Both iOS and watchOS consume `Shared.framework` from Gradle. CI failed with `No such module 'Shared'` because XcodeGen searched `shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)` while Gradle only wrote `shared/build/bin/<target>/debugFramework/`.
- **Choice:** Add `syncXcodeFrameworks` Gradle task + CI rsync into the exact `$(SDK_NAME)` folders; exclude `x86_64` on watch simulator (no `watchosX64` target).
- **Alternatives:** Fat XCFramework via `./gradlew assembleXCFramework`; Xcode Run Script calling `embedAndSignAppleFrameworkForXcode`.

## iOS / watchOS deployment vs GitHub Actions Xcode

- **Context:** Product intent is iOS 26+ / watchOS 26+ (Liquid Glass). GitHub `macos-15` currently exposes Xcode 16.4 (iOS 18.5 / watchOS 11.5 SDKs). Setting deployment target 26.0 made xcodebuild warn that the supported range tops out at 11.5.99 for watchOS.
- **Choice:** Set XcodeGen deployment targets to **iOS 18.0 / watchOS 11.0** so CI can compile; CI prefers Xcode 26+ when installed. Document product floor remains 26+ once runners catch up.
- **Alternatives:** Require a self-hosted macOS runner with Xcode 26; keep 26.0 and accept CI red until GHA images update.

## In-memory SQLDelight tests location

- **Context:** `JdbcSqliteDriver` is JVM-only.
- **Choice:** Repository tests in `androidHostTest` (runs on Linux CI).
- **Alternatives:** Add a `jvm()` KMP target solely for tests.

## Wear applicationId

- **Context:** Wear apps often share the phone applicationId for Play packaging; Robolectric also needs a distinct package for reliable activity resolution.
- **Choice:** `com.omnigart.wear` for the Wear module in Phase 0.
- **Alternatives:** Same applicationId as phone with a different activity; instrumentation tests on device.

## AGP 9 KMP Android library plugin

- **Context:** AGP 9 rejects pairing `com.android.library` with `org.jetbrains.kotlin.multiplatform`.
- **Choice:** `com.android.kotlin.multiplatform.library` with `kotlin { android { ... } }` and `androidHostTest`.
- **Alternatives:** Temporary `android.builtInKotlin=false` / `android.newDsl=false` flags.

## Design package integration

- **Context:** PR #1 shipped `design/` self-contained; Phase 0 shells previously used Material defaults / SF Symbols.
- **Choice:** Wire generated Compose theme/icons into `androidApp`, Swift colour helpers + icon asset catalogs into iOS/watch shells, default launcher / App Icon from design outputs. Design check runs on every CI push (not only path-filtered).
- **Alternatives:** Keep design as a docs-only package until a dedicated UI polish PR.

## Planning module shape

- **Context:** PR #3 ships `shared/planning` as a JVM(+common) KMP module with content under `content/exercises/`.
- **Choice:** Include `:shared:planning` beside `:shared` (foundation). Planning stays **JVM-first** for schema validation; not yet a dependency of the Apple/Android `Shared` framework. CI runs `:shared:planning:jvmTest`.
- **Alternatives:** Promote planning to full apple/android targets and `api`-link from `:shared` once content loading on device is required.

## Content & citations (from planning PR)

1. **Exercise-specific EMG/biomechanics refs** — Many movements currently cite NSCA essentials + ACSM RT overview rather than movement-specific trials. Prefer adding verified meta-analyses/EMG papers per pattern family later; omit rather than invent DOIs.
2. **Yoga / pilates evidence bar** — Technique-accurate but mostly ACSM flexibility/neuromotor guidance. Require discipline-specific systematic reviews before marking a claim “supported”?
3. **Swim stroke coaching cues** — Confirm preferred cue dialect for freestyle catch and breaststroke kick legality notes.
4. **Ukrainian terminology** — Confirm preferred sports terms with a native-speaking coach; current strings are drafts.
5. **“Russian twist” naming** — English exercise name retained; no `ru` locale. Rename to “seated rotation” for neutrality?

## Schema & product (from planning PR)

6. **Media pipeline** — `media.*Key` placeholders assume a future CDN layout.
7. **SQLDelight mapping** — Planning ships JSON + Kotlin models; table shapes land with persistence work.
8. **Equipment model** — Is `none` vs `bodyweight` redundant for filters?
9. **Tracking types** — Need richer swim-specific tracking than `laps`/`distance`?
10. **Contraindications severity** — Free-text tags now; structured codes later?

## Planning engine (from planning PR)

11. **Sex as input** — Included for future norms; unused in v1 rules beyond profile plumbing.
12. **Health/readiness sources** — Apple Health / Health Connect field mapping TBD.
13. **Deload every 6 weeks** — Heuristic vs readiness-only deloads.
14. **AI model allow-list** — Re-verify OpenRouter ZDR endpoints at ship time.
15. **EU in-region** — `eu.openrouter.ai` needs Business/Enterprise confirmation.
16. **Concurrent training interference** — Lift+run template exists; engine does not yet model interference quantitatively.

## Process (from planning PR)

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

## Naming: no competitor product names

- **Context:** Owner rule for all current and future work.
- **Choice:** Never mention competitor app/product names (examples include popular strength loggers, wearable brands, and recovery/coaching apps) in the repo, code, docs, commits, or PR titles/bodies. Use neutral descriptions of product categories and patterns instead. Platform APIs remain allowed: HealthKit, Health Connect, Google Health API, Polar BLE SDK.
- **Alternatives:** Keep a private research notebook outside the repo if named product comparisons are needed.

## Autopilot v1 roadmap

- **Context:** Owner standing instruction after Phase 0 + health-integration PR.
- **Choice:** Continue autonomously through `docs/plans/` phases 1→9 (strength → food/water → readiness → calc settings → encrypted backup → achievements → watch → running → swimming). One feature branch/PR per phase, stacked if prior is unmerged; write/update `docs/plans/phase-N.md` before implementing; never block — log questions here.
- **Alternatives:** Pause after health for explicit go-ahead (rejected by standing instruction).

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
