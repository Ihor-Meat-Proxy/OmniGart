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
            let hr = await loadHeartRate(request: request)
            let syncId = recordLocalWorkout(request: request, energy: energy, hr: hr)
            let saved = try await saveToHealthKit(
                request: request,
                energy: energy,
                age: age,
                syncId: syncId,
                hr: hr
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

    private struct HeartRateBundle {
        var samples: [HealthKitWorkoutWriter.HeartRatePoint]
        var during: [HealthKitWorkoutWriter.HeartRatePoint]
        var averageBpm: Double?
        var maxBpm: Double?
    }

    private static func loadHeartRate(request: MindfulBodySaveRequest) async -> HeartRateBundle {
        let samples =
            (try? await HealthKitWorkoutWriter.fetchHeartRate(
                from: request.start,
                to: request.end.addingTimeInterval(90)
            )) ?? []
        let during = samples.filter { $0.date <= request.end }
        let avg = during.map(\.bpm).filter { $0 > 0 }
        let averageBpm = avg.isEmpty ? nil : avg.reduce(0, +) / Double(avg.count)
        return HeartRateBundle(
            samples: samples,
            during: during,
            averageBpm: averageBpm,
            maxBpm: avg.max()
        )
    }

    private static func recordLocalWorkout(
        request: MindfulBodySaveRequest,
        energy: HealthKitWorkoutWriter.EnergyInput,
        hr: HeartRateBundle
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
            averageHeartRateBpm: hr.averageBpm.map { KotlinDouble(value: $0) },
            maxHeartRateBpm: hr.maxBpm.map { KotlinDouble(value: $0) },
            sessionId: nil,
            planId: nil,
            heartRateEpochMs: hr.during.map { Int64($0.date.timeIntervalSince1970 * 1000) },
            heartRateBpm: hr.during.map(\.bpm),
            syncVersion: 1
        )
    }

    private static func saveToHealthKit(
        request: MindfulBodySaveRequest,
        energy: HealthKitWorkoutWriter.EnergyInput,
        age: Int,
        syncId: String,
        hr: HeartRateBundle
    ) async throws -> HealthKitWorkoutWriter.Result? {
        try await HealthKitWorkoutWriter.save(
            options: .init(
                activityType: request.kind.activityType,
                start: request.start,
                end: request.end,
                energy: energy,
                syncIdentifier: syncId,
                heartRate: hr.during,
                indoor: true,
                saveMindfulMinutes: request.saveMindfulMinutes,
                estimatedEffortScore: HealthKitWorkoutWriter.estimateEffortScore(
                    averageBpm: hr.averageBpm,
                    ageYears: age
                ),
                heartRateRecoveryBpm: HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
                    samples: hr.samples,
                    workoutEnd: request.end
                )
            )
        )
    }
}
