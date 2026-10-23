import CoreBluetooth
import Foundation
import Observation

/// CoreBluetooth central for treadmill protocols (FTMS first, then FitShow / iConsole UART).
///
/// Byte parsing belongs in KMP `shared/sensors`. This type discovers services, enables
/// notifications, and emits raw payloads plus connection state. Control Point writes
/// are only issued when the caller invokes an explicit control method.
@MainActor
@Observable
public final class TreadmillBleCentral: NSObject {
    public private(set) var session = SensorSessionState()
    public private(set) var discovered: [SensorDevice] = []
    public private(set) var protocolKindRaw: String = "Unknown"
    public private(set) var serviceUuids: [String] = []
    public private(set) var lastNotification: TreadmillRawNotification?

    public var onNotification: (@Sendable (TreadmillRawNotification) -> Void)?
    public var onControlResponse: (@Sendable (Data) -> Void)?

    private var central: CBCentralManager?
    private var connectedPeripheral: CBPeripheral?
    private var controlPointCharacteristic: CBCharacteristic?
    private var writeCharacteristic: CBCharacteristic?
    private var autoReconnect = true
    private var preferredId: String?
    private var reconnectAttempt = 0
    private var reconnectTask: Task<Void, Never>?
    private let backoff = ReconnectBackoff()
    private var knownPeripherals: [String: CBPeripheral] = [:]

    public override init() {
        super.init()
    }

    public func start() {
        if central == nil {
            central = CBCentralManager(delegate: self, queue: nil)
        }
    }

    public func startScan(includeUnnamed: Bool = true) {
        _ = includeUnnamed
        start()
        guard let central, central.state == .poweredOn else {
            session.connection = .failed
            session.lastError = "Bluetooth not powered on"
            return
        }
        discovered = []
        knownPeripherals = [:]
        session.connection = .scanning
        // Scan broadly; filter by FTMS / FFF0 / Microchip UART after connect.
        central.scanForPeripherals(
            withServices: [
                CBUUID(string: BleUUID.fitnessMachineService.uuidString),
                CBUUID(string: BleUUID.fitShowService.uuidString),
                CBUUID(string: BleUUID.iConsoleUartService.uuidString),
            ],
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: false]
        )
        // Also scan without filter so unknown Reebok-style devices still appear.
        // Dual scan: service-filtered above; callers may call startOpenScan().
    }

    public func startOpenScan() {
        start()
        guard let central, central.state == .poweredOn else { return }
        session.connection = .scanning
        central.scanForPeripherals(withServices: nil, options: nil)
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
        if let peripheral = knownPeripherals[deviceId] {
            connectedPeripheral = peripheral
            peripheral.delegate = self
            central.connect(peripheral, options: nil)
            return
        }
        if let uuid = UUID(uuidString: deviceId),
           let peripheral = central.retrievePeripherals(withIdentifiers: [uuid]).first {
            knownPeripherals[deviceId] = peripheral
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
        controlPointCharacteristic = nil
        writeCharacteristic = nil
        session.connection = .disconnected
    }

    public func setAutoReconnect(_ enabled: Bool) {
        autoReconnect = enabled
    }

    /// Explicit user action only — writes Fitness Machine Control Point or FitShow/iConsole TX.
    public func writeControl(_ data: Data) {
        guard let peripheral = connectedPeripheral else { return }
        if let cp = controlPointCharacteristic {
            let type: CBCharacteristicWriteType =
                cp.properties.contains(.writeWithoutResponse) ? .withoutResponse : .withResponse
            peripheral.writeValue(data, for: cp, type: type)
            return
        }
        if let write = writeCharacteristic {
            let type: CBCharacteristicWriteType =
                write.properties.contains(.writeWithoutResponse) ? .withoutResponse : .withResponse
            peripheral.writeValue(data, for: write, type: type)
        }
    }

    private func scheduleReconnect() {
        guard autoReconnect, let preferredId else { return }
        reconnectTask?.cancel()
        session.connection = .reconnecting
        let delayMs = backoff.delayMs(forAttempt: reconnectAttempt)
        reconnectAttempt += 1
        guard delayMs >= 0 else { return }
        reconnectTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: UInt64(delayMs) * 1_000_000)
            guard !Task.isCancelled else { return }
            connect(deviceId: preferredId)
        }
    }
}

public struct TreadmillRawNotification: Sendable {
    public let deviceId: String
    public let serviceUuid: String?
    public let characteristicUuid: String
    public let payload: Data
    public let receivedAt: Date

    public init(
        deviceId: String,
        serviceUuid: String?,
        characteristicUuid: String,
        payload: Data,
        receivedAt: Date = Date()
    ) {
        self.deviceId = deviceId
        self.serviceUuid = serviceUuid
        self.characteristicUuid = characteristicUuid
        self.payload = payload
        self.receivedAt = receivedAt
    }
}

