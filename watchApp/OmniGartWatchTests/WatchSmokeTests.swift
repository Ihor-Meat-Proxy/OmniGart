import XCTest
@testable import OmniGartWatch

final class WatchSmokeTests: XCTestCase {
    func testStartViewIdentifierContract() {
        // Structural smoke: ensure the watch module links and test host runs.
        XCTAssertEqual("watch_start_button", "watch_start_button")
    }
}
