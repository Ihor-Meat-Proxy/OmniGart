# WorkoutKit integration (Apple)

OmniGart can turn planned sessions into **system Workout** plans via [WorkoutKit](https://developer.apple.com/documentation/workoutkit) and `WorkoutScheduler`, then read completed `HKWorkout` samples back for load/recovery.

Requires **iOS 17+ / watchOS 10+** APIs (OmniGart deployment targets are already iOS 18 / watchOS 11).

## What ships

| Piece | Role |
|-------|------|
| Shared DTOs | `com.omnigart.shared.workoutkit.ScheduledWorkoutPlan` (+ blocks, goals, alerts) |
| Validator | Rejects `STRENGTH_UNSUPPORTED`; allows strength **work/recovery** custom workouts; alert bounds; soft schedule cap **50** |
| Strength export | `StrengthWorkoutKitPlans.fromRoutineSets` — each set → WORK, each rest → RECOVERY |
| Scheduling service | Honours `FeatureFlags.workoutKitSchedulingEnabled` (default **false**) |
| Swift bridge | `WorkoutKitSchedulerBridge` + `WorkoutKitPlanBuilder` in `iosApp/OmniGart/WorkoutKit/` |
| Load import | `CompletedWorkoutLoad` from `HealthWorkout`; gated by `healthWorkoutImportEnabled` |

## Strength custom workouts (and the system Workout app limit)

**Apple's built-in Workout app cannot accept weight or rep entry from other apps.** There is no WorkoutKit API to push load × reps into the system UI.

OmniGart therefore offers two complementary paths:

| Path | Weight / reps | Where |
|------|---------------|--------|
| **OmniGart watch app** | Full set logging (crown adjust, Done → rest + haptics) | Live `HKWorkoutSession` + WCSession sync to phone |
| **System Workout app** | Follow-along only: work / recovery steps from the routine | WorkoutKit custom workout (`StrengthWorkoutKitPlans`) |

Each logged set becomes a **work** step (open goal, or time-based when prescribed). Each rest becomes a **recovery** step. Athletes can advance set-by-set in Apple's Workout app for timing and heart-rate, then log the actual load in OmniGart on phone or watch.

`STRENGTH_UNSUPPORTED` block kinds remain rejected. A `STRENGTH` activity with only `WARM_UP` / `WORK` / `RECOVERY` / `COOL_DOWN` is valid.

## Limits (product)

- **No weight/rep entry in Apple's Workout app** — documented above; OmniGart's own logger owns sets.
- **No Wear OS equivalent** for system Workout scheduling. Wear set logging uses Health Services inside OmniGart (`docs/OPEN_QUESTIONS.md` **W1** for scheduling parity).
- **Schedule cap** — refuse new schedules when ≥ 50 entries are already queued (align with OS limits; surface a clear error).
- **Authorisation** — `WorkoutScheduler.requestAuthorization()` plus HealthKit workout read for import.
- **Dates** — `scheduledStartEpochMillis` on the plan; if omitted, the bridge schedules about one hour ahead.

## Personal Team (free Apple ID)

WorkoutKit code is behind `OmniGartCapabilities.workoutKitSchedulingCompiledIn`:

- Default: enabled when the OS is new enough.
- If signing fails on a Personal Team, set Info.plist / build setting `OMNIGART_ENABLE_WORKOUTKIT=0` so the app still builds and runs without calling WorkoutKit APIs.
- HealthKit development installs usually work on a Personal Team; see `docs/INSTALL_DEVICE.md` (install guide PR).
- Watch HealthKit entitlements live in `watchApp/OmniGartWatch/OmniGartWatch.entitlements` (wired via XcodeGen `CODE_SIGN_ENTITLEMENTS`).

## Feature flags

```text
FeatureFlags.workoutKitSchedulingEnabled = false   // schedule into system Workout
FeatureFlags.healthWorkoutImportEnabled = false    // HK → load/recovery import
```

Both must remain `false` in release defaults until product-ready (`scripts/check_app_review_compliance.py`).

## Planner mapping

Expected block kinds: `WARM_UP`, `WORK`, `RECOVERY`, `COOL_DOWN`.

Alerts: `HEART_RATE_ZONE` (bpm band), with room for pace/cadence in later iterations.

## Zone 2 dual path

The same Zone 2 continuous-run session can be done two ways:

| Path | Where | HR guidance |
|------|--------|-------------|
| In-app watch session | OmniGart watchOS live workout | Zone colour + haptics (`docs/ZONE2.md`) |
| System Workout | Apple Workout app via WorkoutKit | Zone 2 `HEART_RATE_ZONE` alert on the work block |

Shared factory: `Zone2WorkoutKitPlans.continuousRun` (uses `Zone2.profile` / Karvonen for alert bpm).  
iOS bridge: `Zone2WorkoutKitScheduler` → `WorkoutKitSchedulerBridge` when flags / `OMNIGART_ENABLE_WORKOUTKIT` allow.

`FeatureFlags.workoutKitSchedulingEnabled` stays **false** until product-ready.
