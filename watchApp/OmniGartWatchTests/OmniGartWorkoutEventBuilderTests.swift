import Foundation
import HealthKit
import XCTest
@testable import OmniGartWatch

final class OmniGartWorkoutEventBuilderTests: XCTestCase {
    private let at = Date(timeIntervalSince1970: 1_700_000_000)

    func testPauseAndResumeUseZeroDuration() {
        for type: HKWorkoutEventType in [.pause, .resume, .motionPaused, .motionResumed, .marker] {
            XCTAssertTrue(
                OmniGartWorkoutEventBuilder.requiresZeroDuration(type),
                "\(type.rawValue) should require zero duration"
            )
            let interval = OmniGartWorkoutEventBuilder.dateInterval(for: type, start: at)
            XCTAssertEqual(interval.duration, 0, accuracy: 0.000_1)
            XCTAssertEqual(interval.start, at)
            let event = OmniGartWorkoutEventBuilder.makeEvent(type: type, at: at)
            XCTAssertEqual(event.dateInterval.duration, 0, accuracy: 0.000_1)
            XCTAssertEqual(event.type, type)
        }
    }

    func testSegmentAndLapUseNonZeroDuration() {
        for type: HKWorkoutEventType in [.segment, .lap] {
            XCTAssertFalse(
                OmniGartWorkoutEventBuilder.requiresZeroDuration(type),
                "\(type.rawValue) should require non-zero duration"
            )
            let point = OmniGartWorkoutEventBuilder.dateInterval(for: type, start: at)
            XCTAssertEqual(
                point.duration,
                OmniGartWorkoutEventBuilder.defaultSegmentDuration,
                accuracy: 0.000_1
            )
            let end = at.addingTimeInterval(45)
            let ranged = OmniGartWorkoutEventBuilder.dateInterval(for: type, start: at, end: end)
            XCTAssertEqual(ranged.duration, 45, accuracy: 0.000_1)
            let event = OmniGartWorkoutEventBuilder.makeEvent(
                type: type,
                at: at,
                metadata: ["OmniGartSegmentLabel": "Squat #1"]
            )
            XCTAssertGreaterThan(event.dateInterval.duration, 0)
            XCTAssertEqual(event.type, type)
        }
    }

    func testSegmentIgnoresEndNotAfterStart() {
        let same = OmniGartWorkoutEventBuilder.dateInterval(for: .segment, start: at, end: at)
        XCTAssertEqual(
            same.duration,
            OmniGartWorkoutEventBuilder.defaultSegmentDuration,
            accuracy: 0.000_1
        )
        let before = OmniGartWorkoutEventBuilder.dateInterval(
            for: .segment,
            start: at,
            end: at.addingTimeInterval(-5)
        )
        XCTAssertEqual(
            before.duration,
            OmniGartWorkoutEventBuilder.defaultSegmentDuration,
            accuracy: 0.000_1
        )
    }
}
