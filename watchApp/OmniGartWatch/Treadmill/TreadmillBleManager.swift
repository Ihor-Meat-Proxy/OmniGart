import CoreBluetooth
import Foundation

/// CoreBluetooth central for FTMS, RSC, and iConsole BLE UART treadmills.
@MainActor
final class TreadmillBleManager: NSObject, ObservableObject {
    enum ConnectionState: String {
        case idle
        case scanning
        case connecting
        case connected
        case reconnecting
        case failed
    }

    struct DiscoveredDevice: Identifiable, Hashable {
        let id: UUID
        let name: String
        let protocolHint: String
    }

    @Published private(set) var state: ConnectionState = .idle
    @Published private(set) var discovered: [DiscoveredDevice] = []
    @Published private(set) var lastError: String?
    @Published private(set) var live = TreadmillLiveSnapshot()
    @Published private(set) var diagnosticsText: String = ""

    var allowRemoteControl = false
    var watchHeartRateBpm: Int?

    private var central: CBCentralManager?
    private var peripheral: CBPeripheral?
    private var preferredId: UUID?
    private var controlPoint: CBCharacteristic?
    private var iconsoleRx: CBCharacteristic?
    private var reconnectTask: Task<Void, Never>?
    private var iconsolePollTask: Task<Void, Never>?
    private var reconnectAttempt = 0
    private var diagLines: [String] = []
    private let rememberedKey = "omnigart.treadmill.remembered"
    private let hrMax: Int

    init(hrMax: Int = 190) {
        self.hrMax = hrMax
        super.init()
    }

    func start() {
        if central == nil {
            central = CBCentralManager(delegate: self, queue: nil)
        }
    }

    func startScan() {
        start()
        guard let central, central.state == .poweredOn else {
            state = .failed
            lastError = "Bluetooth not powered on"
            return
        }
        discovered = []
        state = .scanning
        appendDiag("INFO scan start")
        // Broad scan: many iConsole units do not advertise FTMS UUID in the advertisement.
        central.scanForPeripherals(
            withServices: nil,
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: false]
        )
    }

    func stopScan() {
        central?.stopScan()
        if state == .scanning { state = .idle }
    }

    func connect(id: UUID) {
        start()
        guard let central else { return }
        stopScan()
        preferredId = id
        UserDefaults.standard.set(id.uuidString, forKey: rememberedKey)
        state = .connecting
        if let known = central.retrievePeripherals(withIdentifiers: [id]).first {
            peripheral = known
            known.delegate = self
            central.connect(known, options: nil)
            return
        }
        state = .failed
        lastError = "Device not found — scan again"
    }

    func connectRemembered() {
        guard let raw = UserDefaults.standard.string(forKey: rememberedKey),
              let id = UUID(uuidString: raw) else { return }
        connect(id: id)
    }

    func disconnect() {
        reconnectTask?.cancel()
        iconsolePollTask?.cancel()
        preferredId = nil
        if let peripheral {
            central?.cancelPeripheralConnection(peripheral)
        }
        peripheral = nil
        state = .idle
        live = TreadmillLiveSnapshot()
    }

    func setTargetSpeedKmh(_ speed: Double) {
        guard allowRemoteControl, let controlPoint, let peripheral else { return }
        peripheral.writeValue(FtmsTreadmillParser.requestControl(), for: controlPoint, type: .withResponse)
        peripheral.writeValue(FtmsTreadmillParser.setTargetSpeedKmh(speed), for: controlPoint, type: .withResponse)
        appendDiag("INFO control set speed \(speed)")
    }

    func setTargetInclinePercent(_ incline: Double) {
        guard allowRemoteControl, let controlPoint, let peripheral else { return }
        peripheral.writeValue(FtmsTreadmillParser.requestControl(), for: controlPoint, type: .withResponse)
        peripheral.writeValue(
            FtmsTreadmillParser.setTargetInclinePercent(incline),
            for: controlPoint,
            type: .withResponse
        )
        appendDiag("INFO control set incline \(incline)")
    }

    func exportDiagnosticsURL() -> URL? {
        let body = """
        OmniGart treadmill diagnostics
        Protocol capture only — not a medical record.
        ---
        \(diagLines.joined(separator: "\n"))
        """
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("omnigart-treadmill-diagnostics.txt")
        do {
            try body.write(to: url, atomically: true, encoding: .utf8)
            return url
        } catch {
            return nil
        }
    }

    private func appendDiag(_ line: String) {
        diagLines.append("t=\(diagLines.count) \(line)")
        diagnosticsText = diagLines.joined(separator: "\n")
    }

    private func scheduleReconnect() {
        guard let preferredId else { return }
        reconnectTask?.cancel()
        state = .reconnecting
        let attempt = reconnectAttempt
        reconnectAttempt += 1
        let delay = min(60.0, pow(2.0, Double(min(attempt, 5))))
        reconnectTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
            guard !Task.isCancelled else { return }
            connect(id: preferredId)
        }
    }

    private func apply(_ parsed: TreadmillParsedMetrics) {
        let displayHr = watchHeartRateBpm ?? parsed.machineHeartRateBpm
        live = TreadmillLiveSnapshot(
            protocolKind: parsed.protocolKind,
            connected: true,
            speedKmh: parsed.speedKmh,
            paceMinPerKm: parsed.paceMinPerKm,
            inclinePercent: parsed.inclinePercent,
            distanceMetres: parsed.distanceMetres,
            elapsedSeconds: parsed.elapsedSeconds,
            heartRateBpm: displayHr,
            energyKcal: parsed.energyKcal,
            steps: parsed.steps,
            zone: TreadmillHeartRateZone.zone(bpm: displayHr, hrMax: hrMax)
        )
    }
}

