# OmniGart ↔ HealthKit ↔ Health Connect mapping

Status: product contract for the local-first health store (`shared/.../health/store`).  
Canonical storage is always OmniGart’s SQLDelight DB. Platform sync is optional and **append-only**.

## Principles

1. **Local-first.** Every workout and sample OmniGart records or receives is stored locally, even when Health permission is denied.
2. **One canonical record.** Stable `syncIdentifier` (dedupe key). `syncVersion` is stamped at 1 on first platform insert; bumped only for user-initiated **restore** after platform deletion.
3. **Append-only platform writes (hard rule).** OmniGart must **never delete or rewrite** any data in Apple Health or Health Connect — **not even records OmniGart created**. Before writing, exist-check `HKMetadataKeySyncIdentifier` / `clientRecordId`; if present, **skip**. Never call HealthKit `delete` / `deleteObjects` or Health Connect `deleteRecords` / `updateRecords`.
4. **Local edits stay in OmniGart.** Changing a workout after the first successful platform insert updates the local DB only.
5. **Platform deletion → keep local.** If Apple Health / Health Connect deletes a sample, mark the OmniGart row `removedFromPlatform` and keep all data. Do not auto re-insert.
6. **User restore (optional).** From the workout detail screen or Data privacy → **Restore all removed**, the user can re-insert the local copy. Still append-only: bump `syncVersion` / `clientRecordVersion`, exist-check, then insert (HKWorkout save / `insertRecords`). On success, clear `removedFromPlatform`. If the sync id is already present, clear the flag and skip. Ask for write permission when missing. Deleting again on the platform re-enters the same flow.
7. **Never corrupt foreign data.** Do not write back foreign-app data. Restore is OmniGart-origin only.
8. **Richest local copy.** Prefer full-resolution series over averages; merge without silent overwrite.
9. **Conflict policy.** Prefer original source, keep both values with provenance, show the user (`health_conflict` table).

## Sync identity

| Layer | Identifier | Version |
| --- | --- | --- |
| OmniGart | `HealthSyncIdentity.syncIdentifier` (`omnigart.workout.<localId>`) | `syncVersion` (Long, starts at 1; bump only for restore) |
| Apple Health | `HKMetadataKeySyncIdentifier` | `HKMetadataKeySyncVersion` (first insert = 1; restore bumps; never overwrite existing) |
| Health Connect | `Metadata.clientRecordId` | `Metadata.clientRecordVersion` (first insert = 1; restore bumps; never `updateRecords`) |

Also stored: `omnigart.origin`, `omnigart.healthSchemaVersion` (= 1).

**First-write path:** `PlatformAppendOnlyPolicy.shouldAttemptPlatformWrite` → exist-check sync id → `insert` only → `markPlatformLinked`.

**Restore path:** `shouldAttemptPlatformRestore` → `prepareRestore` (bump version) → exist-check → if present, `reconcileAlreadyOnPlatform`; else insert with HR/energy/distance/events as available → `completeRestore` (clear `removedFromPlatform`). UI: workout detail + Data privacy bulk restore (`Restore to Apple Health` / `Restore to Health Connect`).

## Workout field matrix

| OmniGart field | Unit | HealthKit | Health Connect | Notes |
| --- | --- | --- | --- | --- |
| `id` | string | local only (UUID also in `platformRecordId`) | local only (`metadata.id` mirrored) | Stable OmniGart primary key |
| `identity.syncIdentifier` | string | `HKMetadataKeySyncIdentifier` | `clientRecordId` | Dedupe / first-write key; skip write if exists |
| `identity.syncVersion` | long | `HKMetadataKeySyncVersion` | `clientRecordVersion` | First insert = 1; bump only for user restore after platform deletion |
| `activityType` | enum | `HKWorkoutActivityType` | `ExerciseSessionRecord.exerciseType` | See activity map below |
| `startAt` / `endAt` | Instant (UTC) | `HKWorkout.start/end` | session start/end + zone offset | Convert TZ at platform boundary |
| `activeEnergyKcal` | kcal | `activeEnergyBurned` (kcal) | `ActiveCaloriesBurnedRecord` (kcal) | Never fold basal into active |
| `basalEnergyKcal` | kcal | `basalEnergyBurned` (kcal) | `BasalMetabolicRateRecord` / resting energy where available | HC basal often daily BMR; session basal may be OmniGart-only |
| `distanceMetres` | m | `distanceWalkingRunning` / cycling / swimming | `DistanceRecord` | Activity-specific HK types |
| `steps` | count | `stepCount` | `StepsRecord` | |
| `heartRateSeries[]` | bpm @ Instant | `HKQuantityType.heartRate` samples | `HeartRateRecord.samples` | Full series preferred |
| `averageHeartRateBpm` / `maxHeartRateBpm` | bpm | derived / workout statistics | derived | Recomputed from series when present |
| `averageCadenceSpm` | spm | `HKQuantityType.walkingStepSpeed` / cycling cadence | `SpeedRecord` / cadence extensions | Granularity differs; keep local richest |
| `route[]` | lat/lon/alt | `HKWorkoutRoute` | `ExerciseRoute` (HC) | May be unavailable without location permission |
| `events[]` pause/resume/lap/segment | — | `HKWorkoutEvent` | exercise segments / rest | Segment model ≠ HC single session; store both locally |
| `sets[]` reps/load | kg, reps | OmniGart metadata (`OmniGartTotal*`) + local DB | `ExerciseSessionRecord` + local DB | Per-set detail is OmniGart-richest |
| `planId` / `sessionId` | string | metadata / local link | metadata / local link | Links WorkoutKit / logging session |
| `origin` / `sourceBundle` / `sourceName` | enum/string | `HKSource` + OmniGart metadata | `DataOrigin` + metadata | Filter write-back by origin |
| `platformRecordId` | string | HK workout UUID | HC `metadata.id` | Set after first successful insert; blocks re-write |
| `deleted` | bool | **local soft-delete only** | **local soft-delete only** | Never deletes platform objects |
| `removedFromPlatform` | bool | set when HK anchored query reports deletion | set when HC Changes reports deletion | Keep local data; user may restore (append-only re-insert) |

