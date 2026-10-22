# Phase 1 — Strength logging

## Goal

Ship a usable strength workout logger on phone (Android + iOS shells), backed by the shared domain + SQLDelight, with templates from the exercise library, history, RIR, 1RM via formula strategies, and workout writes into the health integration layer.

## In scope

- Start / end workout session; add exercises from `content/exercises` (search/filter via planning module APIs or a thin shared wrapper).
- Sets: weight × reps, warmup / drop / failure flags, **RIR**, rest timer state (local).
- Templates: load starter templates; save current session as user template.
- History: list past sessions; open detail; basic edit/delete.
- **1RM** estimates using the formula-strategy catalog (user default; Phase 4 will expand settings UX).
- Write completed workouts to HealthKit / Health Connect / Google Health API through the Phase H adapters (best-effort; fail soft with user-visible status).
- en-GB + uk strings; design-system icons for workouts / add-set / timer.
- Unit tests for repositories, 1RM strategy selection, and fake health write paths.

## Out of scope

- Full periodisation UI (planning engine may be called but not a planner screen yet).
- Barcode food, water reminders, readiness card, backup, achievements.
- Deep watch workout UI (Phase 7).

## Dependencies

- Phase 0 foundation + design + exercise/planning packages.
- Health integration layer (adapters + fakes).

## Verification

- `./gradlew checkNoRuLocale :shared:testAndroidHostTest :shared:planning:jvmTest :androidApp:testDebugUnitTest ktlintCheck detekt`
- Apple simulator build/test on CI for shells that gain new screens.
- Manual: log a 3-exercise session, see history, confirm fake health client received a workout write.

## Open questions to log if blocking

- Default 1RM formula when user has not chosen one yet.
- Whether templates live only in SQLDelight or also as exportable JSON packs.
