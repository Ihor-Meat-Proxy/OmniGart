# Treadmill run / walk — Bluetooth, programs, heart rate, GymKit

Status: product contract for dedicated **Treadmill run** and **Treadmill walk** workouts on iPhone, Apple Watch, Android phone, and Wear OS. Protocol parsers and program/HR logic live in `shared/treadmill`. Platform BLE centrals mirror the sensors layout.

Wellness framing only. No medical claims. No competitor product names in user-facing copy. Never Russian locale.

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

Parser: `FtmsTreadmillDataParser`. Control Point (`FtmsControlPoint`) is **opt-in** (user toggle; default off). Always **Request Control** before speed/incline writes.

### 1.2 FitShow-style (`FFF0`) — budget treadmills

| Item | UUID |
|------|------|
| Service | `FFF0` |
| Notify | `FFF1` |
| Write | `FFF2` |

Frame: `0x02 | CMD | [SCMD] | DATA… | FCS | 0x03` (FCS = XOR of CMD…DATA). Status `0x51` carries state, speed (0.1 km/h), incline, time, distance (0.001 km), energy, optional steps/HR. Parser: `FitShowCodec` / `FitShowStatusParser`. **Partially verified** against published protocol notes and fixtures; OEM variance expected (high-speed uint16 mode included).

### 1.3 Running Speed and Cadence (RSC) — older machines

| Item | UUID |
|------|------|
| RSC Service | `0x1814` |
| RSC Measurement | `0x2A53` |

Parser: `RscMeasurementParser`. No incline / remote speed via RSC alone.

### 1.4 Discovery order

1. **FTMS** (`0x1826`) → Treadmill Data; optional Control Point if opted in.
2. Else **FitShow** (`FFF0`) → notify + poll status; optional control writes if opted in.
3. Else **RSC** (`0x1814`) → RSC Measurement.
4. Else **iConsole** proprietary UART (below).
5. Else run **without** treadmill link: watch/phone motion estimate + optional post-run calibration.

Remember last successful device id. Auto-reconnect with exponential backoff + full jitter (cap 60 s).

---

## 2. iConsole consoles (Reebok and related)

Proprietary serial over Classic SPP (phone-limited) or Microchip BLE transparent UART:

| Role | UUID |
|------|------|
| TX notify | `49535343-1E4D-4BD9-BA61-23C647249616` |
| RX write | `49535343-8841-43F4-A8D4-ECBE34729BB3` |

Framing `0xF0 … checksum` (sum mod 256); offset-biased telemetry. Clean-room Kotlin under AGPL-3.0 from public RE notes (bike-centric; treadmill variants may differ — use diagnostics captures).

If the same console also exposes FTMS, FTMS wins.

Credits: [haraldh/iconsole](https://github.com/haraldh/iconsole), iconsole-plus (GPL-2.0, docs only), Bluetooth SIG, Microchip RN4870 docs. FitShow cross-checks: published integrator notes / [blak3r/treadspan](https://github.com/blak3r/treadspan) (MIT, docs only).

---

## 3. Run and walk (with or without Bluetooth)

Both **Treadmill run** and **Treadmill walk** work with or without a linked treadmill:

| Linked | Distance / pace |
|--------|-----------------|
| FTMS / FitShow / RSC / iConsole | Prefer machine distance, speed, incline |
| No link | Behave like Apple’s default indoor run/walk: watch or phone motion estimate; optional post-run calibration factor |

### 3.1 Apple Health

| Mode | `HKWorkoutActivityType` | Location |
|------|-------------------------|----------|
| Run | `.running` | `.indoor` |
| Walk | `.walking` | `.indoor` |

Append-only: `HKMetadataKeySyncIdentifier` = `omnigart.workout.<localId>`; skip if present. Distance sample when known.

### 3.2 Health Connect

| Mode | Exercise type |
|------|---------------|
| Run + treadmill source | `EXERCISE_TYPE_RUNNING_TREADMILL` when `sourceName` marks treadmill |
| Run | `EXERCISE_TYPE_RUNNING` |
| Walk | `EXERCISE_TYPE_WALKING` |

Same sync-id / version rules; never delete or rewrite platform records.

---

## 4. Run programs

Built-in library (`TreadmillProgramCatalog`): easy run, intervals, hill/incline, tempo, fartlek, progression, walk–run (beginner), recovery walk.

Each program is a list of steps with duration (or distance), optional target speed / incline / pace / HR zone (1–5).

- When FTMS or FitShow accepts control (user armed): auto-set speed and incline on step changes.
- Otherwise: show the same targets as on-screen prompts.
- iOS/watchOS: export to WorkoutKit as **indoor** custom workouts (`TreadmillWorkoutKitPlans` / `TreadmillWorkoutKitScheduler`).
- Available on iPhone, Apple Watch, Android, Wear OS; strings in all 11 required locales (never Russian).

---

## 5. Heart rate

Sources (priority for display):

1. BLE chest strap — standard Heart Rate Service `0x180D` / Measurement `0x2A37` (phone; watch optical preferred during live HK session).
2. Apple Watch / Wear OS optical sensors during the live indoor session.
3. Machine HR from FTMS / FitShow / iConsole when no strap/watch sample.

Zones: five bands. Default **%HRmax**; optional **Karvonen** when resting HR is known. Max HR from user settings or editable **220 − age** fallback (`TreadmillHrSettings`).

Alerts (`TreadmillHrAlertEngine`) — user can disable all or safety/program subsets and edit thresholds:

| Kind | Default behaviour | Wellness copy key |
|------|-------------------|-------------------|
| Above / below program target zone | When a step has a target zone | `treadmill_hr_alert_above_zone` / `_below_zone` |
| Safety high | Sustained ≥ ~95% of max | `treadmill_hr_alert_high` — “Heart rate is very high — consider slowing down or stopping” |
| Safety low | Sustained low HR during effort | `treadmill_hr_alert_low` |
| Sensor dropout | No samples for configurable seconds | `treadmill_hr_alert_dropout` |

Haptic (and platform audio where wired) on alert. **No medical claims.**

---

## 6. GymKit

**No.** There is no public GymKit API for third-party apps. OmniGart uses direct BLE + HealthKit import of completed GymKit sessions (append-only).

---

## 7. Diagnostics

Hidden **Treadmill diagnostics** (long-press title): GATT services/characteristics + raw bytes; export text file for unknown console variants. Protocol capture only — not a medical tool.

---

## 8. Module map

| Path | Role |
|------|------|
| `shared/treadmill` | FTMS / FitShow / RSC / iConsole parsers, programs, HR alerts, metrics merge, calibration |
| `android/treadmill` | Android BLE central |
| `iosApp` / `watchApp` / `androidApp` / `wearApp` | Scan/pair UI, programs, HR overlay, save hooks, WorkoutKit export |

Remote speed/incline remains **user opt-in**. Feature surface ships with Treadmill run/walk workouts.
