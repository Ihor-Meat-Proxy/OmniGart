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
        var energy = resistanceEnergy(
            profile: profile,
            ageYears: age,
            durationHours: window.hours,
            sets: Array(sets),
            demos: demos,
            averageHrBpm: nil
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
                profile: profile,
                window: window,
                energy: energy,
                sessionId: request.sessionId,
                strengthMeta: strengthMeta,
                ageYears: age,
                sets: Array(sets),
                demos: demos,
                refineEnergy: { avgBpm in
                    resistanceEnergy(
                        profile: profile,
                        ageYears: age,
                        durationHours: window.hours,
                        sets: Array(sets),
                        demos: demos,
                        averageHrBpm: avgBpm
                    )
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

    private static func resistanceEnergy(
        profile: UserProfile,
        ageYears: Int,
        durationHours: Double,
        sets: [WorkoutSet],
        demos: [DemoStrengthExercise],
        averageHrBpm: Double?
    ) -> HealthKitWorkoutWriter.EnergyInput {
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
        do {
            let hrSamples =
                (try? await HealthKitWorkoutWriter.fetchHeartRate(
                    from: input.window.start,
                    to: input.window.end.addingTimeInterval(90)
                )) ?? []
            let during = hrSamples.filter { $0.date <= input.window.end }
            let avg = during.map(\.bpm).filter { $0 > 0 }
            let avgBpm = avg.isEmpty ? nil : avg.reduce(0, +) / Double(avg.count)
            let maxBpm = avg.max()
            var energy = input.energy
            if let avgBpm {
                energy = input.refineEnergy(avgBpm)
            }

            let setIds = input.sets.map(\.id)
            let setKeys = input.sets.map(\.exerciseId)
            let setIndexes = input.sets.map { $0.setIndex }
            let setRepsArr = input.sets.map { $0.reps?.int32Value ?? Int32(-1) }
            let setLoad = input.sets.map { $0.weightKg?.doubleValue ?? -1.0 }
            let setWarm = input.sets.map(\.isWarmup)
            let setRir = input.sets.map { $0.rir?.doubleValue ?? -1.0 }
            let setRpe = input.sets.map { $0.rpe?.doubleValue ?? -1.0 }
            let setDrop = input.sets.map(\.isDropSet)
            let setFail = input.sets.map(\.isFailure)

            let syncId = input.core.localHealth.recordOmniGartWorkoutWithSets(
                localId: input.sessionId,
                activityTypeName: "STRENGTH",
                startEpochMs: Int64(input.window.start.timeIntervalSince1970 * 1000),
                endEpochMs: Int64(input.window.end.timeIntervalSince1970 * 1000),
                activeEnergyKcal: KotlinDouble(value: energy.activeKcal),
                basalEnergyKcal: KotlinDouble(value: energy.basalKcal),
                distanceMetres: nil,
                averageHeartRateBpm: avgBpm.map { KotlinDouble(value: $0) },
                maxHeartRateBpm: maxBpm.map { KotlinDouble(value: $0) },
                sessionId: input.sessionId,
                planId: nil,
                heartRateEpochMs: during.map { Int64($0.date.timeIntervalSince1970 * 1000) },
                heartRateBpm: during.map(\.bpm),
                setIds: setIds,
                setExerciseKeys: setKeys,
                setIndexes: setIndexes,
                setReps: setRepsArr,
                setLoadKg: setLoad,
                setIsWarmup: setWarm,
                setRir: setRir,
                setRpe: setRpe,
                setIsDrop: setDrop,
                setIsFailure: setFail,
                syncVersion: 1
            )
            let saved = try await HealthKitWorkoutWriter.save(
                options: .init(
                    activityType: .traditionalStrengthTraining,
                    start: input.window.start,
                    end: input.window.end,
                    energy: energy,
                    syncIdentifier: syncId,
                    heartRate: during,
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

    private static func dateFromEpochMillis(_ millis: Int64) -> Date {
        Date(timeIntervalSince1970: TimeInterval(millis) / 1000.0)
    }
}
