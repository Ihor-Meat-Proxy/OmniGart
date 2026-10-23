import XCTest
@testable import OmniGartWatch

final class Zone2SummaryTests: XCTestCase {
    func testFormatDurationMinutesSeconds() {
        XCTAssertEqual(Zone2Summary.formatDuration(0), "0:00")
        XCTAssertEqual(Zone2Summary.formatDuration(65), "1:05")
        XCTAssertEqual(Zone2Summary.formatDuration(612), "10:12")
    }

    func testFormatDurationHours() {
        XCTAssertEqual(Zone2Summary.formatDuration(3661), "1:01:01")
    }

    func testFormatDistanceMetresAndKilometres() {
        XCTAssertEqual(Zone2Summary.formatDistance(450), "450 m")
        XCTAssertEqual(Zone2Summary.formatDistance(3200), "3.20 km")
    }

    func testFormatCadence() {
        XCTAssertEqual(Zone2Summary.formatCadence(168.4), "168")
        XCTAssertEqual(Zone2Summary.formatCadence(-1), "0")
    }

    func testAverageCadenceIgnoresZeros() {
        XCTAssertEqual(Zone2Summary.averageCadence(samples: []), 0)
        XCTAssertEqual(Zone2Summary.averageCadence(samples: [0, 0]), 0)
        XCTAssertEqual(Zone2Summary.averageCadence(samples: [160, 0, 170]), 165)
    }

    func testDownsampleKeepsEndpointsAndCap() {
        let samples = (0..<100).map {
            Zone2HeartRateSample(offsetSeconds: Double($0), bpm: 120 + $0)
        }
        let down = Zone2Summary.downsample(samples, maxPoints: 10)
        XCTAssertEqual(down.count, 10)
        XCTAssertEqual(down.first?.bpm, 120)
        XCTAssertEqual(down.last?.bpm, 219)
    }

    func testDownsamplePassthroughWhenSmall() {
        let samples = [
            Zone2HeartRateSample(offsetSeconds: 0, bpm: 130),
            Zone2HeartRateSample(offsetSeconds: 10, bpm: 140)
        ]
        XCTAssertEqual(Zone2Summary.downsample(samples, maxPoints: 60), samples)
    }
}
