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
            let hrSamples =
                (try? await HealthKitWorkoutWriter.fetchHeartRate(
                    from: request.start,
                    to: request.end.addingTimeInterval(90)
                )) ?? []
            let during = hrSamples.filter { $0.date <= request.end }
            let avg = during.map(\.bpm).filter { $0 > 0 }
            let avgBpm = avg.isEmpty ? nil : avg.reduce(0, +) / Double(avg.count)
            return try await HealthKitWorkoutWriter.save(
                options: .init(
                    activityType: request.kind.activityType,
                    start: request.start,
                    end: request.end,
                    energy: energy,
                    syncIdentifier: HealthKitWorkoutWriter.syncIdentifier(
                        forSessionId: request.sessionKey
                    ),
                    heartRate: during,
                    indoor: true,
                    saveMindfulMinutes: request.saveMindfulMinutes,
                    estimatedEffortScore: HealthKitWorkoutWriter.estimateEffortScore(
                        averageBpm: avgBpm,
                        ageYears: age
                    ),
                    heartRateRecoveryBpm: HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
                        samples: hrSamples,
                        workoutEnd: request.end
                    )
                )
            )
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
}
