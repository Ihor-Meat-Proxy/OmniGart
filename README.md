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
| Translations (all required locales) | **Done** | en-GB source; uk, pl, be, kk, ka, es, fr, de, cs, zh-Hans — **never Russian** |
| Health integrations (HealthKit, Health Connect, Google Health API) | **Done** | Shared contracts; product sync UI still feature-flagged |
| Nutrition offline packs | **Done** | `shared/nutrition-data` + pack tooling |
| WorkoutKit (system Workout scheduling) | **Done** | Bridge + flags; see [docs/WORKOUTKIT.md](docs/WORKOUTKIT.md) |
| Zone 2 watch live workout + program | **Planned** | Next focus — [docs/plans/watch-workoutkit-zone2.md](docs/plans/watch-workoutkit-zone2.md) |
| Device install guide | **Planned** | [docs/INSTALL_DEVICE.md](docs/INSTALL_DEVICE.md) (landing soon) |
| Encrypted cloud backup | **Planned** | iCloud / Google Drive / Nextcloud |
| Food barcode camera flow | **Planned** | Flagged off |
| Morning readiness check-in UI | **Planned** | Scoring helpers exist |
| Wear OS deep workout parity | **Planned** | Shell exists; no system WorkoutKit equivalent |

## Roadmap

Immediate: **Zone 2** running (zone math + evidence, glanceable watchOS session + haptics, summary, WorkoutKit Zone 2 path) and the personal-device install guide. Longer phase list: [docs/plans/README.md](docs/plans/README.md).

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
./gradlew linkDebugFrameworkIosSimulatorArm64 linkDebugFrameworkWatchosSimulatorArm64
cd iosApp && xcodegen generate && open OmniGart.xcodeproj
```

Install on a personal iPhone / Apple Watch: **[docs/INSTALL_DEVICE.md](docs/INSTALL_DEVICE.md)** (guide in progress — free Personal Team vs paid / TestFlight).

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
