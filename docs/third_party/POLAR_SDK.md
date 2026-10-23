# Polar BLE SDK (third-party)

| Item | Value |
|------|--------|
| Upstream | https://github.com/polarofficial/polar-ble-sdk |
| Version pinned | **8.4.0** (tag `8.4.0`, published 2026-10-01) |
| Android coordinate | `com.github.polarofficial:polar-ble-sdk:8.4.0` (JitPack) |
| iOS | Swift Package Manager, `exact: "8.4.0"`, product `PolarBleSdk` |
| Licence file | Upstream `Polar_SDK_License.txt` — **Polar Software Development Kit Limited License Agreement** |

## Licence summary (non-binding; read the full text)

- Limited, non-exclusive licence to use, copy, and modify the SDK; include Polar copyright/licence notice.
- Allowed for private and commercial apps that transfer Data between Polar products and Licensee Software.
- **Not** intended for life-critical, life-supporting, or medical purposes (Polar disclaimer).
- Polar trademarks/logos may not be used for marketing without consent.
- Provided **AS IS**; no warranty; polar may change the SDK without obligation.
- This is **not** an OSI open-source licence and is **separate from OmniGart’s AGPL-3.0**. Distributing Polar object code with OmniGart must still satisfy Polar’s terms and AGPL obligations for OmniGart code.

## Platform notes (8.4.0)

- Android getting-started text recommends **minSdk 33**; the published library `defaultConfig` historically allowed lower — OmniGart `android/sensors` keeps **minSdk 26** aligned with the phone shell; raise if Polar AAR merge forces it.
- iOS: Polar urges SPM over CocoaPods (Specs repo read-only by December 2026). Xcode 27 / dropping pre-iOS 15 support planned for Polar SDK **9.0.0**.
- `PolarBleApiDefaultImpl.versionInfo()` reports `8.4.0`.

## OmniGart wrappers

- Android: `com.omnigart.sensors.android.polar.PolarBleSensorAdapter`
- iOS: product `OmniGartSensorsPolar` → `PolarSensorBridge` (+ app-level Polar API wiring)
