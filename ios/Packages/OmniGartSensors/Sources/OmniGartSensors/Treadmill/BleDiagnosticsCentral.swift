import CoreBluetooth
import Foundation
import Observation

/// Developer diagnostics: scan, list services/characteristics, subscribe to every
/// notify/indicate characteristic, and collect raw hex with timestamps.
@MainActor
@Observable
public final class BleDiagnosticsCentral: NSObject {
    public private(set) var connection = SensorConnectionState.disconnected
    public private(set) var discovered: [SensorDevice] = []
    public private(set) var entries: [BleDiagnosticsEntry] = []
    public private(set) var deviceCatalog: [BleDiagnosticsDeviceInfo] = []
    public private(set) var lastError: String?

    private var central: CBCentralManager?
    private var connected: CBPeripheral?
    private var known: [String: CBPeripheral] = [:]

    public override init() {
        super.init()
    }

    public func start() {
        if central == nil {
            central = CBCentralManager(delegate: self, queue: nil)
        }
    }

    public func startScan() {
        start()
        guard let central, central.state == .poweredOn else {
            connection = .failed
            lastError = "Bluetooth not powered on"
            return
        }
        discovered = []
        known = [:]
        connection = .scanning
        central.scanForPeripherals(withServices: nil, options: nil)
    }

    public func stopScan() {
        central?.stopScan()
        if connection == .scanning {
            connection = .disconnected
        }
    }

    public func connect(deviceId: String) {
        start()
        guard let central else { return }
        stopScan()
        connection = .connecting
        guard let peripheral = known[deviceId] else {
            connection = .failed
            lastError = "Scan first"
            return
        }
        connected = peripheral
        peripheral.delegate = self
        central.connect(peripheral, options: nil)
    }

    public func disconnect() {
        if let peripheral = connected {
            central?.cancelPeripheralConnection(peripheral)
        }
        connected = nil
        connection = .disconnected
    }

    public func clearLog() {
        entries = []
    }

    public func exportJSON() -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .millisecondsSince1970
        let doc = BleDiagnosticsExport(
            schema: "omnigart.treadmill.diagnostics.v1",
            capturedAt: Date(),
            devices: deviceCatalog,
            entries: entries
        )
        guard let data = try? encoder.encode(doc),
              let text = String(data: data, encoding: .utf8) else {
            return "{\"schema\":\"omnigart.treadmill.diagnostics.v1\",\"entries\":[]}"
        }
        return text
    }
}

public struct BleDiagnosticsEntry: Sendable, Codable, Identifiable {
    public var id: String { "\(timestamp.timeIntervalSince1970)-\(characteristicUuid)-\(hex)" }
    public let timestamp: Date
    public let deviceId: String
    public let deviceName: String?
    public let serviceUuid: String?
    public let characteristicUuid: String
    public let hex: String
}

public struct BleDiagnosticsDeviceInfo: Sendable, Codable, Identifiable {
    public var id: String { deviceId }
    public let deviceId: String
    public let name: String?
    public let serviceUuids: [String]
    public let characteristics: [BleDiagnosticsCharacteristicInfo]
}

public struct BleDiagnosticsCharacteristicInfo: Sendable, Codable {
    public let serviceUuid: String
    public let characteristicUuid: String
    public let properties: [String]
}

private struct BleDiagnosticsExport: Codable {
    let schema: String
    let capturedAt: Date
    let devices: [BleDiagnosticsDeviceInfo]
    let entries: [BleDiagnosticsEntry]
}

extension BleDiagnosticsCentral: CBCentralManagerDelegate {
    public nonisolated func centralManagerDidUpdateState(_ central: CBCentralManager) {
        Task { @MainActor in
            if central.state != .poweredOn {
                connection = .failed
                lastError = "Bluetooth state \(central.state.rawValue)"
            }
        }
    }

