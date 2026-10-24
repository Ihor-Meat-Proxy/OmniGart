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

        // Under -ui-testing the shell starts on Food (see PhoneRootView.initialTab).
        // Still select Food explicitly when the tab control is visible; with 7 tabs iOS
        // may scroll the bar or park overflow under More.
        selectFoodTab(in: app)

        let totals = app.descendants(matching: .any)["food_totals"]
        XCTAssertTrue(totals.waitForExistence(timeout: 10), "Expected food_totals on Food tab")
        let add = app.descendants(matching: .any)["food_add"]
        XCTAssertTrue(add.waitForExistence(timeout: 5), "Expected food_add on Food tab")
    }

    /// Resolves Food across tab-bar buttons, any-descendant ids, More overflow, and a swipe.
    private func selectFoodTab(in app: XCUIApplication) {
        let byId = app.descendants(matching: .any)["tab_food"]
        if byId.waitForExistence(timeout: 5), byId.isHittable {
            byId.tap()
            return
        }

        let tabBar = app.tabBars.firstMatch
        if tabBar.exists {
            let foodButton = tabBar.buttons["tab_food"].exists
                ? tabBar.buttons["tab_food"]
                : tabBar.buttons["Food"]
            if foodButton.exists, foodButton.isHittable {
                foodButton.tap()
                return
            }

            // Scrollable tab bar: nudge left so the first tabs enter the AX tree.
            tabBar.swipeRight()
            let afterSwipe = app.descendants(matching: .any)["tab_food"]
            if afterSwipe.waitForExistence(timeout: 3), afterSwipe.isHittable {
                afterSwipe.tap()
                return
            }

            let more = tabBar.buttons["More"]
            if more.waitForExistence(timeout: 2), more.isHittable {
                more.tap()
                let foodInMore = app.descendants(matching: .any)["tab_food"].exists
                    ? app.descendants(matching: .any)["tab_food"]
                    : app.staticTexts["Food"]
                if foodInMore.waitForExistence(timeout: 5) {
                    foodInMore.tap()
                    return
                }
            }
        }

        // Launch already selects Food under -ui-testing; content asserts cover that path.
        XCTAssertTrue(
            app.descendants(matching: .any)["food_totals"].waitForExistence(timeout: 5)
                || byId.exists,
            "Expected Food tab control or Food content after launch"
        )
    }
}
