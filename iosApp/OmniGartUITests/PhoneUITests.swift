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

        // PhoneRootView forces `.tabBarOnly` so iPad CI exposes a real tab bar.
        // Food is the first PhoneTab — prefer identifier, then title, then index 0.
        // Always use `.firstMatch` so duplicate a11y nodes cannot break tap().
        let tabBar = app.tabBars.firstMatch
        XCTAssertTrue(tabBar.waitForExistence(timeout: 15), "Expected tab bar after shell")
        let foodById = tabBar.buttons["tab_food"].firstMatch
        let foodByTitle = tabBar.buttons["Food"].firstMatch
        let foodByIndex = tabBar.buttons.element(boundBy: 0)
        let foodTab: XCUIElement
        if foodById.waitForExistence(timeout: 5) {
            foodTab = foodById
        } else if foodByTitle.waitForExistence(timeout: 5) {
            foodTab = foodByTitle
        } else {
            foodTab = foodByIndex
        }
        XCTAssertTrue(foodTab.waitForExistence(timeout: 10), "Expected Food tab in the tab bar")
        foodTab.tap()

        let totals = app.descendants(matching: .any)["food_totals"].firstMatch
        XCTAssertTrue(totals.waitForExistence(timeout: 10))
        let add = app.descendants(matching: .any)["food_add"].firstMatch
        XCTAssertTrue(add.waitForExistence(timeout: 5))
    }
}
