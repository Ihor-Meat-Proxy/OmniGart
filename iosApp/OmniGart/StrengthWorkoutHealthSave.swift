import Foundation
import Shared

enum StrengthWorkoutHealthSave {
    static func saveEstimate(
        core: OmniGartCore,
        profileId: String,
        sessionId: String,
        includeWarmups: Bool
    ) async -> HealthKitWorkoutWriter.Result? {
        guard let profile = core.profiles.get(id: profileId) else { return nil }
        let start = dateFromEpochMillis(
            core.strengthLogging.sessionStartEpochMillis(sessionId: sessionId)
        )
        let end = dateFromEpochMillis(
            core.strengthLogging.sessionEndEpochMillis(sessionId: sessionId)
        )
        let durationHours = max(0, end.timeIntervalSince(start) / 3600.0)
        let age = Int(core.ageYears(profile: profile))
        let breakdown = WorkoutEnergyEstimate.shared.strengthBreakdown(
            weightKg: profile.weightKg,
            heightCm: profile.heightCm,
            ageYears: Int32(age),
            sex: profile.biologicalSex,
            durationHours: durationHours,
            met: WorkoutEnergyEstimate.shared.STRENGTH_MET
        )
        let energy = HealthKitWorkoutWriter.EnergyInput(
            activeKcal: breakdown.activeKcal,
            basalKcal: breakdown.basalKcal,
            isEstimate: true
        )
        let volumeSummary = core.strengthLogging.sessionSummary(
            sessionId: sessionId,
            includeWarmups: includeWarmups,
            bodyweightKg: KotlinDouble(value: profile.weightKg),
            unit: .kg
        )
        let sets = core.strengthLogging.getSession(sessionId: sessionId)?.sets ?? []
        var totalReps = 0
        for set in sets {
            if let reps = set.reps {
                totalReps += Int(reps)
            }
        }
        let strengthMeta = HealthKitWorkoutWriter.StrengthMetadata(
            totalSets: sets.count,
            totalReps: totalReps,
            totalWeightKg: volumeSummary.totalWeightLiftedDisplay
        )

        do {
            let recoveryEnd = end.addingTimeInterval(90)
            let hrSamples =
                (try? await HealthKitWorkoutWriter.fetchHeartRate(from: start, to: recoveryEnd))
                ?? []
            let avg = hrSamples.map(\.bpm).filter { $0 > 0 }
            let avgBpm = avg.isEmpty ? nil : avg.reduce(0, +) / Double(avg.count)
            let recovery = HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
                samples: hrSamples,
                workoutEnd: end
            )
            let effort = HealthKitWorkoutWriter.estimateEffortScore(
                averageBpm: avgBpm,
                ageYears: age
            )
            return try await HealthKitWorkoutWriter.save(
                options: .init(
                    activityType: .traditionalStrengthTraining,
                    start: start,
                    end: end,
                    energy: energy,
                    heartRate: hrSamples.filter { $0.date <= end },
                    strength: strengthMeta,
                    indoor: true,
                    estimatedEffortScore: effort,
                    heartRateRecoveryBpm: recovery
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

    private static func dateFromEpochMillis(_ millis: Int64) -> Date {
        Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
    }
}