## Activity type map

| OmniGart | HealthKit | Health Connect |
| --- | --- | --- |
| STRENGTH | `traditionalStrengthTraining` | `EXERCISE_TYPE_STRENGTH_TRAINING` |
| RUNNING | `running` | `EXERCISE_TYPE_RUNNING` |
| CYCLING | `cycling` | `EXERCISE_TYPE_BIKING` |
| SWIMMING | `swimming` | `EXERCISE_TYPE_SWIMMING_POOL` |
| WALKING | `walking` | `EXERCISE_TYPE_WALKING` |
| HIIT | `highIntensityIntervalTraining` | `EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING` |
| YOGA | `yoga` | `EXERCISE_TYPE_YOGA` |
| PILATES | `pilates` | `EXERCISE_TYPE_PILATES` |
| OTHER | `other` | `EXERCISE_TYPE_OTHER_WORKOUT` |

Unmapped platform types import as `OTHER` and keep the raw platform type in `notes` / metadata when available.

## Sample types

| OmniGart `HealthSampleType` | HealthKit | Health Connect | Unit |
| --- | --- | --- | --- |
| HEART_RATE | `heartRate` | `HeartRateRecord` | bpm |
| ACTIVE_ENERGY | `activeEnergyBurned` | `ActiveCaloriesBurnedRecord` | kcal |
| BASAL_ENERGY | `basalEnergyBurned` | basal / BMR records | kcal |
| DISTANCE | distance quantity types | `DistanceRecord` | m |
| STEPS | `stepCount` | `StepsRecord` | count |
| CADENCE | cadence quantities | cadence if available | spm |

## Platform asymmetries

| Case | Handling |
| --- | --- |
| Type exists only on one platform (e.g. mindful minutes on HK) | Keep locally; sync only to supporting platform; document in export JSON |
| Energy: active vs total | Local always stores active + basal separately; never write total-only as active |
| HR series vs single average | Import series when present; if only average exists, store scalar and empty series |
| Session vs segment models | HK events/segments mapped to `local_health_event`; HC single session + optional segments |
| Routes without location permission | Workout still saved; `route` empty |
| Different workout type enums | Map via tables above; unknown → `OTHER` |
| HC basal often not session-scoped | Prefer OmniGart basal estimate for OmniGart-origin sessions; import HC basal only when session-linked |
| User edits a synced workout in OmniGart | Local DB only; platform object unchanged |
| User deletes a workout in Apple Health / HC | Local row kept with `removedFromPlatform=true` |
| User taps Restore | Bump sync version, exist-check, insert; clear flag on success |
| Route on restore | Best-effort; HK route builder needs location permission (may stay empty) |

## Restore flow

1. Platform deletion → `HealthSyncEngine.applyPlatformDeletion` sets `removedFromPlatform` (local data kept).
2. UI shows **Restore to Apple Health** (iOS) or **Restore to Health Connect** (Android) on the workout detail screen, plus **Restore all removed** in Data and privacy.
3. `prepareRestore` bumps `syncVersion` / `clientRecordVersion` and clears stale `platformRecordId`.
4. Writer exist-checks sync id / `clientRecordId`. If present → `reconcileAlreadyOnPlatform` (clear flag, no write).
5. Else append-only insert: iOS `HKWorkoutBuilder` / Android `insertRecords` (session + HR + active energy + distance when present). **Never** platform delete/update APIs.
6. `completeRestore` clears `removedFromPlatform` and stores the new platform record id.
7. If the user deletes the sample on the platform again, step 1 repeats.

## Device transfer (Watch / Wear → phone)

Wire schema: `omnigart.device.workout.transfer` (`DeviceWorkoutTransferDocument`, schemaVersion 1). Keyed by the same `omnigart.workout.<localId>` sync identifier used for platform dedupe. Phone ingest is **idempotent** (`HealthSyncEngine.ingestDeviceTransfer`).

### Apple Watch → iPhone

