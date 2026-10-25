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

    func testSanitizeEnforcesZeroDurationAndMonotonicOrder() {
        let start = at
        let end = at.addingTimeInterval(200)
        // HealthKit throws if pause is constructed with non-zero duration, so inputs
        // must come from the factory. Sanitize still reorders and drops out-of-range.
        let resume = OmniGartWorkoutEventBuilder.makeEvent(
            type: .resume,
            at: start.addingTimeInterval(90)
        )
        let pause = OmniGartWorkoutEventBuilder.makeEvent(
            type: .pause,
            at: start.addingTimeInterval(30)
        )
        let lateSegment = OmniGartWorkoutEventBuilder.makeEvent(
            type: .segment,
            at: end.addingTimeInterval(10),
            end: end.addingTimeInterval(20),
            metadata: ["OmniGartSegmentLabel": "TooLate"]
        )
        let sanitized = OmniGartWorkoutEventBuilder.sanitize(
            [resume, pause, lateSegment],
            workoutStart: start,
            workoutEnd: end
        )
        XCTAssertEqual(sanitized.count, 2)
        XCTAssertEqual(sanitized[0].type, .pause)
        XCTAssertEqual(sanitized[0].dateInterval.duration, 0, accuracy: 0.000_1)
        XCTAssertEqual(sanitized[0].dateInterval.start, start.addingTimeInterval(30))
        XCTAssertEqual(sanitized[1].type, .resume)
        XCTAssertEqual(sanitized[1].dateInterval.duration, 0, accuracy: 0.000_1)
    }

    func testDateIntervalForcesZeroEvenWhenEndPassedForPause() {
        // Callers may pass an end for a pause span; HealthKit still requires duration 0.
        let interval = OmniGartWorkoutEventBuilder.dateInterval(
            for: .pause,
            start: at,
            end: at.addingTimeInterval(45)
        )
        XCTAssertEqual(interval.duration, 0, accuracy: 0.000_1)
        XCTAssertEqual(interval.start, at)
        XCTAssertEqual(interval.end, at)
    }

    func testSanitizeClampsSegmentEndToWorkoutEnd() {
        let start = at
        let end = at.addingTimeInterval(50)
        let segment = OmniGartWorkoutEventBuilder.makeEvent(
            type: .segment,
            at: start.addingTimeInterval(40),
            end: end.addingTimeInterval(25),
            metadata: ["OmniGartSegmentLabel": "Clamp"]
        )
        let sanitized = OmniGartWorkoutEventBuilder.sanitize(
            [segment],
            workoutStart: start,
            workoutEnd: end
        )
        XCTAssertEqual(sanitized.count, 1)
        XCTAssertEqual(sanitized[0].dateInterval.end, end)
        XCTAssertGreaterThan(sanitized[0].dateInterval.duration, 0)
    }
}
