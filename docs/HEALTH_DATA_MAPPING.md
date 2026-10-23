# OmniGart ↔ HealthKit ↔ Health Connect mapping

Status: product contract for the local-first health store (`shared/.../health/store`).  
Canonical storage is always OmniGart’s SQLDelight DB. Platform sync is optional and idempotent.

## Principles

1. **Local-first.** Every workout and sample OmniGart records or receives is stored locally, even when Health permission is denied.
2. **One canonical record.** Stable `syncIdentifier` + monotonic `syncVersion`.
3. **Never corrupt platform data.** Do not write back foreign-app data. Do not delete platform records OmniGart did not create.
4. **Richest local copy.** Prefer full-resolution series over averages; merge without silent overwrite.
5. **Conflict policy.** Prefer original source, keep both values with provenance, show the user (`health_conflict` table).

## Sync identity

| Layer | Identifier | Version |
| --- | --- | --- |
| OmniGart | `HealthSyncIdentity.syncIdentifier` (`omnigart.workout.<localId>`) | `syncVersion` (Long, starts at 1) |
| Apple Health | `HKMetadataKeySyncIdentifier` | `HKMetadataKeySyncVersion` |
| Health Connect | `Metadata.clientRecordId` | `Metadata.clientRecordVersion` |

Also stored: `omnigart.origin`, `omnigart.healthSchemaVersion` (= 1).

## Workout field matrix

| OmniGart field | Unit | HealthKit | Health Connect | Notes |
| --- | --- | --- | --- | --- |
| `id` | string | local only (UUID also in `platformRecordId`) | local only (`metadata.id` mirrored) | Stable OmniGart primary key |
| `identity.syncIdentifier` | string | `HKMetadataKeySyncIdentifier` | `clientRecordId` | Idempotent upsert key |
| `identity.syncVersion` | long | `HKMetadataKeySyncVersion` | `clientRecordVersion` | Re-save updates in place |
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
| `deleted` | bool | soft-delete local; HK delete only if OmniGart-owned | HC delete only if OmniGart-owned | Two-way deletion via anchors / changes tokens |

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

## Incremental sync

| Platform | Mechanism | Stored in |
| --- | --- | --- |
| Apple Health | `HKAnchoredObjectQuery` anchor | `health_sync_state.change_token` (`platform=APPLE_HEALTH`) |
| Health Connect | Changes API token | `health_sync_state.change_token` (`platform=HEALTH_CONNECT`) |

Deletions: platform deletion soft-deletes the local mirror. Local soft-delete of OmniGart-owned records may delete the platform twin. Foreign-origin platform records are never deleted by OmniGart.

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

Shared tests (`HealthDataRoundTripTest`) assert that export → import (JSON) and mapper platform field conversions preserve:

- timestamps, activity type, active/basal energy, distance, steps
- full HR series order and values
- route points, events, sets
- sync identity (identifier + version)

Floating-point: energy to 0.001 kcal, distance to 0.001 m, HR to 0.1 bpm.
