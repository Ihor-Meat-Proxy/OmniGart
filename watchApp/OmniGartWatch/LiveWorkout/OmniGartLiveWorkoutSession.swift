import Foundation
import HealthKit

/// Live indoor (or Zone 2) workout via `HKWorkoutSession` + `HKLiveWorkoutBuilder`.
/// Collects HR, active/basal energy, pause/resume + segment events for Apple Fitness.
@MainActor
final class OmniGartLiveWorkoutSession: NSObject, ObservableObject {
    @Published private(set) var kind: OmniGartWorkoutKind
    @Published private(set) var heartRateBpm: Int = 0
    @Published private(set) var activeEnergyKcal: Double = 0
    @Published private(set) var basalEnergyKcal: Double = 0
    @Published private(set) var elapsed: TimeInterval = 0
    @Published private(set) var isRunning: Bool = false
    @Published private(set) var isPaused: Bool = false
    @Published private(set) var lastError: String?
    @Published private(set) var heartRateSamples: [Zone2HeartRateSample] = []
    @Published private(set) var segmentCount: Int = 0

    private let store = HKHealthStore()
    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?
    private var elapsedTimer: Timer?
    private var startedAt: Date?
    private var accumulatedPause: TimeInterval = 0
    private var pauseBeganAt: Date?
    private var events: [HKWorkoutEvent] = []
    private var pendingSegmentLabel: String?

    init(kind: OmniGartWorkoutKind) {
        self.kind = kind
        super.init()
    }

    func start() async {
        lastError = nil
        resetMetrics()
        guard HKHealthStore.isHealthDataAvailable() else {
            lastError = String(localized: String.LocalizationValue("watch.zone2.error.health_unavailable"))
            return
        }
        guard
            let heartRateType = HKQuantityType.quantityType(forIdentifier: .heartRate),
            let activeEnergyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned),
            let basalEnergyType = HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned)
        else {
            lastError = String(localized: String.LocalizationValue("watch.zone2.error.health_unavailable"))
            return
        }

        var types: Set<HKSampleType> = [
            heartRateType,
            activeEnergyType,
            basalEnergyType,
            HKObjectType.workoutType()
        ]
        if kind == .zone2Run || kind == .treadmillRun,
           let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) {
            types.insert(distanceType)
        }

        do {
            try await store.requestAuthorization(toShare: types, read: types)
            let config = kind.makeConfiguration()
            let session = try HKWorkoutSession(healthStore: store, configuration: config)
            let builder = session.associatedWorkoutBuilder()
            let dataSource = HKLiveWorkoutDataSource(healthStore: store, workoutConfiguration: config)
            dataSource.enableCollection(for: heartRateType, predicate: nil)
            dataSource.enableCollection(for: activeEnergyType, predicate: nil)
            dataSource.enableCollection(for: basalEnergyType, predicate: nil)
            if kind == .zone2Run || kind == .treadmillRun,
               let distanceType = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning) {
                dataSource.enableCollection(for: distanceType, predicate: nil)
            }
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
            isPaused = false
            startElapsedTimer()
        } catch {
            lastError = error.localizedDescription
            isRunning = false
        }
    }

    func pause() {
        guard isRunning, !isPaused else { return }
        session?.pause()
        pauseBeganAt = Date()
        isPaused = true
        let at = Date()
        events.append(HKWorkoutEvent(type: .pause, dateInterval: DateInterval(start: at, duration: 1)))
    }

    func resume() {
        guard isRunning, isPaused else { return }
        session?.resume()
        if let began = pauseBeganAt {
            accumulatedPause += Date().timeIntervalSince(began)
        }
        pauseBeganAt = nil
        isPaused = false
        let at = Date()
        events.append(HKWorkoutEvent(type: .resume, dateInterval: DateInterval(start: at, duration: 1)))
    }

    /// Marks a segment boundary (exercise / pose block).
    func addSegment(label: String) {
        guard isRunning else { return }
        let at = Date()
        let metadata: [String: Any] = [
            "OmniGartSegmentLabel": label
        ]
        events.append(
            HKWorkoutEvent(
                type: .segment,
                dateInterval: DateInterval(start: at, duration: 1),
                metadata: metadata
            )
        )
        segmentCount += 1
        pendingSegmentLabel = label
    }

    @discardableResult
    func stop() async -> OmniGartLiveWorkoutSummary? {
        elapsedTimer?.invalidate()
        elapsedTimer = nil
        if isPaused, let began = pauseBeganAt {
            accumulatedPause += Date().timeIntervalSince(began)
            pauseBeganAt = nil
        }
        let end = Date()
        session?.end()
        do {
            let localId = UUID().uuidString
            let syncId = "omnigart.workout.\(localId)"
            if let builder {
                if !events.isEmpty {
                    try await builder.addWorkoutEvents(events)
                }
                var metadata: [String: Any] = [
                    HKMetadataKeyIndoorWorkout: kind.isIndoor,
                    HKMetadataKeySyncIdentifier: syncId,
                    HKMetadataKeySyncVersion: 1
                ]
                if let label = pendingSegmentLabel {
                    metadata["OmniGartLastSegment"] = label
                }
                try await builder.addMetadata(metadata)
                try await builder.endCollection(at: end)
                _ = try await builder.finishWorkout()
            }
            isRunning = false
            isPaused = false
            let summary = makeSummary(endedAt: end)
            WatchWorkoutTransferBuilder.enqueueLiveSummary(
                summary,
                localId: localId,
                syncIdentifier: syncId
            )
            return summary
        } catch {
            lastError = error.localizedDescription
            isRunning = false
            return nil
        }
    }

    func makeSummary(endedAt: Date = Date()) -> OmniGartLiveWorkoutSummary {
        let bpms = heartRateSamples.map(\.bpm)
        return OmniGartLiveWorkoutSummary(
            kind: kind,
            elapsed: elapsed,
            activeEnergyKcal: activeEnergyKcal,
            basalEnergyKcal: basalEnergyKcal,
            averageHeartRateBpm: bpms.isEmpty
                ? 0
                : Double(bpms.reduce(0, +)) / Double(bpms.count),
            maxHeartRateBpm: bpms.max().map(Double.init) ?? 0,
            heartRateSamples: heartRateSamples,
            segmentCount: segmentCount,
            startedAt: startedAt ?? endedAt,
            endedAt: endedAt
        )
    }

    private func resetMetrics() {
        heartRateBpm = 0
        activeEnergyKcal = 0
        basalEnergyKcal = 0
        elapsed = 0
        heartRateSamples = []
        segmentCount = 0
        events = []
        pendingSegmentLabel = nil
        startedAt = nil
        accumulatedPause = 0
        pauseBeganAt = nil
    }

    private func startElapsedTimer() {
        elapsedTimer?.invalidate()
        elapsedTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self, let start = self.startedAt else { return }
                var pause = self.accumulatedPause
                if let began = self.pauseBeganAt {
                    pause += Date().timeIntervalSince(began)
                }
                self.elapsed = max(0, Date().timeIntervalSince(start) - pause)
            }
        }
    }

    private func ingestHeartRate(_ bpm: Double, at date: Date) {
        let value = Int(bpm.rounded())
        heartRateBpm = value
        let offset = startedAt.map { date.timeIntervalSince($0) } ?? elapsed
        heartRateSamples.append(Zone2HeartRateSample(offsetSeconds: max(0, offset), bpm: value))
    }
}

