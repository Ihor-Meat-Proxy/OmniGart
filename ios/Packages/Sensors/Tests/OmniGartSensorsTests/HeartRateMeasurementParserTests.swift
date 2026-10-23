import XCTest
@testable import OmniGartSensors

final class HeartRateMeasurementParserTests: XCTestCase {
    func testUint8Hr() throws {
        let m = try HeartRateMeasurementParser.parse(Data([0x00, 72]))
        XCTAssertEqual(m.heartRateBpm, 72)
        XCTAssertFalse(m.heartRateIsUint16)
    }

    func testUint16Hr() throws {
        let m = try HeartRateMeasurementParser.parse(Data([0x01, 0x2C, 0x01]))
        XCTAssertEqual(m.heartRateBpm, 300)
        XCTAssertTrue(m.heartRateIsUint16)
    }

    func testRrIntervalTicks() throws {
        // flags RR, HR 60, RR 1024 ticks = 1.0 s
        let m = try HeartRateMeasurementParser.parse(Data([0x10, 60, 0x00, 0x04]))
        XCTAssertEqual(m.rrIntervalTicks, [1024])
        XCTAssertEqual(m.rrIntervalsSeconds[0], 1.0, accuracy: 1e-9)
    }

    func testContactAndEnergy() throws {
        let m = try HeartRateMeasurementParser.parse(Data([0x0E, 80, 0x0A, 0x00]))
        // 0x0E = contact detected + supported + energy
        XCTAssertTrue(m.sensorContactSupported)
        XCTAssertTrue(m.sensorContactDetected)
        XCTAssertEqual(m.energyExpendedKj, 10)
    }
}
