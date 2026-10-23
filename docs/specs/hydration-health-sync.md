# Hydration ↔ Health sync specification

Status: Phase 0 contract (Kotlin interfaces + fakes in `shared/hydration`). No app-shell wiring in this PR.

## Goals

1. Write OmniGart drink logs to Apple HealthKit and Android Health Connect without corrupting system totals.
2. Support idempotent create / edit / delete via a client sync identifier and version.
3. Avoid double-counting samples written by other apps when computing OmniGart progress.

## Decision: write **raw** volume to Health

HealthKit `HKQuantityTypeIdentifierDietaryWater` and Health Connect `HydrationRecord` model **ingested water volume**, not net fluid retention.

The Beverage Hydration Index (Maughan et al. 2016, DOI `10.3945/ajcn.115.114769`) is a relative retention index (water = 1.0). Multiplying litres by BHI before writing would:

- disagree with every other water-logging app;
- invent a quantity the platform schemas do not define;
- break clinicians’ and users’ expectations when reviewing Health totals.

**Rule:** platforms receive raw millilitres/litres. OmniGart keeps BHI-adjusted progress as an internal metric only (`HydrationCountingMode.HYDRATION_ADJUSTED`).

## Identity & versioning

| Field | Purpose |
| --- | --- |
| `clientRecordId` | Stable UUID/string for one OmniGart `DrinkLogEntry` |
| `clientRecordVersion` | Monotonic long; higher wins on upsert; delete requires `≥` stored version |
| `schemaVersion` | `HYDRATION_SYNC_SCHEMA_VERSION` in Kotlin (currently `1`) |
| Source id | `com.omnigart.hydration` |

### HealthKit

- Water → `HKQuantitySample` dietaryWater (litres).
- Caffeine → `HKQuantitySample` dietaryCaffeine (mg).
- Caloric drinks → `HKCorrelation` of type food, containing energy / macro quantity samples, plus metadata:
  - `omnigart.clientRecordId`
  - `omnigart.clientRecordVersion`
  - `omnigart.schemaVersion`
  - `omnigart.beverageId`
- Upsert: fetch by metadata id; replace if incoming version is newer.
- Delete: delete correlation/samples matching `clientRecordId` when version allows.

### Health Connect

- Water → `HydrationRecord` with `metadata.clientRecordId` + `clientRecordVersion`.
- Caffeine / macros → `NutritionRecord` (caffeine, energy, protein, carbs, fat, sugars, sodium as available) with the same client id/version.
- Prefer a single `NutritionRecord` per caloric drink; plain water may be hydration-only (no empty nutrition row).

## Read path & double-counting

Default read filter: **OmniGart source only** (`HealthReadSourceFilter.OMNIGART_ONLY`).

Rationale: if the user also logs water in another app that writes to Health, summing all sources would double-count on days they use both. OmniGart’s daily progress should prefer its own ledger; an optional “include other apps” mode can use `ALL_SOURCES` with an explicit UI warning.

When importing historical Health water to seed OmniGart, mark imported rows with a distinct source tag and do not re-export them.

## Mapping summary

| OmniGart | HealthKit | Health Connect |
| --- | --- | --- |
| Water / sparkling volume | `dietaryWater` | `HydrationRecord` |
| Caffeine mg | `dietaryCaffeine` | `NutritionRecord.caffeine` |
| Caloric drink macros | `HKCorrelation` food + quantities | `NutritionRecord` nutrients |
| BHI-adjusted total | *not written* | *not written* |

## Permissions (host apps)

- iOS: HealthKit share for dietary water, caffeine, and relevant nutrition types; read optional for import.
- Android: Health Connect write for Hydration + Nutrition; read optional.

## Test doubles

`FakeHydrationHealthStore` implements writer + reader with version checks, delete flags, and injectable foreign totals for double-count tests.

## Citations

- Maughan et al. (2016) Am J Clin Nutr — DOI `10.3945/ajcn.115.114769`
- EFSA caffeine (2015) — DOI `10.2903/j.efsa.2015.4102`
