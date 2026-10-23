import XCTest

final class PhoneUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testLaunchShowsShell() throws {
        let app = XCUIApplication()
        if app.state != .notRunning {
            app.terminate()
        }
        app.launchArguments += ["-ui-testing", "-AppleLanguages", "(en-GB)"]
        app.launch()

        let shell = app.descendants(matching: .any)["phone_shell"]
        XCTAssertTrue(
            shell.waitForExistence(timeout: 30),
            "Expected phone_shell accessibility identifier after launch"
        )
    }

    func testOnboardingWelcomeVisible() throws {
        let app = XCUIApplication()
        if app.state != .notRunning {
            app.terminate()
        }
        app.launchArguments += [
            "-ui-testing",
            "-ui-testing-onboarding",
            "-AppleLanguages",
            "(en-GB)"
        ]
        app.launch()

        let flow = app.descendants(matching: .any)["onboarding_flow"]
        XCTAssertTrue(
            flow.waitForExistence(timeout: 30),
            "Expected onboarding_flow on first-launch / forced onboarding"
        )
        let welcome = app.descendants(matching: .any)["onboarding_step_welcome"]
        XCTAssertTrue(welcome.waitForExistence(timeout: 10))
    }

    func testOnboardingSkipReachesShell() throws {
        let app = XCUIApplication()
        if app.state != .notRunning {
            app.terminate()
        }
        app.launchArguments += [
            "-ui-testing",
            "-ui-testing-onboarding",
            "-AppleLanguages",
            "(en-GB)"
        ]
        app.launch()

        let skip = app.descendants(matching: .any)["onboarding_skip"]
        XCTAssertTrue(skip.waitForExistence(timeout: 30))
        skip.tap()

        let shell = app.descendants(matching: .any)["phone_shell"]
        XCTAssertTrue(
            shell.waitForExistence(timeout: 30),
            "Skip should persist a local profile and show the phone shell"
        )
    }
}
