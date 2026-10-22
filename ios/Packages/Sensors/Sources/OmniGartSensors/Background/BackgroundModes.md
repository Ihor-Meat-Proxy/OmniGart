# iOS background modes for Sensors

Configure in the host app (XcodeGen / Info.plist) — this package does not mutate the app target.

## Required Info.plist keys

| Key | Value |
|-----|--------|
| `UIBackgroundModes` | include `bluetooth-central` |
| `NSBluetoothAlwaysUsageDescription` | user-facing reason for BLE access |
| `NSBluetoothPeripheralUsageDescription` | legacy; keep for older iOS if needed |

## State restoration

`BleHeartRateCentral` and `PolarDeviceAdapter` accept a `restorationIdentifier`
(`com.omnigart.sensors.ble.central` / `com.omnigart.sensors.polar` by default).

On relaunch after the system kills the app for memory, Core Bluetooth restores
centrals with that identifier via `centralManager(_:willRestoreState:)`. Re-set
delegates and re-enable notifications for restored peripherals.

Polar SDK 8.4.0 also accepts `restoreIdentifier` in
`PolarBleApiDefaultImpl.polarImplementation(_:features:restoreIdentifier:)`.

## watchOS

Standard HRS central works on watchOS; Polar SDK watchOS support is partial
(see Polar issue #479). Prefer phone-side Polar download after swim, then sync
merged HR to the watch workout via HealthKit.
