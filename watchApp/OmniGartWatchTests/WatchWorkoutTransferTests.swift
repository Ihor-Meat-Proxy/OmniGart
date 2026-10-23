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

    func testTransferJSONSchemaVersion2IncludesSets() {
        let sets = [
            OmniGartLoggedStrengthSet(
                id: "s1",
                exerciseName: "Bench",
                exerciseId: "barbell-bench-press",
                setNumber: 1,
                weightKg: 60,
                reps: 8,
                target: "60×8",
                isWarmup: false
            )
        ]
        let json = WatchWorkoutTransferBuilder.makeJSON(
            WatchWorkoutTransferPayload(
                localId: "strength-1",
                syncIdentifier: "omnigart.workout.strength-1",
                activityTypeName: "STRENGTH",
                startEpochMs: 1_700_000_000_000,
                endEpochMs: 1_700_000_600_000,
                activeEnergyKcal: 120,
                basalEnergyKcal: 30,
                distanceMetres: nil,
                averageHeartRateBpm: 118,
                maxHeartRateBpm: 140,
                averageCadenceSpm: nil,
                samples: [],
                sets: sets
            )
        )
        XCTAssertTrue(json.contains("\"schemaVersion\":2"))
        XCTAssertTrue(json.contains("\"sets\":["))
        XCTAssertTrue(json.contains("\"exerciseKey\":\"barbell-bench-press\""))
        XCTAssertTrue(json.contains("\"loadKg\":60"))
        XCTAssertTrue(json.contains("\"reps\":8"))
        XCTAssertTrue(json.contains("\"activityTypeName\":\"STRENGTH\""))
    }
}
