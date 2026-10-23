# Treadmill run — Bluetooth, indoor sessions, GymKit

Status: product contract for the dedicated **Treadmill run** workout on iPhone, Apple Watch, Android phone, and Wear OS. Protocol parsers live in `shared/treadmill`. Platform BLE centrals mirror the sensors layout (`ios/Packages/OmniGartTreadmill`, `android/treadmill`).

Wellness framing only. No medical claims. No competitor product names in user-facing copy.

---

## 1. Standard Bluetooth treadmill support

### 1.1 Fitness Machine Service (FTMS) — preferred

| Item | UUID |
|------|------|
| Fitness Machine Service | `0x1826` |
| Treadmill Data | `0x2ACD` |
| Fitness Machine Feature | `0x2ACC` |
| Fitness Machine Control Point | `0x2AD9` |
| Fitness Machine Status | `0x2ADA` |
| Training Status | `0x2AD3` |

**Spec:** Bluetooth SIG *Fitness Machine Service* (FTMS). OmniGart implements the treadmill subset only.

**Treadmill Data (`0x2ACD`)** — little-endian; Flags `uint16` then conditional fields:

| Flag bit | Field | Format | Unit |
|----------|-------|--------|------|
| 0 = 0 (More Data false) | Instantaneous Speed | `uint16` | 0.01 km/h |
| 1 | Average Speed | `uint16` | 0.01 km/h |
| 2 | Total Distance | `uint24` | m |
| 3 | Inclination + Ramp Angle | `sint16` + `sint16` | 0.1 %, 0.1 ° |
| 4 | ± Elevation Gain | `uint16` × 2 | 0.1 m |
| 5 | Instantaneous Pace | `uint8` | 0.1 km/min |
| 6 | Average Pace | `uint8` | 0.1 km/min |
| 7 | Total / Hour / Minute Energy | `uint16` + `uint16` + `uint8` | kcal |
| 8 | Heart Rate | `uint8` | bpm |
| 9 | Metabolic Equivalent | `uint8` | 0.1 MET |
| 10 | Elapsed Time | `uint16` | s |
| 11 | Remaining Time | `uint16` | s |
| 12 | Force on Belt + Power | `sint16` + `sint16` | N, W |
| 13 (vendor / GSS ext.) | Steps | `uint24` | steps |

Parser: `FtmsTreadmillDataParser`. Notify at ~1 Hz while connected.

**Control Point (`0x2AD9`)** — opt-in remote control only (user toggle; default off):

| Opcode | Meaning | Parameter |
|--------|---------|-----------|
| `0x00` | Request Control | — |
| `0x01` | Reset | — |
| `0x02` | Set Target Speed | `uint16` LE, 0.01 km/h |
| `0x03` | Set Target Inclination | `sint16` LE, 0.1 % |
| `0x07` | Start or Resume | — |
| `0x08` | Stop or Pause | `0x01` stop / `0x02` pause |
| `0x80` | Response | request opcode + result (`0x01` success, …) |

Encoder/decoder: `FtmsControlPoint`. Always send **Request Control** before speed/incline writes. Some machines ACK via Status (`0x2ADA`) instead of Control Point indication — treat timeout after a successful GATT write as soft success when Status/Data updates.

### 1.2 Running Speed and Cadence (RSC) — older machines

| Item | UUID |
|------|------|
| RSC Service | `0x1814` |
| RSC Measurement | `0x2A53` |
| RSC Feature | `0x2A54` |

**RSC Measurement:** Flags `uint8`; Instantaneous Speed `uint16` (1/256 m/s); Cadence `uint8` (spm); optional Stride Length `uint16` (cm); optional Total Distance `uint32` (dm). Bit 2 = walking (0) / running (1).

Parser: `RscMeasurementParser`. Used when FTMS is absent. No incline or remote speed control via RSC alone.

### 1.3 Discovery order

1. Scan for FTMS (`0x1826`) → subscribe Treadmill Data; optional Control Point if user opted in.
2. Else scan for RSC (`0x1814`) → subscribe RSC Measurement.
3. Else attempt **iConsole** proprietary UART (below).
4. Else run **without** treadmill link: watch/Wear estimated distance + optional post-run calibration.

Remember last successful device id (platform prefs / SQLDelight later). Auto-reconnect with exponential backoff + full jitter (same policy as HR sensors, cap 60 s).

---

