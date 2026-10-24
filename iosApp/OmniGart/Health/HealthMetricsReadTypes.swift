import HealthKit

/// Read-only HealthKit types for the Health / Insights tabs.
/// Never used for write or delete — requestAuthorization write set stays empty here.
enum HealthMetricsReadTypes {
    static func allReadTypes() -> Set<HKObjectType> {
        var read = Set<HKObjectType>()
        let quantityIds: [HKQuantityTypeIdentifier] = [
            .activeEnergyBurned,
            .basalEnergyBurned,
            .appleExerciseTime,
            .stepCount,
            .restingHeartRate,
            .heartRateVariabilitySDNN,
            .heartRate,
            .oxygenSaturation,
            .respiratoryRate,
            .appleSleepingWristTemperature,
            .dietaryEnergyConsumed,
            .dietaryProtein,
            .dietaryCarbohydrates,
            .dietaryFatTotal,
            .bloodPressureSystolic,
            .bloodPressureDiastolic
        ]
        for id in quantityIds {
            if let type = HKQuantityType.quantityType(forIdentifier: id) {
                read.insert(type)
            }
        }
        if let sleep = HKObjectType.categoryType(forIdentifier: .sleepAnalysis) {
            read.insert(sleep)
        }
        if let mindful = HKObjectType.categoryType(forIdentifier: .mindfulSession) {
            read.insert(mindful)
        }
        read.insert(HKObjectType.workoutType())
        return read
    }

    static func requestReadAccess() async {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        do {
            try await HealthKitPlatformBridge.shared.requestAuthorization(
                readTypes: allReadTypes(),
                writeTypes: []
            )
        } catch {
            // Declined — Health tab shows empty state with retry.
        }
    }
}
