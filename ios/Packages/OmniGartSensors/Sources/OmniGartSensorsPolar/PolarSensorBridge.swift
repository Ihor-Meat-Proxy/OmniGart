import Foundation
import OmniGartSensors
import PolarBleSdk

/// Bridge documenting Polar SDK wiring for H10 / Verity Sense.
///
/// Full streaming and offline recording call sites mirror the Android
/// `PolarBleSensorAdapter`. App shells should construct `PolarBleApiDefaultImpl`
/// (or the Swift equivalent from PolarBleSdk) with features:
/// HR, device info, battery, online streaming, offline recording, H10 exercise recording.
///
/// Device capability matrix: `docs/SENSORS.md` and `docs/third_party/POLAR_SDK.md`.
@MainActor
public final class PolarSensorBridge {
    public let sdkVersionHint = "8.4.0"

    public init() {}

    public func recommendedFeaturesDescription() -> String {
        """
        FEATURE_HR, FEATURE_DEVICE_INFO, FEATURE_BATTERY_INFO, \
        FEATURE_POLAR_ONLINE_STREAMING, FEATURE_POLAR_OFFLINE_RECORDING, \
        FEATURE_POLAR_H10_EXERCISE_RECORDING
        """
    }

    /// Maps a Polar device marketing name to OmniGart sensor kind.
    public func kind(forDeviceName name: String) -> SensorKind {
        SensorKind.infer(from: name)
    }
}
