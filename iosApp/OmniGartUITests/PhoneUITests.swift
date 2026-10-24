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

    func testFoodTabShowsTotals() throws {
        let app = XCUIApplication()
        if app.state != .notRunning {
            app.terminate()
        }
        app.launchArguments += ["-ui-testing", "-AppleLanguages", "(en-GB)"]
        app.launch()

        let shell = app.descendants(matching: .any)["phone_shell"]
        XCTAssertTrue(shell.waitForExistence(timeout: 30))

        // Prefer tab-bar query: content-root identifiers are only in the tree for the
        // selected tab. Identifier is set on `Tab` in PhoneRootView; title is the en-GB
        // fallback. On iPad the floating tab bar sometimes surfaces as a plain button.
        let foodTab = firstExisting(
            in: app,
            timeout: 30,
            candidates: [
                app.tabBars.buttons["tab_food"],
                app.tabBars.buttons["Food"],
                app.buttons["tab_food"],
                app.buttons["Food"],
                app.descendants(matching: .any)["tab_food"],
            ]
        )
        XCTAssertNotNil(foodTab, "Expected Food tab in the tab bar")
        foodTab?.tap()

        let totals = app.descendants(matching: .any)["food_totals"]
        XCTAssertTrue(totals.waitForExistence(timeout: 10))
        let add = app.descendants(matching: .any)["food_add"]
        XCTAssertTrue(add.waitForExistence(timeout: 5))
    }

    /// First candidate that appears within `timeout` (polled so one slow path does not
    /// burn the whole budget before trying the next).
    private func firstExisting(
        in _: XCUIApplication,
        timeout: TimeInterval,
        candidates: [XCUIElement]
    ) -> XCUIElement? {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            for candidate in candidates where candidate.exists {
                return candidate
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.2))
        }
        for candidate in candidates where candidate.waitForExistence(timeout: 0.1) {
            return candidate
        }
        return nil
    }
}
