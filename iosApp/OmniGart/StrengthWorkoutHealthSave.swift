import Foundation
import Shared

struct StrengthHealthSaveRequest {
    var core: OmniGartCore
    var profileId: String
    var sessionId: String
    var includeWarmups: Bool
}

@MainActor
enum StrengthWorkoutHealthSave {
    private struct SessionWindow {
        var start: Date
        var end: Date
        var hours: Double
    }

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
                core: request.core,
                window: window,
                energy: energy,
                sessionId: request.sessionId,
                strengthMeta: strengthMeta,
                ageYears: Int(request.core.ageYears(profile: profile))
            )
        )
    }

    private struct PersistInput {
        var core: OmniGartCore
        var window: SessionWindow
        var energy: HealthKitWorkoutWriter.EnergyInput
        var sessionId: String
        var strengthMeta: HealthKitWorkoutWriter.StrengthMetadata
        var ageYears: Int
    }

    private static func sessionWindow(
        core: OmniGartCore,
        sessionId: String
    ) -> SessionWindow {
        let start = dateFromEpochMillis(
            core.strengthLogging.sessionStartEpochMillis(sessionId: sessionId)
        )
        let end = dateFromEpochMillis(
            core.strengthLogging.sessionEndEpochMillis(sessionId: sessionId)
        )
        return SessionWindow(
            start: start,
            end: end,
            hours: max(0, end.timeIntervalSince(start) / 3600.0)
        )
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
            let hrContext = await heartRateContext(for: input.window)
            let syncId = recordLocalWorkout(input: input, hrContext: hrContext)
            let saved = try await saveToHealthKit(input: input, syncId: syncId, hrContext: hrContext)
            if let saved, !saved.skippedBecauseAlreadyExists {
                input.core.localHealth.markPlatformLinked(
                    localId: input.sessionId,
                    platformRecordId: saved.workoutUUID.uuidString
                )
            }
            return saved
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

    private struct HeartRateContext {
        var samples: [HealthKitWorkoutWriter.HeartRatePoint]
        var during: [HealthKitWorkoutWriter.HeartRatePoint]
        var avgBpm: Double?
        var maxBpm: Double?
    }

    private static func heartRateContext(for window: SessionWindow) async -> HeartRateContext {
        let samples =
            (try? await HealthKitWorkoutWriter.fetchHeartRate(
                from: window.start,
                to: window.end.addingTimeInterval(90)
            )) ?? []
        let during = samples.filter { $0.date <= window.end }
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
        input: PersistInput,
        hrContext: HeartRateContext
    ) -> String {
        input.core.localHealth.recordOmniGartWorkout(
            localId: input.sessionId,
            activityTypeName: "STRENGTH",
            startEpochMs: Int64(input.window.start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(input.window.end.timeIntervalSince1970 * 1000),
            activeEnergyKcal: KotlinDouble(value: input.energy.activeKcal),
            basalEnergyKcal: KotlinDouble(value: input.energy.basalKcal),
            distanceMetres: nil,
            averageHeartRateBpm: hrContext.avgBpm.map { KotlinDouble(value: $0) },
            maxHeartRateBpm: hrContext.maxBpm.map { KotlinDouble(value: $0) },
            sessionId: input.sessionId,
            planId: nil,
            heartRateEpochMs: KotlinPrimitiveArrays.longs(
                hrContext.during.map { Int64($0.date.timeIntervalSince1970 * 1000) }
            ),
            heartRateBpm: KotlinPrimitiveArrays.doubles(hrContext.during.map(\.bpm)),
            syncVersion: 1
        )
    }

    private static func saveToHealthKit(
        input: PersistInput,
        syncId: String,
        hrContext: HeartRateContext
    ) async throws -> HealthKitWorkoutWriter.Result? {
        try await HealthKitWorkoutWriter.save(
            options: .init(
                activityType: .traditionalStrengthTraining,
                start: input.window.start,
                end: input.window.end,
                energy: input.energy,
                syncIdentifier: syncId,
                heartRate: hrContext.during,
                strength: input.strengthMeta,
                indoor: true,
                estimatedEffortScore: HealthKitWorkoutWriter.estimateEffortScore(
                    averageBpm: hrContext.avgBpm,
                    ageYears: input.ageYears
                ),
                heartRateRecoveryBpm: HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
                    samples: hrContext.samples,
                    workoutEnd: input.window.end
                )
            )
        )
    }

    private static func dateFromEpochMillis(_ millis: Int64) -> Date {
        Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
    }
}
