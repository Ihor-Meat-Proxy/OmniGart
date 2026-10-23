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

    let band: Zone2Band
    private let haptics: Zone2HapticsController
    private let store = HKHealthStore()
    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?
    private var elapsedTimer: Timer?
    private var startedAt: Date?
    private var lastSampleAt: Date?
    private var cadenceReadings: [Double] = []

    init(band: Zone2Band, haptics: Zone2HapticsController = Zone2HapticsController()) {
        self.band = band
        self.haptics = haptics
        super.init()
    }

    func start() async {
        lastError = nil
        resetMetrics()
        guard HKHealthStore.isHealthDataAvailable() else {
            lastError = String(localized: String.LocalizationValue("watch.zone2.error.health_unavailable"))
            return
        }
        let types: Set<HKSampleType> = [
            HKQuantityType.quantityType(forIdentifier: .heartRate)!,
            HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning)!,
            HKQuantityType.quantityType(forIdentifier: .stepCount)!,
            HKObjectType.workoutType()
        ]
        do {
            try await store.requestAuthorization(toShare: types, read: types)
            let config = HKWorkoutConfiguration()
            config.activityType = .running
            config.locationType = .outdoor
            let session = try HKWorkoutSession(healthStore: store, configuration: config)
            let builder = session.associatedWorkoutBuilder()
            builder.dataSource = HKLiveWorkoutDataSource(healthStore: store, workoutConfiguration: config)
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
        } catch {
            lastError = error.localizedDescription
            isRunning = false
        }
    }

    /// Ends the workout. On success returns a summary for the post-run screen.
    @discardableResult
    func stop() async -> Zone2Summary? {
        elapsedTimer?.invalidate()
        elapsedTimer = nil
        session?.end()
        do {
            if let builder {
                try await builder.endCollection(at: Date())
                _ = try await builder.finishWorkout()
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
        Zone2Summary(
            timeInZone: timeInZone,
            distanceMetres: distanceMetres,
            averageCadenceSpm: averageCadenceSpm,
            elapsed: elapsed,
            heartRateSamples: heartRateSamples
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
        startedAt = nil
        lastSampleAt = nil
    }

    private func startElapsedTimer() {
        elapsedTimer?.invalidate()
        elapsedTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in
                guard let self, let startedAt else { return }
                self.elapsed = Date().timeIntervalSince(startedAt)
            }
        }
    }

    func setHapticsEnabled(_ enabled: Bool) {
        var s = haptics.settings
        s.enabled = enabled
        haptics.updateSettings(s)
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
            for type in collectedTypes {
                guard let quantityType = type as? HKQuantityType else { continue }
                guard let stats = workoutBuilder.statistics(for: quantityType) else { continue }
                switch quantityType.quantityTypeIdentifier {
                case .heartRate:
                    let unit = HKUnit.count().unitDivided(by: .minute())
                    if let v = stats.mostRecentQuantity()?.doubleValue(for: unit) {
                        ingestHeartRate(v, at: Date())
                    }
                case .distanceWalkingRunning:
                    distanceMetres = stats.sumQuantity()?.doubleValue(for: .meter()) ?? distanceMetres
                case .stepCount:
                    // Rough cadence: steps in last rolling window approximated from total / elapsed.
                    if elapsed > 10 {
                        let steps = stats.sumQuantity()?.doubleValue(for: .count()) ?? 0
                        recordCadence((steps / elapsed) * 60.0)
                    }
                default:
                    break
                }
            }
        }
    }
}
