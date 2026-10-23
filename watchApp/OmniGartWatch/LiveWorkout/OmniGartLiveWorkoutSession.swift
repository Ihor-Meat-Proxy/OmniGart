import Foundation
import HealthKit
import WatchKit

/// Default rest between working sets (mirrors shared `DEFAULT_REST_SECONDS`).
enum OmniGartStrengthDefaults {
    static let restSeconds: Int = 90
}

/// One logged strength set for live watch session + device transfer schema v2.
struct OmniGartLoggedStrengthSet: Equatable, Sendable, Identifiable {
    var id: String
    var exerciseName: String
    var exerciseId: String
    var setNumber: Int
    var weightKg: Double
    var reps: Int
    var target: String?
    var isWarmup: Bool
}

/// Live indoor (or Zone 2) workout via `HKWorkoutSession` + `HKLiveWorkoutBuilder`.
/// Collects HR, active/basal energy, pause/resume + segment events for Apple Fitness.
/// Strength kinds also track per-set logs, rest countdown, and wrist haptics.
@MainActor
final class OmniGartLiveWorkoutSession: NSObject, ObservableObject {
    @Published private(set) var kind: OmniGartWorkoutKind
    @Published private(set) var heartRateBpm: Int = 0
    @Published var activeEnergyKcal: Double = 0
    @Published var basalEnergyKcal: Double = 0
    @Published private(set) var elapsed: TimeInterval = 0
    @Published var isRunning: Bool = false
    @Published var isPaused: Bool = false
    @Published var lastError: String?
    @Published private(set) var heartRateSamples: [Zone2HeartRateSample] = []
    @Published private(set) var segmentCount: Int = 0

    // MARK: Strength set logging
    @Published private(set) var loggedSets: [OmniGartLoggedStrengthSet] = []
    @Published private(set) var restRemaining: Int = 0
    @Published var currentExerciseName: String = "Exercise"
    @Published var currentExerciseId: String = "exercise"
    @Published private(set) var currentSetNumber: Int = 1
    @Published var weightKg: Double = 60
    @Published var reps: Int = 8
    @Published var currentTarget: String = ""
    @Published var isWarmup: Bool = false

    private let store = HKHealthStore()
    private var session: HKWorkoutSession?
    private var builder: HKLiveWorkoutBuilder?
    private var elapsedTimer: Timer?
    private var startedAt: Date?
    private var accumulatedPause: TimeInterval = 0
    private var pauseBeganAt: Date?
    private var events: [HKWorkoutEvent] = []
    private var pendingSegmentLabel: String?
    private var restWasActive: Bool = false
    private let hapticsDevice: WKInterfaceDevice
    private var syncIdentifier: String = ""

    init(kind: OmniGartWorkoutKind, hapticsDevice: WKInterfaceDevice = .current()) {
        self.kind = kind
        self.hapticsDevice = hapticsDevice
        super.init()
    }

    /// Marks the current set complete, appends to `loggedSets`, starts rest, plays success haptic.
    @discardableResult
    func logSet(
        exerciseName: String? = nil,
        exerciseId: String? = nil,
        setNumber: Int? = nil,
        weightKg: Double? = nil,
        reps: Int? = nil,
        target: String? = nil,
        isWarmup: Bool? = nil,
        restSeconds: Int = OmniGartStrengthDefaults.restSeconds
    ) -> OmniGartLoggedStrengthSet {
        let name = exerciseName ?? currentExerciseName
        let exId = exerciseId ?? currentExerciseId
        let number = setNumber ?? currentSetNumber
        let load = weightKg ?? self.weightKg
        let repCount = reps ?? self.reps
        let warmup = isWarmup ?? self.isWarmup
        let set = OmniGartLoggedStrengthSet(
            id: UUID().uuidString,
            exerciseName: name,
            exerciseId: exId,
            setNumber: number,
            weightKg: load,
            reps: repCount,
            target: target ?? (currentTarget.isEmpty ? nil : currentTarget),
            isWarmup: warmup
        )
        loggedSets.append(set)
        currentExerciseName = name
        currentExerciseId = exId
        if !warmup {
            currentSetNumber = number + 1
        }
        startRest(seconds: restSeconds)
        hapticsDevice.play(.success)
        addSegment(label: "\(name) #\(number)")
        return set
    }

    func startRest(seconds: Int = OmniGartStrengthDefaults.restSeconds) {
        let s = max(0, seconds)
        restRemaining = s
        restWasActive = s > 0
    }

    func skipRest() {
        restRemaining = 0
        restWasActive = false
    }

    private func tickRest() {
        guard restRemaining > 0 else { return }
        restRemaining -= 1
        if restRemaining == 0, restWasActive {
            restWasActive = false
            hapticsDevice.play(.success)
        }
    }

    func start() async {
        lastError = nil
        resetMetrics()
        syncIdentifier = OmniGartWatchHealthSync.makeSyncIdentifier()
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
        if kind == .zone2Run,
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
            if kind == .zone2Run,
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
            if let builder {
                // Append-only: never finish a second copy for the same sync id.
                if await OmniGartWatchHealthSync.workoutExists(
                    syncIdentifier: syncIdentifier,
                    store: store
                ) {
                    builder.discardWorkout()
                    isRunning = false
                    isPaused = false
                    return makeSummary(endedAt: end)
                }
                if !events.isEmpty {
                    try await builder.addWorkoutEvents(events)
                }
                var metadata = OmniGartWatchHealthSync.syncMetadata(
                    syncIdentifier: syncIdentifier,
                    indoor: kind.isIndoor
                )
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
            // Phone inbox: local store by sync id; never re-write HealthKit on iPhone.
            let localId = syncIdentifier.replacingOccurrences(
                of: OmniGartWatchHealthSync.syncIdPrefix,
                with: ""
            )
            if !localId.isEmpty {
                WatchWorkoutTransferBuilder.enqueueLiveSummary(
                    summary,
                    localId: localId,
                    syncIdentifier: syncIdentifier
                )
            }
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
            endedAt: endedAt,
            sets: loggedSets
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
        loggedSets = []
        restRemaining = 0
        restWasActive = false
        currentSetNumber = 1
        weightKg = 60
        reps = 8
        currentTarget = ""
        isWarmup = false
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
                if !self.isPaused {
                    self.tickRest()
                }
            }
        }
    }

    func ingestHeartRate(_ bpm: Double, at date: Date) {
        let value = Int(bpm.rounded())
        heartRateBpm = value
        let offset = startedAt.map { date.timeIntervalSince($0) } ?? elapsed
        heartRateSamples.append(Zone2HeartRateSample(offsetSeconds: max(0, offset), bpm: value))
    }
}
