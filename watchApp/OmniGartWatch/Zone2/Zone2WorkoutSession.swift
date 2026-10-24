import Foundation
import HealthKit

/// Runs an outdoor run via `HKWorkoutSession` / `HKLiveWorkoutBuilder` and publishes
/// glanceable metrics for the Zone 2 live screen.
@MainActor
final class Zone2WorkoutSession: NSObject, ObservableObject {
    @Published private(set) var heartRateBpm: Int = 0
    @Published private(set) var distanceMetres: Double = 0
    @Published private(set) var cadenceSpm: Double = 0
    @Published private(set) var elapsed: TimeInterval = 0
    @Published private(set) var timeInZone: TimeInterval = 0
    @Published private(set) var zoneState: Zone2ZoneState = .below
    @Published private(set) var isRunning: Bool = false
    @Published private(set) var lastError: String?
    /// HR series for the post-run chart (offset from session start).
    @Published private(set) var heartRateSamples: [Zone2HeartRateSample] = []
    /// Mean of recorded cadence readings (spm); updated as steps arrive.
    @Published private(set) var averageCadenceSpm: Double = 0
    /// Active energy from `HKLiveWorkoutDataSource` (kilocalories).
    @Published private(set) var activeEnergyKcal: Double = 0
    /// Basal energy when HealthKit provides it (kilocalories).
    @Published private(set) var basalEnergyKcal: Double = 0

    let band: Zone2Band
    private let haptics: Zone2HapticsController
    private let store = HKHealthStore()
    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?
    private var elapsedTimer: Timer?
    private var startedAt: Date?
    private var lastSampleAt: Date?
    private var cadenceReadings: [Double] = []
    private var syncIdentifier: String = ""

    init(band: Zone2Band, haptics: Zone2HapticsController = Zone2HapticsController()) {
        self.band = band
        self.haptics = haptics
        super.init()
    }

    func start() async {
        lastError = nil
        resetMetrics()
        syncIdentifier = OmniGartWatchHealthSync.makeSyncIdentifier()
        guard HKHealthStore.isHealthDataAvailable() else {
            lastError = String(localized: String.LocalizationValue("watch.zone2.error.health_unavailable"))
            return
        }
        guard let quantityTypes = zone2QuantityTypes() else {
            lastError = String(localized: String.LocalizationValue("watch.zone2.error.health_unavailable"))
            return
        }
        do {
            try await beginLiveRun(quantityTypes: quantityTypes)
        } catch {
            lastError = error.localizedDescription
            isRunning = false
        }
    }

    private struct Zone2QuantityTypes {
        var heart: HKQuantityType
        var distance: HKQuantityType
        var step: HKQuantityType
        var active: HKQuantityType
        var basal: HKQuantityType
    }

