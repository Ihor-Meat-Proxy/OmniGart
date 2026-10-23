import Foundation
import HealthKit
import Shared

/// Persists an indoor treadmill run or walk (with or without BLE) append-only.
enum TreadmillSessionSave {
    struct Request {
        var kind: TreadmillSessionKindSwift
        var start: Date
        var end: Date
        var live: TreadmillLiveSnapshot
        var deviceName: String?
        var calibrationMetres: Double?
        var watchEstimatedMetres: Double?
        var core: OmniGartCore
        var programId: String?
    }

    @MainActor
    // swiftlint:disable:next function_body_length
    static func save(_ request: Request) async -> String {
        var distance = request.live.distanceMetres
        var usedEstimate = false
        if distance == nil || distance == 0 {
            usedEstimate = true
            if let cal = request.calibrationMetres, cal > 0 {
                distance = cal
            } else {
                distance = request.watchEstimatedMetres
            }
        }
        let notes = TreadmillSessionNotesSwift.encode(
            TreadmillSessionNotesFields(
                protocolKind: request.live.protocolKind,
                deviceName: request.deviceName,
                distanceMetres: distance,
                averageSpeedKmh: request.live.speedKmh,
                averageInclinePercent: request.live.inclinePercent,
                machineAverageHeartRateBpm: request.live.heartRateBpm.map(Double.init),
                energyKcal: request.live.energyKcal,
                steps: request.live.steps,
                usedWatchEstimatedDistance: usedEstimate,
                calibrationFactor: nil
            )
        )
        let localId = UUID().uuidString
        let syncId = request.core.localHealth.recordOmniGartWorkout(
            localId: localId,
            activityTypeName: request.kind.activityTypeName,
            startEpochMs: Int64(request.start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(request.end.timeIntervalSince1970 * 1000),
            activeEnergyKcal: request.live.energyKcal.map { KotlinDouble(value: $0) },
            basalEnergyKcal: nil,
            distanceMetres: distance.map { KotlinDouble(value: $0) },
            averageHeartRateBpm: request.live.heartRateBpm.map { KotlinDouble(value: Double($0)) },
            maxHeartRateBpm: nil,
            sessionId: request.programId,
            planId: nil,
            heartRateEpochMs: [],
            heartRateBpm: [],
            syncVersion: 1,
            notes: notes,
            steps: request.live.steps.map { KotlinLong(value: $0) },
            sourceName: "OmniGart Treadmill"
        )
        let activity: HKWorkoutActivityType = request.kind == .walk ? .walking : .running
        do {
            _ = try await HealthKitWorkoutWriter.save(
                options: HealthKitWorkoutWriter.SaveOptions(
                    activityType: activity,
                    start: request.start,
                    end: request.end,
                    energy: .init(
                        activeKcal: request.live.energyKcal ?? 0,
                        basalKcal: 0
                    ),
                    syncIdentifier: syncId,
                    heartRate: [],
                    indoor: true,
                    distanceMetres: distance,
                    syncVersion: 1
                )
            )
            return String(localized: "treadmill_saved")
        } catch {
            return error.localizedDescription
        }
    }
}