extension OmniGartLiveWorkoutSession: HKWorkoutSessionDelegate {
    nonisolated func workoutSession(
        _ workoutSession: HKWorkoutSession,
        didChangeTo toState: HKWorkoutSessionState,
        from fromState: HKWorkoutSessionState,
        date: Date
    ) {
        Task { @MainActor in
            switch toState {
            case .ended, .stopped:
                isRunning = false
                isPaused = false
            case .paused:
                isPaused = true
            case .running:
                isPaused = false
                isRunning = true
            default:
                break
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

extension OmniGartLiveWorkoutSession: HKLiveWorkoutBuilderDelegate {
    nonisolated func workoutBuilderDidCollectEvent(_ workoutBuilder: HKLiveWorkoutBuilder) {}

    nonisolated func workoutBuilder(
        _ workoutBuilder: HKLiveWorkoutBuilder,
        didCollectDataOf collectedTypes: Set<HKSampleType>
    ) {
        Task { @MainActor in
            let heartRateType = HKQuantityType.quantityType(forIdentifier: .heartRate)
            let activeEnergyType = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned)
            let basalEnergyType = HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned)
            for type in collectedTypes {
                guard let quantityType = type as? HKQuantityType else { continue }
                guard let stats = workoutBuilder.statistics(for: quantityType) else { continue }
                if quantityType == heartRateType {
                    let unit = HKUnit.count().unitDivided(by: .minute())
                    if let heartRate = stats.mostRecentQuantity()?.doubleValue(for: unit) {
                        ingestHeartRate(heartRate, at: Date())
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

struct OmniGartLiveWorkoutSummary: Equatable, Sendable {
    var kind: OmniGartWorkoutKind
    var elapsed: TimeInterval
    var activeEnergyKcal: Double
    var basalEnergyKcal: Double
    var averageHeartRateBpm: Double
    var maxHeartRateBpm: Double
    var heartRateSamples: [Zone2HeartRateSample]
    var segmentCount: Int
    var startedAt: Date
    var endedAt: Date

    var totalEnergyKcal: Double { activeEnergyKcal + basalEnergyKcal }
}
