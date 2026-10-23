import OmniGartSensors
import XCTest

final class ReconnectBackoffTests: XCTestCase {
    func testDelayWithinCap() {
        let backoff = ReconnectBackoff(initialDelayMs: 1_000, maxDelayMs: 8_000, multiplier: 2)
        for attempt in 0..<10 {
            let delay = backoff.delayMs(forAttempt: attempt)
            XCTAssertGreaterThanOrEqual(delay, 0)
            XCTAssertLessThanOrEqual(delay, 8_000)
        }
    }

    func testExhaustedAttempts() {
        let backoff = ReconnectBackoff(maxAttempts: 2)
        XCTAssertEqual(backoff.delayMs(forAttempt: 2), -1)
    }

    func testBleUuidExpansion() {
        XCTAssertEqual(
            BleUUID.heartRateService.uuidString,
            "0000180d-0000-1000-8000-00805f9b34fb"
        )
        XCTAssertEqual(
            BleUUID.heartRateMeasurement.uuidString,
            "00002a37-0000-1000-8000-00805f9b34fb"
        )
    }

    func testWatchLimitationConstantPresent() {
        XCTAssertFalse(AppleBluetoothUsage.watchOSExternalHrLimitation.isEmpty)
    }
}