## 2. iConsole consoles (Reebok and related)

Many consumer treadmills (including Reebok models with an **iConsole** console) expose a **proprietary** serial protocol rather than FTMS. Transport varies by hardware generation:

| Transport | How it appears | Notes |
|-----------|----------------|-------|
| **Bluetooth Classic SPP / RFCOMM** | Device name often `i-CONSOLE` / `iConsole`; Serial Port Profile | Classic SPP is **not** available as a central API on watchOS or modern iOS for arbitrary accessories the way BLE is. Phone may bridge; watch prefers BLE path. |
| **BLE transparent UART** | Microchip RN4870/BM70-style GATT | TX `49535343-1E4D-4BD9-BA61-23C647249616`, RX `49535343-8841-43F4-A8D4-ECBE34729BB3` (ISSC / Microchip transparent UART). Framing is the same proprietary serial protocol over Notify/Write. |

### 2.1 Framing (community reverse engineering)

Documented by open-source projects used **as protocol references only** (clean-room Kotlin under AGPL-3.0; see credits):

- Packet starts with `0xF0`.
- Client commands use second byte `0xA0`–`0xAF`; device responses `0xB0`–`0xBF` (same low nibble for most pairs).
- Last byte = checksum: sum of all prior bytes mod 256.
- Request/response; device does not spontaneously stream without polling.
- **PING** `F0 A0 01 01 92` → **PONG** `F0 B0 …` keeps Bluetooth mode alive; without periodic traffic the console exits BT mode in a few seconds.
- **READ** `F0 A2 01 01 94` → 21-byte telemetry (`F0 B2 …`).

**Telemetry decode (offset-biased encoding):** each multi-byte quantity uses bytes that are stored as `value + 1` (and 100× high digit + low digit for two-byte fields). Fields in the 21-byte READ response:

| Bytes | Meaning |
|-------|---------|
| 2–5 | Time components (day/hour/min/sec style; minutes wrap) |
| 6–7 | Speed (km/h × 10) |
| 8–9 | RPM / cadence |
| 10–11 | Distance (0.1 km units → km) |
| 12–13 | Energy (kcal) |
| 14–15 | Heart rate (bpm) |
| 16–17 | Power (W × 10) |
| 18 | Level / resistance (bike) or machine level |
| 19 | Status (observed: `1` stopped, `2` started) |
| 20 | Checksum |

**Control (bike-oriented in public docs; treadmill variants may differ):**

| Command | Bytes (example) | Role |
|---------|-----------------|------|
| START / STOP | `F0 A5 01 01 02/04 …` | Session |
| SET_LEVEL | `F0 A6 01 01 <level+1> <csum>` | Resistance/level |

Public open sources focus on **bikes/ellipticals**. Treadmill incline/speed set commands are **not fully documented** in those trees. OmniGart therefore:

1. Implements framing, handshake, READ telemetry, and level set from the documented protocol.
2. Maps speed/distance/HR/calories into the live treadmill UI when the READ layout matches.
3. Relies on **Treadmill diagnostics** (raw GATT dump + byte log export) when a Reebok/iConsole unit uses a variant — so a capture can extend the parser without guessing.

If the same console also exposes FTMS, FTMS wins.

### 2.2 Credits & licence posture

