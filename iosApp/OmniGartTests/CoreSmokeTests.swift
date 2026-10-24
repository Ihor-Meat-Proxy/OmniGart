import XCTest
@testable import OmniGart

final class CoreSmokeTests: XCTestCase {
    func testPhoneTabsExist() {
        XCTAssertEqual(PhoneTab.allCases.count, 7)
        XCTAssertEqual(PhoneTab.profile.accessibilityId, "tab_profile")
        XCTAssertEqual(PhoneTab.insights.accessibilityId, "tab_insights")
        XCTAssertEqual(PhoneTab.health.accessibilityId, "tab_health")
    }
}