    public nonisolated func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        Task { @MainActor in
            let id = peripheral.identifier.uuidString
            known[id] = peripheral
            let name =
                peripheral.name
                ?? advertisementData[CBAdvertisementDataLocalNameKey] as? String
            let device = SensorDevice(id: id, name: name, kind: .unknown, rssi: RSSI.intValue)
            if let idx = discovered.firstIndex(where: { $0.id == id }) {
                discovered[idx] = device
            } else {
                discovered.append(device)
            }
        }
    }

    public nonisolated func centralManager(
        _ central: CBCentralManager,
        didConnect peripheral: CBPeripheral
    ) {
        Task { @MainActor in
            connection = .connected
            peripheral.discoverServices(nil)
        }
    }

    public nonisolated func centralManager(
        _ central: CBCentralManager,
        didFailToConnect peripheral: CBPeripheral,
        error: (any Error)?
    ) {
        Task { @MainActor in
            connection = .failed
            lastError = error?.localizedDescription
        }
    }

    public nonisolated func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: (any Error)?
    ) {
        Task { @MainActor in
            connection = .disconnected
            lastError = error?.localizedDescription
        }
    }
}

extension BleDiagnosticsCentral: CBPeripheralDelegate {
    public nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverServices error: (any Error)?
    ) {
        Task { @MainActor in
            if let error {
                lastError = error.localizedDescription
                return
            }
            for service in peripheral.services ?? [] {
                peripheral.discoverCharacteristics(nil, for: service)
            }
        }
    }

    public nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverCharacteristicsFor service: CBService,
        error: (any Error)?
    ) {
        Task { @MainActor in
            if let error {
                lastError = error.localizedDescription
                return
            }
            var chars: [BleDiagnosticsCharacteristicInfo] = []
            for characteristic in service.characteristics ?? [] {
                var props: [String] = []
                if characteristic.properties.contains(.read) { props.append("read") }
                if characteristic.properties.contains(.write) { props.append("write") }
                if characteristic.properties.contains(.writeWithoutResponse) {
                    props.append("writeWithoutResponse")
                }
                if characteristic.properties.contains(.notify) { props.append("notify") }
                if characteristic.properties.contains(.indicate) { props.append("indicate") }
                chars.append(
                    BleDiagnosticsCharacteristicInfo(
                        serviceUuid: service.uuid.uuidString.lowercased(),
                        characteristicUuid: characteristic.uuid.uuidString.lowercased(),
                        properties: props
                    )
                )
                if characteristic.properties.contains(.notify)
                    || characteristic.properties.contains(.indicate) {
                    peripheral.setNotifyValue(true, for: characteristic)
                }
                if characteristic.properties.contains(.read) {
                    peripheral.readValue(for: characteristic)
                }
            }
            let info = BleDiagnosticsDeviceInfo(
                deviceId: peripheral.identifier.uuidString,
                name: peripheral.name,
                serviceUuids: (peripheral.services ?? []).map { $0.uuid.uuidString.lowercased() },
                characteristics: chars
            )
            if let idx = deviceCatalog.firstIndex(where: { $0.deviceId == info.deviceId }) {
                var merged = deviceCatalog[idx].characteristics
                for c in chars where !merged.contains(where: {
                    $0.characteristicUuid == c.characteristicUuid
                }) {
                    merged.append(c)
                }
                deviceCatalog[idx] = BleDiagnosticsDeviceInfo(
                    deviceId: info.deviceId,
                    name: info.name,
                    serviceUuids: info.serviceUuids,
                    characteristics: merged
                )
            } else {
                deviceCatalog.append(info)
            }
            connection = .ready
        }
    }

    public nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: (any Error)?
    ) {
        Task { @MainActor in
            if let error {
                lastError = error.localizedDescription
                return
            }
            guard let data = characteristic.value else { return }
            let hex = data.map { String(format: "%02x", $0) }.joined()
            entries.append(
                BleDiagnosticsEntry(
                    timestamp: Date(),
                    deviceId: peripheral.identifier.uuidString,
                    deviceName: peripheral.name,
                    serviceUuid: characteristic.service?.uuid.uuidString.lowercased(),
                    characteristicUuid: characteristic.uuid.uuidString.lowercased(),
                    hex: hex
                )
            )
        }
    }
}
