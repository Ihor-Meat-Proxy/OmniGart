# Phase 1 — Strength logging

## Goal

Ship a usable strength workout logger on phone (Android + iOS shells), backed by the shared domain + SQLDelight, with templates from the exercise library, history, RIR, 1RM via formula strategies, workout writes into the health layer, and a **workout summary with volume load** (kg/lb per user units).

## In scope

### Logging

- Start / end workout session; add exercises from `content/exercises` (search/filter via planning APIs or a thin shared wrapper).
- Sets: weight × reps, **warmup / working** flag, drop / failure flags, **RIR**, rest timer (local).
- Assisted sets: log assistance mass (kg) to subtract from effective load.
- Weighted bodyweight (e.g. dips/pull-ups): log added external weight on top of BW × coefficient.
- Unilateral: honour exercise library `unilateral`; when logged **per hand**, volume counts **both sides**.
- Templates: load starters; save session as user template.
- History: list / detail / basic edit/delete.
- **1RM** via formula-strategy catalog (default until Phase 4 settings UX).
- Write completed workouts through health adapters (best-effort).
- en-GB + uk; design icons (workouts / add-set / timer).

### Workout summary — volume load

**Volume load** = Σ (effective_load_kg × reps) over sets, displayed in **kg or lb** per user units.

Rules (pure functions in `shared` — `VolumeLoad` / `VolumeLoadStats` / `BodyweightLoadCoefficients` / `MassUnits`, unit-tested):

| Case | Effective load |
|------|----------------|
| External load (barbell/DB machine) | logged weight |
| Warm-up sets | **excluded by default**; include when user toggles “count warm-ups” |
| Bodyweight (`bodyweight_reps` / BW movements) | `bodyweightKg × loadCoefficient` (+ optional added weight); mark **estimated** |
| Assisted | `max(0, baseLoad − assistanceKg)` |
| Unilateral + logged per hand | multiply set contribution by **2** (both sides), driven by library `unilateral` |

Summary must also show:

- **Per-exercise** volume
- **Per-muscle** volume (feeds body-map heatmap in `shared/bodymap` once that package merges; emit stable muscle keys matching the exercise schema)
- **Hard sets** count (working sets; definition aligned with planning volume landmarks — non-warmup completed sets)
- **vs previous** same workout/template (delta volume, hard sets)
- **PRs** (best e1RM / best set volume / best session volume for that exercise — pure helpers)
- Roll into **weekly / monthly** stats aggregates

### Tests

- Repositories, 1RM strategy selection, fake health writes.
- Volume edge cases: warm-up toggle, BW coefficient, assisted floor at 0, unilateral ×2, unit conversion display, previous-session diff, PR detection.

## Out of scope

- Full periodisation UI; barcode food; water reminders; readiness card; backup; achievements.
- Deep watch workout UI (Phase 7).
- Body-map UI itself (consume volume maps when `shared/bodymap` lands).

## Dependencies

- Phase 0 + design + exercise/planning.
- Health integration layer.
- Optional later: `shared/bodymap` for heatmap rendering.

## Verification

```bash
./gradlew checkNoRuLocale \
  :shared:testAndroidHostTest :shared:planning:jvmTest \
  :androidApp:testDebugUnitTest ktlintCheck detekt
```

Manual: log a mixed session (warmup + BW + assisted + unilateral DB), toggle warm-ups, switch kg/lb, confirm summary + weekly stats.

## Provisional decisions (also in OPEN_QUESTIONS)

- Default BW **load coefficients** by movement family (e.g. push-up ≈ 0.64, pull-up ≈ 1.0, dip ≈ 1.0) — documented constants; refine with verified citations when available.
- Hard set = completed, non-warmup strength set with reps ≥ 1.
- Previous “same workout” match key = template/workout id if present, else fuzzy exercise-id multiset.
