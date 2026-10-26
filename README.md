# OmniGart

Open-source (**AGPL-3.0**) wellness app for strength, endurance, hydration, and recovery estimates.

**Offline-first** and **bundle-first**: exercises, locales, evidence, icons, and related packs ship in `generated/content-bundle/` so the apps work without a network. User data stays on device; optional encrypted backup to **iCloud**, **Google Drive**, or **Nextcloud** is on the roadmap (not required to use the app). Copy is wellness-oriented, not medical advice.

## Feature status

| Area | Status | Notes |
|------|--------|--------|
| Strength logging (volume load, bodyweight) | **Done** | Shared calc + iOS logging UI |
| Exercise library + templates | **Done** | Bundled content + planning validators |
| Body maps | **Done** | `shared/bodymap` + content |
| Sensors / BLE heart rate | **Done** | Shared sensors + Polar adapters; see [docs/SENSORS.md](docs/SENSORS.md) |
| Hydration / beverages | **Done** | Catalogue, norms, Health write paths |
| Body measurements | **Done** | Strategies + Health sync contracts |
| Achievements + local challenges | **Done** | Engine + catalogue; see [docs/ACHIEVEMENTS.md](docs/ACHIEVEMENTS.md) |
| Evidence registry + insights | **Done** | Claims with verified DOI/PMID links |
| Yoga + Pilates libraries | **Done** | Content packs + locales |
| Running + swimming content | **Done** | Workout types, plans, locales |
| Translations (all required locales) | **Done** | en-GB source; uk, pl, be, kk, ka, es, fr, de, cs, zh-Hans — **never Russian** (some strings still English placeholders pending native review) |
| Health integrations (HealthKit, Health Connect, Google Health API) | **Done** | Save workouts to Apple Health ([#69](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/69)); local-first store with append-only sync/restore + watch/Wear transfer ([#71](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/71)); Health Connect append-only writes ([#93](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/93)); HealthKit pause-event duration fix ([#96](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/96)). Product sync UI / workers still behind `FeatureFlags.healthSyncEnabled` (**false** by default); see [docs/HEALTH_DATA_MAPPING.md](docs/HEALTH_DATA_MAPPING.md) |
| Nutrition offline packs | **Done** | `shared/nutrition-data` + pack tooling |
| WorkoutKit (system Workout scheduling) | **Done** | Bridge + flags; see [docs/WORKOUTKIT.md](docs/WORKOUTKIT.md) |
| Zone 2 watch live workout + program | **Done** | Zone math + evidence ([#57](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/57), [#58](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/58)); glanceable watchOS session, haptics, summary, WorkoutKit path ([#60](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/60)–[#65](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/65)); `watchApp/OmniGartWatch/LiveWorkout/` — [docs/ZONE2.md](docs/ZONE2.md) |
| Device install guide | **Done** | [docs/INSTALL_DEVICE.md](docs/INSTALL_DEVICE.md) ([#59](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/59)) |
| Onboarding + local profile + real tabs | **Done** | First-launch onboarding + persisted profile ([#66](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/66)); real tab screens ([#67](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/67)); `activeProfile()` ([#95](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/95)) |
| Planner (calendar, weekly scheme, adaptive suggestions) | **Done** | On-device planner calendar + weekly scheme + adaptive suggestions ([#86](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/86)); see [docs/PLANNING_ENGINE.md](docs/PLANNING_ENGINE.md) |
| Food logging (catalogue, diary, encrypted store, morning review) | **Done** | USDA/OFF catalogue, diary UX, encrypted store, rule-based morning review ([#78](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/78), [#87](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/87)); see [docs/FOOD_LOGGING.md](docs/FOOD_LOGGING.md). `FeatureFlags.morningFoodReviewEnabled` remains off |
| Adaptive calorie & macro targets | **Done** | Nutrition target model ([#77](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/77)); see [docs/NUTRITION_MODEL.md](docs/NUTRITION_MODEL.md) |
| Insights & Health tabs | **Done** | Phone Insights + Health tabs over local store + read-only HK/HC ([#76](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/76)); see [docs/INSIGHTS_METRICS.md](docs/INSIGHTS_METRICS.md) |
| Watch / Wear set logging + resistance calories | **Done** | In-workout set logging on watch/Wear with resistance energy estimates ([#75](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/75)) |
| Treadmill (FTMS / RSC / iConsole) + simulator | **Done** | Live treadmill run + parsers ([#73](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/73)); simulator + diagnostics ([#80](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/80)); see [docs/TREADMILL.md](docs/TREADMILL.md) |
| Equipment programmes, ready workouts, Explore library | **Done** | Equipment programmes + ready workouts ([#70](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/70)); Explore routine library + 150+ equipment catalogue ([#79](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/79)) |
| Illustrations (equipment + exercises) | **Done** | Cartoon illustrations for equipment and exercises ([#72](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/72)); `design/illustrations/` |
| Workout category tile grid | **Done** | Category tiles on iPhone and Android ([#85](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/85)) |
| Phone starts watch / Wear workout | **Done** | Phone → watch HealthKit launch + Wear launch listener ([#88](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/88)) |
| Calculation settings (Simple / Advanced) | **Done** | Formula domains + recalc ([#35](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/35)) |
| Activity algorithms | **Done** | Pace, HR zones, swim detection, SWOLF, VO₂max estimates ([#20](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/20)); `shared/activity-algorithms` |
| Wear OS deep workout parity | **Partial** | Set logging ([#75](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/75)); recorder lifecycle + typed BLE HR failures ([#91](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/91)); phone launch listener ([#88](https://github.com/Ihor-Meat-Proxy/OmniGart/pull/88)). No system WorkoutKit equivalent; Zone 2 live path still iOS/watchOS-first |
| Encrypted cloud backup | **Planned** | iCloud / Google Drive / Nextcloud — `FeatureFlags.encryptedBackupEnabled` off |
| Food barcode camera flow | **Planned** | `FeatureFlags.foodBarcodeEnabled` off |
| Morning readiness check-in UI | **Planned** | Scoring helpers exist; `FeatureFlags.readinessCheckInEnabled` off |

## Roadmap

Immediate: watch-to-phone sync race hardening, dedupe iOS/watchOS treadmill parsers into shared KMP, Wear OS workout parity, and replace remaining English placeholder translations with reviewed locale copy. Longer phase list: [docs/plans/README.md](docs/plans/README.md).

## Architecture

| Layer | Role |
|-------|------|
| `shared/` (+ modules) | Kotlin Multiplatform domain, calcs, SQLDelight, content validators |
| `androidApp/` / `wearApp/` | Jetpack Compose phone and Wear OS shells |
| `iosApp/` / `watchApp/` | SwiftUI phone and watchOS (XcodeGen) |
| `content/` → `generated/content-bundle/` | Bundle-first packs (see [docs/CONTENT_BUNDLE.md](docs/CONTENT_BUNDLE.md)) |
| `design/` | Tokens and generated platform assets |

Details: [ARCHITECTURE.md](ARCHITECTURE.md). Open decisions: [docs/OPEN_QUESTIONS.md](docs/OPEN_QUESTIONS.md).

## Languages

**Required:** en-GB (source), uk, pl, be, kk, ka, es, fr, de, cs, zh-Hans.  
**Forbidden:** any Russian locale (`ru` / `ru-*`) — enforced by `scripts/check_no_ru_locale.sh`.  
See [docs/TRANSLATION.md](docs/TRANSLATION.md).

## Requirements

| Platform | Tooling |
|----------|---------|
| Shared / Android / Wear | **JDK 21** (Eclipse Temurin 21 recommended), Android Studio, AGP 9.x, compileSdk **37**. JDK 24+ is not supported yet (detekt 1.23.8 crashes on it). |
| iOS / watchOS | macOS, Xcode, [XcodeGen](https://github.com/yonaskolb/XcodeGen), SwiftLint — deployment **iOS 18** / **watchOS 11** (CI); product may target newer OS when runners allow |

On macOS, point Gradle at JDK 21 if needed:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

## Build

```bash
git clone https://github.com/Ihor-Meat-Proxy/OmniGart.git
cd OmniGart
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # adjust path

./scripts/check_no_ru_locale.sh
./gradlew :shared:testAndroidHostTest \
  :androidApp:assembleDebug :androidApp:testDebugUnitTest \
  :wearApp:assembleDebug :wearApp:testDebugUnitTest
./gradlew ktlintCheck detekt
```

### iOS / watchOS (macOS)

```bash
brew install xcodegen swiftlint   # once
./gradlew linkDebugFrameworkIosSimulatorArm64 linkDebugFrameworkWatchosSimulatorArm64 \
  syncXcodeFrameworks
cd iosApp && xcodegen generate && open OmniGart.xcodeproj
```

`Shared.framework` is synced to `shared/build/xcode-frameworks/<Debug|Release>/<PLATFORM_NAME>/` (e.g. `watchsimulator`, not `watchsimulator27.2`). Xcode **Run** also runs `scripts/xcode_sync_shared_framework.sh` as a pre-build step so newer simulator SDKs resolve without updating Gradle SDK lists.

Install on a personal iPhone / Apple Watch: **[docs/INSTALL_DEVICE.md](docs/INSTALL_DEVICE.md)** (free Personal Team vs paid / TestFlight).

> Linux CI builds Shared + Android/Wear; the macOS job builds and tests iOS/watchOS simulators when those paths change.

## CI

Path-filtered GitHub Actions (see `.github/workflows/`):

- **Lint gate** — ktlint, detekt, locale / App Review checks  
- **KMP + Android + Wear** — assemble and unit tests  
- **iOS + watchOS** — XcodeGen + `xcodebuild` (macOS)  
- Module workflows — Body, Hydration, Sensors, Evidence, Translations, content packs, etc.

## Contributing

- Feature branches only; do not commit to `main`.
- No competitor product names (platform APIs such as HealthKit, Health Connect, and Polar BLE are fine). Ban list: `.github/ci/banned-terms.yml`.
- Wellness wording only; verified DOI/PMID for evidence claims — never invent citations.
- Keep locales complete for required languages; never add Russian catalogues.
- Run local checks (`check_no_ru_locale`, relevant Gradle tests, ktlint/detekt) before pushing.
- Log unresolved product questions in [docs/OPEN_QUESTIONS.md](docs/OPEN_QUESTIONS.md).
- Release / store checklist: [docs/RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md).

## Licence

GNU Affero General Public License v3.0 — see [LICENSE](LICENSE).
