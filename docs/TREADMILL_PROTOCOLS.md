# Treadmill BLE protocols

OmniGart talks to Bluetooth treadmills through a shared KMP parser layer
(`shared/sensors/.../treadmill`) plus platform centrals (CoreBluetooth /
Android BLE). Detection prefers **FTMS**, then FitShow-style `FFF0`, then
iConsole Microchip UART.

| Protocol | Status in this tree | Notes |
|----------|---------------------|--------|
| **FTMS** (Fitness Machine Service `0x1826`) | **Verified** against the Bluetooth SIG FTMS 1.0 field layout with recorded byte fixtures and the in-process simulator | Speed, incline, distance, elapsed time, energy, HR; Control Point start/stop/speed/incline; Feature + speed/incline ranges; Machine Status |
| **FitShow-style** (`FFF0` / `FFF1` / `FFF2`) | **Partially verified** (frame codec + classic status payload fixtures). Live OEM variance is expected | Common on many budget treadmills. High-speed (word) speed mode is implemented but needs a live capture to confirm per device |
| **iConsole UART** (Microchip Transparent UART) | **Speculative for treadmills** | Public reverse-engineering is strongest for bikes. Reebok iConsole units are the first live test target; use **Treadmill diagnostics** to capture GATT + hex if the console is not FTMS |

Never auto-write Control Point / TX commands. The UI requires an explicit
**Arm control** action before start/stop/speed/incline.

---

## FTMS (verified layout)

| UUID | Role |
|------|------|
| `0x1826` | Fitness Machine Service |
| `0x2ACC` | Fitness Machine Feature (read) |
| `0x2ACD` | Treadmill Data (notify) |
| `0x2AD4` | Supported Speed Range (read) |
| `0x2AD5` | Supported Inclination Range (read) |
| `0x2AD9` | Fitness Machine Control Point (write + indicate) |
| `0x2ADA` | Fitness Machine Status (notify) |

Treadmill Data flags are `uint16` LE. When bit 0 (More Data) is clear,
Instantaneous Speed (`uint16`, 0.01 km/h) is present. Subsequent optional
fields follow FTMS 1.0 §4.5 (distance `uint24` metres, inclination pair,
energy, HR, elapsed time, …).

Control Point: Request Control (`0x00`) → Start/Resume (`0x07`) /
Stop or Pause (`0x08`) / Set Target Speed (`0x02`) / Set Target Inclination (`0x03`).

Authoritative reference: Bluetooth SIG Fitness Machine Service 1.0 and the
GATT Specification Supplement (public SIG documents).

---

## FitShow-style (partially verified)

Framed payload over service `FFF0`:

```
0x02 | CMD | [SCMD] | DATA… | FCS | 0x03
```

FCS is the XOR of CMD through the last DATA byte. Status (`0x51`) carries
state, speed (0.1 km/h), incline, time, distance (0.001 km), energy (0.1 kcal),
optional steps/HR. Multi-byte fields are little-endian.

### Credits (AGPL-compatible)

- Vendor FitShow protocol PDFs published for third-party integrators
  (documentation; not proprietary app binaries).
- [blak3r/treadspan](https://github.com/blak3r/treadspan) — MIT — LifeSpan /
  FitShow-style reverse-engineering notes used to cross-check framing and
  status fields (OmniGart re-implements parsers independently).

OmniGart does **not** copy proprietary manufacturer apps or closed SDKs for
this path.

---

## iConsole (speculative on treadmills)

Microchip Transparent UART characteristics:

| Role | UUID |
|------|------|
| TX notify | `49535343-1E4D-4BD9-BA61-23C647249616` |
| RX write | `49535343-8841-43F4-A8D4-ECBE34729BB3` |

Packets: `0xF0 | OP | body… | checksum` with checksum = sum of prior bytes
`& 0xFF`. Many numeric fields use value+1 encoding.

### Credits (AGPL-compatible)

- [haraldh/iconsole](https://github.com/haraldh/iconsole) — MIT — UART packet
  layout and keep-alive / READ ops (bike-centric; adapted cautiously).

Some “iConsole” consumer treadmills may still expose FitShow `FFF0` or FTMS
instead of this UART. Always probe services first. Ihor’s Reebok iConsole
unit is the first hardware target: capture with **Settings → Developer →
Treadmill diagnostics** (DEBUG builds) and attach the JSON export when
extending the parser.

Classic Bluetooth SPP (RFCOMM) is noted for completeness; phone centrals in
this PR focus on BLE. SPP can be added later if diagnostics show only Classic.

---

## Adapter plug-ins

`TreadmillProtocolPlugin` + `TreadmillProtocolRegistry` (FTMS priority 0,
FitShow 10, iConsole 20). New protocols register without touching platform
GATT glue. `TreadmillBridge` exposes primitive-friendly entry points for
Swift / Android shells. `SimulatedTreadmill` provides FTMS notifications for
simulators without BLE hardware.

---

## Local store and health platforms

Finished treadmill runs write through the local-first store (same path as
other OmniGart workouts), export as TCX / GPX-less indoor / JSON (FIT remains
unavailable until an AGPL-compatible writer lands), and append-only Apple
Health / Health Connect indoor running records with treadmill distance when
present. OmniGart never deletes or rewrites existing platform records.
