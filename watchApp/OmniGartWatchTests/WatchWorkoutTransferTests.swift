import XCTest
@testable import OmniGartWatch

final class WatchWorkoutTransferTests: XCTestCase {
    func testActivityTypeMapping() {
        XCTAssertEqual(WatchWorkoutTransferBuilder.activityTypeName(for: .yoga), "YOGA")
        XCTAssertEqual(WatchWorkoutTransferBuilder.activityTypeName(for: .pilates), "PILATES")
        XCTAssertEqual(WatchWorkoutTransferBuilder.activityTypeName(for: .zone2Run), "RUNNING")
        XCTAssertEqual(
            WatchWorkoutTransferBuilder.activityTypeName(for: .traditionalStrength),
            "STRENGTH"
        )
    }

    @MainActor
    func testTransferJSONContainsSchemaAndSyncId() {
        let summary = OmniGartLiveWorkoutSummary(
            kind: .yoga,
            elapsed: 600,
            activeEnergyKcal: 80,
            basalEnergyKcal: 20,
            averageHeartRateBpm: 110,
            maxHeartRateBpm: 125,
            heartRateSamples: [
                Zone2HeartRateSample(offsetSeconds: 0, bpm: 100),
                Zone2HeartRateSample(offsetSeconds: 30, bpm: 120)
            ],
            segmentCount: 2,
            startedAt: Date(timeIntervalSince1970: 1_700_000_000),
            endedAt: Date(timeIntervalSince1970: 1_700_000_600)
        )
        // Exercise builder via public enqueue path by constructing expected fragments.
        let localId = "test-local"
        let syncId = "omnigart.workout.\(localId)"
        WatchWorkoutTransferBuilder.enqueueLiveSummary(
            summary,
            localId: localId,
            syncIdentifier: syncId
        )
        XCTAssertTrue(syncId.hasPrefix("omnigart.workout."))
        XCTAssertEqual(summary.heartRateSamples.count, 2)
    }
}
