import Foundation
import HealthKit
import XCTest
@testable import OmniGart

final class HealthKitWorkoutEventBuilderTests: XCTestCase {
    func testBuildEventsPauseResumeHaveZeroDuration() {
        let pause = Date(timeIntervalSince1970: 1_700_000_000)
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
        let start = Date(timeIntervalSince1970: 1_700_000_100)
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
        let pause = Date(timeIntervalSince1970: 1_700_000_200)
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
}
