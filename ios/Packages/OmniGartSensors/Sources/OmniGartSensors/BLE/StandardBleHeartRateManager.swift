import CoreBluetooth
import Foundation
import Observation

/// CoreBluetooth central for the standard Heart Rate Profile (service 0x180D).
///
/// Parsing of 0x2A37 payloads is performed in KMP `shared/sensors`
/// (`HeartRateMeasurementParser`). This type emits raw notification bytes.
///
/// Swift 6 / strict concurrency: `@MainActor` isolates CBCentralManager delegate traffic.
@MainActor
@Observable
public final class StandardBleHeartRateManager: NSObject {
    public private(set) var session = SensorSessionState()
    public private(set) var discovered: [SensorDevice] = []
    public private(set) var remembered: [SensorDevice] = []

    public var onNotification: (@Sendable (RawHeartRateNotification) -> Void)?

    private var central: CBCentralManager?
    private var connectedPeripheral: CBPeripheral?
    private var autoReconnect = true
    private var preferredId: String?
    private var reconnectAttempt = 0
    private var reconnectTask: Task<Void, Never>?
    private let backoff = ReconnectBackoff()
    private let defaultsKey = "omnigart.sensors.remembered"

    public override init() {
        super.init()
        loadRemembered()
    }

    public func start() {
        if central == nil {
            central = CBCentralManager(delegate: self, queue: nil)
        }
    }

    public func startScan() {
        start()
        guard let central, central.state == .poweredOn else {
            session.connection = .failed
            session.lastError = "Bluetooth not powered on"
            return
        }
        discovered = []
        session.connection = .scanning
        let services = [CBUUID(string: BleUUID.heartRateService.uuidString)]
        central.scanForPeripherals(
            withServices: services,
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: false]
        )
    }

    public func stopScan() {
        central?.stopScan()
        if session.connection == .scanning {
            session.connection = .disconnected
        }
    }

    public func connect(deviceId: String) {
        start()
        guard let central else { return }
        stopScan()
        preferredId = deviceId
        reconnectTask?.cancel()
        session.connection = .connecting
        // Retrieve known peripherals first (paired / recently seen).
        let uuid = UUID(uuidString: deviceId)
        if let uuid,
           let peripheral = central.retrievePeripherals(withIdentifiers: [uuid]).first {
            connectedPeripheral = peripheral
            peripheral.delegate = self
            central.connect(peripheral, options: nil)
            return
        }
        session.connection = .failed
        session.lastError = "Peripheral \(deviceId) not found; scan first"
    }

    public func disconnect() {
        autoReconnect = false
        reconnectTask?.cancel()
        preferredId = nil
        if let peripheral = connectedPeripheral {
            central?.cancelPeripheralConnection(peripheral)
        }
        connectedPeripheral = nil
        session.connection = .disconnected
    }

    public func remember(_ device: SensorDevice) {
        remembered.removeAll { $0.id == device.id }
        remembered.append(device)
        persistRemembered()
    }

    public func forget(deviceId: String) {
        remembered.removeAll { $0.id == deviceId }
        persistRemembered()
    }

    public func setAutoReconnectEnabled(_ enabled: Bool) {
        autoReconnect = enabled
        if !enabled { reconnectTask?.cancel() }
    }
}

