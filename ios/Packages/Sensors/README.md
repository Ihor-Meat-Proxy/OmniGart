# OmniGartSensors (Swift package)

CoreBluetooth Heart Rate Service central (with `bluetooth-central` state
restoration), Polar BLE SDK **8.4.0** adapter, swim-strap session helper, and
`HeartRateSink` protocol (HealthKit implementation lives in the Health adapter).

```swift
.package(url: "https://github.com/polarofficial/polar-ble-sdk.git", exact: "8.4.0")
```

Requires a macOS host + Xcode to resolve SPM / run tests. Parser unit tests do
not need hardware. See `docs/testing/polar-h10.md` for on-device coverage.
