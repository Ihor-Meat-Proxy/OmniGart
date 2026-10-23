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
- **Choice:** Never mention competitor app/product names (examples include popular strength loggers, wearable brands, and recovery/coaching apps) in the repo, code, docs, commits, or PR titles/bodies. Use neutral descriptions of product categories and patterns instead. Platform APIs remain allowed: HealthKit, Health Connect, Google Health API, Polar BLE SDK. The CI scan list is `.github/ci/banned-terms.yml` only.
- **Alternatives:** Keep a private research notebook outside the repo if named product comparisons are needed.

## App Review / health-app compliance

- **Context:** Owner-mandated rules for all app code and copy going forward (wellness framing, JIT permissions, no debug dead ends in release, accurate PrivacyInfo).
- **Choice:** Enforce via `scripts/check_app_review_compliance.py` inside existing CI jobs; human store steps in `docs/RELEASE_CHECKLIST.md`. Privacy policy URL currently points at `https://omnigart.com/privacy` until the live policy is published.
- **Alternatives:** Separate required CI job (rejected — keep gates inside existing jobs).

## Bundle-first content (all phases)

- **Context:** Owner decision: apps must work fully offline; server only for signed between-release updates, optional large media, food packs, and remote config.
- **Choice:** Pack exercises, templates, locales, evidence, icons, body-map paths, achievements, and exercise images into `generated/content-bundle/` via `scripts/pack_bundled_content.py` (Gradle + Xcode pre-build). Shared `LayeredContentLoader` layers **bundled baseline &lt; verified newer update pack**. CI runs `--verify` so the committed bundle stays up to date. Docs: `docs/CONTENT_BUNDLE.md`.
- **Alternatives:** On-demand CDN fetch for all content (rejected — breaks offline); commit only sources and generate exclusively in CI without a committed baseline (rejected — harder offline/dev reproducibility).

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
B7. **Path JSON packaging** — **Decided (bundle-first):** ship `design/body-map/generated/paths/*.json` inside `generated/content-bundle/body-maps/` on all apps. Optional Swift/Compose tables remain a future optimisation, not a requirement for offline.

## Google Health API identity

- **Context:** Spec requires a Google Health API client with OAuth (no server), distinct from Google Fit and from on-device Health Connect.
- **Choice:** Implement against **Fitbit Web API** OAuth + REST hosts (`www.fitbit.com/oauth2`, `api.fitbit.com`) as Google’s consumer health cloud API after the Fitbit acquisition. Scopes: activity, heartrate, sleep, nutrition, weight, location, profile.
- **Alternatives:** Google Cloud Healthcare API (enterprise); a future first-party Google Health consumer API if/when publicly documented.

## HealthKit Kotlin vs Swift split

- **Context:** HealthKit is an Apple framework; full typed queries are awkward in Kotlin/Native.
- **Choice:** `HealthKitPlatformApi` + `HealthKitClientImpl` in KMP; Swift `HealthKitPlatformBridge` + `HealthKitSampleInjector` owns HKStore. Register the Swift bridge at app launch.
- **Alternatives:** Pure Kotlin/Native cinterop; duplicate domain mapping only in Swift.

## Health Connect testing library

- **Context:** Owner asked for connect-testing if available.
- **Choice:** Depend on `androidx.health.connect:connect-testing:1.0.0-alpha04` (only published line) for androidHostTest, but exercise logic via `FakeHealthConnectGateway` because alpha APIs + Robolectric HC APK support are incomplete on Linux CI.
- **Alternatives:** Instrumented device tests only; wait for stable connect-testing.

## AppAuth placement

- **Context:** AppAuth Android library requires `appAuthRedirectScheme` manifest placeholder.
- **Choice:** Keep OAuth abstract (`AuthBrowser` / PKCE client) in `shared`; add AppAuth dependency + placeholder on `androidApp` for the production browser implementation.
- **Alternatives:** Put AppAuth inside `shared` with a default placeholder.

## Baevsky SI units

- **Context:** Literature mixes ms/sec for Mo and MxDMn; AMo as percent vs fraction.
- **Choice:** 50 ms bins; Mo and MxDMn in **seconds**; AMo as **percent** 0–100; SI = AMo / (2·Mo·MxDMn).
- **Alternatives:** Keep everything in ms and scale the formula.

## Formula strategy defaults

- **Context:** Users may pick alternative formulae later.
- **Choice:** Strategy interfaces + `FormulaCatalog` / `FormulaPreferences`; Phase 0 defaults remain Mifflin–St Jeor, Tanaka, CUN-BAE, Epley, Karvonen 5-zone. Added Harris–Benedict and Fox as first alternatives.
- **Alternatives:** Content-pack driven formula tables.

## Phase 1 volume load — bodyweight coefficients

- **Context:** Effective load for BW movements needs a fraction of body mass; published GRF %BM varies by variation and study.
- **Choice:** Ship `BodyweightLoadCoefficients` + content `bodyweightFraction` / `ExerciseLoadSemantics` (push-up ≈ 0.64, pull-up/dip ≈ 1.0, inverted row ≈ 0.5) as provisional estimates; mark volume as estimated; prompt when body mass is missing/stale via `BodyweightResolver`.
- **Alternatives:** Require always-on scale/Health body mass; per-user calibrated fractions; omit BW contribution until cited.
