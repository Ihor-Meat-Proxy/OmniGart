# OmniGart

Open-source (**AGPL-3.0**) local-first fitness, nutrition and recovery app.

Phase 0 is a **tested foundation**: Kotlin Multiplatform shared core, calculation modules, SQLDelight persistence, and thin native shells (iOS, watchOS, Android, Wear OS).

## Requirements

| Platform | Tooling |
|----------|---------|
| Shared / Android / Wear | JDK 17+, Android Studio Otter+ / AGP 9.4, Android SDK 36 |
| iOS / watchOS | macOS with Xcode that includes **iOS 27.2** and **watchOS 27.2** simulators (deployment target 26.0), [XcodeGen](https://github.com/yonaskolb/XcodeGen), SwiftLint |

## Clone and configure

```bash
git clone https://github.com/Ihor-Meat-Proxy/OmniGart.git
cd OmniGart
# Android SDK path (Android Studio usually writes this):
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # macOS
```

## Shared + Android (Linux or macOS)

```bash
./scripts/check_no_ru_locale.sh
./gradlew :shared:testAndroidHostTest \
  :androidApp:assembleDebug :androidApp:testDebugUnitTest \
  :wearApp:assembleDebug :wearApp:testDebugUnitTest
./gradlew ktlintCheck detekt
```

### Android emulator

1. AVD with API 34+ phone image; run `androidApp`.
2. Wear OS emulator (API 30+); run `wearApp` (applicationId `com.omnigart.android`).

Android Studio: open the repo root, sync Gradle, select `androidApp` or `wearApp` run configuration.

## iOS / watchOS (macOS only)

```bash
brew install xcodegen swiftlint   # once
./gradlew :shared:embedAndSignAppleFrameworkForXcode \
  || ./gradlew linkDebugFrameworkIosSimulatorArm64 linkDebugFrameworkWatchosSimulatorArm64

cd iosApp
xcodegen generate
open OmniGart.xcodeproj
```

### Simulators (owner environment)

```bash
xcrun simctl list devices available | grep -E 'iOS 27|watchOS 27'
# Example — adjust device names to match your install:
xcodebuild -project iosApp/OmniGart.xcodeproj -scheme OmniGart \
  -destination 'platform=iOS Simulator,name=iPhone 17,OS=27.2' \
  test

xcodebuild -project iosApp/OmniGart.xcodeproj -scheme OmniGartWatch \
  -destination 'platform=watchOS Simulator,name=Apple Watch Series 11,OS=27.2' \
  test
```

SwiftLint: `swiftlint lint --config .swiftlint.yml`

> **Linux CI / this cloud agent cannot compile Apple targets.** Treat iOS/watchOS as verified only after the commands above succeed on your Mac.

## Repository layout

See [ARCHITECTURE.md](ARCHITECTURE.md) and [docs/OPEN_QUESTIONS.md](docs/OPEN_QUESTIONS.md).

```
shared/          KMP domain, calcs, SQLDelight, i18n keys
androidApp/      Phone Compose shell
wearApp/         Wear Compose shell
iosApp/          XcodeGen + SwiftUI phone app
watchApp/        SwiftUI watch app sources
content/packs/   Versioned content JSON (not UI strings)
docs/evidence/   Citation schema + entries
```

## Licence

GNU Affero General Public License v3.0 — see [LICENSE](LICENSE).
