import Foundation
import Shared

struct StrengthHealthSaveRequest {
    var core: OmniGartCore
    var profileId: String
    var sessionId: String
    var includeWarmups: Bool
}

enum StrengthWorkoutHealthSave {
    static func saveEstimate(_ request: StrengthHealthSaveRequest) async -> HealthKitWorkoutWriter.Result? {
        guard let profile = request.core.profiles.get(id: request.profileId) else { return nil }
        let window = sessionWindow(core: request.core, sessionId: request.sessionId)
        let energy = estimatedEnergy(
            profile: profile,
            core: request.core,
            durationHours: window.hours
        )
        let strengthMeta = strengthMetadata(
            core: request.core,
            profile: profile,
            sessionId: request.sessionId,
            includeWarmups: request.includeWarmups
        )
        return await persist(
            PersistInput(
                window: window,
                energy: energy,
                sessionId: request.sessionId,
                strengthMeta: strengthMeta,
                ageYears: Int(request.core.ageYears(profile: profile))
            )
        )
    }

    private struct PersistInput {
        var window: (start: Date, end: Date, hours: Double)
        var energy: HealthKitWorkoutWriter.EnergyInput
        var sessionId: String
        var strengthMeta: HealthKitWorkoutWriter.StrengthMetadata
        var ageYears: Int
    }

    private static func sessionWindow(
        core: OmniGartCore,
        sessionId: String
    ) -> (start: Date, end: Date, hours: Double) {
        let start = dateFromEpochMillis(
            core.strengthLogging.sessionStartEpochMillis(sessionId: sessionId)
        )
        let end = dateFromEpochMillis(
            core.strengthLogging.sessionEndEpochMillis(sessionId: sessionId)
        )
        return (start, end, max(0, end.timeIntervalSince(start) / 3600.0))
    }

    private static func estimatedEnergy(
        profile: UserProfile,
        core: OmniGartCore,
        durationHours: Double
    ) -> HealthKitWorkoutWriter.EnergyInput {
        let breakdown = WorkoutEnergyEstimate.shared.strengthBreakdown(
            weightKg: profile.weightKg,
            heightCm: profile.heightCm,
            ageYears: Int32(core.ageYears(profile: profile)),
            sex: profile.biologicalSex,
            durationHours: durationHours,
            met: WorkoutEnergyEstimate.shared.STRENGTH_MET
        )
        return HealthKitWorkoutWriter.EnergyInput(
            activeKcal: breakdown.activeKcal,
            basalKcal: breakdown.basalKcal,
            isEstimate: true
        )
    }

    private static func strengthMetadata(
        core: OmniGartCore,
        profile: UserProfile,
        sessionId: String,
        includeWarmups: Bool
    ) -> HealthKitWorkoutWriter.StrengthMetadata {
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
        return HealthKitWorkoutWriter.StrengthMetadata(
            totalSets: sets.count,
            totalReps: totalReps,
            totalWeightKg: volumeSummary.totalWeightLiftedDisplay
        )
    }

    private static func persist(_ input: PersistInput) async -> HealthKitWorkoutWriter.Result? {
        do {
            let hrSamples =
                (try? await HealthKitWorkoutWriter.fetchHeartRate(
                    from: input.window.start,
                    to: input.window.end.addingTimeInterval(90)
                )) ?? []
            let avg = hrSamples.map(\.bpm).filter { $0 > 0 }
            let avgBpm = avg.isEmpty ? nil : avg.reduce(0, +) / Double(avg.count)
            return try await HealthKitWorkoutWriter.save(
                options: .init(
                    activityType: .traditionalStrengthTraining,
                    start: input.window.start,
                    end: input.window.end,
                    energy: input.energy,
                    syncIdentifier: HealthKitWorkoutWriter.syncIdentifier(
                        forSessionId: input.sessionId
                    ),
                    heartRate: hrSamples.filter { $0.date <= input.window.end },
                    strength: input.strengthMeta,
                    indoor: true,
                    estimatedEffortScore: HealthKitWorkoutWriter.estimateEffortScore(
                        averageBpm: avgBpm,
                        ageYears: input.ageYears
                    ),
                    heartRateRecoveryBpm: HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
                        samples: hrSamples,
                        workoutEnd: input.window.end
                    )
                )
            )
        } catch {
            return HealthKitWorkoutWriter.Result(
                workoutUUID: UUID(),
                activeKcal: input.energy.activeKcal,
                basalKcal: input.energy.basalKcal,
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
