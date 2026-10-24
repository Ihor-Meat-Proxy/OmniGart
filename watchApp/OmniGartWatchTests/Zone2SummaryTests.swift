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

    func testFormatEnergyKcal() {
        XCTAssertEqual(Zone2Summary.formatEnergyKcal(220.4), "220")
        XCTAssertEqual(Zone2Summary.formatEnergyKcal(-5), "0")
    }

    func testTotalEnergyKcal() {
        let summary = Zone2Summary(
            timeInZone: 60,
            distanceMetres: 100,
            averageCadenceSpm: 160,
            elapsed: 120,
            heartRateSamples: [],
            activeEnergyKcal: 200,
            basalEnergyKcal: 40,
            averageHeartRateBpm: 130,
            maxHeartRateBpm: 145
        )
        XCTAssertEqual(summary.totalEnergyKcal, 240, accuracy: 0.01)
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
