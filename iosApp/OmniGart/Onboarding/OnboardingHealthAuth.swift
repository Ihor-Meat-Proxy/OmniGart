import Foundation
import HealthKit

enum OnboardingHealthAuth {
    static func authorizationSets() -> (read: Set<HKObjectType>, write: Set<HKSampleType>) {
        var read: Set<HKObjectType> = []
        var write: Set<HKSampleType> = []
        insertOptional(
            HealthKitPlatformBridge.heartRateType(),
            intoRead: &read,
            intoWrite: &write
        )
        if let resting = HealthKitPlatformBridge.restingHeartRateType() { read.insert(resting) }
        if let hrv = HealthKitPlatformBridge.hrvType() { read.insert(hrv) }
        if let sleep = HealthKitPlatformBridge.sleepType() { read.insert(sleep) }
        insertOptional(
            HealthKitPlatformBridge.dietaryWaterType(),
            intoRead: &read,
            intoWrite: &write
        )
        insertOptional(
            HealthKitPlatformBridge.activeEnergyType(),
            intoRead: &read,
            intoWrite: &write
        )
        insertOptional(
            HealthKitPlatformBridge.basalEnergyType(),
            intoRead: &read,
            intoWrite: &write
        )
        insertOptional(
            HealthKitWorkoutWriter.heartRateRecoveryType(),
            intoRead: &read,
            intoWrite: &write
        )
        insertOptional(
            HealthKitWorkoutWriter.workoutEffortType(),
            intoRead: &read,
            intoWrite: &write
        )
        insertOptional(
            HealthKitWorkoutWriter.mindfulType(),
            intoRead: &read,
            intoWrite: &write
        )
        read.insert(HealthKitPlatformBridge.workoutType())
        write.insert(HealthKitPlatformBridge.workoutType())
        return (read, write)
    }

    private static func insertOptional(
        _ type: HKSampleType?,
        intoRead read: inout Set<HKObjectType>,
        intoWrite write: inout Set<HKSampleType>
    ) {
        guard let type else { return }
        read.insert(type)
        write.insert(type)
    }
}
