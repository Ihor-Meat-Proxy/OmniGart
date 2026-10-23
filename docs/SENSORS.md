# OmniGart sensors

Self-contained BLE heart-rate / HRV / Polar modules for inclusion when the Phase 0 app scaffold (PR #2) lands. **App shells are not edited here.**

| Module | Role |
|--------|------|
| `shared/sensors` | KMP `commonMain`: BLE HR Measurement parser, RR artefact correction, HRV metrics, Baevsky SI, quality score, merge helpers, manager interfaces, **treadmill FTMS / FitShow / iConsole parsers** (see `docs/TREADMILL_PROTOCOLS.md`) |
| `android/sensors` | Android BLE central (standard HR Profile) + Polar BLE SDK 8.4.0 adapter |
| `ios/Packages/OmniGartSensors` | Swift 6 package: CoreBluetooth HR Profile + optional `OmniGartSensorsPolar` (Polar SPM) |

HealthKit / Health Connect **writes** are owned by the later health layer. Export types: `HeartRateSample`, `WorkoutHeartRateSeries`, `MergedWorkoutHeartRate`.

---

## 1. Standard BLE Heart Rate Profile

| Item | UUID |
|------|------|
| Heart Rate Service | `0x180D` |
| Heart Rate Measurement | `0x2A37` |
| Body Sensor Location | `0x2A38` |
| Battery Service / Level | `0x180F` / `0x2A19` |
| Device Information | `0x180A` |

Parser: `HeartRateMeasurementParser` (flags: uint8/uint16 HR, sensor contact, energy expended, RR in 1/1024 s → ms). Exhaustive vectors live in `HeartRateMeasurementParserTest`.

---

## 2. Device manager behaviour

- Scan filtered by Heart Rate Service.
- Remember / forget devices (Android SharedPreferences; iOS UserDefaults — replace with SQLDelight later).
- Auto-reconnect with exponential backoff + full jitter (`ReconnectBackoff`, cap 60 s).
- Multi-sensor: discovery list + one active GATT/Polar connection in the façade (extend to N later).
- Permissions:
  - **Android 12+:** `BLUETOOTH_SCAN` (`neverForLocation`) + `BLUETOOTH_CONNECT`; legacy location ≤ API 30.
  - **iOS:** `NSBluetoothAlwaysUsageDescription`; `UIBackgroundModes` → `bluetooth-central` for phone workouts.
- Battery-friendly: Android `SCAN_MODE_LOW_POWER`; stop scan after connect; backoff reconnect; close GATT on disconnect.

### Watch / Wear limits

| Platform | External BLE HR strap |
|----------|------------------------|
| **watchOS** | CoreBluetooth **central** can talk to a strap for in-app UI / Shared pipeline. **HealthKit workouts** (`HKWorkoutSession` / workout builder) use the Apple Watch optical HR sensor; an external strap is **not** substituted as the HealthKit HR source the way a phone-centric BLE workout might expect. Prefer watch optical HR for HK metrics; optionally mirror strap samples in OmniGart storage. |
| **Wear OS** | Wear can act as BLE central and consume standard HR Profile straps during exercise more freely, subject to OEM battery / companion routing. Prefer on-watch optical HR when the strap is unreachable underwater or out of range. |

Constants: `PlatformPermissions.Apple.WATCHOS_EXTERNAL_HR_LIMITATION`, `AppleBluetoothUsage.watchOSExternalHrLimitation`.

---

## 3. RR / HRV processing (`commonMain`)

| Metric | Notes |
|--------|-------|
| Artefact correction | Lipponen & Tarvainen 2019, DOI [10.1080/03091902.2019.1640306](https://doi.org/10.1080/03091902.2019.1640306) |
| rMSSD, ln rMSSD, SDNN, pNN50 | Task Force 1996 DOI [10.1161/01.CIR.93.5.1043](https://doi.org/10.1161/01.CIR.93.5.1043); Shaffer & Ginsberg 2017 DOI [10.3389/fpubh.2017.00258](https://doi.org/10.3389/fpubh.2017.00258) |
| Baevsky SI | `SI = AMo / (2 × Mo × MxDMn)`, **50 ms** bins; see variants in `BaevskyStressIndex` KDoc (raw SI, percent AMo, Kubios √SI, median-as-mode) |
| Quality score | Retention × physiological RR share × artefact penalty × contact × parse rate |

---

## 4. Polar BLE SDK

| Field | Value |
|-------|--------|
| Version | **8.4.0** (released 2026-10-01) |
| Repo | https://github.com/polarofficial/polar-ble-sdk |
| Licence | **Polar Software Development Kit Limited License Agreement** (`Polar_SDK_License.txt`) — proprietary limited licence (not AGPL). Use/copy/modify with copyright notice; object-code redistribution with Developer Software for Polar data transfer; **not** for life-critical/medical use per Polar terms. |
| Android | JitPack `com.github.polarofficial:polar-ble-sdk:8.4.0` |
| iOS | SPM `exact: "8.4.0"` → product `PolarBleSdk` |

### Offline / swimming matrix (verified from Polar product docs)

| Device | Live HR/RR | ECG | ACC | Offline / internal recording |
|--------|------------|-----|-----|------------------------------|
| **H10** | Yes (HR + RR ms) | Yes (~130 Hz) | Yes | **H10 Exercise Recording** API: start/stop/list/fetch/remove; sample types **HR** (1 s or 5 s) or **RR**; one recording at a time; continues without BLE (pool use) |
| **Verity Sense** | Yes (HR; PPI separate) | No | Yes (+ gyro/mag/PPG) | **SDK Offline Recording** (FW ≥ 2.1.0): HR, PPG, PPI, ACC, gyro, mag; triggers; encryption optional. **File transfer prohibited** while in internal training / swimming mode — return to sensor mode before list/download |
| H9 / OH1 | HR (product-specific) | — | — | See Polar docs; not primary swim offline path for OmniGart |

**Swim merge flow:** start H10 offline (or Verity offline before swim mode) → watch records laps/strokes → after exit, download recording → `OfflineRecordingMerger.merge(SwimWorkoutMarkers, payload)` time-aligns HR into the workout as `HeartRateSource.Merged`.

Non-Polar straps continue via standard HR Profile only (`polarFeatures()` null / unused).

---

## 5. Automatic tests vs real device

### CI job `Sensors` (this PR)

Runs on Ubuntu:

```bash
./gradlew :shared:sensors:jvmTest
```

Covers: BLE parser flag matrix + malformed packets, RR correction / HRV / Baevsky / quality, reconnect backoff, offline merge helpers.

Android unit tests (`:android:sensors:testDebugUnitTest`) when Android SDK is present: remembered store, permission list (Robolectric). **Not** live BLE.

Swift package tests (macOS / Xcode): UUID expansion, reconnect backoff, watch limitation string. CoreBluetooth / Polar need a **physical** iPhone.

### Manual checklist (owner + Polar H10)

1. **Permissions** — fresh install grants Bluetooth; Android neverForLocation scan works without location toggle for BLE-only.
2. **Pair / remember** — scan finds H10; connect; remember; kill app; relaunch auto-reconnects with backoff.
3. **Live HR** — notifications parse to plausible BPM; battery + body location read when exposed.
4. **RR quality** — strap worn; RR list non-empty; `RrProcessor` quality &gt; 0.5 at rest; artefact injection (loose strap) drops quality.
5. **Offline pool** — start H10 RR or HR recording on deck → swim with watch workout → stop recording → list/download → merge with lap markers → HR series spans workout window.
6. **Non-Polar strap** — any BLE HR belt connects via standard profile without Polar feature APIs.
7. **Simulator / emulator** — expect **no** real BLE peripherals; parsers/unit tests only.

---

## 6. Wiring into PR #2 scaffold

When Phase 0 merges:

1. `settings.gradle.kts`: `include(":shared:sensors")`, `include(":android:sensors")` (already prepared on this branch).
2. `androidApp` / `wearApp`: `implementation(project(":android:sensors"))` + merge manifest permissions (shells currently untouched).
3. `iosApp/project.yml`: local SPM path `../ios/Packages/OmniGartSensors`; Info.plist Bluetooth usage + background mode.
4. Feed `RawHeartRateNotification.payload` / Android `HeartRateMeasurement` into Shared HRV + health writers.

---

## 7. Open questions

See `docs/OPEN_QUESTIONS.md` (sensors section).
