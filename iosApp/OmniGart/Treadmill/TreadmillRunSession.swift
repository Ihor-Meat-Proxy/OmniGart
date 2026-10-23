import Foundation
import OmniGartSensors
import Shared

/// Phone-side treadmill run: live BLE (or simulator) samples, reconnect, and save hooks.
@MainActor
@Observable
final class TreadmillRunSession {
    enum Source: String {
        case bluetooth
        case simulator
    }

    private(set) var source: Source = .simulator
    private(set) var connectionLabel: String = "Disconnected"
    private(set) var protocolLabel: String = "Simulated"
    private(set) var sample: TreadmillSampleView = .empty
    private(set) var isRunning = false
    private(set) var controlArmed = false
    private(set) var statusMessage: String?
    private(set) var discovered: [SensorDevice] = []

    private let ble = TreadmillBleCentral()
    private var tickTask: Task<Void, Never>?
    private var hrEpochMs: [Int64] = []
    private var hrBpm: [Double] = []
    private var startedAt: Date?
    private var preferredDeviceId: String?

    var speedText: String {
        guard let speed = sample.speedKmh else { return "—" }
        return String(format: "%.1f", speed)
    }

    var paceText: String {
        guard let pace = sample.paceMinPerKm, pace > 0 else { return "—" }
        let minutes = Int(pace)
        let seconds = Int((pace - Double(minutes)) * 60)
        return String(format: "%d:%02d", minutes, seconds)
    }

    func useSimulator() {
        stop()
        source = .simulator
        TreadmillBridge.shared.simulatorReset()
        protocolLabel = "Simulated"
        connectionLabel = "Simulator ready"
        statusMessage = String(localized: "treadmill_sim_ready")
    }

    func startBleScan() {
        source = .bluetooth
        ble.setAutoReconnect(true)
        ble.onNotification = { [weak self] note in
            Task { @MainActor in
                self?.ingestBle(note)
            }
        }
        ble.onControlResponse = { [weak self] data in
            Task { @MainActor in
                self?.ingestControlResponse(data)
            }
        }
        ble.startScan()
        ble.startOpenScan()
        connectionLabel = "Scanning"
        statusMessage = String(localized: "treadmill_scanning")
        refreshDiscovered()
    }

    func refreshDiscovered() {
        discovered = ble.discovered
        if source == .bluetooth {
            connectionLabel = ble.session.connection.rawValue
            protocolLabel = ble.protocolKindRaw
        }
    }

    func connect(deviceId: String) {
        preferredDeviceId = deviceId
        source = .bluetooth
        ble.connect(deviceId: deviceId)
        connectionLabel = "Connecting"
        statusMessage = String(localized: "treadmill_connecting")
    }

    func armControl() {
        controlArmed = true
        let payload = Data(kotlinBytes: TreadmillBridge.shared.controlRequestControl())
        if source == .simulator {
            _ = TreadmillBridge.shared.simulatorControl(payload: payload.kotlinByteArray)
            statusMessage = String(localized: "treadmill_control_armed")
            return
        }
        ble.writeControl(payload)
        statusMessage = String(localized: "treadmill_control_armed")
    }

    func startBelt() {
        guard controlArmed else {
            statusMessage = String(localized: "treadmill_arm_first")
            return
        }
        if startedAt == nil {
            startedAt = Date()
            hrEpochMs = []
            hrBpm = []
        }
        isRunning = true
        let payload = Data(kotlinBytes: TreadmillBridge.shared.controlStart())
        if source == .simulator {
            _ = TreadmillBridge.shared.simulatorControl(payload: payload.kotlinByteArray)
            startSimulatorTicks()
        } else {
            ble.writeControl(payload)
        }
        statusMessage = String(localized: "treadmill_started")
    }

    func stopBelt() {
        isRunning = false
        let payload = Data(kotlinBytes: TreadmillBridge.shared.controlStop())
        if source == .simulator {
            _ = TreadmillBridge.shared.simulatorControl(payload: payload.kotlinByteArray)
            tickTask?.cancel()
        } else {
            ble.writeControl(payload)
        }
        statusMessage = String(localized: "treadmill_stopped")
    }

    func applyPreset(id: String) {
        guard controlArmed else {
            statusMessage = String(localized: "treadmill_arm_first")
            return
        }
        if source == .simulator {
            _ = TreadmillBridge.shared.simulatorApplyPreset(id: id)
            refreshFromSimulator()
            return
        }
        if let speed = TreadmillBridge.shared.presetSpeedKmh(id: id)?.doubleValue {
            ble.writeControl(Data(kotlinBytes: TreadmillBridge.shared.controlSetSpeed(kmh: speed)))
        }
        if let incline = TreadmillBridge.shared.presetInclinePercent(id: id)?.doubleValue {
            ble.writeControl(
                Data(kotlinBytes: TreadmillBridge.shared.controlSetIncline(percent: incline))
            )
        }
    }

    func stop() {
        tickTask?.cancel()
        tickTask = nil
        isRunning = false
        if source == .bluetooth {
            ble.disconnect()
        }
        connectionLabel = "Disconnected"
    }

