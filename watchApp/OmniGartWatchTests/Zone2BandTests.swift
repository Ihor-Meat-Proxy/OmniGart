import XCTest
@testable import OmniGartWatch

final class Zone2BandTests: XCTestCase {
    func testKarvonenBand() {
        let band = Zone2Band.karvonen(hrMax: 180, hrRest: 60)
        XCTAssertEqual(band.lowerBpm, 132)
        XCTAssertEqual(band.upperBpm, 144)
        XCTAssertTrue(band.contains(140))
        XCTAssertFalse(band.contains(120))
    }

    @MainActor
    func testHapticsDebounceDoesNotCrash() {
        let haptics = Zone2HapticsController(settings: .init(enabled: true, debounceSeconds: 8, hysteresisBpm: 3))
        let band = Zone2Band.karvonen(hrMax: 180, hrRest: 60)
        let t0 = Date()
        haptics.process(bpm: 100, band: band, now: t0)
        haptics.process(bpm: 140, band: band, now: t0.addingTimeInterval(1))
        haptics.process(bpm: 160, band: band, now: t0.addingTimeInterval(2))
        XCTAssertEqual(haptics.state(bpm: 140, band: band), .inTarget)
    }
}
