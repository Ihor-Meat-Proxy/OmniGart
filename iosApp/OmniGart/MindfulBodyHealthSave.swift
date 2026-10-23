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
        do {
            let hrContext = await heartRateContext(for: request)
            let syncId = recordLocalWorkout(
                request: request,
                energy: energy,
                during: hrContext.during,
                avgBpm: hrContext.avgBpm,
                maxBpm: hrContext.maxBpm
            )
            let saved = try await saveToHealthKit(
                request: request,
                energy: energy,
                age: age,
                syncId: syncId,
                hrContext: hrContext
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

    private struct HeartRateContext {
        var samples: [HealthKitWorkoutWriter.HeartRatePoint]
        var during: [HealthKitWorkoutWriter.HeartRatePoint]
        var avgBpm: Double?
        var maxBpm: Double?
    }

    private static func heartRateContext(for request: MindfulBodySaveRequest) async -> HeartRateContext {
        let samples =
            (try? await HealthKitWorkoutWriter.fetchHeartRate(
                from: request.start,
                to: request.end.addingTimeInterval(90)
            )) ?? []
        let during = samples.filter { $0.date <= request.end }
        let avg = during.map(\.bpm).filter { $0 > 0 }
        let avgBpm = avg.isEmpty ? nil : avg.reduce(0, +) / Double(avg.count)
        return HeartRateContext(
            samples: samples,
            during: during,
            avgBpm: avgBpm,
            maxBpm: avg.max()
        )
    }

    private static func recordLocalWorkout(
        request: MindfulBodySaveRequest,
        energy: HealthKitWorkoutWriter.EnergyInput,
        during: [HealthKitWorkoutWriter.HeartRatePoint],
        avgBpm: Double?,
        maxBpm: Double?
    ) -> String {
        let activityName = request.kind == .yoga ? "YOGA" : "PILATES"
        return request.core.localHealth.recordOmniGartWorkout(
            localId: request.sessionKey,
            activityTypeName: activityName,
            startEpochMs: Int64(request.start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(request.end.timeIntervalSince1970 * 1000),
            activeEnergyKcal: KotlinDouble(value: energy.activeKcal),
            basalEnergyKcal: KotlinDouble(value: energy.basalKcal),
            distanceMetres: nil,
            averageHeartRateBpm: avgBpm.map { KotlinDouble(value: $0) },
            maxHeartRateBpm: maxBpm.map { KotlinDouble(value: $0) },
            sessionId: nil,
            planId: nil,
            heartRateEpochMs: during.map { Int64($0.date.timeIntervalSince1970 * 1000) },
            heartRateBpm: during.map(\.bpm),
            syncVersion: 1
        )
    }

    private static func saveToHealthKit(
        request: MindfulBodySaveRequest,
        energy: HealthKitWorkoutWriter.EnergyInput,
        age: Int,
        syncId: String,
        hrContext: HeartRateContext
    ) async throws -> HealthKitWorkoutWriter.Result? {
        try await HealthKitWorkoutWriter.save(
            options: .init(
                activityType: request.kind.activityType,
                start: request.start,
                end: request.end,
                energy: energy,
                syncIdentifier: syncId,
                heartRate: hrContext.during,
                indoor: true,
                saveMindfulMinutes: request.saveMindfulMinutes,
                estimatedEffortScore: HealthKitWorkoutWriter.estimateEffortScore(
                    averageBpm: hrContext.avgBpm,
                    ageYears: age
                ),
                heartRateRecoveryBpm: HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
                    samples: hrContext.samples,
                    workoutEnd: request.end
                )
            )
        )
    }
}
