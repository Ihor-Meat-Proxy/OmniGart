# WorkoutKit integration (Apple)

OmniGart can turn planned endurance sessions into **system Workout** plans via [WorkoutKit](https://developer.apple.com/documentation/workoutkit) and `WorkoutScheduler`, then read completed `HKWorkout` samples back for load/recovery.

Requires **iOS 17+ / watchOS 10+** APIs (OmniGart deployment targets are already iOS 18 / watchOS 11).

## What ships

| Piece | Role |
|-------|------|
| Shared DTOs | `com.omnigart.shared.workoutkit.ScheduledWorkoutPlan` (+ blocks, goals, alerts) |
| Validator | Rejects strength; enforces alert bounds; soft schedule cap **50** |
| Scheduling service | Honours `FeatureFlags.workoutKitSchedulingEnabled` (default **false**) |
| Swift bridge | `WorkoutKitSchedulerBridge` + `WorkoutKitPlanBuilder` in `iosApp/OmniGart/WorkoutKit/` |
| Load import | `CompletedWorkoutLoad` from `HealthWorkout`; gated by `healthWorkoutImportEnabled` |

## Limits (product)

- **No strength sets/reps** via WorkoutKit — strength logging stays in the OmniGart logger. Plans marked `STRENGTH` or `STRENGTH_UNSUPPORTED` are rejected.
- **No Wear OS equivalent** for system Workout scheduling. Wear Zone 2 / live workout parity is a later PR (`docs/OPEN_QUESTIONS.md` **W1**).
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