    func finishedSnapshot() -> TreadmillFinishSnapshot {
        let end = Date()
        let start = startedAt ?? end
        return TreadmillFinishSnapshot(
            start: start,
            end: end,
            distanceMetres: sample.distanceMetres,
            activeKcal: sample.totalEnergyKcal,
            averageInclinePercent: sample.inclinePercent,
            heartRateEpochMs: hrEpochMs,
            heartRateBpm: hrBpm,
            protocolLabel: protocolLabel
        )
    }

    private func startSimulatorTicks() {
        tickTask?.cancel()
        tickTask = Task { @MainActor in
            while !Task.isCancelled {
                refreshFromSimulator()
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }

    private func refreshFromSimulator() {
        let dto = TreadmillBridge.shared.simulatorTick()
        sample = TreadmillSampleView(from: dto)
        connectionLabel = "Simulator"
        protocolLabel = "Simulated"
        recordHr(sample.heartRateBpm)
    }

    private func ingestBle(_ note: TreadmillRawNotification) {
        refreshDiscovered()
        protocolLabel = ble.protocolKindRaw
        connectionLabel = ble.session.connection.rawValue
        let detected = TreadmillBridge.shared.detectProtocol(
            serviceUuids: ble.serviceUuids as NSArray as! [String]
        )
        if detected != "Unknown" {
            protocolLabel = detected
        }
        let payload = note.payload.kotlinByteArray
        if let dto = TreadmillBridge.shared.parseNotification(
            protocolName: protocolLabel,
            characteristicUuid: note.characteristicUuid,
            payload: payload
        ) {
            sample = TreadmillSampleView(from: dto)
            recordHr(sample.heartRateBpm)
        }
    }

    private func ingestControlResponse(_ data: Data) {
        let ok = TreadmillBridge.shared.controlParseResponseSuccess(payload: data.kotlinByteArray)
        statusMessage =
            ok
            ? String(localized: "treadmill_control_ok")
            : String(localized: "treadmill_control_failed")
    }

    private func recordHr(_ bpm: Int?) {
        guard let bpm, bpm > 0 else { return }
        hrEpochMs.append(Int64(Date().timeIntervalSince1970 * 1000))
        hrBpm.append(Double(bpm))
    }
}

struct TreadmillSampleView: Equatable {
    var speedKmh: Double?
    var inclinePercent: Double?
    var distanceMetres: Double?
    var elapsedSeconds: Int?
    var totalEnergyKcal: Double?
    var heartRateBpm: Int?
    var steps: Int?
    var cadenceSpm: Double?

    var paceMinPerKm: Double? {
        guard let speedKmh, speedKmh > 0 else { return nil }
        return 60.0 / speedKmh
    }

    static let empty = TreadmillSampleView()

    init(
        speedKmh: Double? = nil,
        inclinePercent: Double? = nil,
        distanceMetres: Double? = nil,
        elapsedSeconds: Int? = nil,
        totalEnergyKcal: Double? = nil,
        heartRateBpm: Int? = nil,
        steps: Int? = nil,
        cadenceSpm: Double? = nil
    ) {
        self.speedKmh = speedKmh
        self.inclinePercent = inclinePercent
        self.distanceMetres = distanceMetres
        self.elapsedSeconds = elapsedSeconds
        self.totalEnergyKcal = totalEnergyKcal
        self.heartRateBpm = heartRateBpm
        self.steps = steps
        self.cadenceSpm = cadenceSpm
    }

    init(from dto: TreadmillSampleDto) {
        speedKmh = dto.speedKmh.isNaN ? nil : dto.speedKmh
        inclinePercent = dto.inclinePercent.isNaN ? nil : dto.inclinePercent
        distanceMetres = dto.distanceMetres.isNaN ? nil : dto.distanceMetres
        elapsedSeconds = dto.elapsedSeconds >= 0 ? Int(dto.elapsedSeconds) : nil
        totalEnergyKcal = dto.totalEnergyKcal.isNaN ? nil : dto.totalEnergyKcal
        heartRateBpm = dto.heartRateBpm > 0 ? Int(dto.heartRateBpm) : nil
        steps = dto.steps >= 0 ? Int(dto.steps) : nil
        cadenceSpm = dto.cadenceSpm.isNaN ? nil : dto.cadenceSpm
    }
}

struct TreadmillFinishSnapshot: Sendable {
    var start: Date
    var end: Date
    var distanceMetres: Double?
    var activeKcal: Double?
    var averageInclinePercent: Double?
    var heartRateEpochMs: [Int64]
    var heartRateBpm: [Double]
    var protocolLabel: String
}

private extension Data {
    init(kotlinBytes: KotlinByteArray) {
        var out = [UInt8]()
        let size = Int(kotlinBytes.size)
        out.reserveCapacity(size)
        var i: Int32 = 0
        while i < kotlinBytes.size {
            out.append(UInt8(bitPattern: kotlinBytes.get(index: i)))
            i += 1
        }
        self.init(out)
    }

    var kotlinByteArray: KotlinByteArray {
        let result = KotlinByteArray(size: Int32(count))
        enumerated().forEach { index, byte in
            result.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return result
    }
}
