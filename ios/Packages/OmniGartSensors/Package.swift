// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "OmniGartSensors",
    platforms: [
        .iOS(.v18),
        .watchOS(.v11),
    ],
    products: [
        .library(name: "OmniGartSensors", targets: ["OmniGartSensors"]),
        .library(name: "OmniGartSensorsPolar", targets: ["OmniGartSensorsPolar"]),
    ],
    dependencies: [
        // Official Polar BLE SDK 8.4.0 (SPM). Licence: Polar SDK Limited License —
        // see docs/third_party/POLAR_SDK.md. CocoaPods is read-only after Dec 2026.
        .package(url: "https://github.com/polarofficial/polar-ble-sdk.git", exact: "8.4.0"),
    ],
    targets: [
        .target(
            name: "OmniGartSensors",
            dependencies: [],
            path: "Sources/OmniGartSensors",
            swiftSettings: [
                .swiftLanguageMode(.v6),
            ]
        ),
        .target(
            name: "OmniGartSensorsPolar",
            dependencies: [
                "OmniGartSensors",
                .product(name: "PolarBleSdk", package: "polar-ble-sdk"),
            ],
            path: "Sources/OmniGartSensorsPolar",
            swiftSettings: [
                .swiftLanguageMode(.v6),
            ]
        ),
        .testTarget(
            name: "OmniGartSensorsTests",
            dependencies: ["OmniGartSensors"],
            path: "Tests/OmniGartSensorsTests"
        ),
    ]
)
