# Activity algorithms (shared)

Module: `shared/activity-algorithms`.

| Area | API | Notes / evidence |
|------|-----|------------------|
| Pace / splits / cadence | `PaceCalculator`, `CadenceCalculator` | Fixed-distance splits; SPM from timestamps or counts |
| HR zones / time-in-zone | `HeartRateZones` | %HRmax (default 5 zones) or Karvonen %HRR |
| Swim stroke / lap | `SwimAccelDetection` | Spec + synthetic fixtures; field thresholds TBD (A#) |
| SWOLF | `Swolf` | strokes + seconds per length |
| Running power (optional) | `RunningPower` | P ≈ m·v·(Cr + g·grade); Cr≈3.8 J·kg⁻¹·m⁻¹ |
| VO₂max | `Vo2MaxEstimators` | Uth–Sørensen 15.3×HRmax/HRrest; Cooper 22.351×km−11.288; ACSM running VO₂ |

Citations are embedded as string constants on each API (`CITATION` / estimator fields).
