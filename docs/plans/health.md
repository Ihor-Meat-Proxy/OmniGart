# Health integration layer

## Goal

On-device HealthKit and Health Connect plus cloud Google Health API (Fitbit Web API OAuth), behind `FeatureFlags.healthSyncEnabled` (default **false**). Shared domain models and formula strategies for later Calculations settings.

## Architecture

### `HealthClient` (shared)

Unified local health-store contract in `shared/.../health/HealthClient.kt`:

- Authorization status / request
- Read/write: heart rate, resting HR, HRV, sleep, body measurements, nutrition, water, workouts
- Incremental `changes` with `HealthChangeToken`

Domain types live in `HealthModels.kt`. Tests use `FakeHealthClient`.

### Platform adapters (gated by `FeatureFlags.healthSyncEnabled`)

| Backend | Implementation | Notes |
|---------|----------------|-------|
| **HealthKit (HK)** | `HealthKitClientImpl` + Swift `HealthKitPlatformBridge` / sample injector | Kotlin owns domain mapping; Swift owns `HKHealthStore`. Entitlements in `OmniGart.entitlements`. |
| **Health Connect (HC)** | `HealthConnectClientImpl` via `HealthConnectGateway` / `SdkHealthConnectGateway` | Host tests use a fake gateway; `connect-testing` is declared but alpha APIs are incomplete on Linux CI. |
| **Google Health API (GHA)** | `GoogleHealthApiClient` + PKCE OAuth (`GoogleHealthAuth`) | Cloud REST (Fitbit Web API hosts after acquisition). Tokens: EncryptedSharedPreferences (Android) / Keychain (Apple). AppAuth browser on `androidApp`. |

UI, workers, and permission prompts must check `FeatureFlags.healthSyncEnabled` before running. Manifest / Info.plist / entitlements may declare capabilities; runtime stays off until the flag flips.

### Baevsky stress index

`BaevskyStressIndex` computes SI from RR intervals (ms):

- 50 ms histogram bins
- Mo and MxDMn in **seconds**; AMo as **percent** 0–100
- SI = AMo / (2 · Mo · MxDMn)

### Formula strategies

`calc/strategy/FormulaStrategies.kt` — selectable formulae with Phase 0 defaults plus first alternatives:

- BMR: Mifflin–St Jeor (default), Harris–Benedict
- HRmax: Tanaka (default), Fox (220 − age)
- Body fat: CUN-BAE
- 1RM: Epley (default), Brzycki
- HR zones: Karvonen 5/3, %HRmax 5

`FormulaCatalog` / `FormulaPreferences` wire defaults for a future Calculations settings UI (Phase 4).

## In scope

- Contracts, adapters, OAuth client, Baevsky, formula strategies
- Manifest / entitlement / usage-string declarations (compliance copy kept)
- Unit / host tests for fakes, OAuth helpers, Baevsky, strategies

## Out of scope

- Enabling sync UI for users (flag stays false until product-ready)
- Calculations settings screens (Phase 4)
- Google Fit

## Dependencies

- Phase 0 foundation
- App Review compliance (permission copy, PrivacyInfo, rationale activities)

## Decisions

Logged under health sections in `docs/OPEN_QUESTIONS.md` (GHA identity, HK Kotlin/Swift split, connect-testing, AppAuth placement, Baevsky units, formula defaults).
