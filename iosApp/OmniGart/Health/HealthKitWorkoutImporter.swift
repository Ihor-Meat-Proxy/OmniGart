import Foundation
import HealthKit
import Shared

/// Reads completed HealthKit workouts (including system Workout app sessions) into the
/// OmniGart local-first store. Never writes back foreign-origin data.
enum HealthKitWorkoutImporter {
    struct ImportedWorkout: Sendable {
        var uuid: UUID
        var activityName: String
        var start: Date
        var end: Date
        var activeKcal: Double?
        var basalKcal: Double?
        var distanceMetres: Double?
        var averageHeartRateBpm: Double?
        var maxHeartRateBpm: Double?
        var syncIdentifier: String?
        var syncVersion: Int
        var sourceBundle: String?
        var sourceName: String?
        var heartRate: [HealthKitWorkoutWriter.HeartRatePoint]
        var planId: String?
    }

    /// Incremental import using an anchored object query.
    static func importAnchored(
        previousAnchor: Data?
    ) async throws -> (workouts: [ImportedWorkout], deletedUUIDs: [UUID], newAnchor: Data?) {
        guard HKHealthStore.isHealthDataAvailable() else {
            return ([], [], previousAnchor)
        }
        let store = HKHealthStore()
        let workoutType = HealthKitPlatformBridge.workoutType()
        return try await withCheckedThrowingContinuation { continuation in
            let query = HKAnchoredObjectQuery(
                type: workoutType,
                predicate: nil,
                anchor: decodeAnchor(previousAnchor),
                limit: HKObjectQueryNoLimit
            ) { _, samples, deleted, newAnchor, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                Task {
                    do {
                        let workouts = try await mapSamples(samples as? [HKWorkout] ?? [])
                        let deletedIds = (deleted ?? []).compactMap { $0.uuid }
                        continuation.resume(
                            returning: (workouts, deletedIds, encodeAnchor(newAnchor))
                        )
                    } catch {
                        continuation.resume(throwing: error)
                    }
                }
            }
            store.execute(query)
        }
    }

    static func persistImported(
        _ imported: ImportedWorkout,
        core: OmniGartCore,
        planId: String?
    ) {
        let localId: String = {
            if let syncId = imported.syncIdentifier, syncId.hasPrefix("omnigart.workout.") {
                return String(syncId.dropFirst("omnigart.workout.".count))
            }
            return imported.uuid.uuidString
        }()
        let syncId = imported.syncIdentifier ?? "omnigart.workout.\(localId)"
        let hrEpoch = imported.heartRate.map { Int64($0.date.timeIntervalSince1970 * 1000) }
        let hrBpm = imported.heartRate.map(\.bpm)
        core.localHealth.importPlatformWorkout(
            localId: localId,
            syncIdentifier: syncId,
            syncVersion: Int64(max(1, imported.syncVersion)),
            activityTypeName: imported.activityName,
            startEpochMs: Int64(imported.start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(imported.end.timeIntervalSince1970 * 1000),
            activeEnergyKcal: imported.activeKcal.map { KotlinDouble(value: $0) },
            basalEnergyKcal: imported.basalKcal.map { KotlinDouble(value: $0) },
            distanceMetres: imported.distanceMetres.map { KotlinDouble(value: $0) },
            averageHeartRateBpm: imported.averageHeartRateBpm.map { KotlinDouble(value: $0) },
            maxHeartRateBpm: imported.maxHeartRateBpm.map { KotlinDouble(value: $0) },
            platform: "APPLE_HEALTH",
            sourceBundle: imported.sourceBundle,
            sourceName: imported.sourceName,
            platformRecordId: imported.uuid.uuidString,
            planId: planId ?? imported.planId,
            heartRateEpochMs: hrEpoch,
            heartRateBpm: hrBpm
        )
    }

    static func syncIntoLocalStore(core: OmniGartCore) async {
        let previous = UserDefaults.standard.data(forKey: anchorDefaultsKey)
        do {
            let result = try await importAnchored(previousAnchor: previous)
            for workout in result.workouts {
                // Never write back; only mirror into OmniGart DB.
                persistImported(workout, core: core, planId: workout.planId)
            }
            if let anchor = result.newAnchor {
                UserDefaults.standard.set(anchor, forKey: anchorDefaultsKey)
                let token = anchor.base64EncodedString()
                core.localHealth.markSyncStatus(
                    platform: "APPLE_HEALTH",
                    authStatus: "AUTHORIZED",
                    changeToken: token
                )
            }
        } catch {
            core.localHealth.markSyncStatus(
                platform: "APPLE_HEALTH",
                authStatus: "DENIED",
                changeToken: nil
            )
        }
    }

    // MARK: - Private

    private static let anchorDefaultsKey = "omnigart.hk.workout.anchor"

    private static func mapSamples(_ workouts: [HKWorkout]) async throws -> [ImportedWorkout] {
        var results: [ImportedWorkout] = []
        for workout in workouts {
            let hr = try await HealthKitWorkoutWriter.fetchHeartRate(
                from: workout.startDate,
                to: workout.endDate
            )
            let active = workout.totalEnergyBurned?.doubleValue(for: .kilocalorie())
            let distance = workout.totalDistance?.doubleValue(for: .meter())
            var avgHr: Double?
            var maxHr: Double?
            if let heartType = HealthKitPlatformBridge.heartRateType(),
               let stats = workout.statistics(for: heartType) {
                let unit = HKUnit.count().unitDivided(by: .minute())
                avgHr = stats.averageQuantity()?.doubleValue(for: unit)
                maxHr = stats.maximumQuantity()?.doubleValue(for: unit)
            }
            let meta = workout.metadata ?? [:]
            let syncId = meta[HKMetadataKeySyncIdentifier] as? String
            let syncVersion = (meta[HKMetadataKeySyncVersion] as? NSNumber)?.intValue ?? 1
            let planId = meta["OmniGartPlanId"] as? String
            results.append(
                ImportedWorkout(
                    uuid: workout.uuid,
                    activityName: mapActivity(workout.workoutActivityType),
                    start: workout.startDate,
                    end: workout.endDate,
                    activeKcal: active,
                    basalKcal: nil,
                    distanceMetres: distance,
                    averageHeartRateBpm: avgHr,
                    maxHeartRateBpm: maxHr,
                    syncIdentifier: syncId,
                    syncVersion: syncVersion,
                    sourceBundle: workout.sourceRevision.source.bundleIdentifier,
                    sourceName: workout.sourceRevision.source.name,
                    heartRate: hr,
                    planId: planId
                )
            )
        }
        return results
    }

    private static func mapActivity(_ type: HKWorkoutActivityType) -> String {
        switch type {
        case .traditionalStrengthTraining, .functionalStrengthTraining: return "STRENGTH"
        case .running: return "RUNNING"
        case .cycling, .handCycling: return "CYCLING"
        case .swimming: return "SWIMMING"
        case .walking, .hiking: return "WALKING"
        case .highIntensityIntervalTraining: return "HIIT"
        case .yoga: return "YOGA"
        case .pilates: return "PILATES"
        default: return "OTHER"
        }
    }

    private static func encodeAnchor(_ anchor: HKQueryAnchor?) -> Data? {
        guard let anchor else { return nil }
        return try? NSKeyedArchiver.archivedData(withRootObject: anchor, requiringSecureCoding: true)
    }

    private static func decodeAnchor(_ data: Data?) -> HKQueryAnchor? {
        guard let data else { return nil }
        return try? NSKeyedUnarchiver.unarchivedObject(ofClass: HKQueryAnchor.self, from: data)
    }
}
