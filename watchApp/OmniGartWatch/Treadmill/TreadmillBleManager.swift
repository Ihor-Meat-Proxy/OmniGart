import CoreBluetooth
import Foundation

/// CoreBluetooth central for FTMS, FitShow, RSC, and iConsole BLE UART treadmills.
/// Discovery preference: FTMS → FitShow → RSC → iConsole.
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
    @Published var activeProtocol: TreadmillProtocolKind = .none

    var allowRemoteControl = false
    var watchHeartRateBpm: Int?

    private var central: CBCentralManager?
    private var peripheral: CBPeripheral?
    private var preferredId: UUID?
    var controlPoint: CBCharacteristic?
    var fitShowWrite: CBCharacteristic?
    var iconsoleRx: CBCharacteristic?
    private var reconnectTask: Task<Void, Never>?
    var iconsolePollTask: Task<Void, Never>?
    var fitShowPollTask: Task<Void, Never>?
    private var reconnectAttempt = 0
    private var diagLines: [String] = []
    private let rememberedKey = "omnigart.treadmill.remembered"
    private let hrMax: Int

    init(hrMax: Int = 190) {
        self.hrMax = hrMax
        super.init()
    }

    var controlAvailable: Bool {
        allowRemoteControl &&
            (
                (activeProtocol == .ftms && controlPoint != nil) ||
                    (activeProtocol == .fitshow && fitShowWrite != nil)
            )
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
        fitShowPollTask?.cancel()
        preferredId = nil
        activeProtocol = .none
        if let peripheral {
            central?.cancelPeripheralConnection(peripheral)
        }
        peripheral = nil
        state = .idle
        live = TreadmillLiveSnapshot()
    }

    func setTargetSpeedKmh(_ speed: Double) {
        guard allowRemoteControl, let peripheral else { return }
        if activeProtocol == .ftms, let controlPoint {
            peripheral.writeValue(FtmsTreadmillParser.requestControl(), for: controlPoint, type: .withResponse)
            peripheral.writeValue(
                FtmsTreadmillParser.setTargetSpeedKmh(speed),
                for: controlPoint,
                type: .withResponse
            )
        } else if activeProtocol == .fitshow, let fitShowWrite {
            let incline = live.inclinePercent ?? 0
            peripheral.writeValue(
                FitShowSwiftParser.setTarget(speedKmh: speed, inclinePercent: incline),
                for: fitShowWrite,
                type: .withResponse
            )
        }
        appendDiag("INFO control set speed \(speed)")
    }

    func setTargetInclinePercent(_ incline: Double) {
        guard allowRemoteControl, let peripheral else { return }
        if activeProtocol == .ftms, let controlPoint {
            peripheral.writeValue(FtmsTreadmillParser.requestControl(), for: controlPoint, type: .withResponse)
            peripheral.writeValue(
                FtmsTreadmillParser.setTargetInclinePercent(incline),
                for: controlPoint,
                type: .withResponse
            )
        } else if activeProtocol == .fitshow, let fitShowWrite {
            let speed = live.speedKmh ?? 0
            peripheral.writeValue(
                FitShowSwiftParser.setTarget(speedKmh: speed, inclinePercent: incline),
                for: fitShowWrite,
                type: .withResponse
            )
        }
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

    func appendDiag(_ line: String) {
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

    func apply(_ parsed: TreadmillParsedMetrics) {
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

extension TreadmillBleManager: CBCentralManagerDelegate {
    // CBCentralManager(queue: nil) delivers callbacks on the main queue.
    nonisolated func centralManagerDidUpdateState(_ central: CBCentralManager) {
        MainActor.assumeIsolated {
            if central.state != .poweredOn {
                state = .failed
                lastError = "Bluetooth unavailable"
            }
        }
    }

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        MainActor.assumeIsolated {
            let name = peripheral.name
                ?? (advertisementData[CBAdvertisementDataLocalNameKey] as? String)
                ?? "Treadmill"
            let lower = name.lowercased()
            let hint: String
            if lower.contains("iconsole") || lower.contains("i-console") {
                hint = "ICONSOLE"
            } else if lower.contains("fitshow") {
                hint = "FITSHOW"
            } else {
                hint = "BLE"
            }
            let device = DiscoveredDevice(id: peripheral.identifier, name: name, protocolHint: hint)
            if !discovered.contains(device) {
                discovered.append(device)
                appendDiag("INFO discovered \(name)")
            }
        }
    }

    nonisolated func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        MainActor.assumeIsolated {
            state = .connected
            reconnectAttempt = 0
            appendDiag("INFO connected")
            peripheral.discoverServices(nil)
        }
    }

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didFailToConnect peripheral: CBPeripheral,
        error: Error?
    ) {
        MainActor.assumeIsolated {
            state = .failed
            lastError = error?.localizedDescription
            scheduleReconnect()
        }
    }

    nonisolated func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: Error?
    ) {
        MainActor.assumeIsolated {
            iconsolePollTask?.cancel()
            fitShowPollTask?.cancel()
            activeProtocol = .none
            live.connected = false
            appendDiag("INFO disconnected")
            if preferredId != nil {
                scheduleReconnect()
            } else {
                state = .idle
            }
        }
    }
}
