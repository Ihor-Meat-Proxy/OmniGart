# Body measurements ↔ Health sync specification

Status: Phase 0 contract (`shared/body` interfaces + fakes). No app-shell wiring in this PR.

## Goals

1. Sync weight, height, waist, and estimated body-fat % to Apple HealthKit and Android Health Connect.
2. Keep **progress photos local-only** (metadata + on-device URI; never Health, never remote by default).
3. Idempotent upsert/delete via `clientRecordId` + monotonic `clientRecordVersion`.

## Mapping

| OmniGart | HealthKit | Health Connect |
| --- | --- | --- |
| Body mass (kg) | `bodyMass` | `WeightRecord` |
| Height (m) | `height` | `HeightRecord` |
| Body fat % | `bodyFatPercentage` | `BodyFatRecord` |
| Waist (m) | `waistCircumference` | `WaistCircumferenceRecord` (or equivalent circumference API) |
| Other girths / skinfolds | *OmniGart-only* (no stable cross-platform type) | *OmniGart-only* |
| Progress photos | **not written** | **not written** |

### Metadata

- `omnigart.clientRecordId`
- `omnigart.clientRecordVersion`
- `omnigart.schemaVersion` (= `BODY_SYNC_SCHEMA_VERSION`)
- `omnigart.bodyFatMethod` when writing %fat (e.g. `cun_bae`, `us_navy`)

### Units

Write Health-native units: kilograms, metres, percent (0–100). Convert from centimetres at the platform boundary.

## Read filtering

Default: OmniGart source only, to avoid double-counting smart-scale or other-app weight entries when the user also logs in OmniGart.

## Photos policy

`ProgressPhotoMeta.localOnly` is required `true`. Host apps must store binaries in app-private storage and must not attach photo payloads to Health writers.

## Citations

Body-fat method evidence is registered in `BodyEvidenceRegistry` (CUN-BAE DOI `10.2337/dc11-1334`, Jackson–Pollock DOIs, Navy NHRC reports, waist-to-height reviews).