1. Watch finishes `HKLiveWorkoutBuilder.finishWorkout()` with `HKMetadataKeySyncIdentifier` / `HKMetadataKeySyncVersion` stamped (first write = 1).
2. Watch queues the full summary + HR samples via WCSession `transferUserInfo` (or `transferFile` when large) — delivery is queued even if the phone is away.
3. iPhone `WatchWorkoutInbox` receives the payload and upserts into the local SQLDelight store by sync id.
4. **iPhone must not write HealthKit again** — the watch already owns the HKWorkout. Import from Apple Health may later see the same sync id and merge; that path never creates a second OmniGart HK insert for the same id.

### Wear OS → phone

1. Wear records with Health Services `ExerciseClient`, then `LocalHealthBridge.recordOmniGartWorkout` on the watch.
2. Wear puts the transfer JSON on the Data Layer path `/omnigart/workout/transfer/<syncId>` (`DataClient`) and keeps the local copy until ack.
3. Phone `WearWorkoutInboxService` ingests into the local store, then append-only `insertRecords` into Health Connect **once**, deduped by `clientRecordId` (exist-check; never `updateRecords` / `deleteRecords`).
4. Phone sends MessageClient ack on `/omnigart/workout/ack` with the sync id; Wear soft-deletes its pending copy and drops the Data Item.
5. Retries are safe: phone ingest and HC exist-check are both idempotent.

## Incremental sync

| Platform | Mechanism | Stored in |
| --- | --- | --- |
| Apple Health | `HKAnchoredObjectQuery` anchor | `health_sync_state.change_token` (`platform=APPLE_HEALTH`) |
| Health Connect | Changes API token | `health_sync_state.change_token` (`platform=HEALTH_CONNECT`) |
| Apple Watch → iPhone | WCSession `transferUserInfo` / `transferFile` | Phone local store only (no second HK write) |
| Wear OS → phone | Data Layer + MessageClient ack | Phone local store + one HC `insertRecords` |

Deletions on the platform **never** delete OmniGart data and **never** trigger platform delete APIs. They only set `removedFromPlatform`. Local soft-delete (`deleted=1`) is OmniGart-only.

## Forbidden platform APIs (CI gate)

`scripts/check_no_health_platform_mutate.sh` (wired into `lint-gate`) fails the build if production sources contain:

- HealthKit: `deleteObjects(`, `healthStore.delete(`, `HKHealthStore().delete(`, `.delete(object` / `.delete(objects`
- Health Connect: `deleteRecords(`, `updateRecords(`

Unit/contract coverage: `PlatformAppendOnlyPolicyTest` + `PlatformAppendOnlyPolicy`.

## Conflict policy (per record)

When Apple Health and Health Connect (or local vs platform) disagree for overlapping fields:

1. Prefer the **original source** (`HealthRecordOrigin`).
2. **Keep both** values in `health_conflict` with provenance.
3. **Show the user** in Profile → Data and privacy → Conflict review.
4. **Never silently overwrite** scalars; merge richest series via `HealthDataMapper.mergeRichest`.

## Export formats

User-triggered; local only (iOS share sheet / Files, Android SAF).

| Format | Contents |
| --- | --- |
| **JSON** | `OmniGartHealthExportDocument` (`schema=omnigart.health.export`, `schemaVersion=1`) — full workouts including series, route, events, sets. Re-importable. |
| **CSV** | Per type: workouts, heart-rate series, other samples |
| **GPX** | Routes (`trk` / `trkpt`) |
| **TCX** | Workouts, laps/segments, trackpoints, calories, avg HR |
| **PDF** | Coach-ready summary (single workout or date range). Shared layout model `WorkoutPdfDocument` rendered on-device with `UIGraphicsPDFRenderer` (iOS) / `PdfDocument` (Android). Includes date, activity, duration, active/total kcal, avg/max HR, HR chart with zones + time-in-zone, distance/pace/cadence when applicable, strength sets/reps/load + total weight + muscle intensity bars, yoga/Pilates segments, OmniGart branding, wellness disclaimer footer. Locale strings for all 11 app languages; fonts: bundled Noto Sans (Latin/Cyrillic), Noto Sans Georgian (ka), platform CJK cascade for zh-Hans. |
| **FIT** | **Not shipped.** No AGPL-compatible FOSS FIT writer is vendored (Garmin FIT SDK is proprietary). Export emits a plain-text note artifact instead. |

## Import

- OmniGart JSON → `HealthExportService.importJson` → local store with `origin=OMNIGART_JSON_IMPORT`.
- Platform workouts (system Workout app / Health Connect) → `HealthSyncEngine.importFromPlatform` with plan link when known.

## Round-trip guarantee

Shared tests (`HealthDataRoundTripTest`, `PlatformAppendOnlyPolicyTest`, `DeviceWorkoutTransferTest`) assert that export → import (JSON) and mapper platform field conversions preserve:

- timestamps, activity type, active/basal energy, distance, steps
- full HR series order and values
- route points, events, sets
- sync identity (identifier + version)
- append-only eligibility (no platform rewrite after link; platform deletion keeps local; restore bumps version and clears flag)

Floating-point: energy to 0.001 kcal, distance to 0.001 m, HR to 0.1 bpm.
