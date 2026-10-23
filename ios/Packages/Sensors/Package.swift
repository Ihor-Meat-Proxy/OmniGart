// swift-tools-version: 5.9
import PackageDescription

/// OmniGart Sensors — BLE Heart Rate Service + Polar BLE SDK adapter.
///
/// Standalone Swift package. Wire into the XcodeGen app (PR #2) via:
/// ```yaml
/// packages:
///   OmniGartSensors:
///     path: Packages/Sensors
/// ```
/// and add `OmniGartSensors` to the OmniGart / OmniGartWatch dependencies.
///
/// Polar BLE SDK version pinned to the latest stable release (8.4.0, 2026-10-01).
let package = Package(
    name: "OmniGartSensors",
    platforms: [
        .iOS(.v16),
        .watchOS(.v9),
    ],
    products: [
        .library(name: "OmniGartSensors", targets: ["OmniGartSensors"]),
    ],
    dependencies: [
        .package(
            url: "https://github.com/polarofficial/polar-ble-sdk.git",
            exact: "8.4.0"
        ),
    ],
    targets: [
        .target(
            name: "OmniGartSensors",
            dependencies: [
                .product(name: "PolarBleSdk", package: "polar-ble-sdk"),
            ],
            path: "Sources/OmniGartSensors"
        ),
        .testTarget(
            name: "OmniGartSensorsTests",
            dependencies: ["OmniGartSensors"],
            path: "Tests/OmniGartSensorsTests"
        ),
    ]
)
