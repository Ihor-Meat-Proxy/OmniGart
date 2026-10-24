import XCTest
@testable import OmniGartWatch

/// Ensures the watch String Catalog is compiled into the app bundle.
/// If Localizable.xcstrings is missing from the target (or only copied as raw JSON),
/// these keys resolve to themselves and the start screen shows raw identifiers.
final class WatchLocalizationTests: XCTestCase {
    private let startScreenKeys = [
        "app_name",
        "watch_subtitle",
        "watch.zone2.start",
        "treadmill_run_title",
    ]

    func testStartScreenKeysResolveFromBundle() {
        let bundle = Bundle.main
        for key in startScreenKeys {
            let value = bundle.localizedString(forKey: key, value: "«missing»", table: nil)
            XCTAssertNotEqual(
                value,
                "«missing»",
                "Key \(key) missing from watch bundle Localizable table"
            )
            XCTAssertNotEqual(
                value,
                key,
                "Key \(key) did not resolve (raw key on screen) — String Catalog not compiled into OmniGartWatch"
            )
            XCTAssertFalse(
                value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                "Key \(key) resolved to an empty string"
            )
        }
    }

    func testAppNameIsBrand() {
        let value = Bundle.main.localizedString(forKey: "app_name", value: "", table: nil)
        XCTAssertEqual(value, "OmniGart")
    }
}
