# Hydration ↔ Health sync specification

Status: Phase 0 contract (Kotlin interfaces + fakes in `shared/hydration`). No app-shell wiring yet.

## Goals

1. Write OmniGart drink logs to Apple HealthKit and Android Health Connect without corrupting system totals.
2. Support append-only first writes via a stable client sync identifier (exist-check, insert if absent).
3. Avoid double-counting samples written by other apps when computing OmniGart progress.

## Decision: write **raw** volume to Health

HealthKit `HKQuantityTypeIdentifierDietaryWater` and Health Connect `HydrationRecord` model **ingested water volume**, not net fluid retention.

The Beverage Hydration Index (Maughan et al. 2016, DOI `10.3945/ajcn.115.114769`) is a relative retention index (water = 1.0). Multiplying litres by BHI before writing would:

- disagree with every other water-logging app;
- invent a quantity the platform schemas do not define;
- break clinicians’ and users’ expectations when reviewing Health totals.

**Rule:** platforms receive raw millilitres/litres. OmniGart keeps BHI-adjusted progress as an internal metric only (`HydrationCountingMode.HYDRATION_ADJUSTED`).

## Identity & append-only writes

| Field | Purpose |
| --- | --- |
| `clientRecordId` | Stable UUID/string for one OmniGart `DrinkLogEntry` (dedupe key) |
| `clientRecordVersion` | Always `1` on platform stamps — **never bump** (HC rewrite risk) |
| `schemaVersion` | `HYDRATION_SYNC_SCHEMA_VERSION` in Kotlin (currently `1`) |
| Source id | `com.omnigart.hydration` |

**Hard rule (same as workouts / weigh-ins):** never delete or rewrite platform health data. Writer API is `appendWater` / `appendCaffeine` / `appendCaloricDrink` only — no upsert or delete contracts. Exist-check by `clientRecordId` (Health Connect: read from EPOCH); if present, skip; else `insertRecords` / HealthKit save once.

### HealthKit

- Water → `HKQuantitySample` dietaryWater (litres).
- Caffeine → `HKQuantitySample` dietaryCaffeine (mg).
- Caloric drinks → `HKCorrelation` of type food, containing energy / macro quantity samples, plus metadata:
  - `omnigart.clientRecordId`
  - `omnigart.clientRecordVersion` (= 1)
  - `omnigart.schemaVersion`
  - `omnigart.beverageId`
- Append: fetch by metadata id; if present, skip; else insert. Never call `deleteObjects`.

### Health Connect

- Water → `HydrationRecord` with `metadata.clientRecordId` + `clientRecordVersion` (= 1).
- Caffeine / macros → `NutritionRecord` (caffeine, energy, protein, carbs, fat, sugars, sodium as available) with the same client id/version.
- Prefer a single `NutritionRecord` per caloric drink; plain water may be hydration-only (no empty nutrition row).
- Exist-check with `TimeRangeFilter.after(Instant.EPOCH)` by `clientRecordId`; never `updateRecords` / `deleteRecords`.

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

`FakeHydrationHealthStore` implements writer + reader with append-only skip-if-present behaviour and injectable foreign totals for double-count tests.

## Citations

- Maughan et al. (2016) Am J Clin Nutr — DOI `10.3945/ajcn.115.114769`
- EFSA caffeine (2015) — DOI `10.2903/j.efsa.2015.4102`
