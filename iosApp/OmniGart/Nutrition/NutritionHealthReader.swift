import HealthKit

/// Read-only HealthKit access for weight, height, and body fat %.
enum NutritionHealthReader {
    private static let store = HKHealthStore()

    static var isAvailable: Bool { HKHealthStore.isHealthDataAvailable() }

    static func requestReadAuthorization() async throws {
        guard isAvailable else { return }
        var types = Set<HKObjectType>()
        if let mass = HKObjectType.quantityType(forIdentifier: .bodyMass) {
            types.insert(mass)
        }
        if let height = HKObjectType.quantityType(forIdentifier: .height) {
            types.insert(height)
        }
        if let fat = HKObjectType.quantityType(forIdentifier: .bodyFatPercentage) {
            types.insert(fat)
        }
        guard !types.isEmpty else { return }
        try await store.requestAuthorization(toShare: [], read: types)
    }

    static func latestWeightKg() async -> Double? {
        await latestQuantity(.bodyMass)?.doubleValue(for: .gramUnit(with: .kilo))
    }

    static func latestHeightCm() async -> Double? {
        guard let meters = await latestQuantity(.height)?.doubleValue(for: .meter()) else {
            return nil
        }
        return meters * 100.0
    }

    static func latestBodyFatPercent() async -> Double? {
        guard let fraction = await latestQuantity(.bodyFatPercentage)?
            .doubleValue(for: .percent())
        else { return nil }
        return fraction * 100.0
    }

    private static func latestQuantity(_ id: HKQuantityTypeIdentifier) async -> HKQuantity? {
        guard let type = HKQuantityType.quantityType(forIdentifier: id) else { return nil }
        return await withCheckedContinuation { continuation in
            let sort = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)
            let query = HKSampleQuery(
                sampleType: type,
                predicate: nil,
                limit: 1,
                sortDescriptors: [sort]
            ) { _, samples, _ in
                let quantity = (samples?.first as? HKQuantitySample)?.quantity
                continuation.resume(returning: quantity)
            }
            store.execute(query)
        }
    }
}
