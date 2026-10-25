import Foundation
import HealthKit
import XCTest
@testable import OmniGart

final class HealthKitWorkoutEventBuilderTests: XCTestCase {
    private let at = Date(timeIntervalSince1970: 1_700_000_000)

    func testPauseAndResumeUseZeroDuration() {
        for type: HKWorkoutEventType in [.pause, .resume, .motionPaused, .motionResumed, .marker] {
            XCTAssertTrue(
                HealthKitWorkoutEventBuilder.requiresZeroDuration(type),
                "\(type.rawValue) should require zero duration"
            )
            let interval = HealthKitWorkoutEventBuilder.dateInterval(for: type, start: at)
            XCTAssertEqual(interval.duration, 0, accuracy: 0.000_1)
            XCTAssertEqual(interval.start, at)
            let event = HealthKitWorkoutEventBuilder.makeEvent(type: type, at: at)
            XCTAssertEqual(event.dateInterval.duration, 0, accuracy: 0.000_1)
            XCTAssertEqual(event.type, type)
        }
    }

    func testSegmentAndLapUseNonZeroDuration() {
        for type: HKWorkoutEventType in [.segment, .lap] {
            XCTAssertFalse(
                HealthKitWorkoutEventBuilder.requiresZeroDuration(type),
                "\(type.rawValue) should require non-zero duration"
            )
            let point = HealthKitWorkoutEventBuilder.dateInterval(for: type, start: at)
            XCTAssertEqual(
                point.duration,
                HealthKitWorkoutEventBuilder.defaultSegmentDuration,
                accuracy: 0.000_1
            )
            let end = at.addingTimeInterval(45)
            let ranged = HealthKitWorkoutEventBuilder.dateInterval(for: type, start: at, end: end)
            XCTAssertEqual(ranged.duration, 45, accuracy: 0.000_1)
        }
    }

    func testBuildEventsPauseResumeHaveZeroDuration() {
        let pause = at
        let resume = pause.addingTimeInterval(30)
        let options = HealthKitWorkoutWriter.SaveOptions(
            activityType: .traditionalStrengthTraining,
            start: pause.addingTimeInterval(-60),
            end: resume.addingTimeInterval(60),
            energy: .init(activeKcal: 10, basalKcal: 1),
            syncIdentifier: "omnigart.workout.test-pause",
            pauseResumeDates: [(pause: pause, resume: resume)]
        )
        let events = HealthKitWorkoutWriter.buildEvents(for: options)
        XCTAssertEqual(events.count, 2)
        XCTAssertEqual(events[0].type, .pause)
        XCTAssertEqual(events[0].dateInterval.duration, 0, accuracy: 0.000_1)
        XCTAssertEqual(events[1].type, .resume)
        XCTAssertEqual(events[1].dateInterval.duration, 0, accuracy: 0.000_1)
    }

    func testBuildEventsSegmentHasNonZeroDuration() {
        let start = at.addingTimeInterval(100)
        let options = HealthKitWorkoutWriter.SaveOptions(
            activityType: .yoga,
            start: start,
            end: start.addingTimeInterval(120),
            energy: .init(activeKcal: 5, basalKcal: 1),
            syncIdentifier: "omnigart.workout.test-segment",
            segments: [
                .init(start: start, end: start.addingTimeInterval(40), label: "Pose A"),
                .init(start: start.addingTimeInterval(50), end: start.addingTimeInterval(50), label: "Point")
            ]
        )
        let events = HealthKitWorkoutWriter.buildEvents(for: options)
        XCTAssertEqual(events.count, 2)
        XCTAssertEqual(events[0].type, .segment)
        XCTAssertEqual(events[0].dateInterval.duration, 40, accuracy: 0.000_1)
        XCTAssertEqual(events[1].type, .segment)
        XCTAssertGreaterThan(events[1].dateInterval.duration, 0)
    }

