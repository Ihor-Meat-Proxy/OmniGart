import Foundation
import HealthKit
import Shared

struct MindfulBodySaveRequest {
    var kind: MindfulBodySessionView.Kind
    var profile: UserProfile
    var core: OmniGartCore
    var start: Date
    var end: Date
    var sessionKey: String
    var saveMindfulMinutes: Bool
}

@MainActor
enum MindfulBodyHealthSave {
    static func saveEstimate(_ request: MindfulBodySaveRequest) async -> HealthKitWorkoutWriter.Result? {
        let age = Int(request.core.ageYears(profile: request.profile))
        let durationHours = max(0, request.end.timeIntervalSince(request.start) / 3600.0)
        let energy = estimatedEnergy(
            kind: request.kind,
            profile: request.profile,
            age: age,
            durationHours: durationHours
        )
        return await persist(request: request, energy: energy, age: age)
    }

    private static func estimatedEnergy(
        kind: MindfulBodySessionView.Kind,
        profile: UserProfile,
        age: Int,
        durationHours: Double
    ) -> HealthKitWorkoutWriter.EnergyInput {
        let breakdown: WorkoutEnergyBreakdown
        if kind == .yoga {
            breakdown = WorkoutEnergyEstimate.shared.yogaBreakdown(
                weightKg: profile.weightKg,
                heightCm: profile.heightCm,
                ageYears: Int32(age),
                sex: profile.biologicalSex,
                durationHours: durationHours
            )
        } else {
            breakdown = WorkoutEnergyEstimate.shared.pilatesBreakdown(
                weightKg: profile.weightKg,
                heightCm: profile.heightCm,
                ageYears: Int32(age),
                sex: profile.biologicalSex,
                durationHours: durationHours
            )
        }
        return HealthKitWorkoutWriter.EnergyInput(
            activeKcal: breakdown.activeKcal,
            basalKcal: breakdown.basalKcal,
            isEstimate: true
        )
    }

    private static func persist(
        request: MindfulBodySaveRequest,
        energy: HealthKitWorkoutWriter.EnergyInput,
        age: Int
    ) async -> HealthKitWorkoutWriter.Result? {
        let heartRate = await WorkoutHeartRateSummary.load(start: request.start, end: request.end)
        let syncId = recordLocalWorkout(request: request, energy: energy, heartRate: heartRate)
        do {
            let saved = try await saveToHealthKit(
                request: request,
                energy: energy,
                age: age,
                heartRate: heartRate,
                syncId: syncId
            )
            if let saved, !saved.skippedBecauseAlreadyExists {
                request.core.localHealth.markPlatformLinked(
                    localId: request.sessionKey,
                    platformRecordId: saved.workoutUUID.uuidString
                )
            }
            return saved
        } catch {
            return HealthKitWorkoutWriter.Result(
                workoutUUID: UUID(),
                activeKcal: energy.activeKcal,
                basalKcal: energy.basalKcal,
                averageHeartRateBpm: nil,
                maxHeartRateBpm: nil,
                energyIsEstimate: true,
                effortScore: nil,
                heartRateRecoveryBpm: nil
            )
        }
    }

    private static func recordLocalWorkout(
        request: MindfulBodySaveRequest,
        energy: HealthKitWorkoutWriter.EnergyInput,
        heartRate: WorkoutHeartRateSummary
    ) -> String {
        request.core.localHealth.recordOmniGartWorkout(
            localId: request.sessionKey,
            activityTypeName: request.kind == .yoga ? "YOGA" : "PILATES",
            startEpochMs: Int64(request.start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(request.end.timeIntervalSince1970 * 1000),
            activeEnergyKcal: KotlinDouble(value: energy.activeKcal),
            basalEnergyKcal: KotlinDouble(value: energy.basalKcal),
            distanceMetres: nil,
            averageHeartRateBpm: heartRate.averageBpm.map { KotlinDouble(value: $0) },
            maxHeartRateBpm: heartRate.maxBpm.map { KotlinDouble(value: $0) },
            sessionId: nil,
            planId: nil,
            heartRateEpochMs: heartRate.during.map { Int64($0.date.timeIntervalSince1970 * 1000) },
            heartRateBpm: heartRate.during.map(\.bpm),
            syncVersion: 1
        )
    }

    private static func saveToHealthKit(
        request: MindfulBodySaveRequest,
        energy: HealthKitWorkoutWriter.EnergyInput,
        age: Int,
        heartRate: WorkoutHeartRateSummary,
        syncId: String
    ) async throws -> HealthKitWorkoutWriter.Result? {
        try await HealthKitWorkoutWriter.save(
            options: .init(
                activityType: request.kind.activityType,
                start: request.start,
                end: request.end,
                energy: energy,
                syncIdentifier: syncId,
                heartRate: heartRate.during,
                indoor: true,
                saveMindfulMinutes: request.saveMindfulMinutes,
                estimatedEffortScore: HealthKitWorkoutWriter.estimateEffortScore(
                    averageBpm: heartRate.averageBpm,
                    ageYears: age
                ),
                heartRateRecoveryBpm: HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
                    samples: heartRate.allSamples,
                    workoutEnd: request.end
                )
            )
        )
    }
}
