import XCTest
@testable import OmniGart

final class WorkoutKitPlanBuilderTests: XCTestCase {
    func testDraftParsesZone2StylePlan() throws {
        let json: [String: Any] = [
            "id": "zone2-1",
            "activityType": "RUNNING",
            "displayNameKey": "running.session.zone2",
            "blocks": [
                [
                    "kind": "WARM_UP",
                    "displayNameKey": "warm",
                    "goal": ["kind": "TIME_SECONDS", "value": 300.0],
                    "repeatCount": 1
                ],
                [
                    "kind": "WORK",
                    "displayNameKey": "z2",
                    "goal": ["kind": "TIME_SECONDS", "value": 1800.0],
                    "alerts": [
                        ["kind": "HEART_RATE_ZONE", "lower": 120.0, "upper": 145.0]
                    ],
                    "repeatCount": 1
                ],
                [
                    "kind": "COOL_DOWN",
                    "displayNameKey": "cool",
                    "goal": ["kind": "TIME_SECONDS", "value": 300.0],
                    "repeatCount": 1
                ]
            ]
        ]
        let draft = try WorkoutKitPlanBuilder.draft(from: json)
        XCTAssertEqual(draft.id, "zone2-1")
        XCTAssertEqual(draft.blocks.count, 3)
        XCTAssertEqual(draft.blocks[1].alertKind, "HEART_RATE_ZONE")
    }

    /// Parses the dictionary shape produced by `Zone2WorkoutKitScheduler` / shared factory.
    func testDraftParsesZone2WorkoutKitSchedulerFactoryShape() throws {
        let start = Date(timeIntervalSince1970: 1_700_000_000)
        let json = Zone2WorkoutKitScheduler.planJSON(
            id: "zone2-factory-1",
            hrMax: 180,
            hrRest: 60,
            workSeconds: 1800,
            scheduledStart: start
        )
        let draft = try WorkoutKitPlanBuilder.draft(from: json)
        XCTAssertEqual(draft.id, "zone2-factory-1")
        XCTAssertEqual(draft.blocks.count, 3)
        XCTAssertEqual(draft.blocks[0].kind, "WARM_UP")
        XCTAssertEqual(draft.blocks[1].kind, "WORK")
        XCTAssertEqual(draft.blocks[2].kind, "COOL_DOWN")
        XCTAssertEqual(draft.blocks[1].goalKind, "TIME_SECONDS")
        XCTAssertEqual(draft.blocks[1].goalValue, 1800.0)
        XCTAssertEqual(draft.blocks[1].alertKind, "HEART_RATE_ZONE")
        // Karvonen 60–70% HRR: rest 60, max 180 → 132–144 (matches Zone2.karvonen)
        XCTAssertEqual(draft.blocks[1].alertLower, 132.0)
        XCTAssertEqual(draft.blocks[1].alertUpper, 144.0)
        XCTAssertNotNil(draft.scheduledStart)
    }

    func testZone2BandMatchesKarvonenDefaults() {
        let band = Zone2WorkoutKitScheduler.zone2Band(hrMax: 180, hrRest: 60)
        XCTAssertEqual(band.lowerBpm, 132)
        XCTAssertEqual(band.upperBpm, 144)
    }

    func testRejectsStrength() {
        let json: [String: Any] = [
            "id": "s1",
            "activityType": "STRENGTH",
            "displayNameKey": "strength",
            "blocks": [["kind": "WORK", "displayNameKey": "sets"]]
        ]
        XCTAssertThrowsError(try WorkoutKitPlanBuilder.draft(from: json)) { error in
            XCTAssertTrue(error is WorkoutKitPlanBuilder.BuilderError)
        }
    }
}
