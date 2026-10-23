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
        let age = Int(request.core.ageYears(profile: profile))
        let demos = request.core.strengthLogging.ensureDemoExercises()
        let sets = request.core.strengthLogging.getSession(sessionId: request.sessionId)?.sets ?? []
        let energyContext = EnergyContext(
            profile: profile,
            ageYears: age,
            durationHours: window.hours,
            sets: Array(sets),
            demos: demos
        )
        let energy = resistanceEnergy(energyContext, averageHrBpm: nil)
        let strengthMeta = strengthMetadata(
            core: request.core,
            profile: profile,
            sessionId: request.sessionId,
            includeWarmups: request.includeWarmups
        )
        return await persist(
            PersistInput(
                core: request.core,
                profile: profile,
                window: window,
                energy: energy,
                sessionId: request.sessionId,
                strengthMeta: strengthMeta,
                ageYears: age,
                sets: Array(sets),
                demos: demos,
                refineEnergy: { avgBpm in
                    resistanceEnergy(energyContext, averageHrBpm: avgBpm)
                }
            )
        )
    }

    private struct PersistInput {
        var core: OmniGartCore
        var profile: UserProfile
        var window: SessionWindow
        var energy: HealthKitWorkoutWriter.EnergyInput
        var sessionId: String
        var strengthMeta: HealthKitWorkoutWriter.StrengthMetadata
        var ageYears: Int
        var sets: [WorkoutSet]
        var demos: [DemoStrengthExercise]
        var refineEnergy: (Double) -> HealthKitWorkoutWriter.EnergyInput
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

    private struct EnergyContext {
        var profile: UserProfile
        var ageYears: Int
        var durationHours: Double
        var sets: [WorkoutSet]
        var demos: [DemoStrengthExercise]
    }

    private static func resistanceEnergy(
        _ context: EnergyContext,
        averageHrBpm: Double?
    ) -> HealthKitWorkoutWriter.EnergyInput {
        let profile = context.profile
        let ageYears = context.ageYears
        let durationHours = context.durationHours
        let sets = context.sets
        let demos = context.demos
        let energySets: [ResistanceEnergySetInput] = sets.map { set in
            let muscles = demos.first { $0.exercise.id == set.exerciseId }?.exercise.primaryMuscles ?? []
            let kind = MuscleMassKindInfer.shared.infer(
                primaryMuscles: muscles,
                compound: muscles.count >= 2
            )
            let load = set.weightKg?.doubleValue ?? 0
            let reps = Int(set.reps?.intValue ?? 0)
            let e1rm =
                load > 0 && reps >= 1
                ? OneRepMax.shared.epley(weightKg: load, reps: Int32(reps))
                : nil
            return ResistanceEnergySetInput(
                loadKg: load,
                reps: Int32(reps),
                estimated1RmKg: e1rm.map { KotlinDouble(value: $0) },
                rpe: set.rpe,
                rir: set.rir,
                isWarmup: set.isWarmup,
                isDropSet: set.isDropSet,
                isFailure: set.isFailure,
                completed: set.completed,
                muscleMassKind: kind,
                secondsPerRep: nil
            )
        }
        let input = ResistanceEnergyInput(
            weightKg: profile.weightKg,
            heightCm: profile.heightCm,
            ageYears: Int32(ageYears),
            sex: profile.biologicalSex,
            sessionDurationSeconds: durationHours * 3600.0,
            sets: energySets,
            restSecondsTotal: 0,
            averageHrBpm: averageHrBpm.map { KotlinDouble(value: $0) },
            activeWorkSeconds: nil,
            bmrKcalPerDay: nil
        )
        let breakdown = ResistanceWorkoutEnergy.shared.estimate(input: input)
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
        let unit = MassUnits.shared.fromUnitSystem(units: profile.preferredUnits)
        let volumeSummary = core.strengthLogging.sessionSummary(
            sessionId: sessionId,
            includeWarmups: includeWarmups,
            bodyweightKg: KotlinDouble(value: profile.weightKg),
            unit: unit
        )
        let sets = core.strengthLogging.getSession(sessionId: sessionId)?.sets ?? []
        var totalReps = 0
        for set in sets {
            if let reps = set.reps {
                totalReps += Int(reps.intValue)
            }
        }
        return HealthKitWorkoutWriter.StrengthMetadata(
            totalSets: sets.count,
            totalReps: totalReps,
            totalWeightKg: volumeSummary.totalWeightLiftedKg
        )
    }

    private static func persist(_ input: PersistInput) async -> HealthKitWorkoutWriter.Result? {
        let heartRate = await WorkoutHeartRateSummary.load(start: input.window.start, end: input.window.end)
        var energy = input.energy
        if let avgBpm = heartRate.averageBpm {
            energy = input.refineEnergy(avgBpm)
        }
        let syncId = recordLocalWorkout(input, energy: energy, heartRate: heartRate)
        do {
            let saved = try await saveToHealthKit(input, energy: energy, heartRate: heartRate, syncId: syncId)
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
}

extension StrengthWorkoutHealthSave {
    private static func recordLocalWorkout(
        _ input: PersistInput,
        energy: HealthKitWorkoutWriter.EnergyInput,
        heartRate: WorkoutHeartRateSummary
    ) -> String {
        input.core.localHealth.recordOmniGartWorkoutWithSets(
            localId: input.sessionId,
            activityTypeName: "STRENGTH",
            startEpochMs: Int64(input.window.start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(input.window.end.timeIntervalSince1970 * 1000),
            activeEnergyKcal: KotlinDouble(value: energy.activeKcal),
            basalEnergyKcal: KotlinDouble(value: energy.basalKcal),
            distanceMetres: nil,
            averageHeartRateBpm: heartRate.averageBpm.map { KotlinDouble(value: $0) },
            maxHeartRateBpm: heartRate.maxBpm.map { KotlinDouble(value: $0) },
            sessionId: input.sessionId,
            planId: nil,
            heartRateEpochMs: KotlinPrimitiveArrays.longArray(
                heartRate.during.map { Int64($0.date.timeIntervalSince1970 * 1000) }
            ),
            heartRateBpm: KotlinPrimitiveArrays.doubleArray(heartRate.during.map(\.bpm)),
            setIds: KotlinPrimitiveArrays.stringArray(input.sets.map(\.id)),
            setExerciseKeys: KotlinPrimitiveArrays.stringArray(input.sets.map(\.exerciseId)),
            setIndexes: KotlinPrimitiveArrays.intArray(input.sets.map { $0.setIndex }),
            setReps: KotlinPrimitiveArrays.intArray(
                input.sets.map { $0.reps?.int32Value ?? Int32(-1) }
            ),
            setLoadKg: KotlinPrimitiveArrays.doubleArray(
                input.sets.map { $0.weightKg?.doubleValue ?? -1.0 }
            ),
            setIsWarmup: KotlinPrimitiveArrays.booleanArray(input.sets.map(\.isWarmup)),
            setRir: KotlinPrimitiveArrays.doubleArray(
                input.sets.map { $0.rir?.doubleValue ?? -1.0 }
            ),
            setRpe: KotlinPrimitiveArrays.doubleArray(
                input.sets.map { $0.rpe?.doubleValue ?? -1.0 }
            ),
            setIsDrop: KotlinPrimitiveArrays.booleanArray(input.sets.map(\.isDropSet)),
            setIsFailure: KotlinPrimitiveArrays.booleanArray(input.sets.map(\.isFailure)),
            syncVersion: 1
        )
    }

    private static func saveToHealthKit(
        _ input: PersistInput,
        energy: HealthKitWorkoutWriter.EnergyInput,
        heartRate: WorkoutHeartRateSummary,
        syncId: String
    ) async throws -> HealthKitWorkoutWriter.Result? {
        try await HealthKitWorkoutWriter.save(
            options: .init(
                activityType: .traditionalStrengthTraining,
                start: input.window.start,
                end: input.window.end,
                energy: energy,
                syncIdentifier: syncId,
                heartRate: heartRate.during,
                strength: input.strengthMeta,
                indoor: true,
                estimatedEffortScore: HealthKitWorkoutWriter.estimateEffortScore(
                    averageBpm: heartRate.averageBpm,
                    ageYears: input.ageYears
                ),
                heartRateRecoveryBpm: HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
                    samples: heartRate.allSamples,
                    workoutEnd: input.window.end
                )
            )
        )
    }

    private static func dateFromEpochMillis(_ millis: Int64) -> Date {
        Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
    }
}
