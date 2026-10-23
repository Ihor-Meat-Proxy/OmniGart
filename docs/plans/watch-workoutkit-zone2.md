# Watch WorkoutKit + Zone 2 running

## Goal

Ship Apple Watch **system Workout** integration via **WorkoutKit** (iOS 17+ / watchOS 10+; project deployment remains iOS 18 / watchOS 11 for CI), a progressive **Zone 2** running program with an in-app watch workout (glanceable live UI + zone haptics), HealthKit completed-workout readback for load/recovery, and a device-install guide that works on a **free Apple ID (Personal Team)** as well as a paid developer account.

Wellness wording only (training guidance, not medical advice). No competitor product names.

## Why this phase (vs roadmap Phase 7 / 8)

`docs/plans/phase-7.md` (watch deepening) and `phase-8.md` (running) remain the long-term roadmap. This plan **pulls forward** the WorkoutKit scheduling path and a concrete Zone 2 running loop so personal-device validation (iPhone + Watch Ultra) is unblocked. Later Phase 7/8 work can reuse these modules.

## Stacked PRs

| Order | Branch pattern | Delivers | Depends on |
|------:|----------------|----------|------------|
| 0 | `cursor/watch-workoutkit-zone2-plan-2ab3` | This plan + OPEN_QUESTIONS notes + plans README row | `main` |
| A | `cursor/workoutkit-integration-2ab3` | WorkoutKit planner → `CustomWorkout` / `WorkoutScheduler`; HK completed-workout import; docs limits; personal-team capability gating | Plan (docs may already be on `main`) |
| B | `cursor/zone2-running-program-2ab3` | Zone math + evidence, progressive program content, watchOS live session UI + haptics, WorkoutKit Zone 2 schedule path, post-run summary | **A** (stack base = A’s branch until A merges, then retarget to `main`) |
| C | `cursor/install-device-doc-2ab3` | `docs/INSTALL_DEVICE.md` (free Personal Team vs paid / TestFlight) | **A** preferred (documents gated capabilities); may land on `main` after A |

Each PR: feature branch, merge commits when updating bases (no force-push on open PRs), local checks before every push, CI green before stacking the next head. No AI/Cursor mentions in commits or PR bodies.

### i18n handoff (translations agent)

- **This stack** owns **en-GB only** for all new user-facing strings (content locale JSON and any `en-GB.lproj` / String Catalog keys).
- Put strings in the appropriate en-GB catalogs with **stable keys** and **comments** (purpose + tone). For watch UI strings, include an explicit **`maxLength`** (characters) in the comment / meta so mid-run type stays glanceable.
- **Do not rename or reshape keys** once a PR that introduces them is open — the translations agent (`bc-f5923d7f`) stacks non–en-GB locale PRs on these source branches.
- Required locales still exclude Russian (`scripts/check_no_ru_locale.sh`). Non–en-GB fills are out of scope for A/B/C implementation PRs.

---

## A — WorkoutKit integration

### In scope

1. **Planner → WorkoutKit**
   - Map OmniGart planned sessions (warm-up, work/rest or interval blocks, cool-down, goals) to `CustomWorkout` / related WorkoutKit plan types.
   - Support goals and alerts where WorkoutKit allows: heart-rate zone, pace, cadence (document API gaps honestly).
2. **`WorkoutScheduler`**
   - Request authorisation; schedule workouts; handle the **scheduled-workout count cap** and date/time placement.
   - Clear / reschedule when the OmniGart plan changes (best-effort; log conflicts in OPEN_QUESTIONS if OS limits bite).
3. **HealthKit readback**
   - After completion (system Workout app or OmniGart), read `HKWorkout` (+ relevant samples) and feed **load / recovery** inputs already used by planning/readiness (reuse `HealthClient` / HK bridge patterns; feature-flag if sync is still gated).
4. **Documentation**
   - `docs/WORKOUTKIT.md` (or section under existing health docs):
     - Requires iOS 17+ / watchOS 10+ APIs (deployment already ≥ that).
     - **No strength set logging** via WorkoutKit (sets/reps stay in-app).
     - **No Wear OS equivalent** for system Workout scheduling — Wear parity is a later PR (see OPEN_QUESTIONS).
5. **Personal Team / signing**
   - Entitlements and XcodeGen flags arranged so a **Personal Team** build still compiles: paid-only capabilities optional or compile-time / runtime gated (see also PR C).

### Out of scope (A)

- Full Zone 2 program UI and live watch metrics screen (PR B).
- Wear OS workout scheduling.
- Strength WorkoutKit plans.

### Key touchpoints (expected)

| Area | Paths / notes |
|------|----------------|
| iOS bridge | `iosApp/OmniGart/Health/…`, new `WorkoutKit/…` Swift types |
| watchOS | Schedule consumption is system Workout app; OmniGart watch may deep-link later |
| Shared | Session DTO → platform mapper; load update from imported workouts |
| Entitlements | `iosApp/OmniGart/OmniGart.entitlements` (+ watch if required) |
| Tests | Mapper unit tests; fake scheduler; HK import fake on shared/host tests |