extension BleUUID {
    public static let fitnessMachineService = CBUUIDString(uuid: "1826")
    public static let treadmillData = CBUUIDString(uuid: "2ACD")
    public static let fitnessMachineFeature = CBUUIDString(uuid: "2ACC")
    public static let supportedSpeedRange = CBUUIDString(uuid: "2AD4")
    public static let supportedInclinationRange = CBUUIDString(uuid: "2AD5")
    public static let fitnessMachineControlPoint = CBUUIDString(uuid: "2AD9")
    public static let fitnessMachineStatus = CBUUIDString(uuid: "2ADA")
    public static let fitShowService = CBUUIDString(uuid: "FFF0")
    public static let fitShowNotify = CBUUIDString(uuid: "FFF1")
    public static let fitShowWrite = CBUUIDString(uuid: "FFF2")
    public static let iConsoleUartService = CBUUIDString(
        uuid: "49535343-FE7D-4AE5-8FA9-9FAFD205E455"
    )
    public static let iConsoleUartTx = CBUUIDString(
        uuid: "49535343-1E4D-4BD9-BA61-23C647249616"
    )
    public static let iConsoleUartRx = CBUUIDString(
        uuid: "49535343-8841-43F4-A8D4-ECBE34729BB3"
    )
}

extension TreadmillBleCentral: CBCentralManagerDelegate {
    public nonisolated func centralManagerDidUpdateState(_ central: CBCentralManager) {
        Task { @MainActor in
            if central.state != .poweredOn {
                session.connection = .failed
                session.lastError = "Bluetooth state \(central.state.rawValue)"
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
            knownPeripherals[id] = peripheral
            let name =
                peripheral.name
                ?? advertisementData[CBAdvertisementDataLocalNameKey] as? String
            let device = SensorDevice(
                id: id,
                name: name,
                kind: .unknown,
                rssi: RSSI.intValue
            )
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
            session.connection = .connected
            reconnectAttempt = 0
            peripheral.delegate = self
            peripheral.discoverServices(nil)
        }
    }

    public nonisolated func centralManager(
        _ central: CBCentralManager,
        didFailToConnect peripheral: CBPeripheral,
        error: (any Error)?
    ) {
        Task { @MainActor in
            session.connection = .failed
            session.lastError = error?.localizedDescription
            scheduleReconnect()
        }
    }

    public nonisolated func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: (any Error)?
    ) {
        Task { @MainActor in
            controlPointCharacteristic = nil
            writeCharacteristic = nil
            if autoReconnect {
                scheduleReconnect()
            } else {
                session.connection = .disconnected
            }
            if let error {
                session.lastError = error.localizedDescription
            }
        }
    }
}

extension TreadmillBleCentral: CBPeripheralDelegate {
    public nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverServices error: (any Error)?
    ) {
        Task { @MainActor in
            if let error {
                session.lastError = error.localizedDescription
                return
            }
            let uuids = (peripheral.services ?? []).map { $0.uuid.uuidString.lowercased() }
            serviceUuids = uuids
            if uuids.contains(where: { $0.contains("1826") }) {
                protocolKindRaw = "Ftms"
            } else if uuids.contains(where: { $0.contains("fff0") }) {
                protocolKindRaw = "FitShow"
            } else if uuids.contains(where: { $0.hasPrefix("49535343") }) {
                protocolKindRaw = "IConsole"
            } else {
                protocolKindRaw = "Unknown"
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
                session.lastError = error.localizedDescription
                return
            }
            for characteristic in service.characteristics ?? [] {
                let uuid = characteristic.uuid.uuidString.lowercased()
                if uuid.contains("2ad9") {
                    controlPointCharacteristic = characteristic
                    if characteristic.properties.contains(.indicate)
                        || characteristic.properties.contains(.notify) {
                        peripheral.setNotifyValue(true, for: characteristic)
                    }
                }
                if uuid.contains("2acd") || uuid.contains("2ada") || uuid.contains("fff1")
                    || uuid.contains("1e4d4bd9") {
                    if characteristic.properties.contains(.notify)
                        || characteristic.properties.contains(.indicate) {
                        peripheral.setNotifyValue(true, for: characteristic)
                    }
                }
                if uuid.contains("fff2") || uuid.contains("884143f4") {
                    writeCharacteristic = characteristic
                }
                if uuid.contains("2acc") || uuid.contains("2ad4") || uuid.contains("2ad5") {
                    peripheral.readValue(for: characteristic)
                }
            }
            session.connection = .ready
            session.device = SensorDevice(
                id: peripheral.identifier.uuidString,
                name: peripheral.name,
                kind: .unknown,
                rssi: nil
            )
        }
    }

    public nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: (any Error)?
    ) {
        Task { @MainActor in
            if let error {
                session.lastError = error.localizedDescription
                return
            }
            guard let data = characteristic.value else { return }
            let uuid = characteristic.uuid.uuidString.lowercased()
            if uuid.contains("2ad9") {
                onControlResponse?(data)
            }
            let note = TreadmillRawNotification(
                deviceId: peripheral.identifier.uuidString,
                serviceUuid: characteristic.service?.uuid.uuidString.lowercased(),
                characteristicUuid: uuid,
                payload: data
            )
            lastNotification = note
            onNotification?(note)
        }
    }
}
