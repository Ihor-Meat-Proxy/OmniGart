import XCTest
@testable import OmniGart

final class HealthKitEnergyEstimateTests: XCTestCase {
    func testEffortScoreScalesWithAverageHeartRate() {
        let low = HealthKitWorkoutWriter.estimateEffortScore(averageBpm: 90, ageYears: 30)
        let high = HealthKitWorkoutWriter.estimateEffortScore(averageBpm: 160, ageYears: 30)
        XCTAssertNotNil(low)
        XCTAssertNotNil(high)
        XCTAssertLessThan(low!, high!)
        XCTAssertLessThanOrEqual(high!, 10)
    }

    func testHeartRateRecoveryRequiresAfterSamples() {
        let end = Date()
        let samples = [
            HealthKitWorkoutWriter.HeartRatePoint(date: end.addingTimeInterval(-30), bpm: 150),
            HealthKitWorkoutWriter.HeartRatePoint(date: end.addingTimeInterval(60), bpm: 120)
        ]
        let recovery = HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(
            samples: samples,
            workoutEnd: end
        )
        XCTAssertEqual(recovery ?? -1, 30, accuracy: 0.1)
    }

    func testHeartRateRecoveryNilWithoutAfterSamples() {
        let end = Date()
        let samples = [
            HealthKitWorkoutWriter.HeartRatePoint(date: end.addingTimeInterval(-30), bpm: 150)
        ]
        XCTAssertNil(
            HealthKitWorkoutWriter.estimateHeartRateRecoveryBpm(samples: samples, workoutEnd: end)
        )
    }
}
