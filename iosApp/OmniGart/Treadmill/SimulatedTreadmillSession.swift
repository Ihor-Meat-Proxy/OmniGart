import Foundation

/// In-process FTMS treadmill mirror of `shared/treadmill` [SimulatedTreadmill] for phone UI without BLE.
@MainActor
final class SimulatedTreadmillSession: ObservableObject {
    @Published private(set) var live = TreadmillLiveSnapshot()
    @Published private(set) var controlArmed = false
    @Published private(set) var beltRunning = false

    let deviceId = "sim-treadmill-ftms"
    let deviceName = "OmniGart Simulated Treadmill"

    private var running = false
    private var targetSpeedKmh = 8.0
    private var targetInclinePercent = 1.0
    private var elapsedSeconds = 0
    private var distanceMetres = 0.0
    private var totalEnergyKcal = 0.0
    private var heartRateBpm = 120
    private var steps: Int64 = 0
    private var tickTask: Task<Void, Never>?
    var watchHeartRateBpm: Int?
    private let hrMax: Int

    init(hrMax: Int = 190) {
        self.hrMax = hrMax
    }

    func reset() {
        tickTask?.cancel()
        tickTask = nil
        controlArmed = false
        beltRunning = false
        running = false
        elapsedSeconds = 0
        distanceMetres = 0
        totalEnergyKcal = 0
        steps = 0
        targetSpeedKmh = 8
        targetInclinePercent = 1
        heartRateBpm = 120
        live = TreadmillLiveSnapshot()
    }

    @discardableResult
    func attach() -> Bool {
        reset()
        return armControl()
    }

    @discardableResult
    func armControl() -> Bool {
        controlArmed = true
        publishCurrent()
        return true
    }

    @discardableResult
    func startBelt() -> Bool {
        guard controlArmed else { return false }
        running = true
        beltRunning = true
        startTicks()
        return true
    }

    @discardableResult
    func stopBelt() -> Bool {
        tickTask?.cancel()
        tickTask = nil
        running = false
        beltRunning = false
        publishCurrent()
        return true
    }

    func setTargetSpeedKmh(_ speed: Double) {
        guard controlArmed else { return }
        targetSpeedKmh = min(20, max(1, speed))
    }

    func setTargetInclinePercent(_ incline: Double) {
        guard controlArmed else { return }
        targetInclinePercent = min(15, max(0, incline))
    }

    private func startTicks() {
        tickTask?.cancel()
        tickTask = Task { @MainActor in
            while !Task.isCancelled {
                tickOnce()
                try? await Task.sleep(nanoseconds: 1_000_000_000)
            }
        }
    }

    private func tickOnce() {
        if running {
            elapsedSeconds += 1
            distanceMetres += targetSpeedKmh * 1000.0 / 3600.0
            totalEnergyKcal += (targetSpeedKmh * 1.05) / 60.0
            steps += Int64(max(0, Int((targetSpeedKmh * 1.8).rounded())))
            heartRateBpm = min(185, max(90, Int((110 + targetSpeedKmh * 4 + targetInclinePercent).rounded())))
        }
        publishCurrent()
    }

    private func publishCurrent() {
        let speed = running ? targetSpeedKmh : 0.0
        let displayHr = watchHeartRateBpm ?? heartRateBpm
        live = TreadmillLiveSnapshot(
            protocolKind: .simulated,
            connected: true,
            speedKmh: speed,
            paceMinPerKm: speed > 0 ? 60.0 / speed : nil,
            inclinePercent: targetInclinePercent,
            distanceMetres: distanceMetres,
            elapsedSeconds: elapsedSeconds,
            heartRateBpm: displayHr,
            energyKcal: totalEnergyKcal,
            steps: steps,
            zone: TreadmillHeartRateZone.zone(bpm: displayHr, hrMax: hrMax)
        )
    }
}
