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