    private func zone2QuantityTypes() -> Zone2QuantityTypes? {
        guard
            let heartRateType = HKQuantityType.quantityType(forIdentifier: .heartRate),
            let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning),
            let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount),
            let activeEnergyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned),
            let basalEnergyType = HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned)
        else {
            return nil
        }
        return Zone2QuantityTypes(
            heart: heartRateType,
            distance: distanceType,
            step: stepType,
            active: activeEnergyType,
            basal: basalEnergyType
        )
    }

    private func beginLiveRun(quantityTypes: Zone2QuantityTypes) async throws {
        let types: Set<HKSampleType> = [
            quantityTypes.heart,
            quantityTypes.distance,
            quantityTypes.step,
            quantityTypes.active,
            quantityTypes.basal,
            HKObjectType.workoutType()
        ]
        try await store.requestAuthorization(toShare: types, read: types)
        let config = HKWorkoutConfiguration()
        config.activityType = .running
        config.locationType = .outdoor
        let session = try HKWorkoutSession(healthStore: store, configuration: config)
        let builder = session.associatedWorkoutBuilder()
        let dataSource = HKLiveWorkoutDataSource(healthStore: store, workoutConfiguration: config)
        dataSource.enableCollection(for: quantityTypes.active, predicate: nil)
        dataSource.enableCollection(for: quantityTypes.basal, predicate: nil)
        dataSource.enableCollection(for: quantityTypes.heart, predicate: nil)
        dataSource.enableCollection(for: quantityTypes.distance, predicate: nil)
        builder.dataSource = dataSource
        session.delegate = self
        builder.delegate = self
        self.session = session
        self.builder = builder
        let start = Date()
        session.startActivity(with: start)
        try await builder.beginCollection(at: start)
        startedAt = start
        isRunning = true
        startElapsedTimer()
        session.startMirroringToCompanionDevice { _, _ in }
    }

    /// Ends the workout. On success returns a summary for the post-run screen.
    /// Append-only: discards the builder if this sync id was already saved.
    @discardableResult
    func stop() async -> Zone2Summary? {
        elapsedTimer?.invalidate()
        elapsedTimer = nil
        session?.end()
        do {
            if let builder {
                if await OmniGartWatchHealthSync.workoutExists(
                    syncIdentifier: syncIdentifier,
                    store: store
                ) {
                    builder.discardWorkout()
                    isRunning = false
                    return makeSummary()
                }
                try await builder.addMetadata(
                    OmniGartWatchHealthSync.syncMetadata(
                        syncIdentifier: syncIdentifier,
                        indoor: false
                    )
                )
                let end = Date()
                try await builder.endCollection(at: end)
                _ = try await builder.finishWorkout()
                isRunning = false
                let summary = makeSummary()
                let localId = syncIdentifier.replacingOccurrences(
                    of: OmniGartWatchHealthSync.syncIdPrefix,
                    with: ""
                )
                if !localId.isEmpty {
                    WatchWorkoutTransferBuilder.enqueueZone2Summary(
                        summary,
                        localId: localId,
                        syncIdentifier: syncIdentifier,
                        startedAt: startedAt ?? end.addingTimeInterval(-summary.elapsed),
                        endedAt: end
                    )
                }
                return summary
            }
            isRunning = false
            return makeSummary()
        } catch {
            lastError = error.localizedDescription
            isRunning = false
            return nil
        }
    }

    func makeSummary() -> Zone2Summary {
        let bpms = heartRateSamples.map(\.bpm)
        return Zone2Summary(
            timeInZone: timeInZone,
            distanceMetres: distanceMetres,
            averageCadenceSpm: averageCadenceSpm,
            elapsed: elapsed,
            heartRateSamples: heartRateSamples,
            activeEnergyKcal: activeEnergyKcal,
            basalEnergyKcal: basalEnergyKcal,
            averageHeartRateBpm: bpms.isEmpty
                ? 0
                : Double(bpms.reduce(0, +)) / Double(bpms.count),
            maxHeartRateBpm: bpms.max().map(Double.init) ?? 0
        )
    }

    private func resetMetrics() {
        heartRateBpm = 0
        distanceMetres = 0
        cadenceSpm = 0
        averageCadenceSpm = 0
        elapsed = 0
        timeInZone = 0
        zoneState = .below
        heartRateSamples = []
        cadenceReadings = []
        activeEnergyKcal = 0
        basalEnergyKcal = 0
        startedAt = nil
        lastSampleAt = nil
    }

    private func startElapsedTimer() {
        elapsedTimer?.invalidate()
        elapsedTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self, let sessionStart = self.startedAt else { return }
                self.elapsed = Date().timeIntervalSince(sessionStart)
            }
        }
    }

    func setHapticsEnabled(_ enabled: Bool) {
        var hapticSettings = haptics.settings
        hapticSettings.enabled = enabled
        haptics.updateSettings(hapticSettings)
    }

    private func ingestHeartRate(_ bpm: Double, at date: Date) {
        let value = Int(bpm.rounded())
        heartRateBpm = value
        if let last = lastSampleAt, zoneState == .inTarget {
            timeInZone += max(0, date.timeIntervalSince(last))
        }
        zoneState = haptics.state(bpm: value, band: band)
        lastSampleAt = date
        let offset = startedAt.map { date.timeIntervalSince($0) } ?? elapsed
        heartRateSamples.append(Zone2HeartRateSample(offsetSeconds: max(0, offset), bpm: value))
        haptics.process(bpm: value, band: band, now: date)
    }

    private func recordCadence(_ spm: Double) {
        cadenceSpm = spm
        guard spm > 0 else { return }
        cadenceReadings.append(spm)
        averageCadenceSpm = Zone2Summary.averageCadence(samples: cadenceReadings)
    }
}

extension Zone2WorkoutSession: HKWorkoutSessionDelegate {
    nonisolated func workoutSession(
        _ workoutSession: HKWorkoutSession,
        didChangeTo toState: HKWorkoutSessionState,
        from fromState: HKWorkoutSessionState,
        date: Date
    ) {
        Task { @MainActor in
            if toState == .ended || toState == .stopped {
                isRunning = false
            }
        }
    }

    nonisolated func workoutSession(_ workoutSession: HKWorkoutSession, didFailWithError error: Error) {
        Task { @MainActor in
            lastError = error.localizedDescription
            isRunning = false
        }
    }
}

extension Zone2WorkoutSession: HKLiveWorkoutBuilderDelegate {
    nonisolated func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) {}

    nonisolated func workoutBuilder(
        _ workoutBuilder: HKLiveWorkoutBuilder,
        didCollectDataOf collectedTypes: Set<HKSampleType>
    ) {
        Task { @MainActor in
            let heartRateType = HKQuantityType.quantityType(forIdentifier: .heartRate)
            let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning)
            let stepType = HKQuantityType.quantityType(forIdentifier: .stepCount)
            let activeEnergyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)
            let basalEnergyType = HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned)
            for type in collectedTypes {
                guard let quantityType = type as? HKQuantityType else { continue }
                guard let stats = workoutBuilder.statistics(for: quantityType) else { continue }
                // Compare quantity types directly — `quantityTypeIdentifier` is unavailable
                // on some watchOS SDK overlays used by CI / newer Xcode.
                if quantityType == heartRateType {
                    let unit = HKUnit.count().unitDivided(by: .minute())
                    if let heartRate = stats.mostRecentQuantity()?.doubleValue(for: unit) {
                        ingestHeartRate(heartRate, at: Date())
                    }
                } else if quantityType == distanceType {
                    distanceMetres = stats.sumQuantity()?.doubleValue(for: .meter()) ?? distanceMetres
                } else if quantityType == stepType {
                    // Rough cadence: steps in last rolling window approximated from total / elapsed.
                    if elapsed > 10 {
                        let steps = stats.sumQuantity()?.doubleValue(for: .count()) ?? 0
                        recordCadence((steps / elapsed) * 60.0)
                    }
                } else if quantityType == activeEnergyType {
                    activeEnergyKcal =
                        stats.sumQuantity()?.doubleValue(for: .kilocalorie()) ?? activeEnergyKcal
                } else if quantityType == basalEnergyType {
                    basalEnergyKcal =
                        stats.sumQuantity()?.doubleValue(for: .kilocalorie()) ?? basalEnergyKcal
                }
            }
        }
    }
}
