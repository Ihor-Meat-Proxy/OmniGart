import XCTest

final class PhoneUITests: XCTestCase {
    func testLaunchShowsShell() throws {
        let app = XCUIApplication()
        app.launch()
        let shell = app.descendants(matching: .any)["phone_shell"]
        XCTAssertTrue(shell.waitForExistence(timeout: 10))
    }
}
