# Polar offline recording — what is stored (SDK 8.4.0)

Sources: Polar BLE SDK product docs and API reference
([PolarH10.md](https://github.com/polarofficial/polar-ble-sdk/blob/master/documentation/products/PolarH10.md),
[SdkOfflineRecordingExplained.md](https://github.com/polarofficial/polar-ble-sdk/blob/master/documentation/SdkOfflineRecordingExplained.md),
[PolarH10OfflineExerciseApi](https://polarofficial.github.io/polar-ble-sdk/polar-sdk-android/com/polar/sdk/api/PolarH10OfflineExerciseApi.html)).

## Polar H10 — internal exercise recording

| Fact | Detail |
|------|--------|
| API | **`PolarH10OfflineExerciseApi`** (not `PolarOfflineRecordingApi`) |
| Capacity | **One recording at a time** — start another only after stop + remove |
| Sample types | `SampleType.HR` or `SampleType.RR` |
| HR interval | Product docs: **HR with one-second sample time**. API intervals: 1 / 5 / 15 / 60 s; interval **ignored for RR** |
| Live (while connected) | HR + RR (ms), ECG µV @ **130 Hz**, accelerometer 25–200 Hz |
| Offline contents | Stored exercise entry with HR samples (1 Hz typical) **or** RR series when started as RR. Product markdown emphasises HR @ 1 s; RR offline is exposed by the SDK API and should be verified on-device |
| Lifecycle | `startRecording` → (BLE may drop; recording continues) → `stopRecording` → `listExercises` → `fetchExercise` → `removeExercise` |
| Status | `requestRecordingStatus` → `(ongoing, entryId)` |

**Implication for swim:** Start HR (1 s) on deck, swim without phone, reconnect, download the single entry, delete it, merge by timestamp with the watch workout.

## Polar Verity Sense — offline recording API

| Fact | Detail |
|------|--------|
| API | **`PolarOfflineRecordingApi`** (from firmware **2.1.0**) |
| Data types | HR, PPG, PPI, ACC, Gyro, Magnetometer (online + offline; SDK mode changes rate/range tables) |
| Memory | Limit 1 = **2 MB**, limit 2 = **300 KB** (SDK offline-recording table) |
| Triggers | `TRIGGER_SYSTEM_START`, `TRIGGER_EXERCISE_START` (swim mode counts), `TRIGGER_DISABLED` |
| Security | Optional `PolarRecordingSecret` encryption |
| Ops | `startOfflineRecording` / `stopOfflineRecording` / `listOfflineRecordings` / `getOfflineRecord` / `removeOfflineRecord` |

Verity Sense is **not** the primary swim-strap path in this PR; H10 ECG strap is. Verity Sense offline is stubbed at the interface level (`listOfflineRecordings` default empty in shared).

## Clock / merge notes

H10 offline HR samples are indexed at 1 Hz from recording start. Prefer setting device time before deck start when the SDK exposes it (`setLocalTime`). Otherwise estimate a constant offset between strap and watch timelines (`ClockOffsetEstimator`) using deck-start / post-swim overlap anchors.
