# Sensors modules — wiring guide

PR #2 (Phase 0 scaffold) is not on `main` yet. These modules are **standalone-buildable**
and intended to plug into that structure when it merges.

## Layout

| Path | Role |
|------|------|
| `shared/sensors` | KMP pure logic: BLE HRS parser, RR artefact filter, HRV (rMSSD / ln rMSSD / SDNN), Baevsky SI, clock-offset + swim merge, `PolarDeviceClient` / `HeartRateSink` interfaces |
| `ios/Packages/Sensors` | Swift package `OmniGartSensors`: CoreBluetooth HRS central (state restoration), Polar BLE SDK **8.4.0** adapter, HealthKit sink protocol |
| `android/sensors` | Android library: BLE scanner/GATT, Polar BLE SDK **8.4.0**, `connectedDevice` foreground service, Health Connect sink protocol |

## Shared KMP (`shared/sensors`)

Already included from root `settings.gradle.kts` as `:shared:sensors`.

```bash
./gradlew :shared:sensors:jvmTest
```

When PR #2 lands, extend `shared/sensors/build.gradle.kts` with `androidTarget()` /
`iosArm64()` / `iosSimulatorArm64()` and depend on it from `shared` / apps.

## iOS Swift package

```yaml
# iosApp/project.yml (XcodeGen) — after PR #2
packages:
  OmniGartSensors:
    path: ../Packages/Sensors   # or Packages/Sensors relative to iosApp
targets:
  OmniGart:
    dependencies:
      - package: OmniGartSensors
```

Info.plist (host app):

- `UIBackgroundModes` → `bluetooth-central`
- `NSBluetoothAlwaysUsageDescription`

Polar SDK is pulled via SPM from `https://github.com/polarofficial/polar-ble-sdk.git` at **exact 8.4.0**.

## Android library

Not included in root `settings.gradle.kts` until AGP is available (PR #2). Then:

```kotlin
// settings.gradle.kts
include(":android:sensors")
project(":android:sensors").projectDir = file("android/sensors")

// repositories — JitPack required for Polar
maven { url = uri("https://jitpack.io") }

// androidApp/build.gradle.kts
dependencies {
    implementation(project(":android:sensors"))
}
```

Manifest merges the `connectedDevice` foreground service and BLE permissions from
`android/sensors/src/main/AndroidManifest.xml`.

## Health adapters

Do **not** depend on the Health adapter PR. Inject:

- shared: `com.omnigart.sensors.health.HeartRateSink`
- iOS: `OmniGartSensors.HeartRateSink`
- Android: `com.omnigart.sensors.health.HeartRateSink`

Implementations (`HealthKitHeartRateSink`, `HealthConnectHeartRateSink`) live elsewhere.

## SDK versions (verified 2026-10-22)

| Library | Version | Source |
|---------|---------|--------|
| Polar BLE SDK | **8.4.0** | [GitHub release 2026-10-01](https://github.com/polarofficial/polar-ble-sdk/releases/tag/8.4.0) |
| kotlinx-coroutines | 1.10.2 | Maven Central |
| Kotlin (root) | 2.4.21 | matches planning module |