    func testBuildEventsPauseWithoutResumeStillZeroDuration() {
        let pause = at.addingTimeInterval(200)
        let options = HealthKitWorkoutWriter.SaveOptions(
            activityType: .functionalStrengthTraining,
            start: pause.addingTimeInterval(-10),
            end: pause.addingTimeInterval(10),
            energy: .init(activeKcal: 1, basalKcal: 0),
            syncIdentifier: "omnigart.workout.test-pause-only",
            pauseResumeDates: [(pause: pause, resume: nil)]
        )
        let events = HealthKitWorkoutWriter.buildEvents(for: options)
        XCTAssertEqual(events.count, 1)
        XCTAssertEqual(events[0].type, .pause)
        XCTAssertEqual(events[0].dateInterval.duration, 0, accuracy: 0.000_1)
    }

    func testSanitizeDropsEventsAfterWorkoutEndAndSortsMonotonic() {
        let start = at
        let end = at.addingTimeInterval(100)
        let late = HealthKitWorkoutEventBuilder.makeEvent(
            type: .pause,
            at: end.addingTimeInterval(5)
        )
        let earlyResume = HealthKitWorkoutEventBuilder.makeEvent(
            type: .resume,
            at: start.addingTimeInterval(40)
        )
        let pause = HealthKitWorkoutEventBuilder.makeEvent(
            type: .pause,
            at: start.addingTimeInterval(20)
        )
        let sanitized = HealthKitWorkoutEventBuilder.sanitize(
            [late, earlyResume, pause],
            workoutStart: start,
            workoutEnd: end
        )
        XCTAssertEqual(sanitized.count, 2)
        XCTAssertEqual(sanitized[0].type, .pause)
        XCTAssertEqual(sanitized[0].dateInterval.start, start.addingTimeInterval(20))
        XCTAssertEqual(sanitized[1].type, .resume)
        XCTAssertEqual(sanitized[1].dateInterval.start, start.addingTimeInterval(40))
        XCTAssertEqual(sanitized[0].dateInterval.duration, 0, accuracy: 0.000_1)
        XCTAssertEqual(sanitized[1].dateInterval.duration, 0, accuracy: 0.000_1)
    }

    func testSanitizeClampsSegmentWithinWorkoutAndForcesNonZero() {
        let start = at
        let end = at.addingTimeInterval(60)
        let overshoot = HKWorkoutEvent(
            type: .segment,
            dateInterval: DateInterval(start: start.addingTimeInterval(50), end: end.addingTimeInterval(30)),
            metadata: ["OmniGartSegmentLabel": "Late"]
        )
        let sanitized = HealthKitWorkoutEventBuilder.sanitize(
            [overshoot],
            workoutStart: start,
            workoutEnd: end
        )
        XCTAssertEqual(sanitized.count, 1)
        XCTAssertEqual(sanitized[0].dateInterval.start, start.addingTimeInterval(50))
        XCTAssertEqual(sanitized[0].dateInterval.end, end)
        XCTAssertGreaterThan(sanitized[0].dateInterval.duration, 0)
    }

    func testDateIntervalForcesZeroEvenWhenEndPassedForPause() {
        let interval = HealthKitWorkoutEventBuilder.dateInterval(
            for: .pause,
            start: at,
            end: at.addingTimeInterval(45)
        )
        XCTAssertEqual(interval.duration, 0, accuracy: 0.000_1)
        XCTAssertEqual(interval.start, at)
        XCTAssertEqual(interval.end, at)
    }

    func testBuildEventsDropsOutOfRangePausePairs() {
        let start = at
        let end = at.addingTimeInterval(120)
        let options = HealthKitWorkoutWriter.SaveOptions(
            activityType: .traditionalStrengthTraining,
            start: start,
            end: end,
            energy: .init(activeKcal: 1, basalKcal: 0),
            syncIdentifier: "omnigart.workout.test-out-of-range",
            pauseResumeDates: [
                (pause: start.addingTimeInterval(-30), resume: start.addingTimeInterval(10)),
                (pause: end.addingTimeInterval(5), resume: end.addingTimeInterval(15))
            ]
        )
        let events = HealthKitWorkoutWriter.buildEvents(for: options)
        // First pair: pause clamped to start, resume kept. Second pair dropped entirely.
        XCTAssertEqual(events.count, 2)
        XCTAssertEqual(events[0].type, .pause)
        XCTAssertEqual(events[0].dateInterval.start, start)
        XCTAssertEqual(events[1].type, .resume)
        XCTAssertEqual(events[1].dateInterval.start, start.addingTimeInterval(10))
    }
}