| Reference | Licence / role |
|-----------|----------------|
| [haraldh/iconsole](https://github.com/haraldh/iconsole) | Community reverse engineering of iConsole serial framing (bike). Used as **protocol documentation only**. |
| [iconsole-plus](https://github.com/christopher-david-smith/iconsole-plus) (GPL-2.0) | BLE client notes (handshake/heartbeat). Used as **protocol documentation only**. |
| Bluetooth SIG FTMS / RSC | Official GATT specs. |
| Microchip RN4870 transparent UART UUIDs | Vendor module docs. |

OmniGart’s Kotlin/Swift implementations are original AGPL-3.0 code. Do not vendor GPL sources into the tree; keep attributions in this doc and `docs/third_party/` if NOTICE files are added later.

---

## 3. Bluetooth from the watch

### 3.1 watchOS + CoreBluetooth

- During an active `HKWorkoutSession`, CoreBluetooth **central** remains usable for in-app BLE (FTMS / RSC / iConsole UART).
- Info.plist: `NSBluetoothAlwaysUsageDescription`; background `bluetooth-central` on the **phone** companion when the phone owns the link.
- **HealthKit distance/HR** for the saved workout still come from the workout builder (watch optical HR preferred). Machine HR is shown live and stored in OmniGart series metadata; it is **not** substituted as the sole HK HR source.
- Classic Bluetooth SPP is not a supported watchOS central path for iConsole Classic-only consoles — prefer phone BLE UART or FTMS.

Constant: document limitation next to sensors watch notes in `docs/SENSORS.md` (cross-link).

### 3.2 Wear OS + BLE

- Wear can act as BLE GATT central during `ExerciseClient` sessions (OEM battery / companion routing apply).
- Prefer on-watch optical HR for Health Services; overlay treadmill speed/incline/distance from FTMS/RSC/iConsole.
- Permissions: `BLUETOOTH_SCAN` (`neverForLocation`) + `BLUETOOTH_CONNECT` (API 31+).

---

## 4. Indoor run configuration

### 4.1 Apple (HealthKit / WorkoutKit)

```text
HKWorkoutConfiguration
  activityType = .running
  locationType = .indoor   // HKWorkoutSessionLocationType.indoor
```

Live session: `HKWorkoutSession` + `HKLiveWorkoutBuilder` (watch) or `HKWorkoutBuilder` (phone). Distance samples: prefer treadmill total distance; if disconnected, watch estimated `distanceWalkingRunning` and optional calibration.

### 4.2 Wear OS (Health Services)

- `ExerciseType.RUNNING` with indoor / treadmill capability when the OEM supports it; otherwise `RUNNING` and treat distance as treadmill-sourced when BLE is linked.
- Data types: heart rate, calories, distance; replace or prefer treadmill distance when connected.

### 4.3 Saving

| Platform | Write |
|----------|--------|
| Apple Health | Indoor run workout; `distanceWalkingRunning` from treadmill; sync id `omnigart.workout.<localId>`; sync version 1; **append-only** (exist-check → insert or skip). |
| Health Connect | `EXERCISE_TYPE_RUNNING_TREADMILL` session + `DistanceRecord` + `SpeedRecord`; same sync id / version rules. |
| Local | Always store full session + treadmill samples (speed, incline, machine HR, steps). |
| Export | GPX/TCX include treadmill extensions; CSV columns for incline/speed; FIT remains stub (`FIT_UNAVAILABLE`) per AGPL policy. |

GymKit-imported workouts (see below) use the same import/dedupe path as other Apple Health runs (`running` / `runningTreadmill` → `RUNNING`).

---

## 5. GymKit — can third-party apps use it?

**No.** GymKit NFC pairing (“Detect Gym Equipment”) is an **Apple system Workout / Fitness** capability for **Apple-certified** machines. There is **no public GymKit API** for third-party apps to initiate pairing or stream live GymKit telemetry the way the system Workout app does.

**What OmniGart does instead:**

1. Support **direct BLE** (FTMS / RSC / iConsole) in OmniGart’s own Treadmill run.
2. **Import** completed GymKit (and other) indoor/treadmill workouts from HealthKit after the fact, with append-only local storage and sync-id dedupe so OmniGart never double-writes them back.
3. Be honest in UI: GymKit pairing happens in Apple’s Workout app; OmniGart can show imported results, not live GymKit NFC.

---

## 6. Without a treadmill connection

1. Start Treadmill run as indoor run anyway.
2. Distance from watch/Wear estimation.
3. After save, optional **calibration**: user enters treadmill console distance → scale factor stored for future estimated sessions (`DistanceCalibration`).

---

## 7. Treadmill diagnostics (hidden)

**Treadmill diagnostics** lists discovered services/characteristics and logs raw notification bytes; export as a text file for unknown console variants (e.g. a specific Reebok iConsole). Entry: long-press the Treadmill run title (or DEBUG menu). Not a medical tool — protocol capture only.

---

## 8. Module map

| Path | Role |
|------|------|
| `shared/treadmill` | FTMS / RSC / iConsole parsers, control-point codecs, live metrics merge, calibration, diagnostics models, JVM tests with byte fixtures |
| `ios/Packages/OmniGartTreadmill` | CoreBluetooth central |
| `android/treadmill` | Android BLE central (when SDK present) |
| `watchApp` / `iosApp` / `androidApp` / `wearApp` | Scan/pair UI, live screen, save hooks, diagnostics |

Feature surface ships with the Treadmill run workout; remote speed/incline remains **user opt-in**.
