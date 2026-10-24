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

        // Identifier is set once on `Tab` in PhoneRootView (`.tabBarOnly` keeps a bottom
        // bar on iPad CI). Prefer tabBars, then any button — always resolve via firstMatch
        // so a stray duplicate never fails tap with "Multiple matching elements".
        let foodById = app.tabBars.buttons["tab_food"]
        let foodByTitle = app.tabBars.buttons["Food"]
        let foodByButton = app.buttons.matching(identifier: "tab_food").firstMatch
        let foodTab: XCUIElement
        if foodById.waitForExistence(timeout: 10) {
            foodTab = foodById
        } else if foodByTitle.waitForExistence(timeout: 5) {
            foodTab = foodByTitle
        } else {
            foodTab = foodByButton
        }
        XCTAssertTrue(foodTab.waitForExistence(timeout: 10), "Expected Food tab control")
        foodTab.tap()

        let totals = app.descendants(matching: .any)["food_totals"]
        XCTAssertTrue(totals.waitForExistence(timeout: 10))
        let add = app.descendants(matching: .any)["food_add"]
        if !add.waitForExistence(timeout: 5) {
            app.swipeUp()
        }
        XCTAssertTrue(
            add.waitForExistence(timeout: 10),
            "Expected food_add on Food tab (scroll if nutrition card pushed it off-screen)"
        )
    }
}
