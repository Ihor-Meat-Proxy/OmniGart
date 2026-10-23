# Zone 2 heart-rate bands

Wellness training guidance for easy aerobic running. Not a medical lactate test.

## Default model

**% heart-rate reserve (Karvonen)** using the Zone 2 slice of the shared five-zone table (**60–70% HRR**):

`target = HRrest + fraction × (HRmax − HRrest)`

Implementation: `com.omnigart.activity.hr.Zone2` (default `Zone2Method.HRR_KARVONEN`).

Evidence claim: `formula-zone2-hrr-karvonen` → Karvonen 1957 (`karvonen_hr_reserve_1957` / PMID 13470504).

## Options

| Method | Band | Evidence claim |
|--------|------|----------------|
| `%HRmax` | 60–70% of HRmax | `formula-zone2-percent-hrmax` (Tanaka HRmax + Karvonen zone framing) |
| LTHR-relative | 80–90% of lactate-threshold HR | `formula-zone2-lthr-band` (provisional; OPEN_QUESTIONS **W3**) |

Programme framing for easy aerobic volume: `cardio-zone2-easy-distribution` → Seiler 2010 (DOI `10.1123/ijspp.5.3.276`).

## Related code

- `shared/activity-algorithms/.../hr/HeartRateZones.kt` — general zones + time-in-zone
- `shared/activity-algorithms/.../hr/Zone2.kt` — Zone 2 profile helpers
- WorkoutKit scheduling of a Zone 2 session: follow-up PR (uses `EndurancePlanFactory` + HR alert)

## Locales

Claim copy: `content/evidence/locales/en-GB.json` (+ `uk` required by claims schema). Other locales filled by the translations stack.