extension StandardBleHeartRateManager: CBCentralManagerDelegate {
    public nonisolated func centralManagerDidUpdateState(_ central: CBCentralManager) {
        Task { @MainActor in
            if central.state != .poweredOn, session.connection == .scanning {
                session.connection = .failed
                session.lastError = "Bluetooth state \(String(describing: central.state))"
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
            let name = peripheral.name
                ?? advertisementData[CBAdvertisementDataLocalNameKey] as? String
            let kind = SensorKind.infer(from: name)
            let device = SensorDevice(
                id: peripheral.identifier.uuidString,
                name: name,
                kind: kind,
                rssi: RSSI.intValue
            )
            if let idx = discovered.firstIndex(where: { $0.id == device.id }) {
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
            reconnectAttempt = 0
            session.connection = .connected
            session.device = SensorDevice(
                id: peripheral.identifier.uuidString,
                name: peripheral.name,
                kind: SensorKind.infer(from: peripheral.name)
            )
            peripheral.discoverServices([
                CBUUID(string: BleUUID.heartRateService.uuidString),
                CBUUID(string: BleUUID.batteryService.uuidString),
                CBUUID(string: BleUUID.deviceInformation.uuidString),
            ])
        }
    }

    public nonisolated func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: Error?
    ) {
        Task { @MainActor in
            session.connection = .disconnected
            session.lastError = error?.localizedDescription
            connectedPeripheral = nil
            scheduleReconnectIfNeeded()
        }
    }

    public nonisolated func centralManager(
        _ central: CBCentralManager,
        didFailToConnect peripheral: CBPeripheral,
        error: Error?
    ) {
        Task { @MainActor in
            session.connection = .failed
            session.lastError = error?.localizedDescription ?? "Connect failed"
            scheduleReconnectIfNeeded()
        }
    }
}

extension StandardBleHeartRateManager: CBPeripheralDelegate {
    public nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverServices error: Error?
    ) {
        Task { @MainActor in
            guard error == nil else {
                session.lastError = error?.localizedDescription
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
        error: Error?
    ) {
        Task { @MainActor in
            guard error == nil else { return }
            for characteristic in service.characteristics ?? [] {
                if characteristic.uuid == CBUUID(string: BleUUID.heartRateMeasurement.uuidString) {
                    peripheral.setNotifyValue(true, for: characteristic)
                }
                if characteristic.uuid == CBUUID(string: BleUUID.batteryLevel.uuidString) ||
                    characteristic.uuid == CBUUID(string: BleUUID.bodySensorLocation.uuidString) {
                    peripheral.readValue(for: characteristic)
                }
            }
            session.connection = .ready
        }
    }

    public nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        Task { @MainActor in
            guard error == nil, let data = characteristic.value else { return }
            if characteristic.uuid == CBUUID(string: BleUUID.heartRateMeasurement.uuidString) {
                onNotification?(
                    RawHeartRateNotification(
                        deviceId: peripheral.identifier.uuidString,
                        payload: data
                    )
                )
            }
            if characteristic.uuid == CBUUID(string: BleUUID.batteryLevel.uuidString),
               let level = data.first {
                session.batteryPercent = Int(level)
            }
        }
    }
}

extension StandardBleHeartRateManager {
    private func scheduleReconnectIfNeeded() {
        guard autoReconnect, let preferredId else { return }
        reconnectTask?.cancel()
        let attempt = reconnectAttempt
        reconnectAttempt += 1
        let delayMs = backoff.delayMs(forAttempt: attempt)
        guard delayMs >= 0 else {
            session.connection = .failed
            session.lastError = "Reconnect attempts exhausted"
            return
        }
        session.connection = .reconnecting
        reconnectTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: UInt64(delayMs) * 1_000_000)
            guard !Task.isCancelled else { return }
            connect(deviceId: preferredId)
        }
    }

    private func loadRemembered() {
        guard let data = UserDefaults.standard.data(forKey: defaultsKey),
              let decoded = try? JSONDecoder().decode([CodableDevice].self, from: data)
        else { return }
        remembered = decoded.map(\.asDevice)
    }

    private func persistRemembered() {
        let payload = remembered.map(CodableDevice.init)
        if let data = try? JSONEncoder().encode(payload) {
            UserDefaults.standard.set(data, forKey: defaultsKey)
        }
    }
}

private struct CodableDevice: Codable {
    var id: String
    var name: String?
    var kind: String

    init(_ device: SensorDevice) {
        id = device.id
        name = device.name
        kind = device.kind.rawValue
    }

    var asDevice: SensorDevice {
        SensorDevice(id: id, name: name, kind: SensorKind(rawValue: kind) ?? .unknown)
    }
}

extension SensorKind {
    public static func infer(from name: String?) -> SensorKind {
        guard let name else { return .unknown }
        let lower = name.lowercased()
        if lower.contains("h10") { return .polarH10 }
        if lower.contains("sense") { return .polarVeritySense }
        if lower.contains("polar") { return .polarOther }
        return .standardHeartRateProfile
    }
}
