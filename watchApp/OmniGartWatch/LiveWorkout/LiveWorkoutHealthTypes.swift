import Foundation
import HealthKit

/// HealthKit quantity types a live workout reads, writes, and collects.
struct LiveWorkoutHealthTypes {
    let heartRate: HKQuantityType
    let activeEnergy: HKQuantityType
    let basalEnergy: HKQuantityType
    let distance: HKQuantityType?

    init?(kind: OmniGartWorkoutKind) {
        guard
            let heartRate = HKQuantityType.quantityType(forIdentifier: .heartRate),
            let activeEnergy = HKQuantityType.quantityType(forIdentifier: .activeEnergyBurned),
            let basalEnergy = HKQuantityType.quantityType(forIdentifier: .basalEnergyBurned)
        else { return nil }
        self.heartRate = heartRate
        self.activeEnergy = activeEnergy
        self.basalEnergy = basalEnergy
        switch kind {
        case .zone2Run, .treadmillRun, .treadmillWalk:
            distance = HKQuantityType.quantityType(forIdentifier: .distanceWalkingRunning)
        default:
            distance = nil
        }
    }

    var collectedQuantityTypes: [HKQuantityType] {
        [heartRate, activeEnergy, basalEnergy] + (distance.map { [$0] } ?? [])
    }

    var sampleTypes: Set<HKSampleType> {
        var types: Set<HKSampleType> = [heartRate, activeEnergy, basalEnergy, HKObjectType.workoutType()]
        if let distance {
            types.insert(distance)
        }
        return types
    }
}
