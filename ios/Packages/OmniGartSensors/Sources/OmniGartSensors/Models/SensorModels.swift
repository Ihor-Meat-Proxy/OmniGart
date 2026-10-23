import Foundation

/// Mirrors shared/sensors Kotlin models for the Swift BLE layer.
/// Byte-level HR Measurement parsing stays in KMP `HeartRateMeasurementParser`;
/// this package transports raw notifications and connection state.

public struct SensorDevice: Sendable, Hashable, Identifiable {
    public let id: String
    public let name: String?
    public let kind: SensorKind
    public let rssi: Int?

    public init(id: String, name: String?, kind: SensorKind = .unknown, rssi: Int? = nil) {
        self.id = id
        self.name = name
        self.kind = kind
        self.rssi = rssi
    }
}

public enum SensorKind: String, Sendable {
    case standardHeartRateProfile
    case polarH10
    case polarVeritySense
    case polarOther
    case unknown
}

public enum SensorConnectionState: String, Sendable {
    case disconnected
    case scanning
    case connecting
    case connected
    case ready
    case reconnecting
    case failed
}

public struct SensorSessionState: Sendable {
    public var device: SensorDevice?
    public var connection: SensorConnectionState
    public var batteryPercent: Int?
    public var lastError: String?

    public init(
        device: SensorDevice? = nil,
        connection: SensorConnectionState = .disconnected,
        batteryPercent: Int? = nil,
        lastError: String? = nil
    ) {
        self.device = device
        self.connection = connection
        self.batteryPercent = batteryPercent
        self.lastError = lastError
    }
}

public struct RawHeartRateNotification: Sendable {
    public let deviceId: String
    public let payload: Data
    public let receivedAt: Date

    public init(deviceId: String, payload: Data, receivedAt: Date = Date()) {
        self.deviceId = deviceId
        self.payload = payload
        self.receivedAt = receivedAt
    }
}

public enum BleUUID {
    public static let heartRateService = CBUUIDString(uuid: "180D")
    public static let heartRateMeasurement = CBUUIDString(uuid: "2A37")
    public static let bodySensorLocation = CBUUIDString(uuid: "2A38")
    public static let batteryService = CBUUIDString(uuid: "180F")
    public static let batteryLevel = CBUUIDString(uuid: "2A19")
    public static let deviceInformation = CBUUIDString(uuid: "180A")
    public static let cccd = CBUUIDString(uuid: "2902")

    public struct CBUUIDString: Sendable {
        public let uuidString: String
        public init(uuid: String) {
            // Expand 16-bit assigned numbers to Bluetooth Base UUID.
            if uuid.count == 4 {
                self.uuidString = "0000\(uuid.lowercased())-0000-1000-8000-00805f9b34fb"
            } else {
                self.uuidString = uuid.lowercased()
            }
        }
    }
}

public enum AppleBluetoothUsage {
    public static let usageDescriptionKey = "NSBluetoothAlwaysUsageDescription"
    public static let usageDescription =
        "OmniGart connects to Bluetooth heart-rate sensors during workouts."
    public static let backgroundModeBluetoothCentral = "bluetooth-central"

    /// watchOS cannot substitute an external BLE strap as the HealthKit workout HR source.
    public static let watchOSExternalHrLimitation =
        "External BLE HR straps are not substituted into HealthKit workout HR on watchOS; " +
        "use the watch optical sensor for HKWorkoutSession and optionally mirror strap data in-app."

    public static let treadmillUsageDescription =
        "OmniGart uses Bluetooth to connect to treadmills, heart-rate straps, and similar " +
        "fitness accessories you choose, so live speed, incline, distance, and heart-rate " +
        "samples can improve on-device wellness estimates."
}