---

## B — Zone 2 running program

### Zone model

- **Default:** Zone 2 from **%HRR / Karvonen** using resting HR and measured or estimated max HR (Tanaka / existing `FormulaCatalog` HRmax options).
- **Options:** LTHR-based bands and **%HRmax** Zone 2 (reuse / extend `shared/activity-algorithms` `HeartRateZones` + `shared/.../calc` strategies).
- **Evidence:** formulas and verified publication links in the **evidence registry** (`content/evidence/claims`, citations). Prefer existing Karvonen / Tanaka entries; add LTHR / Zone 2 prescription claims only with verified DOI/PMID (else OPEN_QUESTIONS).

### Program content

- Progressive **weekly** Zone 2–biased sessions under `content/running/` (new plan JSON + workout-type if needed), schema-valid, bundled via existing pack scripts.
- en-GB name/description keys with comments; no non–en-GB locales in this PR.

### watchOS in-app workout

- `HKWorkoutSession` + `HKLiveWorkoutBuilder` (running).
- **One glanceable screen:** current HR, current zone (colour band), time in zone, cadence, distance; large legible type; Always On friendly; elapsed time only if it fits without clutter.
- Design tokens from `design/generated` watch guidance where available.

### Haptics

- Distinct patterns for **enter target zone** vs **leave above** vs **leave below**.
- Debounce / hysteresis (~5–10 s and/or a few bpm) to avoid chatter.
- User setting to disable; default on for first ship (confirm in OPEN_QUESTIONS if product prefers default off).

### WorkoutKit dual path

- Same Zone 2 session exposable as a WorkoutKit workout with a **Zone 2 HR alert** for the **system Workout** app (depends on PR A scheduler/mapper).

### Summary

- After finish: time in zone, distance, average cadence, HR chart (Swift Charts or existing chart helper). Wellness copy only.

### Out of scope (B)

- **Wear OS parity** (note in OPEN_QUESTIONS; later PR).
- Full race plans beyond the Zone 2 progressive block.

### en-GB string keys (illustrative — keep stable once PR opens)

Prefix suggestion: `watch.zone2.*`, `running.plan.zone2_*`, `settings.haptics.zone2_*`.

| Key (example) | Comment must include |
|---------------|----------------------|
| `watch.zone2.hr_label` | maxLength ≈ 4–6 (e.g. “HR”) |
| `watch.zone2.zone_label` | maxLength for “Z2” / “Zone 2” short form |
| `watch.zone2.time_in_zone` | maxLength for compact duration label |
| `watch.zone2.cadence` | units abbreviation length |
| `watch.zone2.distance` | units abbreviation length |
| `watch.zone2.elapsed` | optional; only if shown |

Exact key list lands in PR B’s en-GB catalog; translations agent stacks other locales afterward.

---

## C — `docs/INSTALL_DEVICE.md`

### In scope

Step-by-step install for **personal iPhone 16 Pro Max + Apple Watch Ultra** via Xcode:

1. **Free Apple ID / Personal Team**
   - 7-day provisioning, app ID limits, device registration.
   - Which capabilities work: HealthKit (typically yes for development), WorkoutKit / background modes / associated domains — **document what we verified** and what requires a paid account.
   - How OmniGart gates paid-only capabilities so the project **builds** on a Personal Team.
2. **Paid Apple Developer Program**
   - Archive / TestFlight path for iOS + watchOS companion.
3. **Prerequisites**
   - Xcode version, signing team selection (`DEVELOPMENT_TEAM`), enabling Developer Mode on phone/watch, first-run Health permissions (JIT explainers).

### Out of scope

- App Store Connect production release checklist (see `docs/RELEASE_CHECKLIST.md`).

---

## Testing & CI (every implementation PR)

- Shared/Kotlin unit tests for zone math, mappers, hysteresis.
- Swift tests where CI macOS job already builds watch/iOS (smoke + mapper).
- `scripts/check_no_ru_locale.sh`; banned-terms; app-review wellness copy checks when UI strings ship.
- Prefer path-filtered workflows; avoid waking unrelated heavy jobs.
- Local: relevant Gradle tests + `xcodebuild` smoke when touching Apple targets (or document skip if no macOS agent — CI remains gate).

## Feature flags

- Reuse or extend `FeatureFlags` so WorkoutKit scheduling and live Zone 2 watch workout can ship dark / off until product-ready, matching health-sync gating style.

## Decisions to log (OPEN_QUESTIONS)

- Wear OS Zone 2 / system-workout parity deferred.
- Default haptic on vs off.
- LTHR Zone 2 band edges if publication support is thin.
- Personal Team: exact capability matrix after trying a real free-team archive.
- Whether HK workout import updates load while `healthSyncEnabled` is still false (import-only path vs flag).