struct TreadmillLiveSnapshot: Equatable {
    var protocolKind: TreadmillProtocolKind = .none
    var connected: Bool = false
    var speedKmh: Double?
    var paceMinPerKm: Double?
    var inclinePercent: Double?
    var distanceMetres: Double?
    var elapsedSeconds: Int?
    var heartRateBpm: Int?
    var energyKcal: Double?
    var steps: Int64?
    var zone: Int?
}

extension TreadmillBleManager: CBCentralManagerDelegate {
    nonisolated func centralManagerDidUpdateState(_ central: CBCentralManager) {
        Task { @MainActor in
            if central.state != .poweredOn {
                self.state = .failed
                self.lastError = "Bluetooth unavailable"
            }
        }
    }

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        Task { @MainActor in
            let name = peripheral.name
                ?? (advertisementData[CBAdvertisementDataLocalNameKey] as? String)
                ?? "Treadmill"
            let lower = name.lowercased()
            let hint = (lower.contains("iconsole") || lower.contains("i-console")) ? "ICONSOLE" : "BLE"
            let device = DiscoveredDevice(id: peripheral.identifier, name: name, protocolHint: hint)
            if !self.discovered.contains(device) {
                self.discovered.append(device)
                self.appendDiag("INFO discovered \(name)")
            }
        }
    }

    nonisolated func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        Task { @MainActor in
            self.state = .connected
            self.reconnectAttempt = 0
            self.appendDiag("INFO connected")
            peripheral.discoverServices(nil)
        }
    }

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didFailToConnect peripheral: CBPeripheral,
        error: Error?
    ) {
        Task { @MainActor in
            self.state = .failed
            self.lastError = error?.localizedDescription
            self.scheduleReconnect()
        }
    }

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: Error?
    ) {
        Task { @MainActor in
            self.iconsolePollTask?.cancel()
            self.live.connected = false
            self.appendDiag("INFO disconnected")
            if self.preferredId != nil {
                self.scheduleReconnect()
            } else {
                self.state = .idle
            }
        }
    }
}

extension TreadmillBleManager: CBPeripheralDelegate {
    nonisolated func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        Task { @MainActor in
            guard let services = peripheral.services else { return }
            for service in services {
                self.appendDiag("SERVICE \(service.uuid.uuidString)")
                peripheral.discoverCharacteristics(nil, for: service)
            }
        }
    }

    nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverCharacteristicsFor service: CBService,
        error: Error?
    ) {
        Task { @MainActor in
            guard let chars = service.characteristics else { return }
            for char in chars {
                self.appendDiag(
                    "CHAR svc=\(service.uuid.uuidString) char=\(char.uuid.uuidString) props=\(char.properties.rawValue)"
                )
                let u = char.uuid.uuidString.lowercased()
                if u.contains("2acd") || u.contains("2a53") {
                    peripheral.setNotifyValue(true, for: char)
                } else if u.contains("2ad9") {
                    self.controlPoint = char
                } else if u == IConsoleSwiftParser.uartTx.lowercased() {
                    peripheral.setNotifyValue(true, for: char)
                } else if u == IConsoleSwiftParser.uartRx.lowercased() {
                    self.iconsoleRx = char
                    self.startIConsolePolling(peripheral: peripheral, rx: char)
                }
            }
        }
    }

    nonisolated func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        Task { @MainActor in
            guard let data = characteristic.value else { return }
            let hex = data.map { String(format: "%02x", $0) }.joined(separator: " ")
            self.appendDiag("RAW \(characteristic.uuid.uuidString) \(hex)")
            let lower = characteristic.uuid.uuidString.lowercased()
            if lower.contains("2acd"), let parsed = FtmsTreadmillParser.parse(data) {
                self.apply(parsed)
            } else if lower.contains("2a53"), let parsed = RscMeasurementSwiftParser.parse(data) {
                self.apply(parsed)
            } else if lower == IConsoleSwiftParser.uartTx.lowercased(),
                      let parsed = IConsoleSwiftParser.parseTelemetry(data) {
                self.apply(parsed)
            }
        }
    }

    private func startIConsolePolling(peripheral: CBPeripheral, rx: CBCharacteristic) {
        iconsolePollTask?.cancel()
        iconsolePollTask = Task { @MainActor in
            peripheral.writeValue(IConsoleSwiftParser.ping(), for: rx, type: .withResponse)
            while !Task.isCancelled {
                peripheral.writeValue(IConsoleSwiftParser.read(), for: rx, type: .withResponse)
                try? await Task.sleep(nanoseconds: 500_000_000)
                peripheral.writeValue(IConsoleSwiftParser.ping(), for: rx, type: .withoutResponse)
            }
        }
    }
}
