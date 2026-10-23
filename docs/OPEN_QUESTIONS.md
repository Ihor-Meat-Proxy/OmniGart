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
