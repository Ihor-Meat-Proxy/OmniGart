import XCTest
@testable import OmniGart

final class CoreSmokeTests: XCTestCase {
    func testPhoneTabsExist() {
        XCTAssertEqual(PhoneTab.allCases.count, 5)
        XCTAssertEqual(PhoneTab.profile.accessibilityId, "tab_profile")
    }
}
